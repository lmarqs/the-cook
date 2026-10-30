"""The owner's labels for judged expectations, and the judge's agreement with them."""

import csv
import json
from pathlib import Path

from tool import stats
from tool.cases import EVALS

COLUMNS = ["run", "grader", "expectation", "material", "judge", "evidence", "owner"]


def write(batch_dir: Path) -> Path:
    path = batch_dir / "labels.csv"
    kept = {}
    if path.exists():
        with path.open() as existing:
            kept = {(r["run"], r["grader"]): r["owner"] for r in csv.DictReader(existing)}
    with path.open("w", newline="") as out:
        writer = csv.DictWriter(out, COLUMNS)
        writer.writeheader()
        for directory in sorted((batch_dir / "runs").iterdir()):
            grading = directory / "grading.json"
            if not grading.exists():
                continue
            for e in json.loads(grading.read_text())["expectations"]:
                if e["type"] != "judge":
                    continue
                writer.writerow({
                    "run": directory.name, "grader": e["name"], "expectation": e["text"],
                    "material": str((directory / "repo").relative_to(EVALS)), "judge": "pass" if e["passed"] else "fail",
                    "evidence": e["evidence"], "owner": kept.get((directory.name, e["name"]), ""),
                })
    return path


def agreement(path: Path) -> tuple[float | None, int]:
    with path.open() as labels:
        rows = [r for r in csv.DictReader(labels) if r["owner"].strip().lower() in ("pass", "fail")]
    judge = [r["judge"] == "pass" for r in rows]
    owner = [r["owner"].strip().lower() == "pass" for r in rows]
    return stats.kappa(judge, owner), len(rows)
