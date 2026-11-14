---
description: Runtime evidence and the native configuration contract for Beads tracker adoption under bd 1.3.1.
audience: Cook maintainers who know recipe ownership and need concrete inputs for US-004.
---

# Beads tracker adoption runtime spike

`bd` 1.3.1 supports the agreed tracker ownership and sharing contract when initialization uses
`bd bootstrap --yes`, recipe-owned root ignore rules, and an explicit local-only policy for personal use.
Team publication requires a separate, deliberate `bd dolt push --yes`; its first adoption of Git origin
also commits `sync.remote` to the source repository. A second clone recovers the same task graph after
bootstrap. Both sharing modes resolve the main checkout's database from linked worktrees.

These observations cover US-001 only. Viewer, ralph runtime compatibility, cooked output, mode transitions
and actual recipe discard remain acceptance work for later stories. No recipe or engine implementation
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
remote authentication, other operating systems, configuration overrides not exercised here, Viewer or ralph
compatibility, or final recipe behavior. It intentionally leaves tasks and native remotes intact.

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

The tracker inputs are ready for US-004 implementation. Keep native initialization/publication separate from
cooking, and verify cooked transitions and discard in the later acceptance stories.
