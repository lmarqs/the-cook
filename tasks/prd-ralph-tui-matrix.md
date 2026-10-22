# PRD: ralph#ralph-tui across setups, trackers, harnesses and brownfield repositories

## Overview

`io.github.lmarqs.cookbook:ralph:4.2.0#ralph-tui` assumes one shape: a team that commits `.ralph-tui/`, runs Beads or
a PRD serially, and never looks at what its runs commit. A stress test over personal and team setups, Beads and
`prd.json`, Claude and Codex, committed and ignored tracker data, serial and parallel runs, and greenfield and
brownfield repositories found cells that fail, some silently. medprev-rest-api's field report, checked against its
source at ab57e679c9, added real brownfield evidence: `bin/clean` deletes other runs' parallel worktrees with their
uncommitted files, cannot recognise a cherry-picked delivery, and ralph-tui 0.12.0 completes a story on the agent's
`<promise>COMPLETE</promise>` whatever its checks say.

This feature fixes `bin/clean`, answers the open behaviour with spikes, and then makes the launcher keep the same
guarantees in every cell: the team's prompts, no permission bypass by default, and run branches that carry only the
work (plus a committed PRD's marks). The completion gate stays project code, described by a skill the recipe cooks.

Facts the stories rely on, from ralph-tui 0.12.0 (`dist/cli.js` of the npm package) and bd 1.3.1:

| # | Fact | Where |
| --- | --- | --- |
| F1 | ralph-tui reads `.ralph-tui/config.toml` only from its cwd and that directory's parent | `findProjectConfigPath` |
| F2 | The Claude plugin adds `--dangerously-skip-permissions` unless the entry's `[agents.options]` sets `skipPermissions = false`; an entry's top-level `skipPermissions` is dropped silently | `AgentPluginConfigSchema`, Claude plugin `initialize` |
| F3 | Tracker: `--tracker` > `tracker` > `defaultTracker` > built-in `beads-bv`; `--prd` switches to `json` only without `--tracker` | config resolution |
| F4 | Prompt: `--prompt` > `prompt_template` > `<cwd>/.ralph-tui/templates/<type>.hbs` > user templates > built-in; parallel workers inherit it | `promptTemplate` resolution |
| F5 | Built-in Beads prompts tell workers to run `bd close`, `br close` and `br sync` | built-in templates |
| F6 | Serial: the task is completed, then auto-commit runs `git add -A`. Parallel: the task is completed after the merge and nothing commits it | engine `runIteration`, parallel merge |
| F7 | Beads trackers need `<cwd>/.beads`; bd in a worktree uses the main checkout's database and honours `BEADS_DIR`; ralph-tui calls `bd sync --flush-only`, which bd 1.3.1 lacks | Beads tracker plugins, bd 1.3.1 |
| F8 | `run` matches flags exactly, so `--epic=x` is not parsed; it takes a repeated `--epic` and `--epics a,b` | `run` argument parser |
| F9 | Parallel workers live at `.worktrees/ralph/.ralph-worktrees/<name>/worker-<id>` for a run at `.worktrees/ralph/<name>`, on `ralph-parallel/<session>/<task>` branches; only `config.toml` is copied in, no setup runs, and ralph-tui force-removes a worker path before reuse | `getWorktreeBaseDir`, `copyConfig`, `cleanupStaleWorktree` |
| F10 | `<promise>COMPLETE</promise>` in the agent's final stdout completes and commits the task at any exit code | `PROMISE_COMPLETE_PATTERN` |
| F11 | `rate[- ]limit`, 429, `too many requests`, `quota exceeded` or `overloaded` on stderr makes the iteration rate-limited | `RateLimitDetector.detect` |
| F12 | `ralph-tui setup` writes `agentOptions = {}` and never writes `prompt_template` | setup wizard |
| F13 | Under `workspace-write`, `.git` is read-only to Codex, and `/tmp` is writable by default, so scratch repositories under `/tmp` give false passes | the-cook #42 US-002 |
| F14 | `bin/clean` removes every `ralph-parallel/*` worktree in the clone with `--force`, for any `<name>`, and checks only its own worktree for changes | `cookbook/ralph/bin/clean.sh` |
| F15 | `bin/clean` counts a branch as delivered only when `git rev-list --count HEAD..ralph/<name>` is 0 | `cookbook/ralph/bin/clean.sh` |

Decisions already made by the maintainer:

- Personal setup means a repository that does not mention ralph-tui: everything stays inside the repository,
  `~/.config` is out of scope, and the only trace is the `cook.lock` entry the developer leaves uncommitted.
- Committed Beads data never rides in a run branch; a committed PRD's completion marks land in the run branch.
- The worker gate is a skill with a contract and pseudo code, not recipe code; #39's "no generated launcher" stands.
- Each run picks its tracker with `--epic` or `--prd`; a cook-time `--tracker` only narrows what is seeded and offered.
- Brownfield never touches what the recipe did not create.
- Setup and teardown run the main checkout's copy, with the worktree as the working directory.

## Goals

- `.ralph-tui/bin/clean <name>` never removes a worktree or branch that run `<name>` did not create, and never removes
  uncommitted changes without `--force`.
- Every cell of {team, personal} × {beads, prd.json} × {claude, codex} × {committed, ignored} × {serial, parallel
  where allowed}, greenfield, upgraded from 4.2.0, over a `ralph-tui setup` config and over a medprev-shaped clone,
  runs with the team's prompt, no bypass flag, and a run branch holding only the work and, for a tracked PRD, its marks.
- Each open behaviour question is answered by a spike whose commands and observed output are committed.
- A team can write its own completion gate from the cooked `ralph-tui-worker` skill without reading ralph-tui's
  source.

## Quality Gates

These commands must pass for every user story:

- `./gradlew check cliBuild jpackageTest` - tests, Spotless, CodeNarc, the CLI build and the packaged-binary test
- `git diff --check` - no whitespace errors

## User Stories

### US-001: bin/clean removes only its own run's parallel worktrees

**Description:** As a developer with several runs or an earlier runner's worktrees in one clone, I want
`.ralph-tui/bin/clean <name>` to remove only what run `<name>` created, so other work is never lost.

**Acceptance Criteria:**

- [ ] A test in `core/src/test/groovy/thecook/RalphTuiCookTest.groovy` fails on the 4.2.0 `clean.sh`: it creates a
  `ralph-parallel/*` worktree outside `.worktrees/ralph/.ralph-worktrees/<name>/` with an uncommitted file, runs
  `bin/clean <name>`, and asserts that worktree, its file and its branch remain.
- [ ] `cookbook/ralph/bin/clean.sh` removes only worktrees whose path is under
  `<root>/.worktrees/ralph/.ralph-worktrees/<name>/`, and their branches; it never lists or removes others, with or
  without `--force`.
- [ ] Without `--force`, a worktree under that directory with uncommitted changes makes `clean` exit 1 before removing
  anything, naming the worktree and that `--force` deletes them.
- [ ] With `--force`, those worktrees are removed and each removal is printed, as today.
- [ ] Existing `RalphTuiCookTest` tests pass unchanged.

### US-002: bin/clean recognises a cherry-picked or rebased delivery

**Description:** As a team that delivers run branches by cherry-pick or rebase, I want `bin/clean` to recognise them as
delivered, so `--force` and its skipped dirty check are not the norm.

**Acceptance Criteria:**

- [ ] `clean.sh` counts `ralph/<name>` as delivered when `git cherry <upstream> ralph/<name>` lists no line starting
  with `+`, where `<upstream>` is `refs/remotes/origin/HEAD` when it resolves and `HEAD` otherwise.
- [ ] Its refusal names the commits not found upstream and says a squash merge, or a cherry-pick that resolved
  conflicts, still needs `--force`.
- [ ] Tests: a cherry-picked branch is cleaned without `--force`; a rebased branch is cleaned; an undelivered branch is
  refused; with `origin/HEAD` set ahead of a lagging `HEAD`, a branch delivered upstream is cleaned.

### US-003: Spike S1, a personal run

**Description:** As the maintainer, I want to know how ralph-tui behaves with an untracked, excluded `.ralph-tui/`, so
the personal launcher is built on observed behaviour.

**Acceptance Criteria:**

- [ ] Scratch repositories live under the worktree's ignored `build/spikes/`, never under `/tmp` (F13); fake `claude`
  and `codex` executables on `PATH` answer `--version`, log argv, stdin and environment to a file, and change one file.
- [ ] Serial and `--parallel` runs with `.ralph-tui/config.toml` copied into the run worktree, `/.ralph-tui/` and
  `/.worktrees/ralph/` in `info/exclude`, and `--prompt` naming the main checkout's template.
- [ ] Records: the argv each fake received; whether `--dangerously-skip-permissions` appears; the prompt received;
  every file ralph-tui writes, by path; whether `git check-ignore` matches a run worktree path before it exists;
  whether `options.model = "claude-sonnet-5-5"` reaches the fake as `--model claude-sonnet-5-5`.
- [ ] Results in `tasks/spikes/ralph-tui-matrix/S1.md`: each command, its output trimmed to what proves the answer, and
  a verdict per question.

### US-004: Spike S2, Beads in worktrees

**Description:** As the maintainer, I want to know which database and export bd and bv use from a run worktree, so
committed Beads data stays out of run branches.

**Acceptance Criteria:**

- [ ] For three Beads shares, clone (`/.beads/*` ignored except `PRIME.md`), stealth (nothing tracked) and team
  (tracked files with `export.auto`), with and without `BEADS_DIR=<main>/.beads`: records which database `bd close`
  writes, which export file changes, what `bv` shows, and `git status` in the worktree and the main checkout.
- [ ] Records whether an empty `.beads/` directory in the worktree is enough for ralph-tui's Beads tracker detection,
  and what `bd sync --flush-only` prints and changes.
- [ ] Results in `tasks/spikes/ralph-tui-matrix/S2.md`, with a verdict on whether `BEADS_DIR` alone keeps the
  worktree's `.beads/` unchanged.

### US-005: Spike S3, tracker flags

**Description:** As the maintainer, I want to confirm how ralph-tui takes epics, trackers and template variables, so
the launcher passes them correctly.

**Acceptance Criteria:**

- [ ] Records whether `--epic a --epic b` and `--epics a,b` both offer both epics' tasks, whether `--prd x --tracker
  beads` keeps Beads (F3), and whether `{{selectionReason}}` renders under the bv tracker in one shared Beads template.
- [ ] Results in `tasks/spikes/ralph-tui-matrix/S3.md`.

### US-006: Spike S4, committed and untracked PRDs

**Description:** As the maintainer, I want to see where a PRD's completion marks land, so a tracked PRD's marks reach
the run branch and an untracked one stays local.

**Acceptance Criteria:**

- [ ] Serial run with a tracked PRD passed as `<worktree>/<path>`: records whether one commit carries the code change
  and the PRD mark, and that the main checkout stays clean.
- [ ] Serial run with an untracked PRD passed by the main checkout's absolute path: records that it is updated in place
  and nothing about it is committed.
- [ ] Parallel run with a tracked PRD: records whether the mark is left uncommitted (F6).
- [ ] Results in `tasks/spikes/ralph-tui-matrix/S4.md`.

### US-007: Spike S5, brownfield

**Description:** As the maintainer, I want to cook the new recipe over existing setups, so upgrades and adoptions
never overwrite or delete a team's work.

**Acceptance Criteria:**

- [ ] Cooks over: a 4.2.0 cook; a `config.toml` shaped like `ralph-tui setup` output (F12); a config shaped like
  medprev's (JSON-only `defaultTracker`, shim `command`s, `[errorHandling] strategy = "retry"`, `options.model`); a
  clone holding foreign `ralph-parallel/*` worktrees, one dirty.
- [ ] Records the diff each cook proposes, and the result of `bin/clean` in the last clone.
- [ ] Results in `tasks/spikes/ralph-tui-matrix/S5.md`, listing which config keys the brownfield guard must read.

### US-008: Spike S6, the worker contract

**Description:** As the maintainer, I want a throwaway wrapper written only from the skill's pseudo code to work
under ralph-tui, so the `ralph-tui-worker` skill is correct before teams rely on it.

**Acceptance Criteria:**

- [ ] A throwaway POSIX `sh` wrapper under `build/spikes/`, following the pseudo code in the Technical Considerations,
  run as each entry's `command` for the fake `claude` and `codex`, serial and parallel.
- [ ] Records: whether a relative and an absolute `command` resolve from a run worktree and from a parallel worker;
  that a failing check withholds the promise and the task stays open; that a non-zero exit with `strategy = "retry"`,
  `maxRetries = 2`, `continueOnNonZeroExit = false` is retried twice then skipped; whether an exit 0 without the
  promise is retried up to `maxIterations` and what `--iterations 0` does; whether stopping ralph-tui kills a fake
  agent's child process without the wrapper's own process group; whether a setup marker in `git rev-parse --git-dir`
  runs setup once in each parallel worker.
- [ ] Results in `tasks/spikes/ralph-tui-matrix/S6.md`, with each pseudo-code line marked confirmed or corrected.

### US-009: The launcher passes the team's prompt and runs setup from the main checkout

**Description:** As a team, I want every run, in every tracker flavour and in parallel workers, to use our prompt, and
setup to work from any base, so workers follow our rules.

**Acceptance Criteria:**

- [ ] `bin/run` passes `--prompt <main checkout>/.ralph-tui/templates/beads.hbs` for `--epic` runs and `json.hbs` for
  `--prd` runs, unless the run's arguments carry `--prompt`.
- [ ] One seeded `beads.hbs` serves `beads`, `beads-bv`, `beads-rust` and `beads-rust-bv`: its rules say never run `bd`
  or `br` and never touch `.beads/`, and a `{{#if selectionReason}}` block shows bv's reason (as S3 confirms).
- [ ] `bin/run` runs `<main checkout>/<setup>` and `bin/clean` runs `<main checkout>/<teardown>`, each with the
  worktree's root as the working directory; a `--base` older than the setup script works.
- [ ] Tests assert the argv the fake `ralph-tui` receives for each case, and setup run from a base without it.

### US-010: Tracker narrowing and flag handling

**Description:** As a team that uses only JSON PRDs, or only Beads, I want the recipe to seed and offer only that
tracker, and every run to pass its tracker flags as ralph-tui parses them.

**Acceptance Criteria:**

- [ ] New param `--tracker`, `many`, `sorted`, choices `beads` and `json`, default both; `ralph-tui.groovy` documents it.
- [ ] Only the chosen trackers' prompts are seeded, and the instructions section and `bin/run`'s usage line name only
  their flags; the config seed sets `tracker` for Beads alone and `defaultTracker = "json"` for JSON alone.
- [ ] `bin/run` refuses `--epic`/`--epics` without `beads`, and `--prd` without `json`, naming the cook to change it.
- [ ] `bin/run` rewrites `--epic=`, `--epics=`, `--prd=`, `--tracker=` and `--prompt=` to the two-word form, passes
  every `--epic` and `--epics`, refuses `--tracker` together with `--prd`, and refuses `--tracker json`.
- [ ] Tests cover each refusal, the rewriting, and two epics reaching the fake `ralph-tui`.

### US-011: The PRD rule

**Description:** As a team with a committed PRD, I want its completion marks in the run branch, and as a developer with
an untracked PRD, I want it updated in place.

**Acceptance Criteria:**

- [ ] A PRD tracked in the main checkout with uncommitted changes, or different at `--base` from `HEAD`, makes `bin/run`
  exit 1 with the reason.
- [ ] A tracked PRD is passed as `<worktree>/<path>` with `--serial` added; an explicit `--parallel` is refused with
  the reason (F6).
- [ ] An untracked, ignored or outside-the-repository PRD is passed by its absolute path, as today.
- [ ] Tests cover each case, matching S4's results.

### US-012: Beads data stays out of run branches

**Description:** As a team that commits `.beads/`, I want run branches never to carry Beads changes, so delivery holds
only the work.

**Acceptance Criteria:**

- [ ] As S2 settles: `bin/run` exports `BEADS_DIR=<main checkout>/.beads`; creates the worktree's `.beads/` when absent;
  marks tracked `.beads/` files skip-worktree in the run worktree.
- [ ] `bin/clean` prints each `ralph/<name>` commit that touches `.beads/` before removing anything.
- [ ] Tests: the fake `ralph-tui` edits a tracked `.beads/` file and runs `git add -A && git commit`, and the commit
  has no `.beads/` change; the fake's printed environment has `BEADS_DIR`.

### US-013: Personal setup

**Description:** As a developer in a repository that does not use ralph-tui, I want to run it with nothing committed,
so the repository stays agnostic.

**Acceptance Criteria:**

- [ ] New param `--share`, choices `team` and `personal`, default `team`; existing argument lines cook unchanged.
- [ ] `--share=personal` writes no `.gitignore` section, refuses `--instructions`, and refuses a relative
  `--worker-settings`; After cooking prints the `info/exclude` line and says to leave the `cook.lock` entry
  uncommitted.
- [ ] The personal `bin/run` refuses when `git ls-files .ralph-tui` is non-empty; copies `config.toml` into the run
  worktree; checks with `git check-ignore` that the copy and the run worktree path are ignored, and otherwise removes
  the copy, exits 1 and prints `printf '/.ralph-tui/\n/.worktrees/ralph/\n' >> "$(git rev-parse --git-path info/exclude)"`.
- [ ] The team `bin/run` refuses when the worktree lacks `.ralph-tui/config.toml`.
- [ ] Both check `command -v ralph-tui` first and print how to install it.
- [ ] Tests cover each refusal and a personal run reaching the fake `ralph-tui` with the config copied.

### US-014: Brownfield guard on an existing config

**Description:** As a team cooking over a config written by `ralph-tui setup` or by hand, I want the cook to refuse a
config that would run Claude in bypass mode, and to accept every other team choice.

**Acceptance Criteria:**

- [ ] The model reads an existing `.ralph-tui/config.toml` with `groovy.toml` through `current(...)`.
- [ ] It refuses when the default agent (`agent`, `defaultAgent` or the first entry) uses the Claude plugin without
  `skipPermissions = false` under `[agents.options]`, naming the key to add, and naming a top-level
  `skipPermissions` as the cause when present (F2).
- [ ] It accepts a medprev-shaped config, and named non-default bypass entries.
- [ ] Tests: setup-shaped refused; top-level `skipPermissions` refused; medprev-shaped accepted; upgrading over a 4.2.0
  kitchen proposes only owned changes.

### US-015: The ralph-tui-worker skill

**Description:** As a team that needs a completion gate, I want a skill that states ralph-tui's worker contract and
gives pseudo code for a wrapper, so we can write it in our own terms.

**Acceptance Criteria:**

- [ ] New `cookbook/ralph/skills/ralph-tui-worker/SKILL.md`, with frontmatter `name` and `description`, holding: the
  contract (F9, F10, F11, the `--version` probe, argv order with Codex's trailing `-` on stdin, the retry triple, the
  environment passthrough, no shell metacharacters in `command`); the pseudo code as S6 corrected it; the wiring for
  team and personal setups; the fake-agent tests a team runs on its wrapper.
- [ ] New param `--skills`, a path, default none: with it, the skill is written as owned files under
  `<dir>/ralph-tui-worker/`; without it, nothing is written.
- [ ] Tests: the skill written under the named directory and recorded in `cook.lock`; nothing written without the
  param; the skill removed when a later cook drops the param.

### US-016: Documentation

**Description:** As a person cooking the recipe, I want its help, class doc and README to describe every cell and trap,
so I can choose params without reading the source.

**Acceptance Criteria:**

- [ ] `cookbook/ralph/ralph-tui.groovy` help covers API, Consumes, Produces, Connections, Run-time effects, Re-cooking
  and Traps for every new param, and a table of the cells and what each guarantees, per `docs/ux/messages-and-docs.md`.
- [ ] Traps: F9's missing setup and force-removal; F10; F11; `defaultFlags` and `skipPermissions` placement; F13;
  `CLAUDE.local.md` above a worktree silencing its `AGENTS.md`; Codex workers getting no session-start hooks; an
  earlier runner's worktrees and branches left untouched; a team-set `[parallel] worktreeDir` escaping `bin/clean`.
- [ ] The `RalphTui` class doc's model claim matches S1's result.
- [ ] `cookbook/ralph/README.md` describes the shares, the tracker param and the skill.
- [ ] The help standard test passes.

### US-017: End-to-end matrix verification

**Description:** As the maintainer, I want every cell run once with fake harnesses against the built CLI, so the
release is backed by observed results.

**Acceptance Criteria:**

- [ ] Runs, from the CLI built by `./gradlew cliBuild`, each cell of {team, personal} × {beads, prd.json} × {claude,
  codex} × {committed, ignored} × {serial, parallel where allowed}, greenfield, upgraded from 4.2.0, over a setup
  config and over a medprev-shaped clone, in scratch repositories under `build/spikes/`.
- [ ] For each cell records: the argv and prompt received, the run branch's files, the main checkout's `git status`,
  and `bin/clean`'s result with a foreign worktree present.
- [ ] Results in `tasks/spikes/ralph-tui-matrix/matrix.md`, one row per cell with pass or the defect found.

## Functional Requirements

- FR-1: `bin/clean <name>` must remove only worktrees under `.worktrees/ralph/.ralph-worktrees/<name>/`, the run
  worktree and their branches.
- FR-2: `bin/clean` must refuse uncommitted changes in any worktree it would remove unless `--force` is given.
- FR-3: `bin/clean` must treat a branch as delivered when `git cherry` finds every commit upstream.
- FR-4: `bin/run` must pass the team's prompt from the main checkout to every run unless the run names its own.
- FR-5: `bin/run` must pass tracker flags in the form ralph-tui parses, and refuse combinations ralph-tui would
  misread.
- FR-6: A tracked PRD's run must be serial and read the PRD from the run worktree; an untracked PRD's from the main
  checkout.
- FR-7: Run branches must carry no `.beads/` change.
- FR-8: `--share=personal` must write nothing that git tracks except what the developer chooses to commit, and its
  launcher must refuse until `.ralph-tui/` and `.worktrees/ralph/` are ignored.
- FR-9: The cook must refuse an existing config whose default agent is Claude in bypass mode.
- FR-10: `--tracker` must limit the seeded prompts, the instructions and the usage line to the chosen trackers.
- FR-11: `--skills` must cook the `ralph-tui-worker` skill into the named directory, and nothing without it.

## Non-Goals

- A recipe-owned worker wrapper or completion gate; the skill describes one.
- Writing outside the repository, including `~/.config/ralph-tui`.
- Recognising or cleaning an earlier runner's worktrees and branches.
- `agents#connect` changes (the `./` form of relative denies, Codex rules for a Codex-only role) and `git#worktrees`
  Traps; each gets its own issue.
- An engine-level clone-local lock that would keep a personal cook out of `cook.lock`.
- beads-rust (`br`) runs; it is not installed, so its cells are marked untested.
- Real-harness smoke runs with real `claude` and `codex`; the maintainer runs them after US-017.
- This repository's own uncommitted ralph-tui cook, and the release; the maintainer decides both.

## Technical Considerations

- Files: `cookbook/ralph/RalphTui.groovy`, `cookbook/ralph/ralph-tui.groovy`, `cookbook/ralph/bin/run.sh`,
  `cookbook/ralph/bin/clean.sh`, `cookbook/ralph/templates/{beads,json}.hbs`,
  `cookbook/ralph/skills/ralph-tui-worker/SKILL.md`, `cookbook/ralph/README.md`,
  `core/src/test/groovy/thecook/RalphTuiCookTest.groovy`.
- Reuse: `@param(choices, many, sorted, default, path)`, `@recipe(arguments = true)`, `@Seed`, `@Section`,
  `@Sections`, `current(file)`, the default `groovy.toml` import, the `shellQuoted` substitution in `bin/run`, and how
  `agents#seed` ships `cookbook/agents/skills/agents-layout/` as owned files. Test helpers in `RalphTuiCookTest`:
  `repository()`, `launch()` with its fake `ralph-tui`, `commitAll()`, `git()`, `executable()`, `write()`.
- `bin/run` keeps `exec ralph-tui run`, so no step may need the launcher after ralph-tui starts.
- Never hand-edit `cook.lock` or `build/`; never weaken a test.
- Spike stories run `git init`, `ralph-tui`, `bd`, `bv` and shell scripts in scratch repositories. Under this
  repository's policy (`.agents/permission/policy.yaml` allows only `git status|diff|log|push` and `./gradlew check`),
  an unattended worker is refused those commands; run the spike stories with a policy that allows them, or by hand.
  Codex workers cannot commit in a nested scratch repository (F13), so spikes run with Claude.
- Pseudo code the skill and S6 start from:

  ```
  if args == [--version]: exec agent --version
  refuse when cwd is the main checkout (git rev-parse --git-dir equals --git-common-dir)
  refuse bypass flags: --dangerously-skip-permissions, --permission-mode bypassPermissions,
    --dangerously-bypass-approvals-and-sandbox, --yolo, danger-full-access,
    --ignore-rules, --ignore-user-config, --approve-for-me
  if no setup marker in $(git rev-parse --git-dir): run <main checkout>/<setup>; write the marker
  base = git rev-parse HEAD
  add run-time args (Claude: permission mode, --settings role; Codex: -c options before the trailing -)
  run the agent in its own process group, forwarding TERM, INT and HUP; stdout to a temporary file; stdin passed on
  if the agent exits non-zero: keep its output in an ignored log; exit with its code
  run <check> base, output to an ignored log, never stderr
  if the check fails: keep the output; print one fixed line to stderr; exit 1
  write the held stdout; exit 0
  ```

## Success Metrics

- US-001's test fails on 4.2.0 and passes after the fix.
- `tasks/spikes/ralph-tui-matrix/matrix.md` shows every cell passing, or a linked defect.
- In a medprev-shaped clone, `bin/clean` removes no worktree it did not create.
- The Done check passes after every story.

## Open Questions

- Does `options.model` with a full model id reach Claude as `--model`? (S1)
- Is an exit 0 without the promise retried without limit under `--iterations 0`? (S6)
- Does stopping ralph-tui stop an agent's child processes when no wrapper starts its own process group? (S6)
- Does `BEADS_DIR` alone keep a worktree's `.beads/` unchanged, or is skip-worktree the main guard? (S2)
