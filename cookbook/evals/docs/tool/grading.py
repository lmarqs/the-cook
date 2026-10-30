"""Grades a batch's runs into grading.json, in skill-creator's shape with the study's outcome added."""

import json
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from tool import graders, judge, traces
from tool.cases import Study
from tool.graders import Run


def load_run(study: Study, directory: Path) -> Run:
    """The run, its record parsed again from its event stream when that is kept, so parser fixes reach old runs."""
    record = json.loads((directory / "run.json").read_text())
    events = directory / "events.jsonl"
    if events.exists():
        lines = [line for line in events.read_text().splitlines() if line.startswith("{")]
        record.update(getattr(traces, record["harness"])(lines))
        (directory / "run.json").write_text(json.dumps(record, indent=2) + "\n")
    return Run(directory / "repo", record, study.case(record["case"]), (directory / "changes.patch").read_text())


def grade_run(study: Study, directory: Path) -> dict:
    run = load_run(study, directory)
    expectations = []
    for grader in run.case.graders:
        judged_by = None
        if run.record.get("error") and not run.record.get("files_changed"):
            passed, evidence = False, f"the run failed: {run.record['error']}"
        elif grader.type == "judge":
            passed, evidence, judged_by = judge.ask(run, grader.text, grader.options, study.harnesses)
        else:
            passed, evidence = graders.grade(grader, run)
        expectations.append({
            "name": grader.name, "type": grader.type, "text": grader.text,
            "passed": bool(passed), "verdict_missing": passed is None, "evidence": evidence, "judged_by": judged_by,
        })
    passed_count = sum(e["passed"] for e in expectations)
    outcome = [e for e in expectations if e["name"] in study.outcome]
    grading = {
        "expectations": expectations,
        "summary": {"passed": passed_count, "failed": len(expectations) - passed_count, "total": len(expectations),
                    "pass_rate": passed_count / len(expectations) if expectations else 0.0},
        "outcome": {"graders": study.outcome, "pass": all(e["passed"] for e in outcome) if outcome else False,
                    "share": sum(e["passed"] for e in outcome) / len(outcome) if outcome else 0.0},
    }
    (directory / "grading.json").write_text(json.dumps(grading, indent=2) + "\n")
    return grading


def grade_batch(study: Study, batch_dir: Path, parallel: int, regrade: bool) -> None:
    directories = [d for d in sorted((batch_dir / "runs").iterdir()) if (d / "run.json").exists()]
    if not regrade:
        directories = [d for d in directories if not (d / "grading.json").exists()]

    def one(directory: Path) -> None:
        grading = grade_run(study, directory)
        print(f"graded {directory.name}: outcome {'pass' if grading['outcome']['pass'] else 'fail'}, "
              f"{grading['summary']['passed']}/{grading['summary']['total']}", flush=True)

    with ThreadPoolExecutor(max_workers=parallel) as pool:
        list(pool.map(one, directories))
