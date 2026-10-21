# PRD: Beads, ralph-tui and worktrees as cookbook plugins on the agents layout

Tracking issue: #42. Source: the approved plan, grilled with the maintainer, the architecture review on #42, and two
sibling repositories (medprev-rest-api, medprev-management) as evidence of what the recipes must carry. They are not
targets.

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
| Worker permissions | a policy role and a `bd` shim | none; `danger-full-access`, because the sandbox refuses the Beads database in the main checkout |

Both also share: `configVersion = "2.1"`, `autoCommit = true`, a prompt that forbids closing beads and pushing, a
launcher that runs ralph-tui in a dedicated worktree, `--epic` on every run, and a clean step.

This PRD adds three cookbook artifacts, one extension point on the agents layout, and a documentation standard that
describes every recipe by what it provides, consumes and connects to.

### The connection rule

A recipe provides capabilities and declares what it can connect to. It never decides for the team what a connection
means. Connections are made at three times by three actors, and every recipe's help says which:

| When | Who | What | Examples |
| --- | --- | --- | --- |
| Cook time | Cook, automatically | Mechanical wiring with one correct form, cheap to undo | a hook script in `session-start.d/`, an ignore line, a mise wrapper |
| Cook time | The person, by parameter | Naming the other side of a connection | `--harness`, `--role`, `--settings`, `--share` |
| Commit time | The team, in seeds it owns | Anything that expresses a policy | what a worker may do, which agent entries exist, how a worktree is set up |
| Launch time | The session | Run-time choices | which agent entry, which epic or PRD, model, headless, serial or parallel, delivery |

"Maximum setup" means everything mechanical is written, every connection point is visible in help and after-text, and
nothing is assumed about what people want. Each artifact cooks alone, with no layout and no harness, and has a complete
answer for someone who never cooked `agents`.

### Decisions, as grilled

1. Platform and plugins: `agents` publishes the extension points (`session-start.d/`, roles, Launching table). Tool
   artifacts name them only as path conventions and still cook on their own.
2. Beads does not imply a harness.
3. Worktrees belong to git. The path is the branch: `.worktrees/<branch>`. Ralph runs use the branch `ralph/<name>` at
   `.worktrees/ralph/<name>`.
4. Delivery is chosen per session: the recipes never push or open pull requests.
5. Workers always run unattended, as `claude --print` or `codex exec`. Watching the TUI gives pause and quit, never
   approvals.
6. One vocabulary: `--harness` names the program (Claude Code, Codex) in every recipe; ralph-tui calls the same thing an
   agent plugin, which the glossary notes.
7. Model and effort are never Cook's. Model: per run (`ralph-tui run --model`) or per entry (`[agents.options] model`).
   Effort: Claude per run through `CLAUDE_CODE_EFFORT_LEVEL`, which ralph-tui passes through; Codex per entry
   (`-c model_reasoning_effort=...` in `defaultFlags`). The seeds set neither.
8. Upgrades follow Cook's default: seeds keep the team's edits, and owned files update through the reviewed diff.
9. Greenfield only: no adoption path, leftover listing or migration.

## Interfaces

### `agents` (platform)

| | |
| --- | --- |
| API | `#seed`; `#connect --harness=claude\|codex...`; stable paths: `AGENTS.md`, `.agents/permission/policy.yaml`, `.agents/adapters/<harness>/`, `.agents/hooks/session-start.d/`, `.agents/adapters/claude/roles/<role>.json`, the Launching table in `.agents/README.md` |
| Produces | the layout; rendered permissions and role files; links; the `.agents/hooks/session-start` dispatcher and one session-start hook key per harness; `.gitignore` section `harnesses` |
| Consumes | `policy.yaml` (team); `.agents/adapters/codex/hooks.json` when present (team); every `.agents/hooks/session-start.d/*.sh` at run time |
| Extension points | a script in `session-start.d/` (plain text out, harness name in `$1`); a role in `policy.yaml`, launched with its rendered file; a line in the Launching table |

### `git#worktrees`

| | |
| --- | --- |
| API | no parameters; the rule `.worktrees/<branch>`; the optional `.worktrees/setup` script a person or a runner runs in a new worktree |
| Produces | `.gitignore` section `worktrees` (`/.worktrees/*`, `!/.worktrees/README.md`); seed `.worktrees/README.md` |
| Consumes | nothing |

### `beads#tracker`

| | |
| --- | --- |
| API | `--share=team\|clone` (required); `--harness=claude\|codex...` (optional, many); after-text: `bd init …`, team-mode config, the `AGENTS.md` line, the hook line per harness |
| Produces | `.gitignore` section `beads`; seed `.beads/PRIME.md`; with `--harness`: the owned `hooks.SessionStart` key in `.claude/settings.json` (`bd prime`) and the owned SessionStart entry in `.codex/hooks.json` (`bd codex-hook SessionStart`); with `.agents/hooks/` in the draft: owned `session-start.d/beads.sh` |
| Consumes | `bd` at run time; optional: the layout's `session-start.d/`, detected |
| Never touches | `AGENTS.md`, `.beads/config.yaml`, `metadata.json`, bd's hooks, the database, the Dolt remote |

### `ralph#ralph-tui`

| | |
| --- | --- |
| API | `--harness=claude\|codex...` (required, many; the first is `defaultAgent`); `--role=<name>` or `--settings=<path>` (optional, exclusive, Claude entries only); `.ralph-tui/bin/run <name> (--epic <id> \| --prd <file>) [--base <ref>] [ralph-tui run args…]`; `.ralph-tui/bin/clean <name> [--force]`; the rule `.worktrees/ralph/<name>` on `ralph/<name>`; `mise run ralph:run`, `ralph:clean` when `mise.toml` exists |
| Produces | seeds: `config.toml` (one `[[agents]]` per harness, `tracker = "beads"`), `templates/beads.hbs`, `templates/json.hbs`; owned: `bin/run`, `bin/clean`, `.gitignore` section `ralph-tui`, mise task wrappers |
| Consumes | `ralph-tui` and `git` at run time; `bd` only when a run passes `--epic`; `.worktrees/setup` when it exists; `--role`: `.agents/adapters/claude/roles/<name>.json` must be in the draft, refused otherwise; `--settings`: the path as given, never written; `mise.toml`, detected, for the wrappers only |
| Never touches | `AGENTS.md`, `policy.yaml`, any settings or role file, version pins, remotes, pull requests, the tracker's state |

### Contact points

| Contact | Connected by |
| --- | --- |
| beads → session context | Cook (`session-start.d/`), or the person (`--harness`) |
| ralph → worker limits | the person (`--role` or `--settings`); the session (`--agent <entry>`); Codex workers by the sandbox |
| ralph → tracker | the session (`--epic` or `--prd`) |
| ralph → worktree setup | the team (`.worktrees/setup`) |
| ralph → base branch | the session (`--base`, default `HEAD`) |
| ralph → mise | Cook (wrappers only) |
| anything → `AGENTS.md` | the team, from each after-text |
| anything → delivery | the session |

## Goals

- In an empty repository, cooking `git#worktrees`, `beads#tracker`, `ralph#ralph-tui` and the agents layout in any
  order gives a setup where every session starts with `bd prime` context, and `.ralph-tui/bin/run <name> --epic <id>`
  completes a bead in `.worktrees/ralph/<name>`.
- In an empty repository with no layout and no `bd`, `ralph#ralph-tui --harness=claude` and
  `.ralph-tui/bin/run <name> --prd <file>` complete a story.
- Each artifact's help reads as API, Produces, Consumes and Connections, and the tests prove the Connections section.

## Quality Gates

These commands must pass for every user story:
- `./gradlew check cliBuild jpackageTest`
- `git diff --check`

Each story is one or more commits titled `<type>(<area>): <sentence>`, test-first. The commits are pushed to `main`
with a comment on #42. US-002 and US-010 commit only if they find a defect.

## User Stories

### US-001: The recipe documentation standard
**Description:** As a reader of `./cook help`, I want every recipe described by what it provides, consumes and connects
to, so that I can compose recipes without reading their source.

**Acceptance Criteria:**
- [ ] `docs/ux/messages-and-docs.md` ("Cookbook units") and `cookbook/README.md` ("Recipe documentation") define the
  `@help` sections in order: Lead; API (parameters as invocations per situation, and the stable paths, scripts,
  commands and naming rules the recipe publishes); Consumes (tools at run time with the versions written for, what it
  reads in the draft and what each triggers, other artifacts' paths it names, what the person does by hand first);
  Produces (Target, Part, When and kind: owned, seed, link; closing with Never touches); Connections (each contact
  point and who makes it: Cook, the person, the team, the session; order and arguments that must agree); Run-time
  effects (what installed scripts do when run); Re-cooking (arguments to repeat, what an upgrade changes, how
  conditional parts behave under `--check`); Traps (known faults of the versions written for). `@yield` stays After
  cooking.
- [ ] `docs/ux/generated-content-standards.md` adds: every owned section opens with a comment line naming the recipe
  and the arguments it was cooked with; a refused connection names what to define and what to cook.
- [ ] `#seed` and `#connect` help follow the sections; `#connect`'s `harnesses` section opens with its argument line.
- [ ] `cookbook/README.md`'s rendered-help example is regenerated from `#connect`.

### US-002: Spike the unknowns before writing recipes
**Description:** As the maintainer, I want the behaviors the design relies on proven in a scratch repository, so that
the recipes encode what the tools do rather than what their docs say.

**Acceptance Criteria:**
- [ ] In a scratch repository with `bd init` and one epic, the planned `.ralph-tui/config.toml` passes
  `ralph-tui doctor`. ralph-tui's debug output shows `defaultFlags` ahead of `--print` for Claude and `exec` for
  Codex. A headless run with the fake agent from the `testing-tui-runtime` skill completes one bead.
- [ ] With `tracker = "beads"` in the config and no `bd` on `PATH`, `ralph-tui run --prd ./prd.json` completes one
  story with the fake agent.
- [ ] A Claude worker launched as the seeded entry (`--permission-prompts none --permission-mode acceptEdits`) edits a
  file and runs a command the project's `.claude/settings.json` allows; one it does not allow is denied.
- [ ] `claude --print --settings <role file>` adds the role's denies to the project's settings: `git push` and
  `bd update <id> --status closed` are refused, and the deny rules that do it are recorded.
- [ ] A Claude `hooks.SessionStart` hook and a Codex SessionStart hook running one dispatcher both add its output as
  context. Recorded: the envelope each harness takes (Codex: `hookSpecificOutput.additionalContext`, as
  `bd codex-hook SessionStart` prints it; whether Claude takes the same), the Codex `hooks.json` shape and matcher, and
  how the dispatcher encodes plain text into it with the tools a kitchen has.
- [ ] Recorded: whether `bd update` from a Codex `workspace-write` worker with network off reaches the database
  through git's common directory, outside the worktree. medprev-management's config says it does not.
- [ ] Recorded: `CLAUDE_CODE_EFFORT_LEVEL` set when `bin/run` starts reaches a Claude worker's reported effort.
- [ ] Results go on #42. The plan changes only where a result contradicts it, and the comment names each change.

### US-003: agents#connect owns the session-context contract
**Description:** As a tool recipe, I want to add session-start context by writing one script into a directory, so
that I never learn a harness's hook format and never collide with another tool over the same hook key.

**Acceptance Criteria:**
- [ ] `#connect` owns `.agents/hooks/session-start` (mode 0755). Run as `session-start <harness>`, it runs every
  `.agents/hooks/session-start.d/*.sh` in name order with the harness name as `$1`, joins their standard output, and
  prints it as `{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"<text>"}}`, which both
  harnesses take (US-002), JSON-encoded with `sh` and `awk` only. It exits 0, and prints nothing for an empty or
  missing directory.
- [ ] For `--harness=claude`, `#connect` owns `hooks.SessionStart` in `.agents/adapters/claude/settings.json`, running
  `"$CLAUDE_PROJECT_DIR"/.agents/hooks/session-start claude`. For `--harness=codex`, it owns the SessionStart entry in
  `.agents/adapters/codex/hooks.json` (matcher `startup|resume|clear`), linked as `.codex/hooks.json`. Its command
  resolves the root through `git rev-parse --show-toplevel`, because Codex runs a relative hook path from the session's
  directory (US-002).
- [ ] `CookbookCookTest` cases: both harnesses get the key and the script. A `hooks.PreToolUse` the team wrote
  survives a second cook. Discard removes only what `#connect` wrote. A second cook prints `Already cooked.`.
- [ ] `design.md` gains "Tools that add session context", and "Cook owns only the keys it renders" lists the
  session-start hook. The `SKILL.md` owners table and the hooks row of "Where a change goes" say a team's own
  session-start script goes in `.agents/hooks/session-start.d/`. The trap that Codex trusts hooks per checkout path, so
  a worktree runs none without `--dangerously-bypass-hook-trust`, is recorded there.
- [ ] This repository's cooked layout is re-cooked, so `.agents/skills/agents-layout/` and `cook.lock` match.

### US-004: git#worktrees
**Description:** As anyone working in parallel branches, I want one ignored worktree root with a README that states
the naming rule and names the setup script, so that people, agents and loop runners put worktrees in the same place and
make them runnable the same way.

**Acceptance Criteria:**
- [ ] `cookbook/git/Git.groovy` becomes the model this recipe compiles against. `worktrees.groovy` takes no arguments.
- [ ] It owns `.gitignore` section `worktrees`: the argument line, `/.worktrees/*`, `!/.worktrees/README.md`.
- [ ] It seeds `.worktrees/README.md`: what the directory is; the rule `.worktrees/<branch>`;
  `git worktree add .worktrees/<branch> -b <branch>`, `git worktree remove .worktrees/<branch>` and
  `git worktree prune`; `.worktrees/setup`, an optional executable the team writes and runs in a new worktree
  (dependencies, `mise trust`, local files), which runners run too; the in-repo traps from `design.md` (Claude loads
  the worktree's own `AGENTS.md`; a `CLAUDE.local.md` in the main checkout reaches the worktree and turns it off).
- [ ] Tests: exact files and `cook.lock` on an empty kitchen; a second cook prints `Already cooked.`; a hand-edited
  README is kept; `git check-ignore` passes on `.worktrees/x/a` and not on `.worktrees/README.md`.
- [ ] `./cook help io.github.lmarqs.cookbook:git#worktrees` renders the standard's sections.

### US-005: beads#tracker
**Description:** As a team using Beads, with or without agents, I want one recipe to write the ignore rules, the
prime text and, when I name a harness or have the layout, the session hook, so that `bd init` is the only manual step.

**Acceptance Criteria:**
- [ ] `--share` is required, with choices `team` and `clone`; `--share=fork` is refused naming the choices.
  `--harness` is optional, many, with choices `claude` and `codex`.
- [ ] `.gitignore` section `beads` opens with the argument line. `clone` writes `/.beads/*` and `!/.beads/PRIME.md`;
  `team` writes `/.beads/issues.jsonl`, because the export carries whatever people paste into issues, and bd's own
  `.beads/.gitignore` covers the rest. The after-text says `bd init` appends unrooted lines of its own to the root
  `.gitignore`, outside the section, which the team may delete.
- [ ] Seeds `.beads/PRIME.md` with the rules for that `--share`, including "a worker never closes its bead or
  pushes".
- [ ] With `--harness=claude`, owns the key `hooks.SessionStart` in `.claude/settings.json`, running `bd prime`. With
  `--harness=codex`, owns the SessionStart entry in `.codex/hooks.json`, running `bd codex-hook SessionStart`. With the
  layout, those files are links and the engine refuses the write; the help says to leave `--harness` out then.
- [ ] Owns `.agents/hooks/session-start.d/beads.sh` (0755) when `.agents/hooks/` is in the draft. It prints `bd prime`
  and nothing when `bd` or the database is missing. `bd prime` is 5.5 KB and fires on every session start, including
  each worker iteration; the help says so.
- [ ] No `AGENTS.md` section. The after-text gives the line to add: context arrives from `bd prime` at session start;
  if it is missing, run `bd prime`.
- [ ] The after-text gives `bd init --prefix <p> --skip-hooks --skip-agents --non-interactive`. For `team`, it adds
  `bd config set sync.remote <url>`, `bd config set export.auto true`, `bd hooks install --beads` and
  `git config core.hooksPath .beads/hooks`, warning that `core.hooksPath` is per clone and replaces any hooks path the
  project already uses. It says `bd init` makes a git commit of its own files and the root `.gitignore`. It says never to run `bd setup`, and, with the layout, to cook `agents#connect`.
- [ ] Never writes `.beads/config.yaml`, `metadata.json`, bd's git hooks or the database.
- [ ] Tests: `team` and `clone` on an empty kitchen (exact files and `cook.lock`, second cook unchanged, hand-edited
  seed kept); `--harness=claude --harness=codex` without the layout (exact keys, team's other hooks kept, discard
  restores); with the layout, the hook file appears and `--harness` is refused; `beads` with `agents#connect` in both
  orders.
- [ ] `cookbook/beads/ready.sh` is deleted.

### US-006: ralph#ralph-tui configuration and prompts
**Description:** As a team using ralph-tui, I want a seeded configuration and prompts that run unattended on the first
try and connect to worker limits only when I name them.

**Acceptance Criteria:**
- [ ] `--harness` is many and required, with choices `claude` and `codex`; `defaultAgent` is the first. `--role=<name>`
  and `--settings=<path>` are optional and exclusive; both together are refused. `--role` is refused when
  `.agents/adapters/claude/roles/<name>.json` is not in the draft, naming `roles.<name>` in
  `.agents/permission/policy.yaml` and `agents#connect` as the fix. `--settings` names a file the recipe never writes.
- [ ] Seeds `.ralph-tui/config.toml` with `configVersion = "2.1"`, `tracker = "beads"`, `defaultAgent`,
  `maxIterations = 10` with a comment stating what ralph-tui counts, `autoCommit = true`,
  `[errorHandling] strategy = "skip"`, `[parallel] directMerge = true` and one `[[agents]]` per harness, named after it.
  `defaultFlags` sits at the top of the entry and plugin settings under `[agents.options]`: ralph-tui 0.12 replaces a
  `defaultFlags` under options and drops unknown top-level agent keys, silently (US-002). Claude:
  `defaultFlags = ["--permission-prompts", "none", "--permission-mode", "acceptEdits"]` plus
  `"--settings", "<file>"` from `--role` or `--settings`; `skipPermissions = false`. Codex:
  `defaultFlags = ["-c", "approval_policy=never", "-c", "sandbox_workspace_write.network_access=false"]`,
  `fullAuto = false`, `sandbox = "workspace-write"`. No model, no effort.
- [ ] Seeds `.ralph-tui/templates/beads.hbs` and `json.hbs`; ralph-tui loads `<tracker>.hbs` for the tracker a run
  uses. Both say: stay in this worktree, leave committing to ralph-tui (`autoCommit`; a Codex `workspace-write`
  worker cannot write `.git`, US-002), never push, print `<promise>COMPLETE</promise>` only after the
  checks pass, `BLOCKED:` otherwise. `beads.hbs` adds "never close or update the bead; ralph-tui closes it";
  `json.hbs` adds "never edit the PRD file; ralph-tui marks the story done". Neither asks a worker to run `bd`: the
  task text is in the prompt, and a Codex `workspace-write` worker can neither read nor write the database.
- [ ] Owns `.gitignore` section `ralph-tui`: the argument line, `/.ralph-tui/*`, `!/.ralph-tui/config.toml`,
  `!/.ralph-tui/templates/`, `!/.ralph-tui/bin/`, `/.worktrees/ralph/`.
- [ ] No `AGENTS.md` section, no deny file, no version pins. The class doc shows: a role-bound entry and a bypass entry
  (`skipPermissions = true`) as entries to add; model per run or per entry; Claude effort through
  `CLAUDE_CODE_EFFORT_LEVEL`, Codex effort per entry; ralph-tui 0.12's Claude plugin accepts only `sonnet`, `opus`,
  `haiku`; its parallel mode starts blocked children and refuses fewer than three tasks; Codex workers in worktrees run
  no project hooks without `--dangerously-bypass-hook-trust`; a Codex `workspace-write` worker can neither read nor
  write the Beads database nor `.git` (US-002); Claude ignores the project's `permissions.allow` in a workspace it has
  not trusted, so an unattended worker's allows come through `--role` or `--settings`; denies are prefix rules, a guardrail, and the structural limit is a worker without push
  credentials.
- [ ] Tests: both harnesses on an empty kitchen (exact seed text, exact `cook.lock`); `--role=worker` with the role
  file present and absent (refusal text); `--settings=x.json` (flag written, file untouched); both together refused;
  with `git#worktrees` in both orders; with `beads#tracker` in one invocation.
- [ ] `prd-story.groovy` and `bead-task.groovy` are deleted.

### US-007: ralph#ralph-tui launcher, clean and mise tasks
**Description:** As whoever launches ralph (a person, a coordinating agent or a schedule), I want one launcher that
names the worktree after the branch, so that every session finds, sets up and cleans runs the same way.

**Acceptance Criteria:**
- [ ] Owns `.ralph-tui/bin/run` (0755): `run <name> (--epic <id> | --prd <file>) [--base <ref>] [ralph-tui run
  args…]`. It refuses when `git status --porcelain .ralph-tui` in the main checkout is non-empty, because an
  uncommitted cook is absent from every worktree. It creates or reuses `.worktrees/ralph/<name>` on `ralph/<name>`
  from `<ref>`, default `HEAD`; runs `.worktrees/setup` there when executable; prints the path and branch; then
  `exec ralph-tui run --epic <id>` or `--prd <absolute path>` there, passing the remaining arguments through. It sets
  no environment and strips none, so `CLAUDE_CODE_EFFORT_LEVEL` reaches Claude workers.
- [ ] Owns `.ralph-tui/bin/clean <name> [--force]` (0755). It refuses when the run branch has commits not in `HEAD`
  or the worktree is dirty, unless `--force`; the message says a squash merge needs `--force`. It removes the run
  worktree, every worktree whose branch is `ralph-parallel/*` found through `git worktree list --porcelain`, those
  branches, and the run branch.
- [ ] When `mise.toml` is in the draft: owns `.mise/tasks/ralph/run` and `clean` (0755, each
  `exec .ralph-tui/bin/<name> "$@"`). No keys in `mise.toml`. The after-text gives `bun install -g ralph-tui@0.12.0`.
- [ ] Tests: no mise files without `mise.toml`; with it, the wrappers, and `mise.toml` unchanged. The scripts are
  executable in the kitchen and pass `sh -n`.
- [ ] `cookbook/ralph/run.sh` is deleted.

### US-008: Publish the four cookbook artifacts
**Description:** As a user of the cookbook, I want `git`, `beads` and `ralph` published beside `agents`, so that
`./cook io.github.lmarqs.cookbook:<artifact>:<version>#<recipe>` resolves for each.

**Acceptance Criteria:**
- [ ] `cookbook/build.gradle` builds sources and javadoc jars for `agents`, `git`, `beads` and `ralph` in one loop.
- [ ] `centralBundle` and `centralBundleTest` in the root `build.gradle` expect four cookbook artifacts, and
  `./gradlew centralBundle centralBundleTest -PreleaseVersion=0.0.0` passes.
- [ ] Docs that say only `agents` runs or is published now name all four: `README.md`, `cookbook/README.md`,
  `docs/adrs/0029-cookbook-distribution.md`, `docs/architecture.md` and `AGENTS.md`. `AGENTS.md` stays under
  800 words. `docs/features/glossary.md` says harness and ralph-tui's agent plugin name the same thing.

### US-009: The cookbook README shows real help
**Description:** As a reader of `cookbook/README.md`, I want the rendered-help example to be real output, so that the
example cannot drift from the recipe.

**Acceptance Criteria:**
- [ ] The example matches the output of `./cook help io.github.lmarqs.cookbook:ralph#ralph-tui` character for
  character.

### US-010: End-to-end proof
**Description:** As the maintainer, I want the whole setup proven in fresh repositories before release, with and
without the layout.

**Acceptance Criteria:**
- [ ] Scratch repositories live outside `/tmp`, because Codex's sandbox writes `/tmp` by default and gives false
  passes (US-002). Recorded: whether a worktree of a trusted checkout counts as trusted for Claude's project allows.
- [ ] Fresh scratch repository, no remote, CLI built from `main`: cook `agents#seed`, `agents#connect` for both
  harnesses, `git#worktrees`, `beads#tracker --share=clone`, `ralph#ralph-tui --harness=claude --harness=codex`; run
  `bd init` as the after-text says. A Claude session's context contains `bd prime` output.
  `.ralph-tui/bin/run <name> --epic <id> --headless` with the fake agent completes one bead in
  `.worktrees/ralph/<name>`, and `.ralph-tui/bin/clean <name>` removes it after merge.
- [ ] Second fresh scratch repository, no layout, no `bd` on `PATH`: cook only `ralph#ralph-tui --harness=claude`.
  `.ralph-tui/bin/run <name> --prd tasks/prd.json --headless` with the fake agent completes one story, and nothing
  under `.beads/` is created.
- [ ] Results go on #42. A defect gets a `fix` commit with a test that failed before it.

### US-011: Release and follow-up
**Description:** As the maintainer, I want the release started only with my approval, and the `ralph-tui-run` skill
pointed at the new interface.

**Acceptance Criteria:**
- [ ] The agent stops and asks before starting the `release` workflow.
- [ ] An issue in `lmarqs/skills`, linked from #42, asks `ralph-tui-run` to read `.worktrees/README.md`, use
  `.ralph-tui/bin/run` when the project has it, set Claude effort through `CLAUDE_CODE_EFFORT_LEVEL`, and refresh
  `references/harness-options.md`'s header (validated against Claude Code 2.1.306 and codex-cli 0.160.1 on #42).

## Functional Requirements

- FR-1: `agents#connect` runs every `.agents/hooks/session-start.d/*.sh` at session start in each connected harness,
  in the envelope that harness takes.
- FR-2: Only `agents#connect` owns the layout's session-start keys. Tool recipes write into `session-start.d/`, or, with
  `--harness` and no layout, into the harness's own file.
- FR-3: `git#worktrees` takes no arguments and owns only its `.gitignore` section and a seeded README.
- FR-4: `beads#tracker` cooks with no layout and no harness, and writes a hook only where the person or the draft names
  one.
- FR-5: `ralph#ralph-tui` never writes outside `.ralph-tui/`, `.gitignore` and, with `mise.toml`, `.mise/tasks/ralph/`.
- FR-6: `ralph#ralph-tui` writes no deny, settings, role or pin. Worker limits enter only through `--role`,
  `--settings` or an entry the team adds.
- FR-7: Run worktrees live at `.worktrees/ralph/<name>` on branch `ralph/<name>`, based on `HEAD` unless `--base`.
- FR-8: No recipe pushes, opens a pull request or runs `bd`.
- FR-9: Every owned section opens with a comment line naming the recipe and its arguments.
- FR-10: Every refused connection names what to define and what to cook.

## Non-Goals

- Adopting existing Beads or ralph-tui setups, including medprev-rest-api and medprev-management.
- A worktree location recommended by the agents layout, or a worker role shipped by it.
- Delivery: pushing, pull requests or merge strategy. Each session chooses.
- Writing bd's own files (`config.yaml`, `metadata.json`, hooks, database) or running `bd setup`.
- Model or effort parameters.
- Other trackers (`beads-bv`, `beads-rust`, `jira`): the team edits the seeded `tracker` line.
- Other loop runners and orchestrators. They plug in later through the same extension points.
- `--dangerously-bypass-hook-trust` in the seeded Codex flags.

## Technical Considerations

- Recipes compile only against their own artifact's models (ADR 0025 §6). `current(path)` sees files written by
  earlier recipes in the same invocation, which is how draft detection works in any order; the dispatcher reads its
  directory at run time, so hook order never matters.
- Seeds are written only when absent, are not recorded in `cook.lock`, and are ignored by the overlap check. Cook
  refuses to write through symlinks, which is what makes `beads --harness` safe beside the layout.
- `cook.lock` records no arguments; the argument line in each owned section (FR-9) is what re-cooks and `--check` read.
- A section that appears once its condition holds is drift under `--check`, by design; the Re-cooking help section
  says so for `beads.sh` and the mise wrappers.
- ralph-tui 0.12.0 verified: `--prd` without `--tracker` switches to `json`; `parallel.mode` defaults to `never`;
  `--variant` reaches neither Claude nor Codex; the worker environment loses only `*_API_KEY`, `*_SECRET_KEY`,
  `*_SECRET`; worker branches are `ralph-parallel/...`; `bd close <id> --force` is run by ralph-tui's own process.
- US-002 verified: both harnesses take plain stdout or the `hookSpecificOutput.additionalContext` envelope; Codex
  hook trust is keyed by the absolute path of `hooks.json`; ralph-tui passes `CLAUDE_CODE_EFFORT_LEVEL` to workers and
  `claude --print` honours it; role denies passed with `--settings` add to the project's, and `git -C . push` gets past
  a `git push` prefix rule.
- `bd` 1.3.1 verified: finds the main checkout's database from a worktree through git's common directory;
  `bd codex-hook SessionStart` prints the Codex JSON envelope; `bd init` appends unrooted lines to the root
  `.gitignore` and writes `.beads/.gitignore`.
- Spotless `misc` checks cookbook `.md`, `.sh` and `.toml`; `.hbs` files are checked only by `git diff --check`.

## Success Metrics

- US-010 passes as written in one attempt after US-009.
- The cookbook publishes four artifacts. Each new recipe's empty-kitchen test pins its exact `cook.lock`, and each
  Connections section has a test.

## Open Questions

None blocking. US-002 settles the dispatcher's envelope and encoding, bd's write path from a sandboxed Codex worker,
and the effort variable's reach. If bd cannot write from that worker, nothing changes: the prompts already ask workers
for no `bd` writes.
