"""A run the provider refused is run again; grading can redo some graders; the report weighs effort when it is primary."""

import hashlib
import json
from pathlib import Path

from tool import grading, harness, report
from tool.cases import Case, Study, load_study


def result(error=None, files=()):
    return {"error": error, "files_changed": list(files)}


def test_a_run_the_provider_refused_is_run_again_until_it_is_not():
    outcomes = iter([result("Selected model is at capacity. Please try a different model."),
                     result('{"type": "overloaded_error"}'), result(None, ["docs/a.md"])])

    final = harness.with_retries(lambda: next(outcomes))

    assert final["files_changed"] == ["docs/a.md"]
    assert final["attempts"] == 3


def test_a_run_is_kept_when_it_changed_files_or_failed_for_another_reason():
    after_work = harness.with_retries(lambda: result("Selected model is at capacity.", ["docs/a.md"]))
    other = harness.with_retries(lambda: result("error_max_turns"))

    assert after_work["attempts"] == 1
    assert other["attempts"] == 1


def test_the_provider_refusing_every_attempt_ends_after_three():
    final = harness.with_retries(lambda: result("429 Too Many Requests"))

    assert final["attempts"] == 3
    assert final["error"] == "429 Too Many Requests"


def write_run(directory: Path, case: str) -> None:
    (directory / "repo").mkdir(parents=True)
    record = {"id": directory.name, "case": case, "arm": "skill", "harness": "claude", "run": 1,
              "files_changed": [], "steps": [], "last_message": "", "error": None}
    (directory / "run.json").write_text(json.dumps(record))
    (directory / "changes.patch").write_text("")


def write_graded_run(directory: Path, study: Study) -> list[str]:
    write_run(directory, "change-decision")
    names = [g.name for g in study.case("change-decision").graders]
    old = {"expectations": [{"name": n, "type": "x", "text": "", "passed": True, "verdict_missing": False,
                             "evidence": f"old {n}", "judged_by": None, "grader_sha": "old"} for n in names]}
    (directory / "grading.json").write_text(json.dumps(old))
    return names


def test_grading_named_graders_keeps_every_other_verdict(tmp_path, monkeypatch):
    study = load_study("e1")
    directory = tmp_path / "change-decision--skill--claude--r1"
    names = write_graded_run(directory, study)
    monkeypatch.setattr(grading.graders, "grade", lambda grader, run: (False, f"new {grader.name}"))

    graded = grading.grade_run(study, directory, only={"02-structure"})

    evidence = {e["name"]: e["evidence"] for e in graded["expectations"]}
    assert evidence["02-structure"] == "new 02-structure"
    assert all(evidence[n] == f"old {n}" for n in names if n != "02-structure")
    assert [e["name"] for e in graded["expectations"]] == names
    assert graded["outcome"]["pass"] is False


def test_each_verdict_records_the_fingerprint_of_its_grader_file(tmp_path, monkeypatch):
    study = load_study("e1")
    directory = tmp_path / "change-decision--skill--claude--r1"
    write_graded_run(directory, study)
    monkeypatch.setattr(grading.graders, "grade", lambda grader, run: (True, "ok"))

    graded = grading.grade_run(study, directory, only={"02-structure"})

    path = study.case("change-decision").dir / "graders" / "02-structure.md"
    sha = next(e["grader_sha"] for e in graded["expectations"] if e["name"] == "02-structure")
    assert sha == hashlib.sha256(path.read_bytes()).hexdigest()


def test_effort_counts_the_steps_before_the_owning_page_and_censors_a_page_never_read():
    steps = [{"kind": "search", "path": "docs"}, {"kind": "read", "path": "docs/a.md"},
             {"kind": "read", "path": "docs/own.md"}]

    assert report.effort({"steps": steps}, {"owning_page": "docs/own.md"}) == (2, False)
    assert report.effort({"steps": steps[:2]}, {"owning_page": "docs/own.md"}) == (2, True)
    assert report.effort({"steps": steps}, {}) == (None, False)


def effort_batch(root: Path, control_steps: int) -> Path:
    batch = root / "b1"
    for case in ("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8"):
        for arm, before in (("ref", 1), ("ctl", control_steps)):
            directory = batch / "runs" / f"{case}--{arm}--claude--r1"
            directory.mkdir(parents=True)
            steps = [{"kind": "search", "path": "docs"}] * before + [{"kind": "read", "path": "docs/own.md"}]
            (directory / "run.json").write_text(json.dumps({
                "id": directory.name, "case": case, "arm": arm, "harness": "claude", "run": 1, "steps": steps,
                "tokens": {"input": 100 + before, "output": 10}, "duration_s": 1.0, "cost_usd": 0.1, "error": None}))
            (directory / "grading.json").write_text(json.dumps({
                "expectations": [{"name": "01", "passed": True, "verdict_missing": False}],
                "summary": {"passed": 1, "total": 1}, "outcome": {"pass": True, "share": 1.0}}))
    return batch


def effort_study() -> Study:
    cases = [Case("s", f"c{n}", Path("."), "", {}, {"owning_page": "docs/own.md"}) for n in range(1, 9)]
    return Study("s", ["ref", "ctl"], {"claude": {}}, 1, ["01"], {"success": 0.1, "tokens": 0.15}, cases,
                 primary="effort")


def test_with_effort_primary_the_control_separates_when_it_takes_more_steps(tmp_path):
    text = report.write(effort_study(), [effort_batch(tmp_path, control_steps=6)]).read_text()

    assert "does not separate" not in text
    assert "| claude: ctl vs ref | 8 |" in text


def test_with_effort_primary_a_control_that_takes_no_more_steps_is_flagged(tmp_path):
    text = report.write(effort_study(), [effort_batch(tmp_path, control_steps=1)]).read_text()

    assert "The control `ctl` does not separate from `ref` on claude" in text


def test_labels_point_at_each_run_relative_to_the_tool(tmp_path, monkeypatch):
    from tool import label

    monkeypatch.setattr(label, "EVALS", tmp_path)
    directory = tmp_path / "results" / "e3" / "b1" / "runs" / "c1--leads--codex--r1"
    directory.mkdir(parents=True)
    (directory / "grading.json").write_text(json.dumps({"expectations": [
        {"name": "03", "type": "judge", "text": "t", "passed": True, "evidence": "e"}]}))

    text = label.write(tmp_path / "results" / "e3" / "b1").read_text()

    assert "results/e3/b1/runs/c1--leads--codex--r1/repo" in text
    assert str(tmp_path) not in text
