from pathlib import Path

from tool import traces

SAMPLES = Path(__file__).parent / "samples"


def sample(name: str) -> list[str]:
    return (SAMPLES / f"{name}.jsonl").read_text().splitlines()


def test_claude_sample_gives_its_read_tokens_cost_and_answer():
    record = traces.claude(sample("claude"))

    assert record["steps"] == [{"kind": "read", "path": "docs/README.md", "tool": "Read"}]
    assert record["tokens"] == {"input": 4 + 29386 + 11600, "cached": 29386, "output": 179, "reasoning": 38}
    assert round(record["cost_usd"], 4) == 0.1023
    assert record["last_message"].startswith("`docs/README.md` is the map")
    assert record["init"]["mcp_servers"] == []
    assert not record["is_error"]


def test_codex_sample_gives_its_read_its_edit_and_tokens():
    record = traces.codex(sample("codex"))

    kinds = [(s["kind"], s.get("path")) for s in record["steps"] if s["kind"] in ("read", "edit")]
    assert kinds == [("read", "docs/README.md"), ("edit", "note.txt")]
    assert record["tokens"] == {"input": 45756, "cached": 40832, "output": 169, "reasoning": 0}
    assert record["cost_usd"] is None
    assert "note.txt" in record["last_message"]


def test_shell_steps_unwrap_a_login_shell_and_split_its_commands():
    steps = traces.shell_steps("/usr/bin/zsh -lc \"sed -n '1,80p' docs/a.md | head -5 && cat x.py > out.txt\"")

    assert steps == [
        {"kind": "read", "path": "docs/a.md", "tool": "sed"},
        {"kind": "read", "path": "x.py", "tool": "cat"},
        {"kind": "edit", "path": "out.txt", "tool": "shell"},
    ]


def test_a_search_is_not_a_read():
    assert [s["kind"] for s in traces.shell_steps("rg -n capture docs")] == ["search"]


def test_first_index_finds_the_first_matching_step():
    steps = [{"kind": "read", "path": "a"}, {"kind": "edit", "path": "b"}, {"kind": "read", "path": "b"}]

    assert traces.first_index(steps, "read", "b") == 2
    assert traces.first_index(steps, "edit") == 1
    assert traces.first_index(steps, "read", "c") is None


def test_redirecting_a_descriptor_or_to_dev_null_is_not_an_edit():
    steps = traces.shell_steps("ls -R docs larder 2>/dev/null; cat a.py 2>&1 >> log.txt")

    assert [s for s in steps if s["kind"] == "edit"] == [{"kind": "edit", "path": "log.txt", "tool": "shell"}]
    assert steps[0] == {"kind": "search", "tool": "ls", "command": "ls -R docs larder"}
