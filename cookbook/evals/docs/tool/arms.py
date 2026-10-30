"""Each arm's transform of a cooked fixture: the one difference a study measures."""

import re
import shutil
import textwrap
from pathlib import Path

from tool.cases import Case

SKILL_DIRS = [".claude/skills/docs-layout", ".agents/skills/docs-layout"]
FACT = re.compile(r"<!-- fact:(?P<id>[\w-]+) -->\n(?P<body>.*?)\n<!-- /fact:(?P=id) -->\n", re.S)
COOKED = re.compile(r"<!-- cook:begin [^>]*-->.*?<!-- cook:end [^>]*-->\n?", re.S)
LEAD = re.compile(r"\A(# [^\n]+\n)\n(?P<lead>(?:(?!#)[^\n]+\n)+)\n")


def wrap(text: str) -> str:
    """One paragraph at the fixture's 120 columns, so injected text reads like the pages around it."""
    return textwrap.fill(" ".join(text.split()), 120, break_on_hyphens=False, break_long_words=False)


def docs_pages(repo: Path) -> list[Path]:
    return sorted(p for p in (repo / "docs").rglob("*.md") if p.name != "README.md")


def strip_fact_markers(repo: Path) -> None:
    for page in docs_pages(repo):
        text = page.read_text()
        page.write_text(FACT.sub(lambda m: m["body"] + "\n", text))


def facts(repo: Path) -> dict[str, tuple[Path, str]]:
    found = {}
    for page in docs_pages(repo):
        for match in FACT.finditer(page.read_text()):
            found[match["id"]] = (page, match["body"])
    return found


def remove_skill(repo: Path, case: Case) -> None:
    for directory in SKILL_DIRS:
        shutil.rmtree(repo / directory)


def remove_criteria(repo: Path, case: Case) -> None:
    map_page = repo / "docs" / "README.md"
    text, count = COOKED.subn("", map_page.read_text())
    assert count == 1, "the map holds one cooked section"
    map_page.write_text(text)


def scatter(repo: Path, case: Case) -> None:
    """Copies each fact to the case's pages; the copy at `stale_at` says the case's `stale` text instead."""
    known = facts(repo)
    for fact in case.data["facts"]:
        _, body = known[fact["id"]]
        for copy in fact["copies"]:
            page = repo / copy
            text = wrap(fact["stale"]) if copy == fact.get("stale_at") else body
            page.write_text(page.read_text().rstrip("\n") + "\n\n" + text.strip() + "\n")


def remove_leads(repo: Path, case: Case) -> None:
    for page in docs_pages(repo):
        page.write_text(LEAD.sub(lambda m: m[1] + "\n", page.read_text(), count=1))


def false_leads(repo: Path, case: Case) -> None:
    for relative, lead in case.data["false_leads"].items():
        page = repo / relative
        text, count = LEAD.subn(lambda m: m[1] + "\n" + wrap(lead) + "\n\n", page.read_text(), count=1)
        assert count == 1, f"{relative} has a lead to replace"
        page.write_text(text)


TRANSFORMS = {
    ("e1", "skill"): [],
    ("e1", "no-skill"): [remove_skill],
    ("e1", "no-criteria"): [remove_criteria],
    ("e2", "owned"): [],
    ("e2", "scattered"): [scatter],
    ("e3", "leads"): [],
    ("e3", "no-leads"): [remove_leads],
    ("e3", "false-leads"): [false_leads],
}


def apply(study: str, arm: str, repo: Path, case: Case) -> None:
    for transform in TRANSFORMS[(study, arm)]:
        transform(repo, case)
    strip_fact_markers(repo)
