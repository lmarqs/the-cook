"""E4's reader sees the repository its writer left, without the sources the writer worked from, in history too."""

import json
from pathlib import Path

import pytest

from tool import fixtures, harness
from tool.cases import load_study

PAGE = "docs/features/reminders.md"


@pytest.fixture(scope="module")
def source_batch(tmp_path_factory) -> Path:
    """A stage-A batch: one run that wrote a reminders page, one the provider refused, one that wrote nothing."""
    batch = tmp_path_factory.mktemp("e1") / "pilot"
    writer = load_study("e1").case("reminders-why")
    repo = fixtures.build(writer, "bare", tmp_path_factory.mktemp("a") / "repo")
    (repo / PAGE).write_text("# Reminders\n\nSMS, because the 2025 email trial raised missed pickups to 13%.\n")
    fixtures.git(repo, "add", "-A")
    runs = {
        "reminders-why--bare--claude--r1": (fixtures.git(repo, "diff", "--cached"), [PAGE], None),
        "reminders-why--skill--claude--r1": ("", [], "Selected model is at capacity."),
        "reminders-why--no-skill--claude--r1": ("", [], None),
        "tenet--bare--codex--r2": ("", [], None),
    }
    for run_id, (patch, changed, error) in runs.items():
        case, arm, name, run = run_id.split("--")
        directory = batch / "runs" / run_id
        directory.mkdir(parents=True)
        (directory / "changes.patch").write_text(patch)
        (directory / "run.json").write_text(json.dumps({
            "id": run_id, "case": case, "arm": arm, "harness": name, "run": int(run[1:]),
            "files_changed": changed, "error": error}))
    return batch


def files(repo: Path) -> set[str]:
    return {str(p.relative_to(repo)) for p in repo.rglob("*") if p.is_file() and ".git" not in p.parts}


def test_the_reader_gets_the_writers_pages_and_no_sources(source_batch, tmp_path):
    study = load_study("e4")
    run = source_batch / "runs" / "reminders-why--bare--claude--r1"

    repo = fixtures.build_handoff(study, study.case("reminders-why"), "bare", run, tmp_path / "repo")

    assert "13%" in (repo / PAGE).read_text()
    assert not any(p.startswith("notes/") for p in files(repo))
    assert not any(".claude/skills/docs-layout" in p for p in files(repo))
    history = fixtures.git(repo, "log", "--all", "-p")
    assert "Issue 52" not in history
    assert fixtures.git(repo, "rev-list", "--count", "--all").strip() == "1"


def test_the_unwritten_control_is_the_writers_repository_before_any_writing(tmp_path):
    study = load_study("e4")

    repo = fixtures.build_handoff(study, study.case("tenet"), "unwritten", None, tmp_path / "repo")

    assert not (repo / PAGE).exists()
    assert not any(p.startswith("notes/") for p in files(repo))
    assert (repo / "larder" / "catalog.py").exists()
    assert (repo / ".claude" / "skills" / "docs-layout" / "SKILL.md").exists()


def test_reader_jobs_follow_the_writers_runs_and_add_the_control(source_batch):
    study = load_study("e4")

    selected = harness.jobs(study, None, None, None, 1, seed=1, source_batch=source_batch)

    by_id = {j.id: j for j in selected}
    assert by_id["reminders-why--bare--claude--r1"].source == str(source_batch / "runs" / "reminders-why--bare--claude--r1")
    assert "reminders-why--no-skill--claude--r1" in by_id
    assert "reminders-why--skill--claude--r1" not in by_id, "the provider refused the writer's run"
    assert by_id["reminders-why--unwritten--claude--r1"].source == ""
    assert by_id["tenet--unwritten--codex--r2"].source == ""
    assert sorted(by_id) == sorted(["reminders-why--bare--claude--r1", "reminders-why--no-skill--claude--r1",
                                    "tenet--bare--codex--r2", "reminders-why--unwritten--claude--r1",
                                    "tenet--unwritten--codex--r2"])
