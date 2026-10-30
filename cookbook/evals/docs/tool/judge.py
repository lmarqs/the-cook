"""The rubric grader: a model of the other family than the run's, blind to the arm, answering in JSON."""

import json
import re
import subprocess
import tempfile
from pathlib import Path

from tool import sandbox, traces
from tool.graders import Run, matching

OTHER = {"claude": "codex", "codex": "claude"}
REDACT = re.compile(r"docs-layout|\.claude/skills|\.agents/skills|\bskills?\b", re.I)
PROMPT = """You grade one result of a coding agent against one expectation. Judge only the material below; do not
read files or run commands.

Expectation: {text}

Reference answer, what a result that meets the expectation contains: {reference}

{material}

Answer with one JSON object and nothing else: {{"passed": true or false, "evidence": "<the sentence or fact in the
material your verdict rests on, quoted, or what is missing>"}}. When the material does not show the expectation met,
it is not met."""


def blind(text: str) -> str:
    return REDACT.sub("[redacted]", text)


def material(run: Run, options: dict) -> str:
    parts = []
    for item in options.get("show", ["last_message"]):
        if item == "last_message":
            parts.append(f"The agent's last message:\n<<<\n{run.record['last_message']}\n>>>")
        elif item == "diff":
            parts.append(f"The change the agent made:\n<<<\n{run.patch}\n>>>")
        else:
            for path in matching(run, item):
                parts.append(f"File {path}:\n<<<\n{(run.repo / path).read_text()}\n>>>")
    if not parts:
        parts.append("The agent produced no material for this expectation.")
    return blind("\n\n".join(parts))


def verdict(text: str) -> tuple[bool | None, str]:
    for match in reversed(list(re.finditer(r"\{.*?\}", text, re.S))):
        try:
            answer = json.loads(match.group(0))
        except json.JSONDecodeError:
            continue
        if isinstance(answer.get("passed"), bool):
            return answer["passed"], str(answer.get("evidence", ""))
    return None, f"no verdict in: {text[:200]}"


def ask(run: Run, text: str, options: dict, models: dict) -> tuple[bool | None, str, str]:
    """Returns the verdict, its evidence, and the judging harness."""
    judge = OTHER[run.record["harness"]]
    prompt = PROMPT.format(text=text, reference=options.get("reference", "none given"), material=material(run, options))
    found = sandbox.binaries()
    if judge == "claude":
        argv = [str(found["claude"]), "-p", "--output-format", "stream-json", "--verbose", "--tools", "",
                "--strict-mcp-config", "--mcp-config", f"{sandbox.EVAL_DIR}/mcp.json", "--no-session-persistence",
                "--model", models["claude"]["model"], "--max-turns", "2"]
    else:
        argv = [str(found["codex"]), "exec", "--json", "--skip-git-repo-check", "-s", "read-only",
                "-c", "approval_policy=never", "--ephemeral", "-C", "/work", "-m", models["codex"]["model"]]
        if models["codex"].get("effort"):
            argv += ["-c", f"model_reasoning_effort={models['codex']['effort']}"]
        argv.append("-")
    with tempfile.TemporaryDirectory() as scratch:
        work, eval_dir = Path(scratch) / "work", Path(scratch) / "eval"
        work.mkdir()
        eval_dir.mkdir()
        (eval_dir / "mcp.json").write_text('{"mcpServers": {}}\n')
        result = subprocess.run(sandbox.command(work, argv, eval_dir=eval_dir), input=prompt,
                                capture_output=True, text=True, timeout=600)
    lines = [line for line in result.stdout.splitlines() if line.startswith("{")]
    answer = getattr(traces, judge)(lines)["last_message"]
    passed, evidence = verdict(answer)
    return passed, evidence, judge
