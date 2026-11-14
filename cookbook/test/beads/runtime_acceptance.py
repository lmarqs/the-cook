#!/usr/bin/env python3
"""Opt-in native acceptance. See runtime-acceptance.md; never run by Gradle tests."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shlex
import shutil
import subprocess
import time


TRACKER = "io.github.lmarqs.cookbook:beads:0.0.0#tracker"
VIEWER = "io.github.lmarqs.cookbook:beads:0.0.0#viewer"
NAME = "io.github.lmarqs.cookbook:beads#"
REPO = Path(__file__).resolve().parents[3]
ENV = {
    k: v for k, v in os.environ.items()
    if not k.startswith(("BD_", "BEADS_", "GIT_", "BV_", "RALPH_", "AWS_", "OPENAI_", "ANTHROPIC_"))
    and not any(s in k for s in ("TOKEN", "API_KEY", "SECRET", "CREDENTIAL"))
}
ENV.update(
    GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL="/dev/null",
    GIT_AUTHOR_NAME="Cook acceptance", GIT_AUTHOR_EMAIL="cook@example.invalid",
    GIT_COMMITTER_NAME="Cook acceptance", GIT_COMMITTER_EMAIL="cook@example.invalid",
    BEADS_ACTOR="cook-acceptance", BD_NON_INTERACTIVE="1", NO_COLOR="1",
    BD_METRICS_DISABLED="true",
)


class Acceptance:
    def __init__(self, root):
        self.root = root.resolve()
        assert self.root.is_relative_to(Path("/tmp")), "Use a disposable /tmp directory"
        self.bd = shutil.which("bd")
        self.bv = shutil.which("bv")
        self.ralph = shutil.which("ralph-tui")
        self.cook = REPO / "cook"
        self.results = []

    def run(self, cwd, args, extra=None, expected=0, timeout=180):
        args = [str(a) for a in args]
        env = dict(ENV)
        env.update(extra or {})
        number = len(list(self.root.glob("command-*.json"))) + 1
        start = time.monotonic()
        proc = subprocess.run(args, cwd=cwd, env=env, text=True, capture_output=True, timeout=timeout)
        record = dict(
            command=args, cwd=str(cwd), environment_overrides=extra or {},
            exit=proc.returncode, stdout=proc.stdout, stderr=proc.stderr,
            seconds=round(time.monotonic() - start, 3),
        )
        (self.root / f"command-{number:04}.json").write_text(json.dumps(record, indent=2) + "\n")
        print(f"{number:04} {Path(cwd).name}: {args[:4]} -> {proc.returncode}", flush=True)
        if expected is not None:
            assert proc.returncode == expected, f"command-{number:04}.json: {proc.stderr}\n{proc.stdout}"
        return proc

    def check(self, name, condition):
        assert condition, name
        self.results.append(name)
        (self.root / "assertions.json").write_text(json.dumps(self.results, indent=2) + "\n")

    def git(self, repo, *args):
        return self.run(repo, ["git", *args]).stdout

    def native(self, repo, *args, **kwargs):
        return self.run(repo, [self.bd, *args], **kwargs)

    def cook_recipe(self, repo, recipe, share=None):
        args = [self.cook, "--batch-approve", recipe, "--instructions=AGENTS.md"]
        if share:
            args.append("--share=" + share)
        guard = self.root / "cook-only-bin"
        guard.mkdir(exist_ok=True)
        for tool in ("bd", "bv", "ralph-tui"):
            wrapper = guard / tool
            wrapper.write_text('#!/bin/sh\necho native >> '
                               + shlex.quote(str(self.root / "native-during-cook.log"))
                               + '\necho "Unexpected native command while cooking" >&2\nexit 97\n')
            wrapper.chmod(0o755)
        proc = self.run(repo, args, {"PATH": str(guard) + ":" + ENV["PATH"]})
        self.check(f"{repo.name}: cooking invokes no native tools", not (self.root / "native-during-cook.log").exists())
        if proc.stdout.strip() == "Already cooked.":
            guidance = (repo / "AGENTS.md").read_text()
            expected = "tracker/tracker" if share else "viewer/viewer"
            self.check(f"{repo.name}: installed guidance on no-op", expected in guidance)
        else:
            self.check(f"{repo.name}: after-cooking guidance {recipe.split('#')[-1]}",
                       "bd bootstrap --yes" in proc.stdout if share else "--robot-triage" in proc.stdout)
        return proc

    def commit(self, repo, message):
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-m", message)

    def snapshot(self, repo, label):
        data = {name: self.git(repo, *args) for name, args in {
            "status": ("status", "--short", "--untracked-files=all"),
            "index": ("ls-files", "--stage"), "config": ("config", "--local", "--list"),
            "commits": ("log", "-3", "--format=%h %s"), "refs": ("show-ref",),
            "diff": ("diff", "HEAD"),
        }.items()}
        (self.root / f"{repo.name}-{label}.json").write_text(json.dumps(data, indent=2) + "\n")

    def graph(self, repo):
        data = json.loads(self.native(repo, "list", "--all", "--limit", "0", "--json").stdout)
        return sorted(data, key=lambda item: item["id"])

    def preservation(self, repo):
        common = Path(self.git(repo, "rev-parse", "--path-format=absolute", "--git-common-dir").strip())
        return {str(p): p.read_bytes() for p in (
            common / "hooks" / "pre-commit", common / "info" / "exclude",
            repo / ".beads" / "README.md",
        ) if p.exists()}

    def setup(self, name, source=None):
        repo = self.root / name
        if source:
            self.run(self.root, ["git", "clone", source, repo])
        else:
            repo.mkdir()
            self.git(repo, "init", "--initial-branch=main")
        (repo / "cook.toml").write_text(f'repositories = ["{REPO / "cookbook"}"]\n')
        (repo / "AGENTS.md").write_text("# Synthetic instructions\n\nKeep team guidance.\n")
        if not source:
            (repo / ".gitignore").write_text("unrelated-cache/\n")
        self.git(repo, "config", "core.hooksPath", ".git/hooks")
        hooks = repo / ".git/hooks/pre-commit"
        hooks.write_text("#!/bin/sh\nexit 0\n")
        hooks.chmod(0o755)
        with (repo / ".git/info/exclude").open("a") as stream:
            stream.write("\n/machine-local/\n")
        if source:
            self.git(repo, "remote", "add", "upstream", source)
            self.native(repo, "bootstrap", "--yes")
            with (repo / ".beads/config.yaml").open("a") as stream:
                stream.write("custom:\n  acceptance: preserve\n")
            (repo / ".beads/README.md").write_text("Team-owned synthetic seed.\n")
        return repo

    def seed(self, repo):
        for suffix, title, kind, priority in (
            ("epic", "Synthetic acceptance epic", "epic", 1),
            ("quick", "Independent P1", "task", 1),
            ("gate", "P2 unblocks three", "task", 2),
            ("next1", "Dependent one", "task", 2),
            ("next2", "Dependent two", "task", 2),
            ("next3", "Dependent three", "task", 2),
        ):
            self.native(repo, "create", title, "--id", repo.name + "-" + suffix,
                        "--type", kind, "--priority", str(priority), "--json")
            if suffix != "epic":
                self.native(repo, "dep", "add", repo.name + "-" + suffix, repo.name + "-epic", "--type", "parent-child")
            if suffix.startswith("next"):
                self.native(repo, "dep", "add", repo.name + "-" + suffix, repo.name + "-gate")

    def viewer(self, repo, store, label):
        # This is the recipe's final after-cooking sequence, including native directory resolution.
        proc = self.run(repo, ["/bin/sh", "-c", '''
beads_dir="${BEADS_DIR:-$(dirname -- "$(git rev-parse --path-format=absolute --git-common-dir)")/.beads}"
bd --db "$beads_dir" export -o "$beads_dir/issues.jsonl" &&
  bv --db "$beads_dir" --robot-triage
'''], {"PATH": str(Path(self.bd).parent) + ":" + str(Path(self.bv).parent) + ":" + ENV["PATH"],
       "BEADS_DIR": str(store) if label == "external" else ""})
        data = json.loads(proc.stdout)
        (self.root / f"{repo.name}-{label}-triage.json").write_text(json.dumps(data, indent=2) + "\n")
        output = self.run(repo, [self.bv, "--db", store, "--robot-graph"]).stdout
        graph = json.loads(output)
        selected = {item["id"] for item in graph["adjacency"]["nodes"]}
        self.check(f"{repo.name}: {label} Viewer selected graph",
                   selected == {item["id"] for item in self.graph(store.parent)})

    def failures(self, repo, worktree):
        store = repo / ".beads"
        prefix = "empty-team" if repo.name.startswith("existing") else repo.name
        original = self.native(repo, "show", prefix + "-gate", "--json").stdout
        title = json.loads(original)[0]["title"]
        export = store / "issues.jsonl"
        previous = export.read_bytes()
        self.native(repo, "update", prefix + "-gate", "--title", "Unexported synthetic change")
        failure_bin = self.root / "failure-bin"
        failure_bin.mkdir(exist_ok=True)
        wrapper = failure_bin / "bd"
        wrapper.write_text('#!/bin/sh\nif [ "$1" = export ]; then\n'
                           '  echo "synthetic export failure 73" >&2; exit 73\nfi\n'
                           'exec "' + self.bd + '" "$@"\n')
        wrapper.chmod(0o755)
        for location in (repo, worktree):
            proc = self.run(location, [self.bv, "--db", store, "--robot-next"],
                            {"PATH": str(failure_bin) + ":" + ENV["PATH"]})
            auth = json.loads(proc.stdout)["source_authority"]
            self.check(f"{location.name}: stale failure diagnostics", auth["state"] == "partial"
                       and not auth["claim_safe"] and auth["readiness"] == "provisional"
                       and auth["sources"][0]["stale"]
                       and "synthetic export failure 73" in str(auth["sources"][0]["warnings"]))
        self.check(f"{repo.name}: stale bytes unchanged", export.read_bytes() == previous)
        archived = export.with_suffix(".saved")
        export.rename(archived)
        proc = self.run(worktree, [self.bv, "--db", store, "--robot-next"],
                        {"PATH": str(failure_bin) + ":" + ENV["PATH"]}, expected=1)
        authority = json.loads(proc.stdout)["source_authority"]
        self.check(f"{repo.name}: missing export failure", authority["state"] == "unknown")
        archived.rename(export)
        self.native(repo, "update", prefix + "-gate", "--title", title)
        self.viewer(worktree, store, "recovered")

    def runners(self):
        registry = Path.home() / ".config/ralph-tui/sessions.json"
        before = json.loads(registry.read_text()) if registry.exists() else {"sessions": {}}
        trace = self.root / "trace-bin"
        trace.mkdir(exist_ok=True)
        for label, real in (("bd", self.bd), ("bv", self.bv)):
            wrapper = trace / label
            wrapper.write_text(
                "#!/usr/bin/env python3\nimport os, sys, json\n"
                "with open(" + repr(str(self.root / "tracker-trace.jsonl")) + ", 'a') as stream:\n"
                "    stream.write(json.dumps(dict(pid=os.getpid(), ppid=os.getppid(), cwd=os.getcwd(), "
                "binary=" + repr(label) + ", args=sys.argv[1:])) + '\\n')\n"
                "os.execv(" + repr(real) + ", [" + repr(real) + "] + sys.argv[1:])\n"
            )
            wrapper.chmod(0o755)
        worker = self.root / "fake-worker.py"
        worker.write_text(
            "#!/usr/bin/env python3\nimport sys, json, os\n"
            "if '--version' in sys.argv:\n    print('Synthetic worker 1.0.0'); sys.exit(0)\n"
            "prompt = sys.stdin.read()\n"
            "with open(" + repr(str(self.root / "worker-trace.jsonl")) + ", 'a') as stream:\n"
            "    stream.write(json.dumps(dict(pid=os.getpid(), cwd=os.getcwd(), prompt=prompt)) + '\\n')\n"
            "print(json.dumps({'type':'assistant','message':{'content':[{'type':'text',"
            "'text':'Synthetic work. <promise>COMPLETE</promise>'}]}}), flush=True)\n"
            "print(json.dumps({'type':'result','subtype':'success',"
            "'result':'Synthetic work. <promise>COMPLETE</promise>'}), flush=True)\n"
        )
        worker.chmod(0o755)
        for share in ("team", "clone"):
            for tracker in ("beads", "beads-bv"):
                repo = self.setup("runner-" + share + "-" + tracker, str(self.root / "team.git"))
                self.cook_recipe(repo, TRACKER, share)
                self.cook_recipe(repo, VIEWER)
                self.run(repo, [self.cook, "--batch-approve", "io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui",
                                "--harness=claude", "--tracker=" + tracker, "--share=" + share])
                config = repo / ".ralph-tui/config.toml"
                config.write_text(config.read_text() + f'''\n[[agents]]
name = "fake"
plugin = "claude"
command = "{worker}"
[agents.options]
printMode = "stream"
skipPermissions = false
''')
                # These are team-owned seeded runner settings, rather than Cook-owned policy.
                config.write_text(config.read_text().replace("autoCommit = true", "autoCommit = false"))
                self.commit(repo, "Synthetic native runner configuration")
                worktree = repo / ".worktrees/ralph/acceptance"
                proc = self.run(repo, [repo / ".ralph-tui/bin/run", "acceptance", "--epic", "empty-team-epic",
                                      "--agent", "fake", "--iterations", "1", "--serial", "--headless",
                                      "--no-setup", "--no-notify", "--no-sandbox", "--prompt",
                                      worktree / ".ralph-tui/templates" / (tracker + ".hbs")],
                                {"PATH": str(trace) + ":" + ENV["PATH"]})
                selected = "empty-team-quick" if tracker == "beads" else "empty-team-gate"
                self.check(f"{repo.name}: native serial iteration selects expected task",
                           f"Task {selected}: COMPLETED" in proc.stdout
                           and "Iterations: 1, Tasks completed: 1" in proc.stdout)
                state = json.loads(self.native(worktree, "show", selected, "--json").stdout)[0]
                self.check(f"{repo.name}: native runner owns completion", state["status"] == "closed"
                           and state["close_reason"] == "Completed by agent")
                self.check(f"{repo.name}: selected main store", not (worktree / ".beads/embeddeddolt").exists())
        self.json_runner(worker)
        after = json.loads(registry.read_text()) if registry.exists() else {"sessions": {}}
        result = {
            "existing_registry_entries_preserved": all(after.get("sessions", {}).get(k) == v
                                                       for k, v in before.get("sessions", {}).items()),
            "existing_entry_count": len(before.get("sessions", {})),
            "new_entry_count": len(set(after.get("sessions", {})) - set(before.get("sessions", {}))),
        }
        self.check("unrelated native session registry entries preserved", result["existing_registry_entries_preserved"])
        (self.root / "registry-verification.json").write_text(json.dumps(result, indent=2) + "\n")
        calls = [json.loads(line) for line in (self.root / "tracker-trace.jsonl").read_text().splitlines()]
        workers = [json.loads(line) for line in (self.root / "worker-trace.jsonl").read_text().splitlines()]
        self.check("five synthetic workers only", len(workers) == 5)
        for entry in workers[:4]:
            mutations = [call for call in calls if call["cwd"] == entry["cwd"]
                         and call["binary"] == "bd" and call["args"][0] in ("update", "close")]
            self.check(entry["cwd"] + ": worker never mutates tracker", mutations
                       and all(call["ppid"] != entry["pid"] for call in mutations)
                       and "Never close or update the bead, and do not run `bd`" in entry["prompt"])
        (self.root / "runners-complete.json").write_text(json.dumps({"assertions": len(self.results)}) + "\n")

    def json_runner(self, worker):
        repo = self.setup("runner-json")
        self.run(repo, [self.cook, "--batch-approve", "io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui",
                        "--harness=claude", "--tracker=json", "--share=team"])
        config = repo / ".ralph-tui/config.toml"
        config.write_text(config.read_text().replace("autoCommit = true", "autoCommit = false") + f'''
[[agents]]
name = "fake"
plugin = "claude"
command = "{worker}"
[agents.options]
printMode = "stream"
skipPermissions = false
''')
        prd = repo / "prd.json"
        prd.write_text(json.dumps({"project": "Synthetic JSON", "branchName": "main", "description": "No Beads",
                                  "userStories": [{"id": "US-001", "title": "Synthetic JSON story",
                                                   "description": "No network or agent", "acceptanceCriteria": [],
                                                   "priority": 1, "passes": False, "dependsOn": []}]}) + "\n")
        self.commit(repo, "Synthetic JSON runner")
        native_free = self.root / "json-bin"
        native_free.mkdir()
        for tool in ("bun", "ralph-tui"):
            (native_free / tool).symlink_to(shutil.which(tool))
        env = {"PATH": str(native_free) + ":/usr/bin:/bin"}
        self.check("JSON PATH contains no bd", shutil.which("bd", path=env["PATH"]) is None)
        self.check("JSON PATH contains no bv", shutil.which("bv", path=env["PATH"]) is None)
        worktree = repo / ".worktrees/ralph/acceptance"
        proc = self.run(repo, [repo / ".ralph-tui/bin/run", "acceptance", "--prd", prd, "--agent", "fake",
                              "--iterations", "1", "--serial", "--headless", "--no-setup", "--no-notify",
                              "--no-sandbox", "--prompt", worktree / ".ralph-tui/templates/json.hbs"], env)
        self.check("JSON runs without Beads", "Task US-001: COMPLETED" in proc.stdout
                   and json.loads(prd.read_text())["userStories"][0]["passes"])

    def idempotent(self, repo, share):
        paths = [repo / p for p in (".gitignore", "AGENTS.md", ".beads/config.yaml", "cook.lock")]
        before = {str(p): p.read_bytes() for p in paths}
        self.cook_recipe(repo, TRACKER, share)
        self.cook_recipe(repo, VIEWER)
        self.check(f"{repo.name}: byte-identical recooking", before == {str(p): p.read_bytes() for p in paths})
        self.run(repo, [self.cook, "check"])

    def lifecycle(self):
        remote = self.root / "team.git"
        self.run(self.root, ["git", "init", "--bare", "--initial-branch=main", remote])
        fixtures = []
        for share in ("team", "clone"):
            repo = self.setup("empty-" + share)
            preserve = self.preservation(repo)
            self.cook_recipe(repo, TRACKER, share)
            self.check(f"{repo.name}: cooking initializes no store", not (repo / ".beads/embeddeddolt").exists())
            self.check(f"{repo.name}: tracker-only ownership", "#viewer" not in (repo / "cook.lock").read_text())
            self.commit(repo, "Synthetic cooked tracker")
            self.native(repo, "bootstrap", "--yes")
            status = self.native(repo, "dolt", "status").stdout
            if share == "clone":
                self.check("fresh clone: effective disabled sync", "Remote sync: disabled" in status)
            self.seed(repo)
            self.cook_recipe(repo, VIEWER)
            self.idempotent(repo, share)
            self.commit(repo, "Synthetic cooked Viewer")
            self.check(f"{repo.name}: bootstrap preserves local content", self.preservation(repo) == preserve)
            fixtures.append((repo, share))
        team = fixtures[0][0]
        self.git(team, "remote", "add", "origin", str(remote))
        self.git(team, "remote", "get-url", "origin")
        self.git(team, "push", "-u", "origin", "main")
        self.check("team: clean first-publication index/worktree", not self.git(team, "status", "--porcelain"))
        self.native(team, "dolt", "push", "--yes")
        self.git(team, "diff", "--check")
        self.git(team, "log", "-1", "--oneline")
        self.git(team, "push", "origin", "main")
        original = self.graph(team)
        for share in ("team", "clone"):
            repo = self.setup("existing-" + share, str(remote))
            self.check(f"{repo.name}: native bootstrap restores IDs/dependencies/content", self.graph(repo) == original)
            preserve = self.preservation(repo)
            self.run(repo, [self.cook, "discard", "--batch-approve", NAME + "viewer"])
            self.cook_recipe(repo, TRACKER, share)
            self.check(f"{repo.name}: existing tracker-only ownership",
                       "#viewer" not in (repo / "cook.lock").read_text())
            self.cook_recipe(repo, VIEWER)
            self.idempotent(repo, share)
            self.check(f"{repo.name}: existing hooks/excludes/seeds preserved", self.preservation(repo) == preserve)
            self.commit(repo, "Synthetic existing adoption")
            fixtures.append((repo, share))
        for repo, share in fixtures:
            before = self.graph(repo)
            status = self.native(repo, "dolt", "status").stdout
            if share == "clone":
                self.check(f"{repo.name}: sync disabled before personal writes", "Remote sync: disabled" in status)
                refs = self.git(remote, "show-ref")
                prefix = "empty-team" if repo.name.startswith("existing") else repo.name
                self.native(repo, "create", "Private synthetic task", "--id", prefix + "-personal", "--json")
                before = self.graph(repo)
                self.native(repo, "dolt", "push")
                self.native(repo, "dolt", "pull")
                self.native(repo, "dolt", "remote", "add", "synthetic", "git+file://" + str(remote), expected=1)
                self.check(f"{repo.name}: personal writes never change team remote",
                           self.git(remote, "show-ref") == refs)
                overridden = self.native(repo, "dolt", "status", extra={"BD_DOLT_LOCAL_ONLY": "false"}).stdout
                self.check(f"{repo.name}: environment override reported", "Remote sync: disabled" not in overridden)
                override = repo / ".beads/config.local.yaml"
                override.write_text("dolt:\n  local-only: false\n  auto-push: false\n")
                self.check(f"{repo.name}: native local override reported",
                           "Remote sync: disabled" not in self.native(repo, "dolt", "status").stdout)
                override.unlink()
                self.check(f"{repo.name}: policy restored before writes",
                           "Remote sync: disabled" in self.native(repo, "dolt", "status").stdout)
            worktree = self.root / (repo.name + "-wt")
            self.git(repo, "worktree", "add", "-b", repo.name + "-wt", str(worktree))
            self.check(f"{repo.name}: linked worktree shares graph", self.graph(worktree) == before)
            self.viewer(repo, repo / ".beads", "main")
            self.viewer(worktree, repo / ".beads", "worktree")
            if repo.name.startswith("empty"):
                self.failures(repo, worktree)
                before = self.graph(repo)
            self.check(f"{repo.name}: no second worktree database", not (worktree / ".beads/embeddeddolt").exists())
            self.snapshot(repo, "before-transitions")
            preserve = self.preservation(repo)
            export = (repo / ".beads/issues.jsonl").read_bytes()
            metadata = repo / ".beads/metadata.json"
            native_metadata = metadata.read_bytes() if metadata.exists() else None
            for mode in ("clone", "team"):
                self.cook_recipe(repo, TRACKER, mode)
                self.check(f"{repo.name}: transition to {mode} preserves graph", self.graph(repo) == before)
                self.check(f"{repo.name}: transition to {mode} preserves native content",
                           self.preservation(repo) == preserve)
                if mode == "clone":
                    tracked = self.git(repo, "ls-files", ".beads")
                    if share == "team" or repo.name.startswith("existing"):
                        self.check(f"{repo.name}: tracked config stays tracked", ".beads/config.yaml" in tracked)
                        self.git(repo, "rm", "--cached", ".beads/config.yaml")
                    self.git(repo, "add", ".")
                    self.check(f"{repo.name}: normal clone staging excludes native files",
                               not self.git(repo, "ls-files", ".beads"))
                    status = self.native(repo, "dolt", "status").stdout
                    self.check(f"{repo.name}: clone native gate", "Remote sync: disabled" in status)
            self.check(f"{repo.name}: transitions preserve export",
                       (repo / ".beads/issues.jsonl").read_bytes() == export)
            lock = (repo / "cook.lock").read_text()
            self.check(f"{repo.name}: independent ownership entries",
                       NAME + "tracker" in lock and NAME + "viewer" in lock)
            self.run(repo, [self.cook, "discard", "--batch-approve", NAME + "viewer"])
            lock = (repo / "cook.lock").read_text()
            self.check(f"{repo.name}: Viewer discard retains tracker ownership",
                       NAME + "viewer" not in lock and NAME + "tracker" in lock)
            self.check(f"{repo.name}: Viewer discard retains native export",
                       (repo / ".beads/issues.jsonl").read_bytes() == export)
            self.run(repo, [self.cook, "discard", "--batch-approve", NAME + "tracker"])
            self.check(f"{repo.name}: engine removes last ownership record", not (repo / "cook.lock").exists())
            self.check(f"{repo.name}: all owned sections removed", "cook:begin" not in (repo / "AGENTS.md").read_text()
                       and "cook:begin" not in (repo / ".gitignore").read_text())
            self.check(f"{repo.name}: unrelated instructions preserved",
                       (repo / "AGENTS.md").read_text() == "# Synthetic instructions\n\nKeep team guidance.\n")
            remaining = (repo / ".beads/config.yaml").read_text() if (repo / ".beads/config.yaml").exists() else ""
            self.check(f"{repo.name}: five owned keys removed", all(key not in remaining for key in
                       ("local-only:", "auto-push:", "no-git-ops:", "git-add:", "auto:")))
            if repo.name.startswith("existing"):
                self.check(f"{repo.name}: native remote and unrelated YAML preserved",
                           "remote:" in remaining and "acceptance: preserve" in remaining)
            self.check(f"{repo.name}: tracker discard preserves graph", self.graph(repo) == before)
            self.check(f"{repo.name}: discard preserves local content", self.preservation(repo) == preserve)
            if native_metadata is not None:
                self.check(f"{repo.name}: discard preserves native metadata", metadata.read_bytes() == native_metadata)
            self.snapshot(repo, "after-discard")
        self.viewer(fixtures[0][0], fixtures[1][0] / ".beads", "external")
        (self.root / "lifecycle-complete.json").write_text(json.dumps({"assertions": len(self.results)}) + "\n")


def matches_pinned_version(tool, output, expected):
    patterns = {
        "bd": r"bd version (\S+)(?: \([^()\r\n]+\))?",
        "bv": r"bv v(\S+)",
        "ralph-tui": r"ralph-tui (\S+)",
    }
    match = re.fullmatch(patterns[tool], output.strip())
    return match is not None and match.group(1) == expected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=["lifecycle", "runners"])
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    suite = Acceptance(args.root)
    if args.phase == "lifecycle":
        args.root.mkdir(parents=True, exist_ok=False)
    else:
        assert (args.root / "lifecycle-complete.json").exists(), "Run lifecycle first"
    for tool, binary, version in (("bd", suite.bd, "1.3.1"), ("bv", suite.bv, "0.25.2"),
                                  ("ralph-tui", suite.ralph, "0.12.0")):
        assert binary, "Install pinned native prerequisites explicitly before running"
        proc = suite.run(args.root, [binary, "--version"])
        suite.check("pinned version " + version, matches_pinned_version(tool, proc.stdout + proc.stderr, version))
    binaries = {str(Path(path).resolve()): hashlib.sha256(Path(path).read_bytes()).hexdigest()
                for path in (suite.bd, suite.bv, suite.ralph)}
    (args.root / "binary-sha256.json").write_text(json.dumps(binaries, indent=2) + "\n")
    (args.root / "platform.json").write_text(json.dumps({"platform": platform.platform()}, indent=2) + "\n")
    if args.phase == "lifecycle":
        suite.lifecycle()
    else:
        suite.runners()


if __name__ == "__main__":
    main()
