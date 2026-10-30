"""Studies and their cases, read from `studies/<study>/`."""

import hashlib
from dataclasses import dataclass, field
from pathlib import Path

import yaml

EVALS = Path(__file__).resolve().parent.parent
REPO = EVALS.parents[2]
STUDIES = EVALS / "studies"
FIXTURE = EVALS / "fixture" / "larder"
RESULTS = EVALS / "results"
PRIMARY = ("pass", "effort")


def front_matter(text: str) -> tuple[dict, str]:
    """Splits a Markdown file into its YAML header and its body."""
    if not text.startswith("---\n"):
        return {}, text.strip()
    header, _, body = text[4:].partition("\n---\n")
    return yaml.safe_load(header) or {}, body.strip()


@dataclass
class Grader:
    name: str
    type: str
    text: str
    options: dict
    sha: str = ""


@dataclass
class Case:
    study: str
    id: str
    dir: Path
    prompt: str
    settings: dict
    data: dict
    graders: list[Grader] = field(default_factory=list)

    @property
    def max_turns(self) -> int:
        return int(self.settings.get("max_turns", 40))

    @property
    def timeout(self) -> int:
        return int(self.settings.get("timeout_seconds", 900))


@dataclass
class Study:
    id: str
    arms: list[str]
    harnesses: dict[str, dict]
    runs: int
    outcome: list[str]
    margins: dict
    cases: list[Case]
    primary: str = "pass"

    @property
    def reference(self) -> str:
        return self.arms[0]

    def case(self, case_id: str) -> Case:
        return next(c for c in self.cases if c.id == case_id)


def load_case(study: str, directory: Path) -> Case:
    settings, prompt = front_matter((directory / "prompt.md").read_text())
    data = yaml.safe_load((directory / "case.yaml").read_text()) or {}
    graders = []
    for path in sorted((directory / "graders").glob("*.md")):
        options, text = front_matter(path.read_text())
        sha = hashlib.sha256(path.read_bytes()).hexdigest()
        graders.append(Grader(path.stem, options.pop("type"), text, options, sha))
    return Case(study, directory.name, directory, prompt, settings, data, graders)


def load_study(study: str, root: Path = STUDIES) -> Study:
    """The study and its cases; a case missing a grader the outcome counts is refused: its outcome could never pass."""
    directory = root / study
    config = yaml.safe_load((directory / "study.yaml").read_text())
    primary = config.get("primary", "pass")
    if primary not in PRIMARY:
        raise ValueError(f"{study}: primary is {primary!r}, one of {PRIMARY}")
    cases = [load_case(study, d) for d in sorted((directory / "cases").iterdir()) if d.is_dir()]
    for case in cases:
        names = {g.name for g in case.graders}
        for name in config["outcome"]:
            if name not in names:
                raise ValueError(f"case {case.id} has no grader {name}, which the outcome of {study} counts")
    return Study(
        study,
        config["arms"],
        config["harnesses"],
        int(config.get("runs", 1)),
        config["outcome"],
        config.get("margins", {"success": 0.10, "tokens": 0.15}),
        cases,
        primary,
    )


def studies() -> list[str]:
    return sorted(d.name for d in STUDIES.iterdir() if (d / "study.yaml").exists())
