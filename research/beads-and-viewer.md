---
description: Evidence for deciding what a Cook recipe adds to Beads and Beads Viewer for team adoption and personal use.
audience: Cook maintainers who know its recipe and ownership model and are evaluating tracker setup.
---

# Beads and Beads Viewer research

Research date: 2026-11-14. This is source research, not a record of executed runtime experiments.
The repository targets `bd` 1.3.1 and `bv` 0.25.2. Findings below identify those tags separately from
moving upstream `main`; a newer source implementation does not establish behavior in an installed release.

Beads supplies persistent tasks, dependencies and claims. Beads Viewer adds graph analysis and a human interface.
A Cook recipe earns its place through reviewed configuration, explicit connections and predictable removal across
repositories. Its value depends on whether those benefits exceed maintaining a second setup mechanism alongside
the tools' native ones. Team adoption and personal use need different publication contracts.

## Beads stores work and coordinates claims

`bd` is the Beads CLI. Its task graph retains context across agent sessions, exposes unblocked work through
`bd ready`, and claims tasks atomically with `bd update <id> --claim`. The normal lifecycle creates an issue,
records dependencies, claims it, and closes it when complete. Version 1.3.1 uses Dolt: embedded storage is the
default and permits one writer; server mode supports concurrent writers. Neither capability makes task
descriptions or acceptance criteria correct automatically.
([Beads 1.3.1 README](https://github.com/gastownhall/beads/blob/v1.3.1/README.md))

The local Dolt database holds authoritative task state. Cross-machine sharing uses `bd dolt push` and
`bd dolt pull`; a Git-hosted remote keeps that history under `refs/dolt/data`, separately from source branches.
Normal initialization can derive the Dolt remote from Git origin. A new clone needs `bd bootstrap` to retrieve
that history. Committing `.beads/config.yaml` helps another clone discover the intended remote; committing an
export is not equivalent to replicating the database. JSONL import cannot infer deletion from absent records.
([Sync concepts at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/docs/core-concepts/sync-concepts.md))

Worktrees of one repository share its Beads workspace by default. `BEADS_DIR` can select an external workspace,
including a planning repository shared by several code worktrees. Task changes therefore outlive an individual
code branch, and deleting that branch does not undo task changes. This is useful for session handoffs but needs
an explicit completion policy when a code branch is later abandoned.
([Worktrees at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/docs/reference/worktrees.md))

## Team adoption and personal use publish different things

For team adoption, the tracked configuration, instruction changes and agreed integration belong in review.
Beads keeps its local database directories ignored and writes its own `.beads/.gitignore`. It also supplies Git
hook installation, refresh, inspection and removal. Hooks preserve content outside Beads' markers and can chain
existing hooks. A Cook recipe should identify which program owns each hook rather than rewriting a competing
copy. Beads' hooks can refresh an export when configured; that does not publish Dolt history.
([Git integration at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/docs/reference/git-integration.md))

For personal use, `bd init --stealth` already supplies repository-local exclusions. Its implementation puts
`.beads/` and `.claude/settings.local.json` in `.git/info/exclude`, resolving the shared Git directory so linked
worktrees use the same exclusions. This avoids a shared `.gitignore` edit. Existing tracked files still need
separate treatment: an exclude rule cannot turn tracked configuration into private configuration.
([Stealth implementation at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/cmd/bd/init_stealth.go))

Initialization itself has material effects beyond a config template. In 1.3.1, normal `bd init` can install
agent integrations, stage files and create an initialization commit. Stealth skips the agent setup and normal
auto-commit path, sets `no-git-ops`, and skips discovering a Dolt remote from Git origin. Explicitly configured
sync settings remain a distinct input. The recipe's eventual after-cooking instructions must name these effects
and use flags appropriate to the intended ownership boundaries; this research does not prescribe an untested
initialization sequence.
([Initialization implementation at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/cmd/bd/init.go))

Uncommitted configuration and private task data are separate requirements. Version 1.3.1's `dolt.local-only`
setting gates Dolt push, pull and remote creation. `no-push` is narrower, and removing a Git ignore rule changes
neither setting. A personal mode should verify the effective route and sync policy, including inherited
configuration. An external planning directory selected through `BEADS_DIR` offers another separation; whether
its tasks sync to a private remote is a further user choice.
([Dolt commands at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/cmd/bd/dolt.go),
[external workspace documentation](https://github.com/gastownhall/beads/blob/v1.3.1/docs/reference/worktrees.md))

Beads configuration has several scopes: user YAML, repository YAML, an explicitly selected workspace, and a
machine-local override file. Flags and environment variables can override YAML. Some project settings live in
the database and travel with task history. Export refresh is disabled by default; when enabled it has an
interval, so “auto-export enabled” does not prove that the next viewer invocation sees a just-written change.
([Configuration at 1.3.1](https://github.com/gastownhall/beads/blob/v1.3.1/docs/reference/configuration.md))

## Native agent setup is the baseline for a Cook recipe

Current upstream `bd setup` already installs, checks and removes integrations. It manages instruction sections,
hooks and, for Codex, a shared agent skill. Its managed sections carry metadata used to report staleness, and
setup preserves surrounding instruction text. It also documents conservative versus team-maintainer policy
profiles. These current-main features should be compared against a pinned implementation before Cook promises
equivalent behavior. The useful Cook question is which additional team convention requires a reviewed,
versioned recipe, rather than how many existing native adapters can be copied.
([Current agent setup documentation](https://github.com/gastownhall/beads/blob/main/docs/getting-started/ide-setup.md))

## Beads Viewer explains the graph and recommends work

`bv` is Beads Viewer. Version 0.25.2 reads JSONL exports from `.beads/`, supports both `issues.jsonl` and legacy
names, and documents `bd export -o .beads/issues.jsonl` for Go Beads. It provides a terminal board and dependency
views; agents should use `--robot-*` commands. `--robot-triage` returns recommendations, `--robot-next` returns a
top candidate, and `--robot-plan` groups work into tracks. Analysis includes PageRank, bottlenecks, critical
paths and cycles. Recommendations describe a snapshot: they do not claim tasks, and tracker state must be
checked when claiming. Expensive metrics can be sampled, skipped or timed out, with status in the output.
([Viewer 0.25.2 README](https://github.com/Dicklesworthstone/beads_viewer/blob/v0.25.2/README.md))

Viewer scoring is a policy, not an objective proof of business importance. Current source combines dependency
centrality, blocker counts, staleness, assigned priority, time to impact, urgency and risk. The inference for
Cook is that installing `bv` does not improve a graph whose dependencies are missing or whose priorities are
wrong. A spike should test whether its recommendations change a realistic choice over `bd ready` on the
team's own work, rather than merely demonstrating that JSON is printed.
([Current scoring implementation](https://github.com/Dicklesworthstone/beads_viewer/blob/main/pkg/analysis/priority.go))

Current Viewer `main` documents automatic refresh for detected Dolt workspaces, bounded exporter execution,
validation before replacing an export, and retention of the preceding snapshot when refresh fails. Those
stronger safeguards must not be assumed for 0.25.2 without checking its source or running it. A stale export
can remain readable, so the integration needs a visible freshness/failure policy.
([Current Viewer export documentation](https://github.com/Dicklesworthstone/beads_viewer/blob/main/README.md#generating-the-jsonl-file-br-and-bd))

Current loader source supports `BEADS_DB`, `BEADS_DIR`, local discovery and discovery of the main Git
repository's workspace from a worktree. It also attaches tracker identity and routing to recommendations.
These rules matter when personal planning state lives outside the code checkout: both the tracker and Viewer
must resolve the same workspace. The existence of a `.beads` directory alone is insufficient evidence.
([Current Viewer loader](https://github.com/Dicklesworthstone/beads_viewer/blob/main/pkg/loader/loader.go))

## Beads Rust is a separate compatibility choice

`br` is Beads Rust, a separate implementation that uses SQLite and JSONL, explicitly avoids implicit Git
operations and uses `br sync --flush-only` to export. It is not another name for `bd`, and its storage and sync
contracts must not be inferred from the Go project's Dolt documentation. Viewer accepts exports from both, but
that does not establish interchangeable initialization, hooks, claims or ralph tracker plugins.
([Beads Rust upstream](https://github.com/Dicklesworthstone/beads_rust/blob/main/README.md))

## The useful spike proves the two publication contracts

Recommendation: test a small real dependency graph in a fresh team clone and a personal checkout, each with a
linked worktree. Record exactly which files, Git metadata, database state and remotes each native command changes.
Confirm that another team clone obtains tasks, that personal setup produces the agreed shared-file diff, and
that private tasks cannot reach the team remote under the selected policy. Mutate a task before running Viewer
to prove freshness, then fail export to observe the fallback. Verify that recooking and discarding recipe-owned
configuration preserve tasks and unrelated hooks. Run those cases on the repository's pins before considering
newer upstream behavior. The result should establish the smallest configuration Cook must own and the native
commands it can delegate to; it should also allow the conclusion that native setup is sufficient.

## Ralph already supplies the sharing precedent

The user's intended cases are a repository adopting Beads as its standard tracker and a contributor using
Beads personally. Ralph's existing `--share=team|clone` provides a starting point. Its clone mode ignores
the generated setup while committing `.gitignore` and `cook.lock`, so another clone can reproduce it.
The recipe also documents a personal `.git/info/exclude` override. In this checkout, that override already
excludes `.ralph-tui/`; its files are present but untracked, while `cook.lock` records the recipe.
Sources: [replicate contract](../docs/features/replicate.md),
[ralph after-cooking instructions](../cookbook/src/ralph/ralph-tui.groovy), and
`git check-ignore -v .ralph-tui/config.toml` (local exclude line 8).

The agreed scope uses recipe-owned `.gitignore` sections for personal and team setup. Cook's engine records
ownership in root `cook.lock`; that is not a recipe-level choice. Beads additionally needs an explicit
decision about where task history is shared. See the [ownership decision](../docs/adrs/0007-ownership.md).

The scope includes correcting ralph's after-cooking instructions and launcher diagnostic: both currently
recommend an unmanaged `.git/info/exclude` entry. Replace that advice with the owned `--share=clone` path.
Verify that changing sharing mode updates the ignore section and discarding removes it while preserving
unrelated rules. Existing manually added exclusions must be identified for user review, not silently deleted
as though Cook owned them. This checkout has such an exclusion.

## Current project inventory

At the start of this research, 16 files contained 220 matching lines for `beads?`, `bd`, or `bv`, case
insensitively. The search included hidden and ignored working files, excluding Git internals, worktree
copies, build outputs, dependency caches and this new research file. The table lists every matching file;
line numbers refer to the checkout inspected on 2026-11-14. Git history was examined separately below.

| File | Relevant lines | What it contains |
| --- | --- | --- |
| [Recipe](../cookbook/src/ralph/ralph-tui.groovy) | 25–322 | Tracker options, pinned versions, manual database prerequisite, paths, worktree and viewer limitations |
| [Model](../cookbook/src/ralph/RalphTui.groovy) | 82–530 | Tracker validation, prompt seeds, relative `beadsDir`, recook validation |
| [Launcher source](../cookbook/src/ralph/bin/run.sh) | 12–13, 186–190 | Selects epic tracker and sets `BEADS_DIR` for `beads-bv` when absent |
| [Beads prompt](../cookbook/src/ralph/templates/beads.hbs) | 2–54 | Task context; runner owns closing beads and committing |
| [Viewer prompt](../cookbook/src/ralph/templates/beads-bv.hbs) | 2–60 | Selection rationale and worker instructions |
| [Artifact README](../cookbook/src/ralph/README.md) | 1 | Beads as a task source for ralph |
| [Cookbook README](../cookbook/README.md) | 219–610 | Installation example and rendered ralph help |
| [Recipe tests](../cookbook/test/ralph/RalphTuiCookTest.groovy) | 23–1855 | Cooking, prompts, tracker conflicts, worktree paths, environment routing, recook and discard behavior |
| [Architecture](../docs/architecture.md) | 331 | Still names a `:beads` cookbook artifact, although its source was removed |
| [Recipe connections decision](../docs/adrs/0030-recipe-connections.md) | 25–26 | Native `bd setup` adapters support explicit recipe connections |
| [Agent instructions](../AGENTS.md) | 95 | Recorded ralph cook arguments include `--tracker=beads` |
| [Ignore rules](../.gitignore) | 27 | Same recorded arguments in the owned ralph section |
| [Ownership record](../cook.lock) | 101 | Beads and JSON tracker arguments |
| Local `.ralph-tui/config.toml` | 2 | `tracker = "beads"` |
| Local `.ralph-tui/bin/run` | 12–13, 186–190 | Generated launcher with the viewer routing logic |
| Local `.ralph-tui/templates/beads.hbs` | 2–54 | Seeded worker prompt |

The last three paths are ignored local files and will not exist in every clone. There is no `.beads/`
database in this checkout and no current `cookbook/src/beads/` or `cookbook/test/beads/` directory.
The architecture reference is an inconsistency to resolve when deciding the artifact's future;
this investigation has not changed the architecture.

## History contains a previous recipe and runtime findings

The standalone recipe was removed in `1ead12c9` ("remove old recipes"); `01193549` removed its stale
tests and other references. These local Git objects preserve the earlier design and evidence:

| Retrieve with `git show` | Evidence |
| --- | --- |
| `6a7f710e^:tasks/prd-ralph-beads.md` | Original issue #42 PRD: team and clone-local examples, spike requirements, greenfield-only scope |
| `6a7f710e^:tasks/prd-ralph-tui-matrix.md` | Issue #53 matrix: committed/ignored setup, Beads/JSON, serial/parallel and adoption cases |
| `ed249205^:tasks/prd-ralph-tui-findings.md` | Issue #87: ignored `.beads/` caused ralph `no_tasks`; relative `beadsDir` was the remedy |
| `ea310ee7^:tasks/prd-replicate.md` | Issue #71: ignored output recreated from committed recipe arguments |
| `1ead12c9^:cookbook/src/beads/tracker.groovy` | Last standalone recipe's API and manual follow-up steps |
| `1ead12c9^:cookbook/src/beads/Beads.groovy` | Ownership, ignore rules and prime seed behavior |
| `1ead12c9^:cookbook/src/beads/hooks/beads.sh` | Session hook suppressing errors from `bd prime` |
| `1ead12c9^:cookbook/src/beads/templates/PRIME-clone.template.md` | Clone-local workflow instructions |
| `1ead12c9^:cookbook/src/beads/templates/PRIME-team.template.md` | Shared workflow instructions |
| `1ead12c9^:cookbook/test/beads/BeadsTrackerCookTest.groovy` | File/ownership tests and fake `bd` execution |
| `73a90184^:cookbook/examples/beads-ralph-prd-codex-claude.md` | Earlier explicitly nonworking integration examples |

Commit `6ca186c0` folded spike observations into the original PRD. The PRDs directed raw runtime reports
to issues #42 and #53; they are not complete local execution transcripts. Viewer integration arrived in
`6da98e0e`; `484b7120` removed an unpopulated prompt variable; `00b109f0` fixed database discovery for runs.
These historical results identify cases worth retaining, but do not prove current releases pass them.

The removed recipe owned ignore rules, optional instruction sections and an optional session hook; it
seeded `.beads/PRIME.md`. Initialization, remotes and Git hook installation were manual. Its value was
reproducible policy and connections. Its limits were concrete:

- Changing `--share` changed ignore rules but retained the old prime seed, potentially leaving instructions
  that contradicted the selected mode. The tests deliberately preserved the edited seed.
- Clone mode still shared `PRIME.md`, root ignore rules and Cook's lock. It did not mean invisible personal adoption.
- Share selection did not establish or verify the effective Dolt remote, or untrack existing files.
- Discard removed owned configuration while leaving the prime seed and database. Manually installed hooks
  and remotes were outside Cook ownership.
- The session hook hid all `bd prime` errors. Tests used a fake `bd`; they did not establish real database,
  fresh-clone, sync, hook coexistence or upgrade behavior.

## Recommendation for the recipe decision

The [adoption PRD](../tasks/prd-beads-adoption.md) turns this research into spike and implementation stories,
including the agreed ralph cleanup. It proposes separate `#tracker` and optional `#viewer` recipes so their
configuration can be updated and discarded independently.

A small spike is justified by the two concrete adoption cases. Reuse ralph's sharing vocabulary and let
the engine manage its ownership record. Keep Viewer optional: it adds prioritization and
inspection, while Beads remains useful without it. Keep ralph integration optional too, because people
can manage tasks directly.

Measure native initialization, a second team clone, recipe-owned personal ignore rules and
sync policy, routing from a linked worktree, and Viewer freshness. Include an existing repository with
hooks and instructions, not just an empty repository. The recipe earns its maintenance cost if these
experiments expose repeatable configuration and connections that Cook can own safely. If only native
initialization remains, a documented native workflow may suffice.
