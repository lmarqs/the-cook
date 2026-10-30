"""Builds one run's repository: the fixture, the case's files, the cooked recipe, the arm's transform, one commit."""

import shutil
import subprocess
from pathlib import Path

from tool import arms
from tool.cases import FIXTURE, REPO, Case

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


def build(case: Case, arm: str, dest: Path) -> Path:
    """Writes the run's repository at `dest` and returns it, committed on `main`."""
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(FIXTURE, dest, ignore=shutil.ignore_patterns("__pycache__", ".pytest_cache"))
    files = case.dir / "files"
    if files.is_dir():
        shutil.copytree(files, dest, dirs_exist_ok=True)
    git(dest, "init", "-q")
    cook(dest)
    arms.apply(case.study, arm, dest, case)
    (dest / ".gitignore").write_text("__pycache__/\n.pytest_cache/\n")
    git(dest, "add", "-A")
    git(dest, "commit", "-q", "-m", "larder")
    return dest
