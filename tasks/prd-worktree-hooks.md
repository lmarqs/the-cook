# PRD: Worktree hooks for agents, with setup and teardown

## Overview

`git#worktrees` gives a repository one root for its linked worktrees, `.worktrees/<branch>`, but Claude Code's own
worktrees (`claude --worktree`, the `EnterWorktree` tool, subagents with `isolation: "worktree"`, workflows and
background sessions) still go to `.claude/worktrees/<name>`, and nothing runs the team's setup in them. Claude Code
2.1.308 lets a `WorktreeCreate` hook replace its default `git worktree` behavior and a `WorktreeRemove` hook clean up
what that hook created ([hooks reference](https://code.claude.com/docs/en/hooks)).

This feature has `agents#connect` write those two hooks when the person names a worktree root, and run a setup and a
teardown script the person names; `git#worktrees` documents `.worktrees/teardown` beside `.worktrees/setup`; and
`ralph#ralph-tui` gets `--worktree-teardown`, which `bin/clean` runs, matching its `--worktree-setup`. Every meeting
point is a parameter the person names ([0030](../docs/adrs/0030-recipe-connections.md)).

## Goals

- A Claude Code worktree in a repository cooked with `--worktree-root=.worktrees` is created at
  `.worktrees/<name>` on branch `<name>`, ready to run after the team's setup script.
- Removing that worktree runs the team's teardown, removes the worktree, and deletes its branch only once merged.
- People, Claude Code and ralph-tui use the same two scripts, named by the same params, `--worktree-setup` and
  `--worktree-teardown`.

## Quality Gates

These commands must pass for every user story:

- `./gradlew check cliBuild jpackageTest` - tests, Spotless, CodeNarc, the CLI build and the packaged-binary test
- `git diff --check` - no whitespace errors

## User Stories

### US-001: The worktree hook script

**Description:** As a team using the agents layout, I want one hook script that creates and removes worktrees at the
root I name, so Claude Code's worktrees follow the repository's rule.

**Acceptance Criteria:**

- [x] New resource `cookbook/agents/hooks/worktree`, POSIX `sh`, run as `worktree create` or `worktree remove` with
  Claude Code's hook JSON on stdin; it needs only `sh`, `sed` and `git`, not `jq`.
- [x] The root, setup and teardown paths are rendered into the script at cook time, shell-quoted, as ralph's
  `bin/run` renders `setup=''` (`cookbook/ralph/bin/run.sh`).
- [x] It finds the main checkout as the first entry of `git worktree list --porcelain`, so a session already inside a
  worktree creates the new one under the main checkout's root, never nested.
- [x] `create` reads `name`, then runs `git worktree add <main>/<root>/<name> -b <name>`, or without `-b` when branch
  `<name>` exists; git's output goes to stderr.
- [x] With a setup path, `create` runs `<worktree>/<setup>` with the worktree's root as the working directory and its
  stdout sent to stderr; when that file is missing, not executable or exits non-zero, it removes the worktree and the
  branch it created in this call, prints what failed to stderr, and exits 1.
- [x] `create` prints the worktree's absolute, normalized path as the last line of stdout and exits 0.
- [x] `remove` reads `worktree_path`; with a teardown path, it runs `<worktree_path>/<teardown>` from the worktree's
  root, and when that file is missing, not executable or exits non-zero, it prints what failed to stderr and exits 1,
  leaving the worktree in place.
- [x] `remove` then runs `git worktree remove --force <worktree_path>` and `git branch -d <branch>`; an unmerged branch
  stays, with a line on stderr naming it, and the hook still exits 0.
- [x] Tests run the script in a scratch Git repository with the documented JSON payloads and assert: the path printed,
  the branch made, a nested call from inside a worktree, setup run and its failure rolled back, teardown run and its
  failure keeping the worktree, the merged branch deleted and the unmerged one kept.

### US-002: `agents#connect --worktree-root`, `--worktree-setup`, `--worktree-teardown`

**Description:** As a person cooking the agents layout, I want to name where worktrees go and which scripts set them up
and tear them down, so Claude Code's worktree hooks are written for me.

**Acceptance Criteria:**

- [x] `cookbook/agents/connect.groovy` declares `--worktree-root`, `--worktree-setup` and `--worktree-teardown`, each
  optional with `default = []` and `path = true`.
- [x] With `--worktree-root` and `--harness=claude`, the recipe owns `.agents/hooks/worktree` (mode 0755) and the keys
  `hooks.WorktreeCreate` and `hooks.WorktreeRemove` in `.agents/adapters/claude/settings.json`, running
  `"$CLAUDE_PROJECT_DIR"/.agents/hooks/worktree create` and `... remove`; the rest of `hooks` stays the team's.
- [x] Without `--worktree-root`, none of them is written, and re-cooking without it removes them.
- [x] `--worktree-setup` or `--worktree-teardown` without `--worktree-root` is refused, naming `--worktree-root`.
- [x] `--worktree-root` without `--harness=claude` is refused: Codex 0.160.1 has no worktree hook event.
- [x] The opening line of the `harnesses` section in `.gitignore` records the new arguments.
- [x] Tests in `core/src/test/groovy/thecook/CookbookCookTest.groovy`, or a new `AgentsWorktreeCookTest.groovy`,
  assert the exact keys and script, the refusals, the removal on re-cook, and that a team's other hook events survive.

### US-003: `agents#connect` help and the agents-layout docs

**Description:** As a reader of the recipe's help and the layout's skill, I want the worktree hooks described, so I
know what they cover and what they do not.

**Acceptance Criteria:**

- [x] `cookbook/agents/connect.groovy` help: API examples, the three params, Produces rows for the script and both
  keys, Connections rows naming the person by each param, and Traps: a `git worktree add` an agent runs through Bash
  bypasses the hook; Claude Code keeps a subagent's or workflow's worktree and never runs `WorktreeRemove` for it;
  Codex's own worktrees are not covered.
- [x] `cookbook/agents/skills/agents-layout/references/design.md`: the Claude Code `--worktree` row of the runners
  table names the hook the layout writes.
- [x] The cooked copy in `.agents/skills/agents-layout/` follows only through `agents#seed`, never by hand.

### US-004: `git#worktrees` documents `.worktrees/teardown`

**Description:** As a team using `git#worktrees`, I want the README to state the teardown script beside the setup
script, so people and runners know both meeting points.

**Acceptance Criteria:**

- [x] `cookbook/git/templates/README.template.md` describes `.worktrees/teardown`: an optional executable the team
  writes and commits, run from a worktree's root before it is removed, to stop services and delete local data.
- [x] `cookbook/git/worktrees.groovy` help: an API row for `.worktrees/teardown`, a Connections row, After cooking
  says to commit it with `git add -f`, and the trap on `/.worktrees/*` names it.
- [x] `GitWorktreesCookTest` asserts the README names `.worktrees/teardown`.

### US-005: `ralph#ralph-tui --worktree-teardown`

**Description:** As a team running ralph-tui, I want `bin/clean` to run the teardown script, so a run's worktree is
torn down the same way as any other.

**Acceptance Criteria:**

- [x] `cookbook/ralph/ralph-tui.groovy` declares `--worktree-teardown`, optional, `path = true`, refused under
  `.ralph-tui/` as `--worktree-setup` is.
- [x] `bin/clean <name>` runs `<worktree>/<teardown>` from the run worktree's root before removing anything; when it
  is missing, not executable or fails, `bin/clean` stops with a message and removes nothing.
- [x] The argument line and the help (API, Consumes, Run-time effects, Re-cooking) include `--worktree-teardown`.
- [x] `cookbook/README.md`'s embedded ralph help is regenerated from `./cook help`, and
  `./gradlew -p cli test --tests thecook.cli.CookCommandTest --rerun` passes.
- [x] `RalphTuiCookTest` covers teardown run, teardown failure stopping `bin/clean`, and the refusal under
  `.ralph-tui/`.

### US-006: Live check with Claude Code

**Description:** As the maintainer, I want the hooks proven with a real Claude Code session, so the documented contract
holds for the version we name.

**Acceptance Criteria:**

- [x] In a scratch repository outside `/tmp`, cook `agents#seed`, `agents#connect --harness=claude
  --worktree-root=.worktrees --worktree-setup=.worktrees/setup --worktree-teardown=.worktrees/teardown` and
  `git#worktrees`; commit, with setup and teardown scripts that each write a marker file.
- [x] `claude --worktree demo -p '<prompt printing the working directory>'` runs in `.worktrees/demo` on branch `demo`,
  and the setup marker exists there.
- [x] Removing the worktree, through a Claude Code path that runs `WorktreeRemove` or, failing one in print mode, the
  hook run with the documented JSON, runs the teardown and removes the worktree; the merged branch is deleted.
- [x] The results, with the Claude Code version, go on the tracking issue.

## Functional Requirements

- FR-1: `agents#connect` writes `hooks.WorktreeCreate`, `hooks.WorktreeRemove` and `.agents/hooks/worktree` only with
  `--worktree-root` and `--harness=claude`.
- FR-2: The create hook places worktree `<name>` at `<main checkout>/<root>/<name>` on branch `<name>`.
- FR-3: The create hook runs the named setup from the new worktree's root and rolls back what it created when setup
  fails.
- FR-4: The remove hook runs the named teardown from the worktree's root and keeps the worktree when teardown fails.
- FR-5: The remove hook deletes the worktree's branch only when it is merged (`git branch -d`).
- FR-6: `--worktree-root`, `--worktree-setup` and `--worktree-teardown` are path params; setup and teardown require
  `--worktree-root` in `agents#connect`.
- FR-7: `ralph#ralph-tui`'s `bin/clean` runs the named teardown before removing a run's worktree, and stops when it
  fails.
- FR-8: `git#worktrees` documents `.worktrees/teardown` as an optional team script; it writes neither script.

## Non-Goals

- Codex worktrees: Codex 0.160.1 has no worktree hook event.
- Redirecting a `git worktree add` an agent runs through Bash, such as with a `PreToolUse` hook.
- Removing worktrees Claude Code keeps for subagents and workflows.
- Re-cooking this repository with the new params.
- Starting the `release` workflow.

## Technical Considerations

- Claude Code reads the last non-empty stdout line of a `WorktreeCreate` command hook as the path, refuses an absolute
  path with `.` or `..` segments, and fails creation on a non-zero exit; a `WorktreeRemove` hook must delete the
  directory itself, and Claude Code never deletes the branch.
- `.worktreeinclude` is not processed once a `WorktreeCreate` hook exists; copying local files is the setup script's.
- Existing patterns: the session-start dispatcher (`Agents.sessionStartDispatcher`, `Claude.startSession`), path
  params (`@param(path = true)`), and ralph's rendered launcher (`RalphTui.run`).

## Success Metrics

- The live check in US-006 passes on Claude Code 2.1.308.
- A repository cooked with the new params needs no hand-written worktree hook.

## Open Questions

None. Claude Code 2.1.308 runs `WorktreeRemove` from a print-mode session through the `ExitWorktree` tool with
`action: "remove"` and `discard_changes: true`; `claude --worktree -p` keeps the worktree on exit.
