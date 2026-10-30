"""Deterministic graders. Each takes a run and returns (passed, evidence)."""

import fnmatch
import re
import shutil
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path

from tool import sandbox, traces
from tool.cases import Case, Grader

DIR_KINDS = ["adrs", "api", "features", "runbooks", "ux"]
PAGE_KINDS = ["architecture.md", "philosophy.md", "glossary.md", "README.md"]
SECTIONS = {
    "adrs": ["Context", "Options", "Decision", "Consequences"],
    "runbooks": ["Before you begin", "Steps", "If it fails"],
}
ADR_NAME = re.compile(r"^\d{4}-[a-z0-9]+(-[a-z0-9]+)*\.md$")
LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)\)")
DECLARED = re.compile(r"^## .+, `(?P<path>[^`]+)`\s*$", re.M)
STOPWORDS = set("a an the of to and or in on for is are it its this that with by as at from them they we our".split())


@dataclass
class Run:
    repo: Path
    record: dict
    case: Case
    patch: str

    @property
    def changed(self) -> list[str]:
        return [p for p in self.record["files_changed"] if (self.repo / p).exists()]


def matching(run: Run, pattern: str, changed_only: bool = True) -> list[str]:
    pool = run.changed if changed_only else [str(p.relative_to(run.repo)) for p in run.repo.rglob("*") if p.is_file()]
    return sorted(p for p in pool if fnmatch.fnmatch(p, pattern))


def file_exists(run: Run, options: dict) -> tuple[bool, str]:
    found = matching(run, options["glob"], options.get("changed_only", True))
    return bool(found), f"matching {options['glob']}: {found or 'none'}"


def regex(run: Run, options: dict) -> tuple[bool, str]:
    flags = 0
    for letter in options.get("flags", ""):
        flags |= {"i": re.I, "s": re.S, "m": re.M}[letter]
    pattern = re.compile(options["pattern"], flags)
    target = options["target"]
    if target == "last_message":
        texts = {"last message": run.record["last_message"]}
    elif target == "diff":
        texts = {"diff": run.patch}
    else:
        texts = {p: (run.repo / p).read_text() for p in matching(run, target, options.get("changed_only", True))}
    hits = {name: m.group(0)[:120] for name, text in texts.items() if (m := pattern.search(text))}
    expect = options.get("expect", "match")
    passed = bool(hits) if expect == "match" else not hits
    return passed, f"{expect} /{options['pattern']}/ in {list(texts) or 'no files'}: {hits or 'no match'}"


def tests_pass(run: Run, options: dict) -> tuple[bool, str]:
    with tempfile.TemporaryDirectory() as scratch:
        work = Path(scratch) / "repo"
        shutil.copytree(run.repo, work, ignore=shutil.ignore_patterns(".git", "__pycache__", ".pytest_cache"))
        for name in options.get("hidden", []):
            shutil.copy(run.case.dir / "hidden" / name, work / "tests" / name)
        command = options.get("command", "python3 -m pytest -q -p no:cacheprovider")
        argv = sandbox.command(work, ["sh", "-c", command], network=False)
        result = subprocess.run(argv, capture_output=True, text=True, timeout=300)
    tail = (result.stdout + result.stderr).strip().splitlines()[-3:]
    return result.returncode == 0, f"exit {result.returncode}: {' / '.join(tail)}"


def read_before_edit(run: Run, options: dict) -> tuple[bool, str]:
    steps = run.record["steps"]
    read = traces.first_index(steps, "read", options["path"])
    edit = traces.first_index(steps, "edit")
    passed = read is not None and (edit is None or read < edit)
    return passed, f"first read of {options['path']} at step {read}; first edit at step {edit}"


def slug(heading: str) -> str:
    return re.sub(r"[^\w\- ]", "", heading.strip().lower()).replace(" ", "-")


def headings(text: str, level: int | None = None) -> list[str]:
    found, fenced = [], False
    for line in text.splitlines():
        if line.startswith("```"):
            fenced = not fenced
        match = re.match(r"^(#{1,6}) (.+)$", line)
        if match and not fenced and (level is None or len(match[1]) == level):
            found.append(match[2].strip())
    return found


def prose(text: str) -> str:
    """The text without fenced blocks, code spans and link targets."""
    text = re.sub(r"```.*?```", "", text, flags=re.S)
    text = re.sub(r"`[^`]*`", "", text)
    return re.sub(r"\]\([^)]*\)", "]", text)


def declared(repo: Path) -> tuple[set[str], set[str]]:
    dirs, pages = set(DIR_KINDS), set(PAGE_KINDS)
    map_page = repo / "docs" / "README.md"
    if map_page.exists():
        for match in DECLARED.finditer(map_page.read_text()):
            path = match["path"]
            (dirs if path.endswith("/") else pages).add(path.rstrip("/"))
    return dirs, pages


def rejected_synonyms(repo: Path) -> list[str]:
    glossary = repo / "docs" / "glossary.md"
    if not glossary.exists():
        return []
    words = []
    for line in glossary.read_text().splitlines():
        if line.startswith("_Avoid_:"):
            words += [w.strip() for w in line.split(":", 1)[1].split(",") if w.strip()]
    return words


def structure_findings(repo: Path, page: str) -> list[str]:
    path = repo / page
    relative = Path(page).relative_to("docs")
    text = path.read_text()
    findings = []
    dirs, pages = declared(repo)
    kind = relative.parts[0] if len(relative.parts) > 1 else None
    if kind is None and relative.name not in pages:
        findings.append(f"{page}: not a page of a declared kind")
    if kind is not None and kind not in dirs:
        findings.append(f"{page}: {kind}/ is not a declared kind")
    if kind is not None and relative.name != "README.md":
        guide = repo / "docs" / kind / "README.md"
        if not guide.exists():
            findings.append(f"{page}: docs/{kind}/README.md is missing")
        elif f"]({relative.name})" not in guide.read_text():
            findings.append(f"{page}: its guide does not list it")
    for target in LINK.findall(text):
        if re.match(r"^[a-z]+:", target):
            continue
        file, _, anchor = target.partition("#")
        linked = (path.parent / file).resolve() if file else path
        if not linked.exists():
            findings.append(f"{page}: link {target} does not resolve")
        elif anchor and linked.suffix == ".md" and anchor not in [slug(h) for h in headings(linked.read_text())]:
            findings.append(f"{page}: anchor #{anchor} is not a heading of {file or page}")
    if kind in SECTIONS and relative.name != "README.md":
        found = [h for h in headings(text, 2) if h in SECTIONS[kind]]
        if found != SECTIONS[kind]:
            findings.append(f"{page}: sections {found}, expected {SECTIONS[kind]}")
    if kind == "adrs" and relative.name != "README.md":
        if not ADR_NAME.match(relative.name):
            findings.append(f"{page}: not named NNNN-<slug>.md")
        numbers = [p.name[:4] for p in (repo / "docs" / "adrs").glob("[0-9][0-9][0-9][0-9]-*.md")]
        if numbers.count(relative.name[:4]) > 1:
            findings.append(f"{page}: number {relative.name[:4]} is used twice")
    if relative.name != "glossary.md":
        body = prose(text)
        for word in rejected_synonyms(repo):
            if re.search(rf"\b{re.escape(word)}(s|es)?\b", body, re.I):
                findings.append(f"{page}: uses the rejected synonym '{word}'")
    return findings


def docs_structure(run: Run, options: dict) -> tuple[bool, str]:
    pages = [p for p in run.changed if p.startswith("docs/") and p.endswith(".md")]
    findings = [f for page in pages for f in structure_findings(run.repo, page)]
    return not findings, "; ".join(findings) if findings else f"no findings in {pages or 'no changed pages'}"


def lead(text: str) -> str | None:
    lines = text.splitlines()
    if not lines or not lines[0].startswith("# "):
        return None
    paragraph = []
    for line in lines[1:]:
        if line.startswith("#"):
            break
        if not line.strip():
            if paragraph:
                break
            continue
        if not paragraph and re.match(r"^(\s*[-*|>]|\s*\d+\.|```|\*\*)", line):
            return None
        paragraph.append(line.strip())
    return " ".join(paragraph) or None


def lead_findings(text: str) -> list[str]:
    found = lead(text)
    if found is None:
        return ["no lead between the title and the first section"]
    words = re.findall(r"[a-z0-9']+", found.lower())
    title = set(re.findall(r"[a-z0-9']+", text.splitlines()[0].lower()))
    findings = []
    if len(words) < 12:
        findings.append(f"the lead has {len(words)} words")
    if set(words) - STOPWORDS <= title:
        findings.append("the lead restates the title")
    return findings


def lead_present(run: Run, options: dict) -> tuple[bool, str]:
    pages = [p for p in matching(run, options["glob"]) if Path(p).name != "README.md"]
    findings = [f"{p}: {f}" for p in pages for f in lead_findings((run.repo / p).read_text())]
    if not pages:
        return False, f"no changed page matches {options['glob']}"
    return not findings, "; ".join(findings) if findings else f"each of {pages} has a lead"


TYPES = {
    "file_exists": file_exists,
    "regex": regex,
    "tests_pass": tests_pass,
    "read_before_edit": read_before_edit,
    "docs_structure": docs_structure,
    "lead_present": lead_present,
}


def grade(grader: Grader, run: Run) -> tuple[bool, str]:
    return TYPES[grader.type](run, grader.options)
