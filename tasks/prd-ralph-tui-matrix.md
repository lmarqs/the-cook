# PRD: ralph#ralph-tui across setups, trackers, harnesses and brownfield repositories

## Overview

`io.github.lmarqs.cookbook:ralph:4.2.0#ralph-tui` assumes one shape: a team that commits `.ralph-tui/`, runs Beads or
a PRD serially, and never looks at what its runs commit. A stress test over personal and team setups, Beads and
`prd.json`, Claude and Codex, committed and ignored tracker data, serial and parallel runs, and greenfield and
brownfield repositories found cells that fail, some silently. medprev-rest-api's field report, checked against its
source at ab57e679c9, added real brownfield evidence: `bin/clean` deletes other runs' parallel worktrees with their
uncommitted files, cannot recognise a cherry-picked delivery, and ralph-tui 0.12.0 completes a story on the agent's
`<promise>COMPLETE</promise>` whatever its checks say.

This feature fixes `bin/clean`, documents how a team gates completion, lets a team narrow the recipe to the trackers
it uses and choose where run worktrees go, and states every other trap as one line of the help. The recipe stays small: what it does not fix, it names.

Facts the stories rely on, from ralph-tui 0.12.0 (`dist/cli.js` of the npm package) and bd 1.3.1:

| # | Fact | Where |
| --- | --- | --- |
| F1 | ralph-tui reads `.ralph-tui/config.toml` only from its cwd and that directory's parent | `findProjectConfigPath` |
| F2 | The Claude plugin adds `--dangerously-skip-permissions` unless the entry's `[agents.options]` sets `skipPermissions = false`; an entry's top-level `skipPermissions` is dropped silently | `AgentPluginConfigSchema`, Claude plugin `initialize` |
| F3 | Tracker: `--tracker` > `tracker` > `defaultTracker` > built-in `beads-bv`; `--prd` switches to `json` only without `--tracker` | config resolution |
| F4 | Prompt: `--prompt` > `prompt_template` > `<cwd>/.ralph-tui/templates/<type>.hbs` > user templates > built-in; parallel workers inherit it | `promptTemplate` resolution |
| F5 | Built-in Beads prompts tell workers to run `bd close`, `br close` and `br sync` | built-in templates |
| F6 | Serial: the task is completed, then, with `autoCommit = true`, auto-commit runs `git add -A`. Parallel: the task is completed after the merge and nothing commits it | engine `runIteration`, parallel merge |
| F7 | Beads trackers need `<cwd>/.beads`; bd in a worktree uses the main checkout's database and honours `BEADS_DIR`; ralph-tui calls `bd sync --flush-only`, which bd 1.3.1 lacks | Beads tracker plugins, bd 1.3.1 |
| F8 | `run` matches flags exactly, so `--epic=x` is not parsed; it takes a repeated `--epic` and `--epics a,b` | `run` argument parser |
| F9 | Parallel workers live at `<dir>/.ralph-worktrees/<base>/worker-<id>` for a run at `<dir>/<base>`, as `.worktrees/ralph/.ralph-worktrees/<name>/worker-<id>` for a run at `.worktrees/ralph/<name>` whose `<name>` has no `/`, unless `[parallel] worktreeDir` is set, on `ralph-parallel/<session>[/<scope>]/<task>` branches; only `config.toml` is copied in, no setup runs, and ralph-tui force-removes a worker path before reuse. The `.ralph-tui/worktrees` default in `DEFAULT_PARALLEL_CONFIG` never applies: `run` passes `worktreeDir: undefined`, which overrides it | `getWorktreeBaseDir`, `WorktreeManager`, `ParallelExecutor`, `copyConfig`, `cleanupStaleWorktree` |
| F10 | `<promise>COMPLETE</promise>` in the agent's final stdout completes and commits the task at any exit code | `PROMISE_COMPLETE_PATTERN` |
| F11 | `rate[- ]limit`, 429, `too many requests`, `quota exceeded` or `overloaded` on stderr makes the iteration rate-limited at any exit code; with exit 1, 2 or 429, so do `throttl`, `limit…exceeded`, `exceeded…limit`, `capacity` and `backoff` | `RateLimitDetector.detect`, `looseRateLimitCheck` |
| F12 | `ralph-tui setup` writes `agentOptions = {}` and never writes `prompt_template` | setup wizard |
| F13 | Under `workspace-write`, `.git` is read-only to Codex, and `/tmp` is writable by default, so scratch repositories under `/tmp` give false passes | the-cook #42 US-002 |
| F14 | `bin/clean` removes every `ralph-parallel/*` worktree in the clone with `--force`, for any `<name>`, and checks only its own worktree for changes | `cookbook/ralph/bin/clean.sh` |
| F15 | `bin/clean` counts a branch as delivered only when `git rev-list --count HEAD..ralph/<name>` is 0 | `cookbook/ralph/bin/clean.sh` |

Decisions already made by the maintainer:

- Simplicity first: a trap the recipe can name in one line gets a Trap line, not launcher or cook-time code. Real-world
  feedback (medprev) decides what gets code; the stress test alone does not.
- The completion gate is project code; the artifact's README gives the contract and pseudo code, and #39's "no
  generated launcher" stands.
- Each run picks its tracker with `--epic` or `--prd`; a cook-time `--tracker` only narrows what is seeded and offered.
- Brownfield never touches what the recipe did not create.

## Goals

- `.ralph-tui/bin/clean <name>` never removes a worktree or branch that run `<name>` did not create, and never removes
  uncommitted changes without `--force`.
- A run branch delivered by merge, rebase or cherry-pick is cleaned without `--force`.
- A team can write its own completion gate from the artifact's README without reading ralph-tui's source.
- A team that uses only JSON PRDs, or only Beads, is seeded and offered only that tracker.
- A team that keeps worktrees outside `.worktrees/` puts run worktrees there with the parameter
  `io.github.lmarqs.cookbook:agents#connect` already names, `--worktree-root`.
- Every trap a team can hit in ralph-tui 0.12, from medprev's field report or the stress test, is one line in the
  recipe's Traps, and the recipe adds no code for it.

## Quality Gates

These commands must pass for every user story:

- `./gradlew check cliBuild jpackageTest` - tests, Spotless, CodeNarc, the CLI build and the packaged-binary test
- `git diff --check` - no whitespace errors

## User Stories

### US-001: bin/clean removes only its own run's parallel worktrees

**Description:** As a developer with several runs or an earlier runner's worktrees in one clone, I want
`.ralph-tui/bin/clean <name>` to remove only what run `<name>` created, so other work is never lost.

**Acceptance Criteria:**

- [x] A test in `core/src/test/groovy/thecook/RalphTuiCookTest.groovy` fails on the 4.2.0 `clean.sh`: it creates a
  `ralph-parallel/*` worktree outside `.worktrees/ralph/.ralph-worktrees/<name>/` with an uncommitted file, runs
  `bin/clean <name>`, and asserts that worktree, its file and its branch remain.
- [x] `cookbook/ralph/bin/clean.sh` removes only worktrees whose path is under the run's F9 directory,
  `.worktrees/ralph/.ralph-worktrees/<name>/` for a `<name>` without `/`, and their branches; it never lists or
  removes others, with or without `--force`.
- [x] Without `--force`, a worktree under that directory with uncommitted changes makes `clean` exit 1 before removing
  anything, naming the worktree and that `--force` deletes them.
- [x] With `--force`, those worktrees are removed and each removal is printed, as today.
- [x] Without `--force`, a worker branch with a commit that neither `HEAD` nor `origin/HEAD` has, as when ralph-tui
  could not merge it into `ralph/<name>`, makes `clean` exit 1, naming the branch and the commit, as US-002 does for
  `ralph/<name>`.
- [x] `cleanRemovesADeliveredRunAndEveryParallelWorktree`, now
  `cleanRemovesADeliveredRunAndTheParallelWorktreesRalphTuiMadeForIt`, placed its `ralph-parallel/*` worktrees under
  `.worktrees/ralph/demo/.ralph-tui/worktrees/`, a path ralph-tui never uses (F9), and asserts `clean` removes them;
  it encodes the defect. It is rewritten to place them at F9's path, and the foreign case moves to the new test. Every
  other `RalphTuiCookTest` test passes unchanged.

### US-002: bin/clean recognises a cherry-picked or rebased delivery

**Description:** As a team that delivers run branches by cherry-pick or rebase, I want `bin/clean` to recognise them as
delivered, so `--force` and its skipped dirty check are not the norm.

**Acceptance Criteria:**

- [x] `clean.sh` counts `ralph/<name>` as delivered when no commit is listed with `+` by both
  `git cherry HEAD ralph/<name>` and, when it resolves, `git cherry refs/remotes/origin/HEAD ralph/<name>`; a
  `git cherry` error stops it.
- [x] A merge commit counts when its result differs from Git's own merge (`git show --remerge-diff` prints a
  change), as a conflict ralph-tui resolved does: a rebase or cherry-pick that drops it is refused; merging delivers it.
- [x] Its refusal names the commits that neither `HEAD` nor `origin/HEAD` has, and only those, and says a squash
  merge, or a cherry-pick that resolved conflicts, still needs `--force`.
- [x] Tests: a cherry-picked branch is cleaned without `--force`; a rebased branch is cleaned; an undelivered branch is
  refused; with `origin/HEAD` set ahead of a lagging `HEAD`, a branch delivered upstream is cleaned.
- [x] `cleanRefusesUndeliveredOrUncommittedWorkUnlessForced` asserts the 4.2.0 refusal text ("has commits that HEAD
  does not", "a squash or rebase merge"); its assertions change to the new text, and it still asserts the refusal.

### US-003: Gating completion, documented

**Description:** As a team whose runs must not complete on the agent's word alone, I want ralph-tui's worker contract
and a wrapper's pseudo code in the artifact's README, so I can write the gate in our own terms.

**Acceptance Criteria:**

- [x] `cookbook/ralph/README.md` has a "Gating completion" section: F10; the `--version` probe; argument order and
  Codex's prompt on stdin; F11's words; the retry settings; the environment filter; the characters `command` refuses;
  F9's missing setup; the wrapper's pseudo code; a `config.toml` example with a relative `command`.
- [x] The relative `command` is observed working in a serial run with a fake agent.
- [x] `cookbook/README.md`'s rendered help matches.

### US-004: `--tracker` narrows what is seeded and offered

**Description:** As a team that uses only JSON PRDs, or only Beads, I want the recipe to seed and offer only that
tracker.

**Acceptance Criteria:**

- [x] New param `--tracker`, `many`, `sorted`, choices `beads` and `json`, default both.
- [x] Only the chosen trackers' prompts are seeded; `tracker = "beads"` is written only with `beads`; `bin/run`'s usage
  line and the instructions section name only the chosen run flags. No refusals and no flag rewriting.
- [x] Existing argument lines cook unchanged; a test covers a JSON-only cook.

### US-005: Traps for what the recipe does not fix

**Description:** As a person cooking the recipe, I want each ralph-tui 0.12 trap as one line in the help, so I can
avoid it without reading the source.

**Acceptance Criteria:**

- [x] Traps: F2's `skipPermissions` placement; F10, pointing to Gating completion; F11; F9's missing setup in parallel
  workers; `git add -A` carrying `.beads/` changes; a tracked PRD with `--parallel` leaving its marks uncommitted (F6);
  an ignored or uncommitted `.ralph-tui/` never reaching the run worktree.
- [x] The help standard test and the rendered-help test pass.

### US-006: `--worktree-root` moves the run worktrees

**Description:** As a team whose worktrees live outside `.worktrees/`, I want to name the worktree root as I do for
`io.github.lmarqs.cookbook:agents#connect`, so run worktrees go where the rest of our worktrees go.

**Acceptance Criteria:**

- [x] New param `--worktree-root`, a path from the repository root, default `.worktrees`, with
  `agents#connect`'s name and meaning: a run's worktree is `<root>/ralph/<name>`, following `<root>/<branch>`.
- [x] `bin/run` and `bin/clean` read the root from one shell-quoted variable; ralph-tui's parallel workers follow it to
  `<root>/ralph/.ralph-worktrees/<name>/` (F9) with no `[parallel] worktreeDir`; `.gitignore` ignores `/<root>/ralph/`;
  the instructions section names `<root>/ralph/<name>`.
- [x] Existing argument lines cook byte-identical scripts; a test cooks `--worktree-root=wt`, runs, adds a parallel
  worker, finds the main checkout clean, and cleans both.

## Functional Requirements

- FR-1: `bin/clean <name>` must remove only worktrees under the run's F9 directory, the run worktree and their
  branches.
- FR-2: `bin/clean` must refuse uncommitted changes in any worktree it would remove unless `--force` is given.
- FR-3: `bin/clean` must treat `ralph/<name>` and each worker branch as delivered when every commit is in `HEAD` or
  `origin/HEAD`: by patch through `git cherry`, and, for a merge commit whose result differs from Git's own merge, by
  ancestry.
- FR-4: `--tracker` must limit the seeded prompts, the config's tracker line, the instructions and the usage line to
  the chosen trackers.
- FR-5: `--worktree-root` must set where `bin/run` creates and `bin/clean` removes run worktrees, and the ignored path.

## Non-Goals

- Any launcher or cook-time code for a trap: personal setup, Beads guards, the PRD rule, a forced `--prompt`, flag
  rewriting, a brownfield config guard. Each is a Trap line.
- A recipe-owned worker wrapper, completion gate or cooked skill; the README describes one.
- A setting for ralph-tui's `[parallel] worktreeDir`; parallel workers follow the run worktree.
- Committed spike results and a matrix document; observations go on #53.
- Writing outside the repository.
- This repository's own uncommitted ralph-tui cook, and the release; the maintainer decides both.

## Technical Considerations

- Files: `cookbook/ralph/RalphTui.groovy`, `cookbook/ralph/ralph-tui.groovy`, `cookbook/ralph/bin/run.sh`,
  `cookbook/ralph/bin/clean.sh`, `cookbook/ralph/README.md`, `cookbook/README.md`,
  `core/src/test/groovy/thecook/RalphTuiCookTest.groovy`.
- `cookbook/README.md` renders the recipe's help and the artifact README; `CookCommandTest` compares them.
- Never hand-edit `cook.lock` or `build/`; never weaken a test.

## Success Metrics

- US-001's tests fail on 4.2.0 and pass after the fix; US-002's fail on US-001's script.
- The Done check passes after every story.
