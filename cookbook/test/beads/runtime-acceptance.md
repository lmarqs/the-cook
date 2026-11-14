---
description: Repeat cooked Beads adoption, removal and native Ralph acceptance with disposable local repositories.
audience: Cookbook maintainers who know Cook and Git and have installed the pinned native tools.
---
# Repeat native Beads acceptance

Run `runtime_acceptance.py` explicitly to exercise the cooked recipes with native tools. Gradle's normal tests
never execute this script, install these tools or contact these remotes. The harness uses synthetic tasks and
creates source and task remotes only inside a new `/tmp` directory. It retains repositories and command evidence
for review. The [spike report](../../../research/beads-adoption-spike.md#cooked-adoption-acceptance) records results.

## Install the prerequisites separately

Use Linux with Python 3.9 or newer, Git, Java 17 and a built Cook JVM CLI (`./gradlew cliBuild`). Install and put on
`PATH` the tested native pins: `bd` 1.3.1, `bv` 0.25.2, `ralph-tui` 0.12.0, and Bun for the installed Ralph bundle.
Embedded Dolt requires no separate Dolt binary or server. The tracker and Viewer recipes' rendered help includes
their pinned installation steps; Ralph's package is `ralph-tui@0.12.0`. The harness checks actual versions and records
binary SHA-256 values before starting. It performs no installation and needs no AI credentials.

The harness isolates Git configuration with `GIT_CONFIG_GLOBAL=/dev/null` and `GIT_CONFIG_NOSYSTEM=1`, supplies an
`example.invalid` Git identity, and removes tracker routing and credential-shaped environment variables. It leaves
`HOME` and user tool settings unchanged. Native Beads warnings remain in command records.

## Run the lifecycle before the runner

From the repository root, choose a path that does not exist:

```sh
python3 cookbook/test/beads/runtime_acceptance.py lifecycle /tmp/the-cook-beads-acceptance
python3 cookbook/test/beads/runtime_acceptance.py runners /tmp/the-cook-beads-acceptance
```

The first phase cooks through the actual built CLI, using this checkout's cookbook source directory as the only
repository in each fixture's `cook.toml`. Native command guards prove that cooking does not invoke native tools.
It follows the final after-cooking bootstrap, publication and explicit Viewer refresh commands, retaining their
output and the CLI's final guidance. A no-op cook reports `Already cooked.`; its installed guidance is checked.

The second phase executes each cooked `.ralph-tui/bin/run` launcher with one serial iteration and a synthetic
worker. The worker emits completion and records its prompt; it never calls a tracker, changes product code or
launches a real agent. The actual native runner selects and closes tasks. Native trace wrappers execute the pinned
`bd` and `bv` binaries unchanged. JSON runs through the cooked launcher with `bd` and `bv` absent from `PATH`.

Ralph 0.12.0 writes its session registry to `~/.config/ralph-tui/sessions.json` and `sessions.lock`, even when the
repository is disposable. A sandbox may require explicit filesystem approval for the second command. Do not
change `HOME`, replace the registry, or reroute user sessions. The harness keeps prior registry entries in memory
only and writes their preservation boolean and entry counts to `registry-verification.json`; it retains the
synthetic sessions. If approval is refused, report the runner phase as blocked rather than passing it.

## Review the matrix and evidence

| Scenario | Evidence and required result |
| --- | --- |
| Empty team and clone | Tracker alone, native bootstrap and graph, then Viewer; cook creates no database |
| Existing team and clone | Restore history first; preserve hooks, excludes, instructions, YAML and seed |
| Repeated cooking | Byte-identical configuration, instructions and engine lock; `cook check` exits 0 |
| Team onboarding | Clean source index, local publication, second code clone bootstrap; identical graph |
| Clone privacy | Disabled sync before writes; push/pull skip, remote add fails, remote refs stay unchanged |
| Native overrides | Environment and config.local.yaml override policy; inspect and restore effective status |
| Linked worktrees | Tracker and exact Viewer refresh sequence resolve main store; no second physical database |
| Viewer freshness | Generate missing export; expose stale authority; missing export plus failure exits 1; recover |
| Mode transitions | Preserve graph and native content; deliberately untrack config with `git rm --cached` |
| Ordered removal | Viewer, then tracker ownership removed; native data, seeds and unrelated text remain |
| Native runners | Team/clone × beads/beads-bv; cooked launcher, correct store/task, runner-owned closure |
| JSON independence | Cooked launcher completes JSON story while both `bd` and `bv` are absent from PATH |

`command-*.json` contains argv, synthetic working directory, explicit environment overrides, exit status, both output
streams and duration. Lifecycle snapshots retain synthetic Git status, index, local configuration, refs, recent
commits and diffs. `assertions.json`, `lifecycle-complete.json` and `runners-complete.json` record completed checks.
The runner phase starts a separate assertion list; the lifecycle count remains in its completion file. Review
`tracker-trace.jsonl`, `worker-trace.jsonl` and registry verification alongside the native command output.

Changing mode does not untrack files, reverse published task history or change a preserved remote. The harness
performs the documented explicit index removal only in disposable fixtures. Discard removes the clone local-only
key and can enable a preserved remote again; inspect effective policy before further writes. Existing manual
`.git/info/exclude` entries remain user-owned and are preserved, including when origin and upstream both exist.

Keep failed-run evidence and use another new `/tmp` path for a complete replay. The two completion files, native
task state and preservation assertions are required before reporting acceptance. A successful runner exit alone
does not establish task completion or freshness.
