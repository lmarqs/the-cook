---
description: Runtime evidence and ownership inputs for Beads tracker, optional Viewer and ralph adoption.
audience: Cook maintainers who know recipe ownership and need concrete inputs for US-004 and US-005.
---

# Beads adoption runtime spike

`bd` 1.3.1 supports the agreed tracker ownership and sharing contract when initialization uses
`bd bootstrap --yes`, recipe-owned root ignore rules, and an explicit local-only policy for personal use.
Team publication requires a separate, deliberate `bd dolt push --yes`; its first adoption of Git origin
also commits `sync.remote` to the source repository. A second clone recovers the same task graph after
bootstrap. Both sharing modes resolve the main checkout's database from linked worktrees.

The tracker experiments cover US-001. The Viewer and ralph experiments below cover US-002; cooked output,
mode transitions and recipe discard remain acceptance work for later stories. No recipe or engine implementation
changed in this spike. The [source research](beads-and-viewer.md) remains separate from these runtime results.

## Environment and retained evidence

The experiments ran on 2026-11-14 with `/home/linuxbrew/.linuxbrew/bin/bd`, reporting
`bd version 1.3.1 (Homebrew)`, on Linux x86_64, WSL2 kernel `6.6.87.2-microsoft-standard-WSL2`.
Embedded Dolt worked without a separate server or Dolt CLI installation.

All repositories, Git remotes and task contents were synthetic. The scratch root is
`/tmp/the-cook-beads-spike-20261114`. Numbered `001.json` through `162.json` contain each experiment
command, working directory, exit status, output, text-file diffs, index contents, local Git configuration,
source commits, refs and discovered `.dolt` directories. Binary index changes are represented by hashes;
Dolt data and Git object contents are excluded from text diffs. The databases themselves are retained.
`spike.py`, `phase2.py` through `phase9.py`, and `verify.py` reproduce the experiments in that scratch root;
reruns need a fresh root because the scripts deliberately refuse to replace existing repositories.
A copy of the scripts and numbered evidence is retained under the ignored
`.superpowers/sdd/plan-beads-adoption/tracker-spike-evidence/` directory in the implementation worktree.
These paths describe retained local evidence, not files expected in another clone.

The recorder removed inherited `BD_*`, `BEADS_*` and `GIT_*` variables and used synthetic Git identities
(`Spike`, `spike@example.invalid`) and actor `spike`. Git system/global configuration was disabled for
these subprocesses. It set `BD_NON_INTERACTIVE=1`, `BD_METRICS_DISABLED=true` and `NO_COLOR=1`.
No home directory was redirected and no global configuration was changed. The existing user bd YAML had
only metrics keys; experiment YAML disabled metrics locally. Production credentials and remotes were unused.

## E1: Native init has effects the recipe must avoid

Commands 001–010 initialized an unborn repository and a repository with unrelated root ignore rules,
`AGENTS.md`, and an executable pre-commit hook:

```sh
bd init --prefix empty --non-interactive --skip-agents --skip-hooks
bd init --prefix existing --non-interactive --skip-agents --skip-hooks
bd dolt status
bd config get dolt.local-only
```

Both init commands succeeded and created `.beads/embeddeddolt`, `.beads/.gitignore`, `.beads/config.yaml`,
`.beads/metadata.json`, `.beads/README.md` and `.beads/.local_version`. They added `beads.role=maintainer`
to local Git configuration. Both created a source commit named `bd init: initialize beads issue tracking`.
The existing instruction and hook bytes survived, and the init commit bypassed the unrelated hook.
The root ignore diff was:

```diff
 unrelated-cache/
+
+# Beads / Dolt files (added by bd init)
+.dolt/
+*.db
+.beads-credential-key
+.beads/proxieddb/
+*.gate.lock*
```

`--skip-agents --skip-hooks` does not disable source staging or committing. Init stages `.beads/`,
then existing instruction/integration paths and root `.gitignore`; a dirty checkout can therefore contribute
unrelated changes. `no-git-ops` does not establish a general ban on native source commits.
The tagged [init implementation](https://github.com/gastownhall/beads/blob/v1.3.1/cmd/bd/init.go)
contains the staging and commit path separately from the agent/hook flags.

Commands 087–092 added synthetic `origin` and `upstream` remotes and predeclared every root ignore pattern.
Even this invocation appended unmanaged exclusions:

```sh
bd init --non-interactive --skip-agents --skip-hooks --role maintainer
```

```diff
+# Beads fork protection (bd init)
+.beads/
+**/RECOVERY*.md
+**/SESSION*.md
```

The full exclude-file diff is preserved in `092.json`. This fork behavior rules out normal init as the
recipe's required generic after-cooking path. `--stealth` is also unsuitable because its documented purpose
includes `.git/info/exclude` changes. None of those exclusions is necessary in the selected bootstrap path.

## E2: Code checkout and task bootstrap are separate operations

Commands 011–037 used `existing`, a local bare remote `team.git`, and a second clone `second`.
The successful graph construction and publication were:

```sh
git init --bare --initial-branch=main /tmp/the-cook-beads-spike-20261114/team.git
git remote add origin /tmp/the-cook-beads-spike-20261114/team.git
bd dolt remote add origin git+file:///tmp/the-cook-beads-spike-20261114/team.git
git push -u origin main
bd create 'Synthetic adoption' --type epic --id existing-epic --json
bd create 'Synthetic prerequisite' --id existing-first --json
bd dep add existing-first existing-epic --type parent-child
bd create 'Synthetic dependent' --id existing-next --json
bd dep add existing-next existing-epic --type parent-child
bd dep add existing-next existing-first
bd dolt push
git clone /tmp/the-cook-beads-spike-20261114/team.git /tmp/the-cook-beads-spike-20261114/second
# In second:
bd list --all --json
bd bootstrap --yes
bd show existing-epic existing-first existing-next --json
```

The initial `bd list` in `second` exited 1 with `no beads database found`. Bootstrap exited 0 and restored
all three IDs, both parent-child edges and the blocking edge. The graph was compared programmatically
between commands 028 and 033. Source branch checkout alone did not reconstruct tasks.
The remote received `refs/dolt/data` and `refs/heads/__dolt_remote_info__`, separately from `refs/heads/main`.
The tagged [sync concepts](https://github.com/gastownhall/beads/blob/v1.3.1/docs/core-concepts/sync-concepts.md)
identify that separate history and bootstrap operation.

Three exploratory failures are material for reproduction. `bd create` rejects combining `--id` and
`--parent`; explicit IDs require a later `bd dep add ... --type parent-child` (015–017).
A completely empty bare Git remote cannot accept Dolt publication before it has a source branch (019).
`bd dolt remote add` also writes `sync.remote` and creates a source commit named `bd: update sync.remote`
(013); a later manual commit consequently found nothing to commit (021).

## E3: Bootstrap works with owned root configuration in both modes

Commands 093–125 repeated team setup with recipe-style configuration and only shared config allowed through
root ignores. Commands 136–162 covered personal setup with both `origin` and `upstream`, followed by an unborn
repository with no remote. The selected root ignore bodies are:

```gitignore
# team
/.beads/*
!/.beads/config.yaml
.dolt/
*.db
.beads-credential-key
.beads/proxieddb/
*.gate.lock*
```

```gitignore
# clone
/.beads/
.dolt/
*.db
.beads-credential-key
.beads/proxieddb/
*.gate.lock*
```

The team allowlist shares configuration while excluding native metadata, instructions, databases, exports
and future local files by default. Clone mode excludes all `.beads` contents. The final wildcard matters:
bootstrap creates the root `.beads.gate.lock`; ignoring only `.beads/` left that runtime lock stageable
(131–132). Final complete rules kept it out (147, 161–162).

The configuration tested in `.beads/config.yaml` was:

```yaml
dolt:
  local-only: false # team; true for clone
  auto-push: false
no-git-ops: true
export:
  auto: false
  git-add: false
```

The experiments additionally set `metrics.disabled=true` for isolation and `custom.unrelated=preserve`
as preservation evidence; those two settings are not proposed recipe contributions. The tagged
[configuration reference](https://github.com/gastownhall/beads/blob/v1.3.1/docs/reference/configuration.md)
explains YAML scopes and startup settings. The five tested settings above are the bounded tracker contract;
Viewer can use explicit `bd export` without taking ownership of tracker settings.

With configuration present, the native initialization is:

```sh
bd bootstrap --yes
bd dolt status
```

Bootstrap succeeded in an unborn repository, a fresh repository with code origin but no published task
history, a repository whose origin had task history, and an existing workspace. It preserved root ignores,
unrelated instructions, hooks, their execution policy, and local excludes. It did not stage source files or
create a source commit. Existing-workspace bootstrap reported `Nothing to do` (128).
For a remote clone it created native `.beads/.gitignore` and `.beads/metadata.json`, and added `sync.remote`
to YAML while preserving the unrelated nested key. The native `.beads/.gitignore` is redundant with the
owned root rules and is not part of the recipe's required ignore configuration. Fresh local bootstrap did
not create that metadata file. Neither path installed hooks or agent integrations or set `beads.role`.

Commands 101–108 established `contract-team-epic`, `contract-team-first` and `contract-team-next`, with both
children attached to the epic and `next` blocked by `first`. Plain `bd dolt push` refused to adopt the Git
remote without consent. The corrected initial publication sequence (117–123) was:

```sh
# First publish the source branch to the intended Git origin, then review that origin.
bd dolt push --yes
git diff --check
git log -1 --oneline
git push origin main
# In a fresh code clone:
bd bootstrap --yes
bd show contract-team-epic contract-team-first contract-team-next --json
bd dolt remote list
```

`bd dolt push --yes` adopted the synthetic origin, wrote `sync.remote`, created source commit
`607daef` (`bd: update sync.remote`), and published Dolt history. This is a required disclosed effect of the
first publication command, even with `no-git-ops: true`. It reformatted YAML indentation but preserved keys.
No root ignore or hook changed. A fresh clone `contract-third` bootstrapped all three IDs and all three
edges. Normal `git add .` afterward left the team source tree clean (124–125).

These commands assume a reviewed, clean index and worktree before publication because native remote adoption
can create a source commit. Only use `--yes` after confirming the intended destination; it consents to
publishing the complete local issue history. Subsequent team sync uses `bd dolt push` / `bd dolt pull`.
Cooking itself performs none of these operations.

## E4: Personal staging and sync policy are independently demonstrated

The final `contract-personal` experiment started with unrelated instructions, ignores and hooks, code origin
`contract.git`, and upstream `team.git`. Its YAML selected `dolt.local-only: true` before bootstrap. Bootstrap
read the existing team history, adopted its task prefix and persisted `sync.remote` even in local-only mode.
The flag is a push/pull/remote-add policy, not a promise that initialization never reads origin history.
The native prefix, rather than the directory name, determined the private ID namespace after adoption.

The decisive commands (143–157) were:

```sh
bd bootstrap --yes
bd create 'Contract private task' --id contract-team-private --json
bd export -o .beads/issues.jsonl
git add .
git check-ignore -v .beads/config.yaml .beads/issues.jsonl .beads/embeddeddolt .beads.gate.lock
bd dolt push --yes
bd dolt pull
bd dolt remote add private-test git+file:///tmp/the-cook-beads-spike-20261114/contract.git
bd dolt status
# In independent team clone contract-third:
bd dolt pull
bd show contract-team-private --json
```

Normal staging left the personal index and source tree clean. The ignore report cited the root owned rules
for all four paths. Both push and pull exited 0 with explicit `Remote sync is disabled` messages. Remote add
exited 1 with `cannot add Dolt remote: remote sync is disabled`. A subsequent pull in the independent team
clone succeeded, but looking up `contract-team-private` exited 1 with `not found`. The final published task ref
was `6b6cf2b9e71937e72e1005b69607d9890954cccf` (157); the private task stayed local.

A separate personal run (044–060) showed the same sync gates with a pre-existing Dolt remote. Local-only
therefore does more than merely omit initial remote creation. The tagged
[Dolt command implementation](https://github.com/gastownhall/beads/blob/v1.3.1/cmd/bd/dolt.go)
contains those local-only gates.

Inherited configuration can override the privacy setting. In commands 084–086,
`env BD_DOLT_LOCAL_ONLY=false bd dolt status` removed the disabled-sync indication. A synthetic
`.beads/config.local.yaml` with `dolt.local-only: false` did the same; restoring `true` restored the disabled
indication. Those tests did not publish personal data. After cooking, inspect `bd dolt status` and resolve
any environment or machine-local override before creating personal tasks. An ignore rule alone proves
neither the effective sync policy nor whether previously tracked files remain tracked.

## E5: Linked worktrees share the main checkout's store

The successful team and personal commands were:

```sh
# In contract-team:
git worktree add -b contract-wt /tmp/the-cook-beads-spike-20261114/contract-wt
# In contract-wt:
bd dolt status
bd show contract-team-next --json
# In contract-personal:
git worktree add -b contract-personal-wt /tmp/the-cook-beads-spike-20261114/contract-personal-wt
# In contract-personal-wt:
bd dolt status
bd show contract-team-private --json
```

Team status resolved `contract-team/.beads/embeddeddolt`, and personal status resolved
`contract-personal/.beads/embeddeddolt` with sync disabled. Both returned the expected tasks, including the
private task in the personal worktree. The earlier init-based runs and fresh local bootstrap run agreed.
No linked worktree contained another physical `.dolt` database directory. No `BEADS_DIR` override or redirect
file was needed for these repository-local worktrees. External workspaces remain a US-002 concern.

## Ownership inputs for US-004

| Target | Owner | Kind | Recook / discard behavior |
| --- | --- | --- | --- |
| Root `.gitignore`, distinct tracker section | Tracker recipe | Owned section, mode-dependent | Replace only its body on mode change; remove its section on discard; preserve unrelated rules |
| `.beads/config.yaml`: `dolt.local-only` | Tracker recipe | Owned YAML key: team `false`, clone `true` | Update by mode and remove only owned key on discard; preserve other keys |
| `.beads/config.yaml`: `dolt.auto-push=false`, `no-git-ops=true`, `export.auto=false`, `export.git-add=false` | Tracker recipe | Owned YAML keys | Reconcile through engine ownership and remove only those keys on discard |
| Explicit instruction targets, distinct tracker section | Tracker recipe | Owned section, mode-dependent | Replace guidance on mode change; remove on discard; retain unrelated text |
| Root `cook.lock` | Cook engine | Engine-generated ownership record | Engine alone records operations and reconciles them; no additional ledger |
| `.beads/config.yaml`: `sync.remote` | Native setup / repository owner | Native-created shared configuration | Preserve across recook/discard; destination changes require explicit user review |
| `.beads/metadata.json`, `.beads/.gitignore`, native README/version files | Native Beads | Runtime/setup files, ignored by root rules | Neither claim ownership nor delete them during discard |
| `.beads/embeddeddolt/`, backup/export/lock files, `.beads.gate.lock` | Native Beads / user | Runtime task data and local state | Preserve across recook, transitions and discard |
| Existing Git hooks, `core.hooksPath`, `.git/info/exclude` | User / native tools | Existing local configuration | Preserve; selected bootstrap requires no new hooks or local exclusions |
| Any future team-owned seed | Team | No seed needed by this spike | Do not invent mode-dependent seed guidance; preserve any existing team seed |

The fixed YAML keys and ignore patterns above are measured inputs, not a new engine contract. Existing
YAML uses normal Cook existing-file semantics. Preserve native `sync.remote` and arbitrary unrelated keys.
Team mode commits only the shared config and owned setup; personal mode leaves native tracker files and
exports local. Before changing modes, inspect tracked files and effective native configuration. Neither
ignore changes nor removing owned YAML keys should be described as task deletion or remote-history rollback.
Discarding clone configuration also removes its owned local-only gate while preserving any native remote;
afterward, `bd` can synchronize again. Help must disclose that consequence before users discard or switch mode.

## Known limits and checks

Fresh local bootstrap prints `no beads configuration found ... using default database name "beads"` because
it omits native metadata. Commands still create and read tasks successfully. Bootstrap also leaves
`beads.role` unset, so writes can suggest `git config beads.role maintainer` or `contributor`; selecting that
user-specific role is not necessary to share the measured graph and is not recipe-owned configuration.
Cloned `.beads` directories can have mode 0755 and trigger a recommendation to run `chmod 700 .beads`.
These warnings are recorded rather than hidden or repaired with an invented metadata seed.

The spike proved embedded mode and local Git transport. It does not establish server-mode concurrency,
remote authentication, other operating systems, configuration overrides not exercised here, or final recipe
behavior. Viewer and ralph observations follow in E6–E9. It intentionally leaves tasks and native remotes intact.

`verify.py` passed assertions comparing the restored graphs, personal staging and publication results,
worktree routing, absence of duplicate worktree stores, unchanged hooks/instructions/excludes, preservation
of unrelated YAML, and configuration override behavior. Its first run incorrectly searched native ignore
comments for an exclude-file mutation; after matching the diff's file header instead, the assertions passed.
The reported failure was in the spike verifier, not a failed privacy or sharing scenario.

Repository checks:

- Quick check: controller ran `./gradlew -p cookbook check` successfully before this spike; this task changes
  research and task documents, with no changed code module.
- Done check: `./gradlew check cliBuild jpackageTest` passed in 50 seconds (69 actionable tasks). Its first
  attempt stopped on an extra trailing blank line in the copied ignored `task-1-brief.md`; trimming that
  scratch-file whitespace fixed the gate. No test, assertion, warning rule or lint configuration was weakened.
- `git diff --check` passed. Both new/modified evidence documents also produced no whitespace diagnostics
  under `git diff --no-index --check /dev/null <file>` (exit 1 denotes the expected content difference).

The tracker inputs above are the US-004 implementation contract. Viewer and ralph findings below supply the
US-005 contract; cooked transitions and discard remain the later acceptance stories.


## E6: Viewer needs an explicit route when a worktree has local configuration

The US-002 experiments use the same `bd` binary, `bv v0.25.2`, and `ralph-tui 0.12.0` on the Linux platform
above. Viewer resolves to `/home/linuxbrew/.linuxbrew/Cellar/bv/0.25.2/bin/bv`; `go version -m` reports
module version `v0.25.2`, clean Git revision `721353e7f9ec69324990f8ba3dbc2ef03a5342ff`, and revision date
2026-10-01. Its SHA-256 is `6e50698db7bd73c469a25e027911929af767dd467242a335770871b83c0aa846`.
The installed ralph bundle SHA-256 is `44bd1d4c721ffb6885cd121014e86d0c4c5eab01d3d7ea4510b233304b934242`.
These are observations of the installed pinned binaries, not assumptions from Viewer main-branch documentation.

Evidence is in the separate synthetic subtree `/tmp/the-cook-beads-spike-20261114/viewer-runtime/`:
`runtime.py`, `phase2.py` through `phase6.py`, numbered command records, failure snapshots, transparent native
command traces and a fake-worker trace. The scripts retain the executed probes: `phase3.py` includes the
sandbox-denied attempts and `phase4.py` the approved reruns. Replay requires a fresh subtree, updating all
fixed scratch paths together, and accounting for the native session-registry write described in E9.
Every command record includes argv, working directory, explicit
routing overrides, status and both output streams. Credential-shaped environment variables were excluded;
no home directory, real agent command or production remote was substituted.

`viewteam` and `viewclone` each contain an epic, independent P1 task, P2 prerequisite and three tasks blocked
by that prerequisite. Main-checkout and worktree `bd list` return the same six IDs (024/027 and 062/065).
Their root configuration follows E3. The team worktree has tracked `.beads/config.yaml`; the clone worktree
has no `.beads` directory because its tracker configuration is local.

A main-checkout `bv --robot-next` automatically generated missing `.beads/issues.jsonl` from the live Dolt
store (026, 064), despite `export.auto=false` in tracker YAML. The installed Viewer runs its own exporter.
The clone worktree discovered the main directory and did the same (067). The team worktree instead selected
its local `.beads` directory and exited 1 with `no beads JSONL file found` (029, 034–036). A present local
configuration directory can therefore prevent the main-store fallback. Explicit `BEADS_DIR` fixed the team
worktree and refreshed a changed title and priority (040). The clone worktree also showed the changed title
from the main store (077–080). There was no second physical task database in either linked worktree.

External routing was exercised in both directions from each main checkout and worktree: `BEADS_DIR` selected
the other synthetic store, and tracker IDs, Viewer graph nodes, `source_path` and recommendation IDs all
followed that selection (095–106). A conflicting `BEADS_DB` overrode `BEADS_DIR` for Viewer (107); explicit
`bv --db <directory>` overrode both (108). Guidance must resolve one intended directory and use it for both
tracker export and Viewer; the working directory alone is insufficient evidence of the selected task source.

The reusable refresh sequence, from either checkout or linked worktree, is:

```sh
beads_dir="${BEADS_DIR:-$(dirname -- "$(git rev-parse --path-format=absolute --git-common-dir)")/.beads}"
bd --db "$beads_dir" export -o "$beads_dir/issues.jsonl" &&
  bv --db "$beads_dir" --robot-triage
```

Use an absolute `BEADS_DIR` when selecting an external store or invoking the sequence from a different
working directory. The explicit `--db` forms were exercised against both local and external stores (155–164).
For a person, replace `--robot-triage` with no robot flag to enter the human TUI. The spike measures robot
output; it does not claim interactive TUI behavior was exercised. `--robot-next` provides a compact single
pick, `--robot-plan` a dependency plan, and `--robot-triage` the broader scoring context.

## E7: Failed refresh can succeed with stale diagnostic output

A wrapper substituted only the failure path: `bd export` exited 73 with a synthetic diagnostic, and all
other `bd` calls delegated to the real binary. Native tracker mutations and all successful compatibility
runs used real pinned tools. Each mode first exported a valid snapshot, then changed its prerequisite title
without exporting. With the failing exporter, Viewer from main and worktree returned exit 0 while preserving
the preceding export bytes (082–083 and 089–090). The old title remained in `diagnostic_top_pick`; live
`bd show` returned the new title (084, 091).

The robot response exposed the failure through these fields:

| Field | Measured stale fallback value |
| --- | --- |
| `source_authority.state` | `partial` |
| `source_authority.claim_safe` | `false` |
| `source_authority.readiness` | `provisional` |
| `source_authority.sources[].stale` | `true` |
| `source_authority.sources[].warnings[]` | `bd export failed, using existing issues.jsonl: exit status 73: synthetic export failure 73` |
| `actionable` | `false` |
| `message` | `No claim command emitted because source authority is incomplete or stale` |
| `diagnostic_top_pick` | Prior snapshot's ranking; no top-level actionable `id` |

Removing the export and failing refresh instead produced exit 1, `source_authority.state=unknown`, a failed
source and a `failed to refresh bd compatibility JSONL` error (087, 094). Restoring the real exporter and
running the explicit refresh sequence recovered the new title (085–086, 092–093). Check the export exit status
before presenting a recommendation as current. Viewer exit 0 alone does not establish freshness; inspect
source authority and stale warnings whenever consuming its automatic refresh. The owned guidance must expose
those diagnostics and require a successful refresh before describing a pick as verified current work.

## E8: Viewer ranks dependency impact but does not claim tasks

`bd ready --parent <epic> --json` lists the independent P1 task before the P2 prerequisite (025, 063).
Viewer triage ranks the prerequisite first at about 0.454 because it unblocks three downstream tasks and has
higher PageRank, while independent P1 scores about 0.140 (032, 070). Viewer graph output contains the same
six task IDs and the parent-child/blocking edges from the selected export. That ranking adds graph context
to the native ready order; it is a policy recommendation, not proof of business priority.

Repeated Viewer calls left task status `open` and assignee unset. A generated `claim_command` is an offered
mutation, not a reservation. A person can claim with `bd update <id> --claim` after checking current tracker
state and choosing the task. Worker restrictions in ralph prompts remain runner-specific.

Native metadata affects the robot response. A fresh local `bd bootstrap --yes` creates a functioning store
without `.beads/metadata.json` (E3). Viewer can rank its export, but `--robot-next` returns
`actionable:false`, `diagnostic_top_pick`, and `No claim command emitted: source has no readable tracker metadata`
(026, 064). Ralph's `beads-bv` plugin treats a response with `message` as a reason to use `bd ready` instead.
Do not synthesize a recipe-owned metadata file to force a different robot shape.

A separate native path published only synthetic task history to local `viewer.git`, cloned the code, and ran
`bd bootstrap --yes`. Bootstrap created native metadata for both a team clone and a clone whose local-only
policy was set before bootstrap (109–123). The same Viewer then returned top-level `id`, `actionable:true`
and explicit `bd` show/claim routing to that clone's store (117, 122). Both fresh-store diagnostics and
metadata-backed recommendations are valid measured results; instructions must disclose the difference.

## E9: Real ralph trackers select different tasks and own completion

The controller executed `python3 /tmp/the-cook-beads-spike-20261114/viewer-runtime/phase4.py` with filesystem
approval after the sandbox refused ralph's native global session registry write (127, 134). The installed
ralph hardcodes `~/.config/ralph-tui/sessions.json` and `sessions.lock`; it does not honor `XDG_CONFIG_HOME`
for that registry. The approved run added two synthetic entries and verified every pre-existing registry
entry was unchanged (`registry-verification.json`). No global agent plugin or home-directory override was used.

Each run used a fresh linked worktree, native `BEADS_DIR` pointing to its main store, and a relative native
`trackerOptions.beadsDir` pointing to that same directory, as the existing ralph recipe does. Configuration
set `autoCommit=false`, one iteration and an agent entry named `fake` with `plugin="claude"` and
`command="/tmp/the-cook-beads-spike-20261114/viewer-runtime/fake-worker.py"`. That local script printed a
synthetic version for detection, captured its synthetic prompt, emitted completion JSON and exited. It made
no tracker calls, wrote no product code, used no AI credentials and launched no real agent binary.

The common command was:

```sh
ralph-tui run --epic viewteam-epic --tracker <beads-or-beads-bv> --agent fake \
  --iterations 1 --serial --headless --no-setup --no-notify --no-sandbox \
  --prompt <repository>/cookbook/src/ralph/templates/<tracker>.hbs
```

The real plain `beads` tracker in `runnerteam-run` selected `viewteam-quick` and closed it (139). The real
`beads-bv` tracker in `runnerclone-run` selected `viewteam-gate`, passed the three-unblocks/centrality reasons
to the fake worker, and closed it (151). Both exited 0 after exactly one iteration and reported one completed
task. Native `bd show` confirmed the selected task `closed` with reason `Completed by agent`; the other ready
task remained open (145, 152). Closing the prerequisite made its three dependents ready (153).

Transparent trace wrappers logged argv, PID and parent PID then executed the actual binaries unchanged.
`tracker-trace.jsonl` shows ralph calling `bd update <selected> --status in_progress`, then calling
`bd close <selected> --force --reason 'Completed by agent'` after fake-worker output. Those mutation calls
have the runner's parent PID, while `worker-trace.jsonl` proves the worker received the selected task and the
recipe's rule that the worker never runs `bd`. Viewer calls during `beads-bv` invoked real `bd export` against
the clone's main store. Selecting or ranking a task alone did not perform a native claim.

Ralph also invokes the legacy `bd sync --flush-only` path. That command is unsupported by `bd` 1.3.1
(exit 1 in 170, 176); the installed runner still selected and closed the correct task, and Viewer performed
its measured explicit native export. Do not describe that legacy sync call as a successful refresh.

Ralph's final session summary said `INTERRUPTED` after logging `max_iterations`, despite exit 0 and verified
tracker completion. This is a measured runner summary quirk, not evidence that the epic is complete. The
spike proves one serial iteration per tracker; it does not claim an entire epic, automatic commits, parallel
execution or the production launcher was exercised. Fresh local stores without native metadata still take
the basic fallback described in E8; metadata-backed native clones demonstrate actual graph selection.

## Ownership inputs for US-005

Viewer has useful independent contributions: local export ignores and reusable guidance for a measured
routing/freshness failure. No native Viewer YAML setting, workspace aggregation file, hook or persistent
environment setting was required by these experiments.

| Target / connection | Owner | Kind | Recook / discard behavior |
| --- | --- | --- | --- |
| Root `.gitignore`, distinct Viewer section containing `/.beads/issues.jsonl` | Viewer recipe | Owned section | Reconcile/remove only Viewer section; preserve tracker's broader rules and unrelated patterns |
| Explicit `--instructions=<path>` targets, distinct Viewer section | Viewer recipe | Owned section, repeatable parameter, default `[]` like ralph/git recipes | Replace only Viewer guidance on recook; remove it on discard |
| Native `BEADS_DIR`, `bv --db <directory>` and `bd --db <directory>` | User invocation | Runtime connection | Resolve one intended store; write no persistent environment configuration |
| `.beads/issues.jsonl` | Native exporter / user | Runtime compatibility snapshot | Preserve on recook/discard; contents never become recipe-owned |
| `.beads/config.yaml`, `.beads/metadata.json` and task database | Tracker recipe / native setup / user | Existing respective ownership | Viewer neither claims nor changes their ownership |
| `cook.lock` | Cook engine | Engine-generated ownership | Record Viewer operations independently; recipe does not edit it |

The standalone Viewer ignore is useful for existing native Beads users: the native `.beads/.gitignore`
observed in E1 does not ignore root `issues.jsonl`. With Cook's tracker, the same path is already covered by
tracker rules, but the sections have independent owners and remain composable. External exports are written
to the selected store, outside the cooking repository; local root ignore rules are not a privacy policy for
that external repository. No extra recipe flag or manufactured cache path is needed.


## US-002 verification and remaining acceptance work

`viewer-runtime/verify.py` passed 48 evidence assertions covering graph IDs and edges, main/worktree parity,
external selection, Viewer-versus-ready ranking, stale bytes and authority fields, recovery, native metadata,
both serial selections and runner-owned closures. The full raw evidence and scripts were copied to the ignored
`.superpowers/sdd/plan-beads-adoption/viewer-spike-evidence/` directory for local review. Numbered probes
155–164 and 167–178 retain sequential route/final-state observations after an earlier concurrent recorder
number overlap; the decisive ralph runs remain 139 and 151.

Task 2 repository checks passed: `./gradlew -p docs check`, `./gradlew check cliBuild jpackageTest`
(69 actionable tasks, 11 executed and 58 up-to-date), and `git diff --check`. Gradle emitted
`Caught exception: Already watching path: .../.claude/skills/docs-layout` during Spotless but returned
`BUILD SUCCESSFUL`; no rule or warning was suppressed. No product module, recipe output or `cook.lock` changed.

US-005 now has concrete instruction targets, the owned export-ignore path, native connection/refresh commands,
failure diagnostics and the metadata limitation. Implement those bounded contributions; the cooked-output,
transition, removal and final launcher acceptance scenarios remain US-004 through US-007 work.
