"""Reads a run's event stream into one record: steps in order, tokens, cost and the last message.

A step is a read (a file's content printed), a search, an edit, or another command. Paths are relative to the run's
repository. Field names were fixed from the samples in tests/samples/, recorded with Claude Code 2.1.314 and Codex
0.160.1.
"""

import json
import re
import shlex

READ_TOOLS = {"Read"}
SEARCH_TOOLS = {"Grep", "Glob"}
EDIT_TOOLS = {"Edit", "Write", "MultiEdit", "NotebookEdit"}
PRINTERS = {"cat", "head", "tail", "sed", "nl", "less", "more", "bat", "awk"}
SEARCHERS = {"rg", "grep", "find", "ls", "fd", "tree"}
SEPARATORS = {"&&", "||", ";", "|", "&"}
SHELLS = re.compile(r"(^|/)(ba|z|da)?sh$")


def relative(path: str) -> str:
    for prefix in ("/work/", "./"):
        if path.startswith(prefix):
            path = path[len(prefix):]
    return path


def empty() -> dict:
    return {
        "steps": [],
        "tokens": {"input": 0, "cached": 0, "output": 0, "reasoning": 0},
        "cost_usd": None,
        "turns": None,
        "last_message": "",
        "is_error": False,
        "error": None,
        "init": None,
    }


def claude(lines: list[str]) -> dict:
    record = empty()
    seen = set()
    for line in lines:
        event = json.loads(line)
        kind = event.get("type")
        if kind == "system" and event.get("subtype") == "init":
            record["init"] = {k: event.get(k) for k in ("model", "claude_code_version", "skills", "mcp_servers", "plugins")}
        elif kind == "assistant":
            for block in event["message"]["content"]:
                if block.get("type") != "tool_use" or block["id"] in seen:
                    continue
                seen.add(block["id"])
                name, args = block["name"], block.get("input", {})
                if name in READ_TOOLS:
                    record["steps"].append({"kind": "read", "path": relative(args.get("file_path", "")), "tool": name})
                elif name in EDIT_TOOLS:
                    path = args.get("file_path") or args.get("notebook_path", "")
                    record["steps"].append({"kind": "edit", "path": relative(path), "tool": name})
                elif name in SEARCH_TOOLS:
                    record["steps"].append({"kind": "search", "tool": name, "input": args})
                elif name == "Bash":
                    record["steps"] += shell_steps(args.get("command", ""))
                else:
                    record["steps"].append({"kind": "other", "tool": name, "input": args})
        elif kind == "result":
            models = event.get("modelUsage") or {}
            if models:
                record["tokens"] = {
                    "input": sum(m.get("inputTokens", 0) + m.get("cacheReadInputTokens", 0)
                                 + m.get("cacheCreationInputTokens", 0) for m in models.values()),
                    "cached": sum(m.get("cacheReadInputTokens", 0) for m in models.values()),
                    "output": sum(m.get("outputTokens", 0) for m in models.values()),
                    "reasoning": sum(m.get("thinkingTokens", 0) for m in models.values()),
                }
            record["cost_usd"] = event.get("total_cost_usd")
            record["turns"] = event.get("num_turns")
            record["last_message"] = event.get("result") or ""
            record["is_error"] = bool(event.get("is_error"))
            record["error"] = event.get("api_error") or (event.get("subtype") if event.get("is_error") else None)
    return record


def codex(lines: list[str]) -> dict:
    record = empty()
    for line in lines:
        event = json.loads(line)
        kind = event.get("type")
        item = event.get("item") or {}
        if kind == "item.completed" and item.get("type") == "command_execution":
            record["steps"] += shell_steps(item.get("command", ""))
        elif kind == "item.completed" and item.get("changes"):
            for change in item["changes"]:
                record["steps"].append({"kind": "edit", "path": relative(change.get("path", "")), "tool": item["type"]})
        elif kind == "item.completed" and item.get("type") == "agent_message":
            record["last_message"] = item.get("text", "")
        elif kind == "item.completed" and item.get("type") == "error":
            record["error"] = item.get("message")
        elif kind == "turn.completed":
            usage = event.get("usage", {})
            record["tokens"]["input"] += usage.get("input_tokens", 0)
            record["tokens"]["cached"] += usage.get("cached_input_tokens", 0)
            record["tokens"]["output"] += usage.get("output_tokens", 0)
            record["tokens"]["reasoning"] += usage.get("reasoning_output_tokens", 0)
            record["turns"] = (record["turns"] or 0) + 1
        elif kind in ("turn.failed", "error"):
            record["is_error"] = True
            record["error"] = json.dumps(event.get("error") or event.get("message"))
    return record


def shell_steps(command: str) -> list[dict]:
    """The steps one shell command takes: files it prints, searches it runs, files it writes."""
    try:
        argv = shlex.split(command)
    except ValueError:
        return [{"kind": "other", "tool": "shell", "command": command}]
    if len(argv) >= 3 and SHELLS.search(argv[0]) and argv[1] in ("-c", "-lc"):
        return shell_steps(argv[2])
    try:
        lexer = shlex.shlex(command, posix=True, punctuation_chars=";&|<>")
        lexer.whitespace_split = True
        tokens = list(lexer)
    except ValueError:
        return [{"kind": "other", "tool": "shell", "command": command}]
    steps, simple = [], []
    for token in tokens + [";"]:
        if token in SEPARATORS:
            steps += simple_steps(simple, command)
            simple = []
        else:
            simple.append(token)
    return steps or [{"kind": "other", "tool": "shell", "command": command}]


def simple_steps(tokens: list[str], command: str) -> list[dict]:
    if not tokens:
        return []
    steps = []
    words = []
    target = None
    for index, token in enumerate(tokens):
        following = tokens[index + 1] if index + 1 < len(tokens) else ""
        if target:
            if target == "file" and not token.startswith("/dev/"):
                steps.append({"kind": "edit", "path": relative(token), "tool": "shell"})
            target = None
        elif token.isdigit() and following[:1] in (">", "<"):
            continue
        elif token in (">", ">>", ">|", "&>", "&>>"):
            target = "file"
        elif token in (">&", "<&", "<", "<<", "<<<"):
            target = "other"
        else:
            words.append(token)
    if not words:
        return steps
    name = words[0].rsplit("/", 1)[-1]
    operands = [w for w in words[1:] if not w.startswith("-")]
    if name == "sed" and any(w.startswith(("-i", "--in-place")) for w in words[1:]):
        files = [o for o in operands[1:] if looks_like_path(o)]
        return [{"kind": "edit", "path": relative(o), "tool": name} for o in files] + steps
    if name in PRINTERS:
        if name in ("sed", "awk") and operands:
            operands = operands[1:]
        reads = [{"kind": "read", "path": relative(o), "tool": name} for o in operands if looks_like_path(o)]
        return reads + steps
    if name in SEARCHERS:
        return [{"kind": "search", "tool": name, "command": " ".join(words)}] + steps
    return [{"kind": "other", "tool": name, "command": command}] + steps


def looks_like_path(word: str) -> bool:
    return "/" in word or "." in word.rsplit("/", 1)[-1]


def first_index(steps: list[dict], kind: str, path: str | None = None) -> int | None:
    for index, step in enumerate(steps):
        if step["kind"] == kind and (path is None or step.get("path") == path):
            return index
    return None
