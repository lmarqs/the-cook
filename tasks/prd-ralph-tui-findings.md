# PRD: `ralph#ralph-tui` finds beads from a run worktree and states which model wins

## Overview

A run of the medprev-rest-api #1316 epic, with Codex on ralph-tui 0.12.0, surfaced two gaps in
`io.github.lmarqs.cookbook:ralph#ralph-tui` at `fa9f71c`. Both were checked against ralph-tui's
`dist/cli.js`.

1. **An `--epic` run from a run worktree finds no tasks.** ralph-tui's beads tracker only checks that
   `<cwd>/<beadsDir>` exists (`BeadsTrackerPlugin.detect()`), and `beadsDir` defaults to `.beads`. A run worktree has
   no `.beads/` when no file under it is committed, as in medprev, which ignores `/.beads/`. The tracker then reports
   itself not ready, and the run stops as `no_tasks` with no error. `bd` itself finds the main checkout's database
   through the Git common directory. `beads-bv` extends the beads tracker and runs the same check, so the `BEADS_DIR`
   export in `cookbook/src/ralph/bin/run.sh` does not get it past the check.
2. **An entry's `model` silently wins over `ralph-tui run --model`.** ralph-tui passes the agent
   `[...defaultFlags, --model <run>, ...pluginArgs]`, and the plugin's arguments carry `--model <entry>` after it.
   Codex's `exec` keeps the entry's. The recipe never seeds `model`, but `RalphTui.groovy:15-16` presents the two as
   interchangeable.

The fix seeds `[trackerOptions] beadsDir` in the configuration with the depth that `--worktree-root` gives, and
documents both behaviors.

Tracking issue: #87.

## Goals

- A configuration seeded with `--tracker=beads` or `--tracker=beads-bv` lets an `--epic` run in
  `<worktree root>/ralph/<name>` find its tasks with no edit by hand.
- The recipe's help and model doc state that both models are passed, the entry's last, so a team sets a model in one
  place.
- The help names the cases the seeded depth does not cover.

## Quality Gates

These commands must pass for every user story:
- `./gradlew -p cookbook check`: the Quick check for the changed module
- `./gradlew check cliBuild jpackageTest`: build, tests, Spotless, CodeNarc and the docs check (the AGENTS.md Done
  check)
- `git diff --check`: no whitespace errors

## User Stories

### US-001: Seed `beadsDir` for the beads trackers

**Description:** As a team running a Beads epic through `.ralph-tui/bin/run`, I want the seeded configuration to
point ralph-tui's beads tracker at the main checkout's `.beads`, so that a run worktree without `.beads/` still finds
its tasks.

**Acceptance Criteria:**
- [ ] `RalphTui.config()` (`cookbook/src/ralph/RalphTui.groovy`) appends a `[trackerOptions]` table with `beadsDir`
      after `[parallel]` when `epicTracker()` is `beads` or `beads-bv`. With `json` only, it writes no
      `[trackerOptions]`.
- [ ] The value is relative: one `../` for each segment of `worktreeRoot`, ignoring empty and `.` segments, plus two
      for `ralph/<name>`, then `.beads`. The default `.worktrees` gives `"../../../.beads"`, and `--worktree-root=a/b`
      gives `"../../../../.beads"`.
- [ ] A one-line TOML comment above the table says why: ralph-tui's beads trackers only check that the directory
      exists, and a run worktree has none.
- [ ] `config()`'s doc comment and the `.ralph-tui/config.toml` row of the help's write table
      (`cookbook/src/ralph/ralph-tui.groovy`) name `beadsDir`.
- [ ] A trap in the help says that the depth follows `<worktree root>/ralph/<name>`. A run name containing `/`, a
      `--worktree-root` changed after seeding, or a configuration seeded before this version needs the line edited
      by hand. Without it, ralph-tui stops as `no_tasks`.
- [ ] Tests in `cookbook/test/ralph/RalphTuiCookTest.groovy` assert the seeded line for `beads`, for `beads-bv` and for
      `--worktree-root=a/b`, and its absence for `json`. Existing exact-text config assertions are updated. The new
      assertions fail before the change.
- [ ] `theHelpStatesTheKnownLimits` asserts the new trap's text.
- [ ] Every line of the help and the after-cook text stays within 120 columns as printed.

### US-002: State that an entry's model wins over `--model`

**Description:** As a team choosing a worker's model, I want the recipe to state which model ralph-tui's worker runs
when both an entry and the run name one, so that a per-run `--model` is not silently ignored.

**Acceptance Criteria:**
- [ ] The model sentence in `RalphTui.groovy`'s class doc (lines 15-17) states three things: ralph-tui 0.12 passes a
      run's `--model` before the plugin's arguments, the plugin's arguments carry the entry's `model`, and Codex runs
      the entry's. It also says to set a model per entry or per run, not both.
- [ ] The "Model and effort" row of the help's Connections table says the same in one line.
- [ ] A trap in the help says: ralph-tui 0.12 passes both models with the entry's last, Codex takes the entry's, and
      nothing in the log says the run's was dropped.
- [ ] `theModelDocumentsTheEntriesToAdd` or `theHelpStatesTheKnownLimits` asserts the new text.
- [ ] No seeded entry gains or loses a `model` key.

## Functional Requirements

- FR-1: A configuration seeded with an epic tracker must hold
  `[trackerOptions] beadsDir = "<relative path to the main checkout's .beads>"` for a run worktree at
  `<worktree root>/ralph/<name>`, with `<name>` free of `/`.
- FR-2: A configuration seeded with only the `json` tracker must hold no `[trackerOptions]` table.
- FR-3: `beadsDir` must be a relative path, because ralph-tui joins it onto the working directory.
- FR-4: The help must name every case where the seeded `beadsDir` does not hold.
- FR-5: The class doc and help must state that an entry's `model` follows a run's `--model` on the worker's command
  line, and that Codex runs the entry's.

## Non-Goals

- No `bin/run` check or refusal when `<worktree>/<beadsDir>` is missing.
- No migration of configurations seeded earlier: seeds are never rewritten.
- No change to the `BEADS_DIR` export for `beads-bv`.
- No `.beads` symlink or directory created in the run worktree.
- No change to which keys a seeded `[[agents]]` entry holds.
- No upstream ralph-tui report as part of these stories.
- No change to this repository's own `.ralph-tui/config.toml`.

## Technical Considerations

- ralph-tui 0.12.0 sources, from `~/.bun/install/global/node_modules/ralph-tui/dist/cli.js`:
  - `BeadsTrackerPlugin.detect()`: about line 32955;
  - `BeadsBvTrackerPlugin`: about line 33346;
  - the argument order: line 1321;
  - the per-run `--model`: line 108995;
  - Codex's `buildArgs`: about line 17473.
- Parallel workers reuse the run worktree's tracker instance (`workerMode.tracker`), so the depth seen from the run
  worktree also holds in parallel mode.
- `beadsDir` also feeds ralph-tui's `{{beadsDbPath}}` template variable. The recipe's seeded prompts do not use it.
- Repositories cooked with `io.github.lmarqs.cookbook:beads#tracker` commit `.beads/PRIME.md`, so their worktrees
  already have `.beads/`. The seeded line points at the same main checkout `.beads` and is harmless there.
- Follow the help's wrapping rules: a long `@help` line continues after a trailing `\` (AGENTS.md Gotchas).

## Success Metrics

- In a scratch repository with `/.beads/` ignored and one ready bead under an epic, a cook of the local cookbook with
  `--tracker=beads`, followed by `.ralph-tui/bin/run demo --agent claude --epic <id> --headless --iterations 1`,
  starts the task instead of stopping as `no_tasks`. The same holds with `--tracker=beads-bv`.
- A reader of `./cook help io.github.lmarqs.cookbook:ralph#ralph-tui` can tell which model a Codex worker runs when an
  entry and the run both name one.

## Open Questions

- Does Claude Code's CLI keep the last `--model` too? The argument order is verified, but Claude's behavior with both
  flags is not. The docs therefore name Codex only.
