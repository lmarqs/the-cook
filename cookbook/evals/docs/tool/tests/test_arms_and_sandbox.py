"""Each arm makes exactly its change, and a run's sandbox sees only its repository and the login files."""

import subprocess
from pathlib import Path

import pytest

from tool import fixtures, sandbox
from tool.cases import HAYSTACK, load_study


def tree(root: Path) -> dict[str, str]:
    return {str(p.relative_to(root)): p.read_text() for p in root.rglob("*")
            if p.is_file() and ".git" not in p.relative_to(root).parts}


@pytest.fixture(scope="module")
def built(tmp_path_factory) -> dict[tuple[str, str], dict[str, str]]:
    trees = {}
    for study_id, case_id in (("e1", "change-decision"), ("e2", "retry-capture"), ("e3", "session-expiry")):
        study = load_study(study_id)
        for arm in study.arms:
            dest = tmp_path_factory.mktemp(f"{study_id}-{arm}") / "repo"
            fixtures.build(study.case(case_id), arm, dest)
            trees[(study_id, arm)] = tree(dest)
    return trees


def changed(a: dict[str, str], b: dict[str, str]) -> set[str]:
    return {p for p in a.keys() | b.keys() if a.get(p) != b.get(p)}


def test_every_arm_is_cooked_without_the_tool_paths(built):
    for files in built.values():
        assert "docs/README.md" in files
        assert "cook.toml" not in files and "cook.lock" not in files
        assert not any("fact:" in text for path, text in files.items() if path.startswith("docs/"))


def test_e1_arms_differ_only_by_the_skill_or_the_criteria(built):
    skill = built[("e1", "skill")]

    removed = changed(skill, built[("e1", "no-skill")])
    assert removed and all(p.split("/")[2] == "docs-layout" for p in removed)
    assert changed(skill, built[("e1", "no-criteria")]) == {"docs/README.md"}
    assert "## Decisions, `adrs/`" not in built[("e1", "no-criteria")]["docs/README.md"]
    assert changed(skill, built[("e1", "bare")]) == removed | {"docs/README.md"}
    assert built[("e1", "bare")]["docs/README.md"] == built[("e1", "no-criteria")]["docs/README.md"]


def test_e2_scattered_adds_three_copies_one_of_them_stale(built):
    owned, scattered = built[("e2", "owned")], built[("e2", "scattered")]

    assert changed(owned, scattered) == {"docs/architecture.md", "docs/features/checkout.md",
                                         "docs/runbooks/gateway-timeouts.md"}
    assert "draws a fresh idempotency key" in scattered["docs/runbooks/gateway-timeouts.md"]
    assert "the same idempotency key" in scattered["docs/architecture.md"]


def test_e3_arms_change_only_leads(built):
    leads, none, false = built[("e3", "leads")], built[("e3", "no-leads")], built[("e3", "false-leads")]

    assert changed(leads, false) == {"docs/features/sessions.md"}
    assert "placeholder" in false["docs/features/sessions.md"]
    for path in changed(leads, none):
        assert path.startswith("docs/")
        assert len(none[path]) < len(leads[path])
    assert "For whoever" not in none["docs/features/sessions.md"]
    assert "## Lifetime" in none["docs/features/sessions.md"]


def test_the_sandbox_home_holds_only_the_login_files_and_the_tools(tmp_path):
    (tmp_path / "marker").write_text("x")

    def listing(path: str) -> list[str]:
        argv = sandbox.command(tmp_path, ["sh", "-c", f'ls -A "{path}"'], network=False)
        return subprocess.run(argv, capture_output=True, text=True, check=True).stdout.split()

    assert set(listing("$HOME")) <= {".claude", ".codex", ".local"}
    assert listing("$HOME/.claude") == [".credentials.json"]
    assert listing("$HOME/.codex") == ["auth.json"]
    assert listing("$HOME/.local/share") == sorted({p.parts[len(sandbox.HOME.parts) + 2]
                                                    for p in sandbox.tool_mounts() if sandbox.HOME in p.parents})
    assert listing("/work") == ["marker"]


@pytest.fixture(scope="module")
def stacked(tmp_path_factory) -> dict[str, Path]:
    study = load_study("e3")
    repos = {}
    for arm in ("leads", "no-leads"):
        dest = tmp_path_factory.mktemp(f"stacked-{arm}") / "repo"
        repos[arm] = fixtures.build(study.case("reminders-email"), arm, dest, haystack=True)
    return repos


def test_the_haystack_adds_its_pages_and_lists_them_beside_the_cases_own(stacked):
    repo = stacked["leads"]
    guide = (repo / "docs" / "features" / "README.md").read_text()

    assert (repo / "docs" / "features" / "volunteer-rota.md").exists()
    assert "](reminders.md)" in guide and "](volunteer-rota.md)" in guide
    assert guide.count("](volunteer-rota.md)") == 1
    for kind in ("features", "runbooks", "adrs"):
        listed = (repo / "docs" / kind / "README.md").read_text()
        for page in (repo / "docs" / kind).glob("*.md"):
            assert page.name == "README.md" or f"]({page.name})" in listed, f"{kind}/{page.name} is listed"


def test_no_leads_strips_the_haystacks_leads_too(stacked):
    assert "For whoever" not in (stacked["no-leads"] / "docs" / "features" / "volunteer-rota.md").read_text()


def test_every_haystack_page_meets_the_structure_and_has_a_lead(stacked):
    from tool import graders

    repo = stacked["leads"]
    pages = [str(p.relative_to(HAYSTACK)) for p in (HAYSTACK / "docs").rglob("*.md") if p.name != "README.md"]

    assert len(pages) >= 30
    findings = [f for page in pages for f in graders.structure_findings(repo, page)]
    findings += [f"{page}: {f}" for page in pages for f in graders.lead_findings((repo / page).read_text())]
    assert findings == []
