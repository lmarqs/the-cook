"""Every deterministic grader against planted defects: each defect must fail, the clean page must pass."""

import shutil
from pathlib import Path

import pytest

from tool import graders, judge
from tool.cases import FIXTURE, STUDIES, load_case
from tool.graders import Run

CLEAN_ADR = """# Run background jobs on Postgres

For whoever adds a background job or changes the database. Jobs live in Postgres through procrastinate, because the
Redis instance holding Celery's queue evicted 300 receipt jobs in March.

## Context

In March the Redis instance holding Celery's queue ran out of memory and evicted queued receipt jobs; 300 members
got no receipt and a volunteer resent them by hand.

## Options

- **Celery with Redis, persistence on**: keeps the code; lost because the volunteers still run Redis for one queue.
- **procrastinate on Postgres**: chosen.

## Decision

Jobs run on procrastinate, because it keeps a job in the same transaction as the pantry order that queues it.

## Consequences

Redis is switched off. See [sessions](../features/sessions.md#lifetime).
"""


@pytest.fixture
def repo(tmp_path: Path) -> Path:
    root = tmp_path / "repo"
    shutil.copytree(FIXTURE, root, ignore=shutil.ignore_patterns("__pycache__", ".pytest_cache"))
    return root


def make_run(repo: Path, changed: list[str], steps=None, last_message="", case="e1/change-decision") -> Run:
    study, case_id = case.split("/")
    record = {"files_changed": changed, "steps": steps or [], "last_message": last_message, "harness": "claude"}
    return Run(repo, record, load_case(study, STUDIES / study / "cases" / case_id), "")


def write_adr(repo: Path, name: str, text: str, listed: bool = True) -> str:
    (repo / "docs" / "adr" / name).write_text(text)
    if listed:
        guide = repo / "docs" / "adr" / "README.md"
        guide.write_text(guide.read_text() + f"- [Run background jobs on Postgres]({name}): jobs live in Postgres.\n")
    return f"docs/adr/{name}"


def structure(repo: Path, page: str) -> tuple[bool, str]:
    return graders.docs_structure(make_run(repo, [page]), {})


def test_a_clean_record_meets_the_structure(repo):
    page = write_adr(repo, "0002-run-background-jobs-on-postgres.md", CLEAN_ADR)

    assert structure(repo, page) == (True, f"no findings in ['{page}']")


@pytest.mark.parametrize("defect, plant, finding", [
    ("unlisted", lambda r: write_adr(r, "0002-run-jobs.md", CLEAN_ADR, listed=False), "its guide does not list it"),
    ("bad name", lambda r: write_adr(r, "jobs-on-postgres.md", CLEAN_ADR), "not named NNNN-<slug>.md"),
    ("number reused", lambda r: write_adr(r, "0001-run-jobs.md", CLEAN_ADR), "is used twice"),
    ("broken link", lambda r: write_adr(r, "0002-run-jobs.md", CLEAN_ADR.replace("sessions.md#", "session.md#")),
     "does not resolve"),
    ("broken anchor", lambda r: write_adr(r, "0002-run-jobs.md", CLEAN_ADR.replace("#lifetime", "#expiry")),
     "is not a heading"),
    ("sections out of order", lambda r: write_adr(r, "0002-run-jobs.md", CLEAN_ADR.replace("## Options", "## Opts")),
     "expected ['Context', 'Options', 'Decision', 'Consequences']"),
    ("rejected synonym", lambda r: write_adr(r, "0002-run-jobs.md", CLEAN_ADR.replace("300 members", "300 users")),
     "rejected synonym 'user'"),
])
def test_each_structural_defect_is_found(repo, defect, plant, finding):
    page = plant(repo)

    passed, evidence = structure(repo, page)

    assert not passed, defect
    assert finding in evidence


def test_the_maps_cooked_section_is_the_recipes_text_not_the_agents(repo):
    cooked = ("<!-- cook:begin io.github.lmarqs.cookbook:docs#seed/kinds -->\n"
              "Wait out [the rate limit](api/orders.md#limits), as customers do.\n"
              "<!-- cook:end io.github.lmarqs.cookbook:docs#seed/kinds -->\n")
    (repo / "docs" / "README.md").write_text(f"# Docs\n\nSee [the glossary](glossary.md).\n{cooked}")

    assert structure(repo, "docs/README.md") == (True, "no findings in ['docs/README.md']")

    (repo / "docs" / "README.md").write_text(f"# Docs\n\nSee [orders](api/orders.md), for customers.\n{cooked}")
    passed, evidence = structure(repo, "docs/README.md")

    assert not passed
    assert evidence == ("docs/README.md: link api/orders.md does not resolve; "
                        "docs/README.md: uses the rejected synonym 'customer'")


def test_a_page_outside_the_declared_kinds_is_found(repo):
    (repo / "docs" / "notes").mkdir()
    (repo / "docs" / "notes" / "jobs.md").write_text(CLEAN_ADR)

    passed, evidence = structure(repo, "docs/notes/jobs.md")

    assert not passed
    assert "notes/ is not a declared kind" in evidence


@pytest.mark.parametrize("text, finding", [
    (CLEAN_ADR, None),
    ("# Run background jobs on Postgres\n\n## Context\n\nText.\n", "no lead"),
    ("# Run background jobs on Postgres\n\nJobs on Postgres.\n\n## Context\n", "the lead has 3 words"),
    ("# Run background jobs on Postgres\n\nRun the background jobs on Postgres, the jobs on Postgres, run them, "
     "the jobs.\n\n## Context\n", "restates the title"),
    ("# Run background jobs on Postgres\n\n- a list is not a lead, whatever it says about the decision taken here\n",
     "no lead"),
])
def test_lead_present_catches_each_missing_or_empty_lead(repo, text, finding):
    page = write_adr(repo, "0002-run-jobs.md", text)

    passed, evidence = graders.lead_present(make_run(repo, [page]), {"glob": "docs/adr/*.md"})

    assert passed == (finding is None)
    if finding:
        assert finding in evidence


def test_read_before_edit_needs_the_read_first(repo):
    read = {"kind": "read", "path": "docs/features/payments.md"}
    edit = {"kind": "edit", "path": "larder/capture.py"}
    options = {"path": "docs/features/payments.md"}

    assert graders.read_before_edit(make_run(repo, [], [read, edit]), options)[0]
    assert not graders.read_before_edit(make_run(repo, [], [edit, read]), options)[0]
    assert not graders.read_before_edit(make_run(repo, [], [edit]), options)[0]


def test_regex_and_file_exists_look_only_at_changed_files(repo):
    page = write_adr(repo, "0002-run-jobs.md", CLEAN_ADR)
    options = {"target": "docs/adr/[0-9]*.md", "pattern": "## Decision\\s.*procrastinate", "flags": "si"}

    assert graders.regex(make_run(repo, [page]), options)[0]
    assert not graders.regex(make_run(repo, []), options)[0]
    assert graders.file_exists(make_run(repo, [page]), {"glob": "docs/adr/[0-9]*.md"})[0]
    assert not graders.file_exists(make_run(repo, []), {"glob": "docs/adr/[0-9]*.md"})[0]


def test_tests_pass_fails_the_fixture_and_passes_a_correct_retry(repo):
    run = make_run(repo, [], case="e2/retry-capture")
    options = {"hidden": ["test_capture_retries.py"]}

    assert not graders.tests_pass(run, options)[0]

    (repo / "larder" / "capture.py").write_text('''import uuid

from larder.gateway import Gateway, GatewayTimeout
from larder.orders import Order


def capture(order: Order, gateway: Gateway) -> str:
    key = uuid.uuid4().hex
    for attempt in range(3):
        try:
            return gateway.capture(order.id, order.amount_cents, idempotency_key=key)
        except GatewayTimeout:
            if attempt == 2:
                raise
''')
    assert graders.tests_pass(run, options)[0]


def test_judge_reads_the_last_json_verdict_and_blinds_the_arm():
    assert judge.verdict('thinking {"x": 1} then {"passed": true, "evidence": "Context, line 1"}') == \
        (True, "Context, line 1")
    assert judge.verdict("no json here")[0] is None
    assert judge.verdict('{"passed": true, "evidence": "it sends f\\"{key}\\" on every attempt"}') == \
        (True, 'it sends f"{key}" on every attempt')
    assert "docs-layout" not in judge.blind("I used the docs-layout skill in .claude/skills/docs-layout")
