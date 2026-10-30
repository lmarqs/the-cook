"""Builds one run's repository: the fixture, the case's files, the cooked recipe, the arm's transform, one commit."""

import shutil
import subprocess
from pathlib import Path

from tool import arms
from tool.cases import FIXTURE, HAYSTACK, REPO, Case, Study, load_study

COOK = REPO / "cook"
RECIPE = "io.github.lmarqs.cookbook:docs:0.0.0#seed"
SKILLS_ROOTS = [".claude/skills", ".agents/skills"]
GIT_ENV = {
    "GIT_AUTHOR_NAME": "larder",
    "GIT_AUTHOR_EMAIL": "larder@example.invalid",
    "GIT_COMMITTER_NAME": "larder",
    "GIT_COMMITTER_EMAIL": "larder@example.invalid",
    "GIT_AUTHOR_DATE": "2026-01-01T00:00:00Z",
    "GIT_COMMITTER_DATE": "2026-01-01T00:00:00Z",
    "PATH": "/usr/bin:/bin",
    "HOME": "/nonexistent",
}


def git(repo: Path, *args: str) -> str:
    return subprocess.run(
        ["git", "-c", "init.defaultBranch=main", "-c", "commit.gpgsign=false", *args],
        cwd=repo, env=GIT_ENV, check=True, capture_output=True, text=True,
    ).stdout


def cook(repo: Path) -> None:
    (repo / "cook.toml").write_text(f'repositories = ["{REPO / "cookbook"}"]\n')
    roots = [f"--skills-root={root}" for root in SKILLS_ROOTS]
    result = subprocess.run(
        [str(COOK), "-C", str(repo), "--batch-approve", RECIPE, *roots], capture_output=True, text=True
    )
    if result.returncode:
        raise RuntimeError(f"cook failed:\n{result.stdout}\n{result.stderr}")
    lock = (repo / "cook.lock").read_text()
    for root in SKILLS_ROOTS:
        assert f"{root}/docs-layout/SKILL.md" in lock, f"cook.lock owns the skill under {root}"
    assert "docs/README.md" in lock, "cook.lock owns the map's section"
    # The run sees what an adopting repository holds after cooking, not the tool's local cookbook path.
    (repo / "cook.toml").unlink()
    (repo / "cook.lock").unlink()


def add_haystack(repo: Path) -> None:
    """Copies the haystack's pages in, and appends each of its guide lines that a guide of the repository lacks."""
    for source in sorted((HAYSTACK / "docs").rglob("*.md")):
        target = repo / source.relative_to(HAYSTACK)
        target.parent.mkdir(parents=True, exist_ok=True)
        if source.name != "README.md":
            shutil.copy(source, target)
            continue
        guide = target.read_text().rstrip("\n") + "\n" if target.exists() else ""
        missing = [line for line in source.read_text().splitlines() if line.startswith("- ") and line not in guide]
        target.write_text(guide + "".join(line + "\n" for line in missing))


def build(case: Case, arm: str, dest: Path, commit: bool = True, haystack: bool = False) -> Path:
    """Writes the run's repository at `dest` and returns it, committed on `main` unless `commit` is false; with
    `haystack`, the shared extra pages are added before the arm's transform."""
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(FIXTURE, dest, ignore=shutil.ignore_patterns("__pycache__", ".pytest_cache"))
    files = case.dir / "files"
    if files.is_dir():
        shutil.copytree(files, dest, dirs_exist_ok=True)
    if haystack:
        add_haystack(dest)
    git(dest, "init", "-q")
    cook(dest)
    arms.apply(case.study, arm, dest, case)
    if commit:
        commit_all(dest)
    return dest


def commit_all(repo: Path) -> None:
    (repo / ".gitignore").write_text("__pycache__/\n.pytest_cache/\n")
    git(repo, "add", "-A")
    git(repo, "commit", "-q", "-m", "larder")


UNWRITTEN = "unwritten"


def build_handoff(study: Study, case: Case, arm: str, source_run: Path | None, dest: Path) -> Path:
    """The reader's repository: the writer's as its run left it, without the case's `remove` paths, in one commit so
    no history holds the sources. The `unwritten` arm starts from the writer's `skill` repository, before any writing."""
    writer = load_study(study.source).case(case.id)
    build(writer, "skill" if arm == UNWRITTEN else arm, dest, commit=False)
    if source_run is not None:
        patch = source_run / "changes.patch"
        if patch.read_text().strip():
            git(dest, "apply", "--whitespace=nowarn", str(patch.resolve()))
    for relative in case.data.get("remove", []):
        target = dest / relative
        if target.is_dir():
            shutil.rmtree(target)
        elif target.exists():
            target.unlink()
    files = case.dir / "files"
    if files.is_dir():
        shutil.copytree(files, dest, dirs_exist_ok=True)
    commit_all(dest)
    return dest
