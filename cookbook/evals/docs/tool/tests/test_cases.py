"""A study's cases are loaded whole: each names every grader the study's outcome counts."""

from pathlib import Path

import pytest

from tool.cases import load_study, studies


def write_study(root: Path, graders: list[str]) -> None:
    study = root / "s1"
    (study / "cases" / "c1" / "graders").mkdir(parents=True)
    (study / "study.yaml").write_text("arms: [a, b]\nharnesses: {}\noutcome: [01-tests]\n")
    (study / "cases" / "c1" / "prompt.md").write_text("Do it.\n")
    (study / "cases" / "c1" / "case.yaml").write_text("{}\n")
    for name in graders:
        (study / "cases" / "c1" / "graders" / f"{name}.md").write_text("---\ntype: regex\n---\nA grader.\n")


def test_a_case_without_an_outcome_grader_is_refused(tmp_path):
    write_study(tmp_path, ["02-other"])

    with pytest.raises(ValueError, match="c1 has no grader 01-tests"):
        load_study("s1", tmp_path)


def test_a_case_with_every_outcome_grader_loads(tmp_path):
    write_study(tmp_path, ["01-tests", "02-other"])

    assert [c.id for c in load_study("s1", tmp_path).cases] == ["c1"]


def test_every_study_in_the_repository_loads():
    for study in studies():
        assert load_study(study).cases
