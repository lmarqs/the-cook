# PRD: Beads, ralph-tui and worktrees as cookbook plugins on the agents layout

Tracking issue: #42. Source: the approved plan, grilled with the maintainer, and two sibling repositories
(medprev-rest-api, medprev-management) as evidence of what the recipes must carry. They are not targets.

## Overview

Beads (`bd`) keeps a project's issues in the clone. ralph-tui works through a tracker's tasks by running coding agents
in a loop. Each tool runs on its own, and the two can be used together. Both sibling repositories use them, set up in
opposite ways:

| Concern | medprev-rest-api | medprev-management |
| --- | --- | --- |
| Beads database | clone-local, `/.beads/` ignored, no remote, no hooks | config tracked, Dolt remote on `origin`, `core.hooksPath` |
| Beads in `AGENTS.md` | one paragraph | bd's managed blocks and a custom `.beads/PRIME.md` |
| ralph-tui config | copied by a Python launcher | `.ralph-tui/config.toml`, tracked |
| Run worktree | `.work/worktrees/ralph-<session>` | `.claude/worktrees/ralph-<epic>` on `ralph/<epic>` |
| Worker permissions | a policy role and a `bd` shim | none; prompt and hooks only |

Both also share: `configVersion = "2.1"`, `autoCommit = true`, a prompt that forbids closing beads and pushing, a
launcher that runs ralph-tui in a dedicated worktree, `--epic` on every run, and a clean step.

This PRD adds three cookbook artifacts and one extension point on the agents layout:

- `agents#connect` gains a session-context contract. A tool adds context at session start by dropping a script into
  `.agents/hooks/session-start.d/`. Without the contract, every tool has to learn each harness's hook format, and two
  recipes that both own `hooks.SessionStart` are refused by the overlap check.
- `io.github.lmarqs.cookbook:git#worktrees` owns the worktree root, because people use worktrees too, not only
  agents. The layout still writes no worktree and recommends no location (`tasks/prd-agents-worktrees.md` FR-5 and
  its non-goals).
- `io.github.lmarqs.cookbook:beads#tracker` needs no layout and no harness.
- `io.github.lmarqs.cookbook:ralph#ralph-tui` keeps its worker limits in its own files, so the limits hold whoever
  launches it.

Decisions, as grilled:

1. Maximum setup: a recipe writes everything it can. Only what Cook cannot do goes in the after-cooking text.
2. Platform and plugins: `agents` publishes the extension points (session-context directory, Launching table, policy
   roles). The tool artifacts plug in only through them, name no harness, and still cook on their own, with less setup.
3. Beads does not imply a harness.
4. Worktrees belong to git. The path is the branch: `.worktrees/<branch>`. Ralph runs use the branch `ralph/<name>`
   at `.worktrees/ralph/<name>`.
5. Delivery is chosen per session: the recipes never push or open pull requests.
6. Workers always run unattended, as `claude --print` or `codex exec`. Watching the TUI gives pause and quit, never
   approvals.
7. Upgrades follow Cook's default: seeds keep the team's edits, and owned files update through the reviewed diff.
8. Greenfield only: no adoption path, leftover listing or migration.

## Goals

- In an empty repository, cooking `git#worktrees`, `beads#tracker`, `ralph#ralph-tui` and the agents layout in any
  order gives a setup where every session starts with `bd prime` context. `.ralph-tui/bin/run <epic>` then completes
  a bead in `.worktrees/ralph/<epic>`.
- Each of the three artifacts cooks on its own and writes only its own files and sections.
- A worker launched by `.ralph-tui/bin/run` cannot push, close a bead or start ralph-tui, whether a person, an agent
  or a schedule launched it.

## Quality Gates

These commands must pass for every user story:
- `./gradlew check cliBuild jpackageTest`
- `git diff --check`

Each story is one or more commits titled `<type>(<area>): <sentence>`, test-first. The commits are pushed to `main`
with a comment on #42. US-001 and US-009 commit only if they find a defect.

## User Stories

### US-001: Spike the unknowns before writing recipes
**Description:** As the maintainer, I want the four behaviors the design relies on proven in a scratch repository,
so that the recipes encode what the tools do rather than what their docs say.

**Acceptance Criteria:**
- [ ] In a scratch repository with `bd init` and one epic, the planned `.ralph-tui/config.toml` passes
  `ralph-tui doctor`. ralph-tui's debug output shows `defaultFlags` ahead of `--print` for Claude and `exec` for
  Codex. A headless run with the fake agent from the `testing-tui-runtime` skill completes one bead.
- [ ] `claude --print --settings .ralph-tui/worker/claude.json` refuses `git push` and `bd close`, while the
  project's `.claude/settings.json` stays in effect.
- [ ] A Claude `hooks.SessionStart` hook and a Codex session-start hook that run one dispatcher script both add its
  plain-text output to the session context. The exact Codex `hooks.json` shape is recorded.
- [ ] With `tracker = "beads"` in the config and no `bd` on `PATH`, `ralph-tui run --prd ./prd.json` completes one
  story with the fake agent.
- [ ] `claude --print --settings .ralph-tui/worker/claude.json` refuses `bd update <id> --status closed`, and the
  deny rule that does it is recorded.
- [ ] Recorded: whether `bd update` from a Codex `workspace-write` worker with network off reaches the database
  through git's common directory, outside the worktree.
- [ ] Recorded: the exact lines bd 1.3.1 writes to ignore files on `bd init`, for `beads#tracker --share=team`.
- [ ] Results go on #42. The plan changes only where a result contradicts it, and the comment names each change.

### US-002: agents#connect owns the session-context contract
**Description:** As a tool recipe, I want to add session-start context by writing one script into a directory, so
that I never learn a harness's hook format and never collide with another tool over the same hook key.

**Acceptance Criteria:**
- [ ] `#connect` owns `.agents/hooks/session-start` (mode 0755). Run as `session-start <harness>`, it runs every
  `.agents/hooks/session-start.d/*.sh` in name order with the harness name as `$1`, joins their standard output, and
  exits 0. It prints nothing for an empty or missing directory.
- [ ] For `--harness=claude`, `#connect` owns `hooks.SessionStart` in `.agents/adapters/claude/settings.json`, which
  runs `.agents/hooks/session-start claude`. For `--harness=codex`, it owns the session-start entry in
  `.agents/adapters/codex/hooks.json`, in the shape US-001 recorded, linked as `.codex/hooks.json`.
- [ ] `CookbookCookTest` cases: both harnesses get the key and the script. A `hooks.PreToolUse` the team wrote
  survives a second cook. Discard removes only what `#connect` wrote. A second cook prints `Already cooked.`.
- [ ] `cookbook/agents/skills/agents-layout/references/design.md` gains "Tools that add session context", and its
  "Cook owns only the keys it renders" section lists the session-start hook. The `SKILL.md` owners table and the hooks
  row of "Where a change goes" say a team's own session-start script goes in `.agents/hooks/session-start.d/`.
- [ ] This repository's cooked layout is re-cooked, so `.agents/skills/agents-layout/` and `cook.lock` match.

### US-003: git#worktrees
**Description:** As anyone working in parallel branches, I want one ignored worktree root with a README that states
the naming rule, so that people, agents and loop runners put worktrees in the same place.

**Acceptance Criteria:**
- [ ] `cookbook/git/Git.groovy` becomes the model this recipe compiles against. `worktrees.groovy` takes no
  arguments.
- [ ] It owns `.gitignore` section `worktrees` with exactly `/.worktrees/*` and `!/.worktrees/README.md`.
- [ ] It seeds `.worktrees/README.md`: what the directory is; the rule `.worktrees/<branch>`;
  `git worktree add .worktrees/<branch> -b <branch>`, `git worktree remove .worktrees/<branch>` and
  `git worktree prune`; the in-repo traps from `design.md` (Claude loads the parent checkout's `AGENTS.md`, and a
  `CLAUDE.local.md` there reaches the worktree); a placeholder for the project's setup steps.
- [ ] Tests: exact files and `cook.lock` on an empty kitchen; a second cook prints `Already cooked.`; a hand-edited
  README is kept; `git check-ignore` passes on `.worktrees/x/a` and not on `.worktrees/README.md`.
- [ ] `./cook help io.github.lmarqs.cookbook:git#worktrees` renders the class doc.

### US-004: beads#tracker
**Description:** As a team using Beads, with or without agents, I want one recipe to write the ignore rules, the
prime text and the agent hook, so that `bd init` is the only manual step.

**Acceptance Criteria:**
- [ ] `--share` is required, with choices `team` and `clone`. `--share=fork` is refused, naming the choices.
- [ ] `.gitignore` section `beads`: `clone` writes `/.beads/*`, `!/.beads/PRIME.md` and `/.beads.gate.lock`; `team`
  writes `/.beads/issues.jsonl` and US-001's recorded lines, rooted.
- [ ] Seeds `.beads/PRIME.md` with the rules for that `--share`, including "a worker never closes its bead or
  pushes".
- [ ] Owns `AGENTS.md` section `beads` only when `AGENTS.md` exists in the draft. The section says: run `bd prime` at
  the start; use `bd ready`, `bd create` and `bd close`; and for `team`, `bd dolt push`.
- [ ] Owns `.agents/hooks/session-start.d/beads.sh` (0755) only when `.agents/permission/policy.yaml` exists in the
  draft. It prints `bd prime` for `claude` and `bd codex-hook SessionStart` for `codex`, and nothing when `bd` or
  `.beads/` is missing.
- [ ] The after-cooking text gives `bd init --prefix <p> --skip-hooks --skip-agents --non-interactive`. For `team`, it
  adds `bd config set sync.remote <url>`, `bd config set export.auto true`, `bd hooks install --beads` and
  `git config core.hooksPath .beads/hooks`. It says never to run `bd setup`, and, with the layout, to cook
  `agents#connect`.
- [ ] Never writes `.beads/config.yaml`, `metadata.json`, bd's git hooks or the database.
- [ ] Tests: `team` and `clone` on an empty kitchen (exact files and `cook.lock`, second cook unchanged, hand-edited
  seed kept); with and without `AGENTS.md`; with the layout the hook file appears, without it no hook file.
- [ ] `cookbook/beads/ready.sh` is deleted.

### US-005: ralph#ralph-tui configuration, prompt and worker limits
**Description:** As a team using ralph-tui, I want a seeded configuration and prompt and enforced worker limits, so
that a run is unattended and safe on the first try.

**Acceptance Criteria:**
- [ ] `--agent` is the only parameter: many and required, with choices `claude` and `codex`; `defaultAgent` is the
  first. There is no tracker parameter: `bin/run` passes `--epic` or `--prd` per run, and ralph-tui switches to the
  json tracker when given `--prd` without `--tracker`.
- [ ] Seeds `.ralph-tui/config.toml` with `configVersion = "2.1"`, `tracker = "beads"`, `defaultAgent`,
  `maxIterations = 10`, `autoCommit = true`, `[errorHandling] strategy = "skip"`,
  `[parallel] directMerge = true` and one `[[agents]]` per agent. Plugin settings sit under `[agents.options]`,
  because ralph-tui 0.12 drops unknown top-level agent keys. Claude:
  `defaultFlags = ["--permission-prompts", "none", "--settings", ".ralph-tui/worker/claude.json"]`,
  `skipPermissions = false`. Codex:
  `defaultFlags = ["-c", "approval_policy=never", "-c", "sandbox_workspace_write.network_access=false"]`,
  `fullAuto = false`, `sandbox = "workspace-write"`.
- [ ] Seeds both `.ralph-tui/templates/beads.hbs` and `json.hbs`; ralph-tui loads `<tracker>.hbs` for the tracker
  a run uses. Both prompts say: stay in this worktree, commit small, never push, print `<promise>COMPLETE</promise>` only after the checks pass, print `BLOCKED:`
  otherwise. `beads.hbs` adds "never close the bead"; `json.hbs` adds "never edit the PRD file", because ralph-tui
  marks the story done.
- [ ] With `--agent=claude`, owns `.ralph-tui/worker/claude.json` with `permissions.deny` for `git push`,
  `bd close`, `bd update` setting status `closed` (in the form US-001 proved), `bd create`, `bd delete`,
  `bd reopen`, `bd dolt push`, `git worktree`, `ralph-tui` and `.ralph-tui/bin/*`. ralph-tui closes the bead itself,
  from its own process (`bd close <id> --force`), so the denies never block it.
- [ ] Owns `.gitignore` section `ralph-tui`: `/.ralph-tui/*`, `!/.ralph-tui/config.toml`, `!/.ralph-tui/templates/`,
  `!/.ralph-tui/worker/`, `!/.ralph-tui/bin/`, `/.worktrees/*`, `!/.worktrees/README.md`.
- [ ] No `AGENTS.md` section. `RalphTui`'s class doc, rendered by `./cook help`, names the ralph-tui 0.12 and bd 1.3
  traps and Codex's per-path hook trust.
- [ ] Tests: both agents on an empty kitchen (exact seed text, worker file, exact `cook.lock`); with `git#worktrees` in both orders, `.worktrees/README.md` stays tracked; with `beads#tracker` in one invocation.
- [ ] `prd-story.groovy` and `bead-task.groovy` are deleted.

### US-006: ralph#ralph-tui launcher, clean and mise tasks
**Description:** As whoever launches ralph (a person, a coordinating agent or a schedule), I want one launcher that
names the worktree after the branch, so that every session finds and cleans runs the same way.

**Acceptance Criteria:**
- [ ] Owns `.ralph-tui/bin/run` (0755). `run <epic|prd.json> [ralph-tui run args...]` creates or reuses
  `.worktrees/ralph/<name>` on `ralph/<name>` from `origin/<default branch>`, where `<name>` is the epic id, or the
  file name without `.json` for a file argument. The argument is a file when it ends in `.json`. It runs
  `exec ralph-tui run --epic <id>` (or `--prd <absolute path>`) there, passing the remaining arguments through, and
  first prints the path and branch.
- [ ] Owns `.ralph-tui/bin/clean` (0755). `clean <name> [--force]` refuses when the run branch has unmerged commits or
  the worktree is dirty, unless `--force`. Otherwise it removes the worktree, the worker worktrees, merged
  `ralph-parallel/*` branches and the run branch.
- [ ] When `mise.toml` exists in the draft: owns `.mise/tasks/ralph/run` and `clean` (0755, each
  `exec .ralph-tui/bin/<name> "$@"`), and keys `tools.bun = "1.3.6"` and `tools."npm:ralph-tui" = "0.12.0"`.
- [ ] Tests: no mise files without `mise.toml`; with it, exact `[tools]` and wrappers, and discard restores the file.
  The scripts are executable in the kitchen. `run` and `clean` pass `sh -n`.
- [ ] `cookbook/ralph/run.sh` is deleted.

### US-007: Publish the four cookbook artifacts
**Description:** As a user of the cookbook, I want `git`, `beads` and `ralph` published beside `agents`, so that
`./cook io.github.lmarqs.cookbook:<artifact>:<version>#<recipe>` resolves for each.

**Acceptance Criteria:**
- [ ] `cookbook/build.gradle` builds sources and javadoc jars for `agents`, `git`, `beads` and `ralph` in one loop.
- [ ] `centralBundle` and `centralBundleTest` in the root `build.gradle` expect four cookbook artifacts, and
  `./gradlew centralBundle centralBundleTest -PreleaseVersion=0.0.0` passes.
- [ ] Docs that say only `agents` runs or is published now name all four: `README.md`, `cookbook/README.md`,
  `docs/adrs/0029-cookbook-distribution.md`, `docs/architecture.md` and `AGENTS.md`. `AGENTS.md` stays under
  800 words.

### US-008: The cookbook README shows real help
**Description:** As a reader of `cookbook/README.md`, I want the rendered-help example to be the real
`#ralph-tui` help, so that the example cannot drift from the recipe.

**Acceptance Criteria:**
- [ ] The example matches the output of `./cook help io.github.lmarqs.cookbook:ralph#ralph-tui` character for
  character.

### US-009: End-to-end proof
**Description:** As the maintainer, I want the whole setup proven in a fresh repository before release.

**Acceptance Criteria:**
- [ ] In a fresh scratch repository with the CLI built from `main`: cook `agents#seed`, `agents#connect` for both
  harnesses, `git#worktrees`, `beads#tracker --share=clone` and `ralph#ralph-tui --agent=claude --agent=codex`; then
  run `bd init` as the after-cooking text says.
- [ ] A Claude session's context contains `bd prime` output.
- [ ] `.ralph-tui/bin/run <epic> --headless` with the fake agent completes one bead in `.worktrees/ralph/<epic>`, and
  `.ralph-tui/bin/clean <epic>` removes it after merge.
- [ ] In a second fresh scratch repository, with no Beads and no layout: cook only
  `ralph#ralph-tui --agent=claude`, and `bd` is not on `PATH`. `.ralph-tui/bin/run tasks/prd.json --headless` with the fake agent
  completes one story in `.worktrees/ralph/prd`, and nothing under `.beads/` is created.
- [ ] Results go on #42. A defect gets a `fix` commit with a test that failed before it.

### US-010: Release and follow-up
**Description:** As the maintainer, I want the release started only with my approval, and the `ralph-tui-run` skill
pointed at the new interface.

**Acceptance Criteria:**
- [ ] The agent stops and asks before starting the `release` workflow.
- [ ] An issue in `lmarqs/skills` asks `ralph-tui-run` to read `.worktrees/README.md` and use `.ralph-tui/bin/run`
  when the project has it, and is linked from #42.

## Functional Requirements

- FR-1: `agents#connect` runs every `.agents/hooks/session-start.d/*.sh` at session start in each connected harness.
- FR-2: Only `agents#connect` owns harness session-start keys. Tool recipes write only into `session-start.d/`.
- FR-3: `git#worktrees` takes no arguments and owns only its `.gitignore` section and a seeded README.
- FR-4: `beads#tracker` cooks with no layout and no harness. It writes its `AGENTS.md` section only when `AGENTS.md`
  exists, and its hook only when the layout exists.
- FR-5: `ralph#ralph-tui` never writes outside `.ralph-tui/`, `.gitignore`, and, with `mise.toml`, `.mise/tasks/ralph/`
  and `mise.toml`'s `tools` keys.
- FR-6: A worker started by ralph-tui from the seeded config cannot push, close, create, delete or reopen a bead, push
  the Dolt remote, manage worktrees or start ralph-tui.
- FR-7: Run worktrees live at `.worktrees/ralph/<name>` on branch `ralph/<name>`.
- FR-8: No recipe pushes, opens a pull request or runs `bd`.

## Non-Goals

- Adopting existing Beads or ralph-tui setups, including medprev-rest-api and medprev-management.
- A worktree location recommended by the agents layout.
- Delivery: pushing, pull requests or merge strategy. Each session chooses.
- Writing bd's own files (`config.yaml`, `metadata.json`, hooks, database) or running `bd setup`.
- A policy role for ralph workers in `.agents/permission/policy.yaml`.
- Other loop runners and orchestrators. They can plug in later through the same extension points.
- `--dangerously-bypass-hook-trust` in the seeded Codex flags.

## Technical Considerations

- Recipes compile only against their own artifact's models (ADR 0025 §6). `current(path)` sees files written by
  earlier recipes in the same invocation, which is how the "only when it exists" rules work in any order.
- Seeds are written only when absent, are not recorded in `cook.lock`, and are ignored by the overlap check. Cook
  refuses to write through symlinks.
- `agents#seed` refuses an existing `AGENTS.md` unless given `--force` or `--skip`. So `beads#tracker` writes its
  section only when the file already exists.
- `beads#tracker` and `ralph#ralph-tui` write the same `.worktrees` pair as `git#worktrees`. Whatever the order,
  the last section re-includes the README.
- Spotless `misc` checks cookbook `.md`, `.sh` and `.toml`; `.hbs` files are checked only by `git diff --check`.
- Codex trusts project hooks per checkout path, and each worktree is a new path. Session-start context may not reach
  Codex workers. The help names the flag and the seed leaves it out.

## Success Metrics

- US-009 passes as written in one attempt after US-008.
- The cookbook publishes four artifacts. Each new recipe's empty-kitchen test pins its exact `cook.lock`.

## Open Questions

None blocking. US-001 settles the Codex hook shape, bd's write path from a sandboxed Codex worker and bd 1.3.1's
ignore lines. If bd cannot write from that worker, Codex workers report through their completion text and ralph's own
process records progress.
