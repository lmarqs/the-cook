"""Runs a study's jobs: one fixture copy and one headless agent per (case, arm, harness, run)."""

import json
import random
import re
import subprocess
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from collections.abc import Callable
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

from tool import fixtures, sandbox, traces
from tool.cases import RESULTS, Case, Study

TRIES = 3
PROVIDER_REFUSAL = re.compile(r"capacity|rate.?limit|overloaded|too many requests|\b(429|500|502|503|529)\b", re.I)
LOST_CONNECTION = re.compile(r"failed to authenticate|oauth|stream disconnected|failed to lookup address|"
                             r"connection (reset|refused)", re.I)


@dataclass(frozen=True)
class Job:
    case: str
    arm: str
    harness: str
    run: int
    source: str = ""

    @property
    def id(self) -> str:
        return f"{self.case}--{self.arm}--{self.harness}--r{self.run}"


def agent_command(harness: str, config: dict, case: Case) -> list[str]:
    found = sandbox.binaries()
    if harness == "claude":
        command = [
            str(found["claude"]), "-p", "--output-format", "stream-json", "--verbose",
            "--permission-mode", "acceptEdits", "--allowedTools", "Bash", "--permission-prompts", "none",
            "--strict-mcp-config", "--mcp-config", f"{sandbox.EVAL_DIR}/mcp.json", "--no-session-persistence",
            "--model", config["model"], "--max-turns", str(case.max_turns),
        ]
        return command + (["--effort", config["effort"]] if config.get("effort") else [])
    if harness == "codex":
        command = [
            str(found["codex"]), "exec", "--json", "--skip-git-repo-check", "-s", "workspace-write",
            "-c", "approval_policy=never", "--ephemeral", "-C", "/work", "-m", config["model"],
        ]
        if config.get("effort"):
            command += ["-c", f"model_reasoning_effort={config['effort']}"]
        return command + ["-"]
    raise ValueError(harness)


def versions() -> dict:
    found = sandbox.binaries()
    out = {}
    for name in ("claude", "codex"):
        result = subprocess.run([str(found[name]), "--version"], capture_output=True, text=True)
        out[name] = result.stdout.strip()
    return out


def jobs(study: Study, cases: list[str] | None, arms: list[str] | None, harnesses: list[str] | None,
         runs: int, seed: int, source_batch: Path | None = None) -> list[Job]:
    """Every (case, arm, harness, run) selected; a study with a `source` takes one job per writer run instead."""
    if study.source:
        selected = handoff_jobs(study, source_batch)
    else:
        selected = [Job(case.id, arm, harness, n) for case in study.cases for arm in study.arms
                    for harness in study.harnesses for n in range(1, runs + 1)]
    selected = [j for j in selected if (not cases or j.case in cases) and (not arms or j.arm in arms)
                and (not harnesses or j.harness in harnesses)]
    random.Random(seed).shuffle(selected)
    return selected


def handoff_jobs(study: Study, source_batch: Path | None) -> list[Job]:
    """A reader job per writer run of a case and arm the study has, except runs the provider refused; and an
    `unwritten` job per case, harness and run number the writers had."""
    if source_batch is None:
        raise ValueError(f"{study.id} reads a batch of {study.source}: pass --from <batch>")
    case_ids = {c.id for c in study.cases}
    selected, slots = [], set()
    for directory in sorted((source_batch / "runs").iterdir()):
        if not (directory / "run.json").exists():
            continue
        record = json.loads((directory / "run.json").read_text())
        if record["case"] not in case_ids or record["harness"] not in study.harnesses:
            continue
        slots.add((record["case"], record["harness"], record["run"]))
        if record["arm"] in study.arms and not refused(record) and not lost(record):
            source = directory.relative_to(RESULTS) if directory.is_relative_to(RESULTS) else directory
            selected.append(Job(record["case"], record["arm"], record["harness"], record["run"], str(source)))
    selected += [Job(case, fixtures.UNWRITTEN, harness, run) for case, harness, run in sorted(slots)
                 if fixtures.UNWRITTEN in study.arms]
    return selected


def refused(result: dict) -> bool:
    """The provider, the login or the network turned the run away before it changed a file: not the arm."""
    error = str(result.get("error") or "")
    return bool(error) and not result["files_changed"] \
        and bool(PROVIDER_REFUSAL.search(error) or LOST_CONNECTION.search(error))


def lost(result: dict) -> bool:
    """The login or the network ended the run after it changed files, so the arm did not decide where it stopped."""
    return bool(result.get("files_changed")) and bool(LOST_CONNECTION.search(str(result.get("error") or "")))


def process_error(exit_code: int, timed_out: bool, stderr: str) -> str | None:
    """The failure of a run that exited non-zero without the event stream naming one, as its stderr tells it."""
    if not exit_code or timed_out:
        return None
    tail = " / ".join(stderr.strip().splitlines()[-3:])
    return f"exit {exit_code}: {tail}" if tail else f"exit {exit_code}"


def with_retries(run_once: Callable[[], dict], tries: int = TRIES) -> dict:
    """Runs again while the provider refuses, up to `tries` in all; the result records how many it took."""
    for attempt in range(1, tries + 1):
        result = run_once()
        result["attempts"] = attempt
        if not refused(result):
            break
    return result


def run_job(study: Study, job: Job, batch_dir: Path) -> dict:
    case = study.case(job.case)
    directory = batch_dir / "runs" / job.id
    directory.mkdir(parents=True, exist_ok=True)

    def run_once() -> dict:
        if study.source:
            source = RESULTS / job.source if job.source else None
            repo = fixtures.build_handoff(study, case, job.arm, source, directory / "repo")
        else:
            repo = fixtures.build(case, job.arm, directory / "repo", haystack=study.haystack)
        argv = sandbox.command(repo, agent_command(job.harness, study.harnesses[job.harness], case),
                               eval_dir=batch_dir / "eval")
        started = datetime.now(timezone.utc)
        clock = time.monotonic()
        timed_out = False
        with open(directory / "events.jsonl", "w") as events, open(directory / "stderr.txt", "w") as errors:
            process = subprocess.Popen(argv, stdin=subprocess.PIPE, stdout=events, stderr=errors, text=True)
            try:
                process.communicate(case.prompt, timeout=case.timeout)
            except subprocess.TimeoutExpired:
                process.kill()
                process.communicate()
                timed_out = True
        duration = time.monotonic() - clock
        lines = [line for line in (directory / "events.jsonl").read_text().splitlines() if line.startswith("{")]
        record = getattr(traces, job.harness)(lines)
        fixtures.git(repo, "add", "-A")
        changed = [line for line in fixtures.git(repo, "diff", "--cached", "--name-only").splitlines() if line]
        (directory / "changes.patch").write_text(fixtures.git(repo, "diff", "--cached"))
        exit_error = process_error(process.returncode, timed_out, (directory / "stderr.txt").read_text())
        return {
            "study": study.id, **asdict(job), "id": job.id,
            "model": study.harnesses[job.harness]["model"], "effort": study.harnesses[job.harness].get("effort"),
            "started": started.isoformat(), "duration_s": round(duration, 1),
            "exit_code": process.returncode, "timed_out": timed_out,
            "files_changed": changed, **record, "exit_error": exit_error, "error": record["error"] or exit_error,
        }

    result = with_retries(run_once)
    (directory / "run.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def run(study: Study, batch: str, selected: list[Job], parallel: int, max_cost_usd: float | None, seed: int) -> Path:
    batch_dir = RESULTS / study.id / batch
    (batch_dir / "eval").mkdir(parents=True, exist_ok=True)
    (batch_dir / "eval" / "mcp.json").write_text('{"mcpServers": {}}\n')
    (batch_dir / "batch.json").write_text(json.dumps({
        "study": study.id, "batch": batch, "seed": seed, "started": datetime.now(timezone.utc).isoformat(),
        "versions": versions(), "harnesses": study.harnesses, "jobs": [j.id for j in selected],
    }, indent=2) + "\n")
    spent = {"usd": 0.0}
    lock = threading.Lock()

    def one(job: Job) -> None:
        with lock:
            if max_cost_usd is not None and job.harness == "claude" and spent["usd"] >= max_cost_usd:
                print(f"skip {job.id}: the batch reached ${max_cost_usd}", flush=True)
                return
        try:
            result = run_job(study, job, batch_dir)
        except Exception as error:  # a failed job is reported and the batch goes on
            print(f"fail {job.id}: {error}", flush=True)
            return
        with lock:
            spent["usd"] += result.get("cost_usd") or 0.0
            print(f"done {job.id}: {result['duration_s']}s, ${result.get('cost_usd') or 0:.3f}, "
                  f"{len(result['files_changed'])} files, error={result['error']}", flush=True)

    with ThreadPoolExecutor(max_workers=parallel) as pool:
        list(pool.map(one, selected))
    return batch_dir
