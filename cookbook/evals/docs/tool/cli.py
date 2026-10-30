"""The `evals` command line."""

import argparse
import random
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

from tool import fixtures, grading, harness, label, report
from tool.cases import EVALS, RESULTS, load_study, studies


def batch_dirs(study: str, batches: list[str]) -> list[Path]:
    return [RESULTS / study / b for b in batches]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="evals", description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)

    fixture = commands.add_parser("fixture", help="build one run's repository, to inspect an arm")
    fixture.add_argument("study", choices=studies())
    fixture.add_argument("case")
    fixture.add_argument("arm")
    fixture.add_argument("dest", type=Path)

    run = commands.add_parser("run", help="run a study's jobs in the sandbox")
    run.add_argument("study", choices=studies())
    run.add_argument("--batch", help="the batch's name; a timestamp by default")
    run.add_argument("--case", action="append")
    run.add_argument("--arm", action="append")
    run.add_argument("--harness", action="append", choices=["claude", "codex"])
    run.add_argument("--runs", type=int)
    run.add_argument("-j", "--parallel", type=int, default=2)
    run.add_argument("--max-cost-usd", type=float)
    run.add_argument("--seed", type=int)

    grade = commands.add_parser("grade", help="grade a batch's runs")
    grade.add_argument("study", choices=studies())
    grade.add_argument("batch")
    grade.add_argument("-j", "--parallel", type=int, default=2)
    grade.add_argument("--regrade", action="store_true", help="grade every run again")
    grade.add_argument("--grader", action="append", help="regrade only this grader of the graded runs")

    labels = commands.add_parser("label", help="write labels.csv for the owner to label judged expectations")
    labels.add_argument("study", choices=studies())
    labels.add_argument("batch")

    kappa = commands.add_parser("kappa", help="the judge's agreement with the owner's labels")
    kappa.add_argument("study", choices=studies())
    kappa.add_argument("batch")

    reporting = commands.add_parser("report", help="write report.md from graded batches")
    reporting.add_argument("study", choices=studies())
    reporting.add_argument("batch", nargs="+")

    commands.add_parser("selftest", help="run the tool's tests, the graders' planted defects and the isolation check")

    args = parser.parse_args(argv)
    if args.command == "fixture":
        study = load_study(args.study)
        print(fixtures.build(study.case(args.case), args.arm, args.dest.resolve()))
    elif args.command == "run":
        study = load_study(args.study)
        seed = args.seed if args.seed is not None else random.SystemRandom().randrange(2**31)
        batch = args.batch or datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        selected = harness.jobs(study, args.case, args.arm, args.harness, args.runs or study.runs, seed)
        print(f"batch {batch}: {len(selected)} jobs, seed {seed}", flush=True)
        print(harness.run(study, batch, selected, args.parallel, args.max_cost_usd, seed))
    elif args.command == "grade":
        study = load_study(args.study)
        only = set(args.grader) if args.grader else None
        grading.grade_batch(study, RESULTS / args.study / args.batch, args.parallel, args.regrade, only)
    elif args.command == "label":
        print(label.write(RESULTS / args.study / args.batch))
    elif args.command == "kappa":
        k, n = label.agreement(RESULTS / args.study / args.batch / "labels.csv")
        print(f"kappa {k} on {n} labels")
    elif args.command == "report":
        dirs = batch_dirs(args.study, args.batch)
        print(report.write(load_study(args.study), dirs, dirs[-1] / "labels.csv"))
    elif args.command == "selftest":
        return subprocess.call([sys.executable, "-m", "pytest", "-q", str(EVALS / "tool" / "tests")], cwd=EVALS)
    return 0
