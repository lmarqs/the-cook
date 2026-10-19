# PRD: Shared agents layout and the agents-layout skill

## Overview

The `io.github.lmarqs:agents` cookbook sets up the files coding agents read in a repository. Teams work with it in
several ways at once: one person runs Claude Code at the keyboard, another leaves Codex running unattended, a third
has Claude coordinate Codex workers through ralph-tui. Today's layout serves one person and one harness well and
breaks down in a team:

- `.claude/` is ignored as a whole, so a fresh clone, a cloud session or CI gets no shared setup until someone cooks;
- `Claude.permit` imposes `defaultMode: acceptEdits` on everyone;
- Claude reads instructions through a `.claude/CLAUDE.md` link while Codex reads `AGENTS.md` directly;
- the policy has no roles, no alternatives and no way to state a rule only one harness can express;
- the artifact ships three placeholder skills and no guidance on the layout itself.

This PRD reshapes the layout so every harness reads the same files the same way, makes the policy rich enough for
real teams, and ships a packaged `agents-layout` skill that guides people to adopt the layout, investigate misuse and
understand its reasoning. The layout guides; it does not control what people do on their own machines
([ADR 0028](../docs/adrs/0028-place-of-the-tool.md)).

Tracking issue: #38. A hand-written mock-up of the target is in `cooked/agentic/seed` (untracked; read only; it
predates some decisions, and US-011 lists where the result differs). Drafts of the skill are in
`tasks/agents-layout-draft/agents-layout/`.

## Goals

- Claude and Codex read one root `AGENTS.md` natively; no harness-only instruction files exist in the layout.
- `.agents/permission/policy.yaml` version 2 expresses commands, tools, alternatives, harness-only rules and
  project-defined roles, and renders to both harnesses.
- A fresh clone works with no cooking: the links in `.claude/` and `.codex/` are committed, and everything else in
  those directories stays ignored.
- The packaged `agents-layout` skill answers: where does a change go, why is it shaped this way, how do I adopt it,
  and what went wrong.
- The release is one clean major: no version 1 policy, no old path, no `#instructions`.

## Quality Gates

These must pass for every user story:

- `./gradlew check cliBuild jpackageTest` - tests, CodeNarc, Spotless, the CLI build and the packaged-CLI test
- `git diff --check` - no whitespace errors

Run `./gradlew spotlessApply` first when Groovy or Gradle files changed, and include its changes.

Every story that changes files ends with:

- A commit whose subject follows `<type>(<area>): <sentence>` (see `AGENTS.md`). The repository signs commits by
  default (`commit.gpgsign`); do not pass `--no-gpg-sign`. A story that changes what existing kitchens get uses
  `feat(cookbook)!:`. The message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `git push` to `main`.
- A comment on issue #38 naming the story, the commit, what changed and what was verified.

A spike story changes no files; it ends with its comment on #38 only.

Never stage `.claude/settings.json`, the repository's own `.gitignore`, `.agents/policy.yaml`, `.beads/` or
`cooked/`: they are the user's own uncommitted files. Do not edit `~/.claude/` or `~/.codex/`, except for a
temporary file a spike creates, records in its comment and removes.

Each story updates, in the same commit, the tests it breaks and the `@help` and `@yield` texts of every recipe whose
writes it changes, so each help table lists exactly what its recipe writes.

## User Stories

### US-001: Spike - Claude role overlay through --settings

**Description:** As the implementer, I want to know whether a deny rule in a file passed with `claude --settings`
blocks a command the project settings allow, so that roles render for Claude in a form that works.

**Acceptance Criteria:**
- [ ] In a scratch git repository made with `mktemp -d` (not in this checkout), `.claude/settings.json` allows
      `Bash(git commit *)` and an overlay file denies it.
- [ ] `claude -p --permission-prompts none --settings <overlay> "<ask it to run git commit --allow-empty -m probe>"`
      is run; `git rev-list --count HEAD` before and after shows whether the commit happened.
- [ ] The same run without `--settings` is the control and must commit. If it does not (for example because project
      allow rules wait for workspace trust), the spike records why and is inconclusive, not a pass or a fail.
- [ ] On fail, the same check is repeated with `--disallowedTools "Bash(git commit *)"` (comma- or space-separated
      rules, per `claude --help`), and its result is recorded as the fallback.
- [ ] The result (pass, fail or inconclusive, the exact commands, Claude Code version) is posted on issue #38.

### US-002: Spike - Codex per-run role

**Description:** As the implementer, I want to know whether a single Codex run can add a role's rules from the
repository, so that roles render for Codex only if a run can actually select them.

**Acceptance Criteria:**
- [ ] Candidates tried, each recorded: `codex exec -c <key>=<value>` overrides, named permission profiles
      (`CODEX_PERMISSION_PROFILE` and any matching `config.toml` key in the Codex docs), any rules-path option, and
      `codex exec --profile`, which layers `$CODEX_HOME/<name>.config.toml`.
- [ ] Pass means: a `codex exec` run started with a candidate refuses `git commit` because of a role rules file in the
      repository, while a plain run in the same scratch repository commits. A candidate that works only with a file
      each person must create under `$CODEX_HOME` is recorded as working but does not pass.
- [ ] The result, the Codex version and the working command (if any) are posted on issue #38.

### US-003: Spike - Claude reading .agents/skills natively

**Description:** As the implementer, I want to know whether Claude Code discovers skills in `.agents/skills/` without
a `.claude/skills` link, so that the layout links only what a harness cannot find by itself.

**Acceptance Criteria:**
- [ ] A scratch repository holds `.agents/skills/probe/SKILL.md` with a distinctive name and description, and no
      `.claude/` directory.
- [ ] `claude -p "List the skills available to you, names only."` is run; pass means `probe` is listed.
- [ ] The result and the Claude Code version are posted on issue #38.

### US-004: Both harnesses read AGENTS.md natively

**Description:** As a team member using either harness, I want Claude and Codex to read the root `AGENTS.md` by
themselves, with no instruction file of their own, so that both get the same instructions.

**Acceptance Criteria:**
- [ ] `cookbook/agents/instructions.groovy` is deleted.
- [ ] Removed from `cookbook/agents/Claude.groovy`: `instruct`, `readInstructions`, `instructionsSection`,
      `refuseImportsElsewhere`, the `instructions` and `sharedInstructions` fields, and the `CLAUDE.md` entry of
      `adapterLinks`.
- [ ] Removed from `cookbook/agents/Codex.groovy`: `instruct`, `readInstructions` and the `codex` section of
      `AGENTS.md`.
- [ ] `Harness.readInstructions` and its call in `Agents.connect` are removed; the class doc comments of `Agents`,
      `Claude` and `Codex` no longer name them.
- [ ] `claude-settings.groovy` and `codex-settings.groovy` lose `--instructions`.
- [ ] `cookbook/cook/setup.groovy` still cooks (run once in a temporary kitchen); its help no longer lists
      `.agents/adapters/claude/CLAUDE.md`.
- [ ] A test shows `#connect --harness=claude --harness=codex` writes no `CLAUDE.md` at any path and no `codex`
      section in `AGENTS.md`.
- [ ] Every test in `core/src/test/groovy/thecook/CookbookCookTest.groovy` that cooked `#instructions` keeps what it
      proves: AGENTS.md seed cases cook `#seed`; section, symbolic-link, lock, discard and composition cases use
      `#connect`'s `.gitignore` section or a recipe the test writes (as the `notes.groovy` cases do).
      `refusesASecondAgentsImportInClaudeInstructions` is deleted with the behavior it tests.
- [ ] `grep -rn --include='*.groovy' '#instructions' core/src/test/groovy/thecook/CookbookCookTest.groovy cookbook`
      returns nothing. (`CoordinatesTest` parses the string only and stays.)
- [ ] Commit type `feat(cookbook)!:`.

### US-005: Policy version 2 model at .agents/permission/policy.yaml

**Description:** As a team, I want `.agents/permission/policy.yaml` version 2 to express commands, tools,
alternatives, harness-only rules and roles, so that one reviewed file holds the whole agreement.

**Acceptance Criteria:**
- [ ] `Policy.parse` in `cookbook/agents/Agents.groovy` accepts only `version: 2` and the top-level fields `version`,
      `commands`, `tools` and `roles`; any other version or field is refused with a message naming
      `.agents/permission/policy.yaml` and the field.
- [ ] `commands` and `tools` each accept `allow`, `ask` and `deny` lists; other keys are refused.
- [ ] A command entry is either a prefix or a harness entry. A prefix is a non-empty list whose items are words or
      non-empty lists of words, each word matching `[a-zA-Z0-9_.\/:=@+-]+`. A harness entry is a map with exactly
      the key `claude` and a non-empty string value. A `tools` entry must be a harness entry.
- [ ] `roles` maps names matching `[a-z][a-z0-9-]*` to a map with exactly the key `deny`, a list of command entries;
      `allow` or `ask` inside a role is refused with a message saying a role only adds denies.
- [ ] `Agents.policy()` reads `.agents/permission/policy.yaml`, and `starterPolicy()` seeds that path.
- [ ] `cookbook/agents/policy.template.yaml` becomes `version: 2` with no `instructions` field; its full comments
      come in US-006.
- [ ] `Policy.instructions` is gone, and `cookbook/cook/setup.groovy` no longer calls `agents.policy().instructions`.
- [ ] Plain prefixes render as before. Rendering of alternatives, harness entries, tools and roles lands in US-007
      and US-008, and no test asserts it in this story.
- [ ] `cooked/` is not committed, so no test reads it; a test that needs the mock-up's policy or output holds a copy.
- [ ] Tests in `CookbookCookTest` (using its `publishDirectory`, `cook` and `kitchen` helpers) cover a valid policy
      with every form, each refusal above with its message, and `version: 1` refused.
- [ ] Commit type `feat(cookbook)!:`.

### US-006: Starter policy version 2

**Description:** As a new user, I want the seeded policy to explain every field with commented examples and grant
nothing, so that I can fill it in without reading code.

**Acceptance Criteria:**
- [ ] `cookbook/agents/policy.template.yaml` explains entries, alternatives, harness entries, precedence (deny, then
      ask, then allow, in every harness and settings scope) and that an unattended run refuses what would ask.
- [ ] It has commented-out examples for `commands` (allow, ask, deny), `tools` and one role (`worker`), taken from
      `cooked/agentic/seed/.agents/permission/policy.yaml`; uncommented, it holds `version: 2` and empty `commands`
      lists only.
- [ ] Each commented example, uncommented, parses (a test uncomments them and parses the result).
- [ ] Parsed as is, it renders empty Claude `allow`, `ask` and `deny` lists and no Codex command rules.
- [ ] The test asserting the seeded text equals the template uses `.agents/permission/policy.yaml`.

### US-007: Claude rendering

**Description:** As a Claude user, I want the policy rendered into Claude's settings with no personal choice imposed,
so that Claude enforces exactly what the team agreed.

**Acceptance Criteria:**
- [ ] `Claude.permit` writes `permissions.allow`, `ask` and `deny` only; no `defaultMode`.
- [ ] A prefix renders one `Bash(<words> *)` rule per combination of alternatives, in entry order; a `claude` entry
      renders `Bash(<pattern>)` under `commands` and `<pattern>` as is under `tools`. For the mock-up's policy the
      result equals `cooked/agentic/seed/.agents/adapters/claude/settings.json` (a test holds both as copies).
- [ ] Roles render in the form US-001 found working: on pass, `.agents/adapters/claude/roles/<role>.json` holding
      `{"permissions":{"deny":[...]}}`, as in `cooked/agentic/seed/.agents/adapters/claude/roles/`; on fail, the
      fallback US-001 recorded. A role removed from the policy has its file removed on the next cook.
- [ ] Tests cover alternatives, both harness-entry placements, role files and their removal, and the absence of
      `defaultMode`.
- [ ] Commit type `feat(cookbook)!:` (kitchens lose `defaultMode`).

### US-008: Codex rendering

**Description:** As a Codex user, I want the same policy rendered into Codex rules, with any gap made visible, so
that the two harnesses differ only where Codex cannot express a rule.

**Acceptance Criteria:**
- [ ] A prefix renders one `prefix_rule` keeping alternatives as nested lists, for example
      `prefix_rule(pattern = ["gh", "pr", ["view", "list"]], decision = "allow")`.
- [ ] Each `claude` entry under `commands` renders a comment line in the `commands` section:
      `# Not enforced by Codex, Claude-only: <pattern>`. `tools` render nothing for Codex.
- [ ] For the mock-up's policy, the lines between the markers of the `commands` section equal those of
      `cooked/agentic/seed/.agents/adapters/codex/rules/cook.rules` (a test holds both as copies; the markers differ,
      since the mock-up uses a deferred marker format).
- [ ] Roles render according to US-002: on pass, `.agents/adapters/codex/roles/<role>.rules` and whatever the
      working selector needs; on fail, no Codex role files.
- [ ] Every rendered rules file loads: `codex execpolicy check --rules <file> git status` prints JSON with a
      `decision` (run by hand, noted in the issue comment).
- [ ] Tests cover nested patterns, the Claude-only comment and the role outcome.

### US-009: Committed links and the ignore section

**Description:** As a team, I want the harness links committed and everything else in `.claude/` and `.codex/`
ignored, with the files that make Claude skip `AGENTS.md` kept out of commits, so that a fresh clone works and the
shared state stays clean.

**Acceptance Criteria:**
- [ ] Claude links `.claude/settings.json` when settings exist, `.claude/commands`, and `.claude/skills` unless
      US-003 passed. No `.claude/hooks` or `.claude/memory` link.
- [ ] Codex links `.codex/config.toml`, `.codex/rules` and `.codex/hooks.json` when each is written. No
      `.codex/hooks` link; `Codex.readDirectories` keeps only its `skills` check.
- [ ] The `harnesses` section of `.gitignore` is `/.claude/*`, then `!/.claude/<link>` for each Claude link made,
      then `/.codex/*` and `!/.codex/<link>` for each Codex link made, then a comment saying Claude skips `AGENTS.md`
      while any of the following exists, then `CLAUDE.md`, `CLAUDE.local.md` and `**/.claude/CLAUDE.md`.
- [ ] The `!` lines come from the links each harness declares (a method on `Harness`, or an equivalent), so the
      section never names a link that is not made.
- [ ] `#seed` still seeds `.agents/{skills,commands,hooks,memory}/.gitkeep` and `.agents/README.md`.
- [ ] A test runs `git init` in its kitchen, cooks `#seed` and `#connect --harness=claude --harness=codex`, and
      asserts with `git check-ignore` that each link is not ignored, and that `.claude/settings.local.json`,
      `.claude/worktrees/x`, `CLAUDE.md`, `CLAUDE.local.md` and `sub/.claude/CLAUDE.md` are ignored.
- [ ] Commit type `feat(cookbook)!:`.

### US-010: The agents-layout skill replaces the placeholder skills

**Description:** As a user of the artifact, I want it to ship the one skill that explains the layout instead of
three placeholders, so that every repository with the layout carries its own guide.

**Acceptance Criteria:**
- [ ] `cookbook/agents/skills/solid/`, `hexagonal-layout/` and `effective-java-core/` are deleted.
- [ ] `cookbook/agents/skills/agents-layout/` holds `SKILL.md` and `references/policy.md` from
      `tasks/agents-layout-draft/agents-layout/`, without the advice to put Claude-only notes in `.claude/rules/`
      (US-012 completes them).
- [ ] `#connect` installs it as owned whole files under `.agents/skills/agents-layout/`.
- [ ] The `Agents.skills` doc comment names `agents-layout` as the packaged skill; any other name is still refused.
- [ ] `cookbook/cook/setup.groovy` no longer adds the placeholder skills or the instruction lines naming them.
- [ ] `cookbook/agents/README.md`, whose opening paragraph `./cook help io.github.lmarqs:agents:0.0.0` prints,
      describes the artifact without coding skills.
- [ ] Tests that used `solid` use `agents-layout`, and a test asserts `#connect` writes
      `.agents/skills/agents-layout/SKILL.md` equal to the packaged file.
- [ ] Commit type `feat(cookbook)!:`.

### US-011: End-to-end check

**Description:** As the implementer, I want to prove what Cook writes for a real kitchen, so that the skill and docs
describe what Cook actually does.

**Acceptance Criteria:**
- [ ] In a `mktemp -d` git repository whose `cook.toml` is
      `repositories = ["<absolute path of this checkout>/cookbook"]`: `./cook -C <kitchen> --batch-approve io.github.lmarqs:agents:0.0.0#seed`, then
      `cooked/agentic/seed/.agents/permission/policy.yaml` copied to `.agents/permission/policy.yaml`, then
      `./cook -C <kitchen> --batch-approve io.github.lmarqs:agents:0.0.0#connect --harness=claude --harness=codex`.
- [ ] The kitchen has exactly the links US-009 names, `.agents/skills/agents-layout/SKILL.md`, the rendered files
      US-007 and US-008 name, and no `CLAUDE.md` at any path.
- [ ] Each difference from `cooked/agentic/seed` is listed in the issue comment with the decision or spike that
      causes it. Expected: no `.claude/hooks`, `.claude/memory` or `.codex/hooks` links; no `!/.claude/rules`,
      `!/.claude/hooks`, `!/.claude/memory` or `!/.codex/hooks` lines; the section markers; the role files as the
      spikes decided; `.claude/settings.local.json`, a hand-written example; the README.
- [ ] After a line with a made-up word is added to `AGENTS.md`, `claude -p` run in the kitchen answers that word.
- [ ] `codex execpolicy check --rules .agents/adapters/codex/rules/cook.rules git push -f origin main` prints
      `"decision":"forbidden"`, and `git status` gives `"decision":"allow"`.
- [ ] `./cook help io.github.lmarqs:agents:0.0.0`, run in this checkout, lists exactly `claude-settings`,
      `codex-settings`, `connect`, `permissions` and `seed`.
- [ ] `grep -rnE 'agents(:[^#[:space:]]*)?#instructions|adapters/claude/CLAUDE\.md|\.agents/policy\.yaml' cookbook
      --include='*.groovy'` returns nothing (a refusal that names `.agents/policy.yaml`, if the open question adds
      one, is the only allowed match).
- [ ] No commit is required unless a defect is found; a fix is committed under the story it belongs to.

### US-012: The agents-layout skill content

**Description:** As someone working in a repository with the layout, I want a skill that tells me where a change
goes, why, how to adopt the layout and how to investigate misuse, with concrete examples, so that I can work in my
own way without breaking the team's setup.

**Acceptance Criteria:**
- [ ] `SKILL.md` leads with the three owners: the team (`AGENTS.md`, the policy, the shared directories), Cook
      (adapters, links, the ignore section, the skill) and each person (`~/.claude/`, `.claude/settings.local.json`,
      `~/.codex/`). Its frontmatter description says when to use it and when not.
- [ ] `references/policy.md` documents every field as a schema table, with examples for a small team, unattended
      workers, tools, and a table of mistakes with the refusal each gets, as Cook prints it.
- [ ] `references/adopt.md` gives numbered steps with before and after examples for:
      - a new repository;
      - a root `CLAUDE.md`: shared lines move to `AGENTS.md`, and the file is removed with `git rm`, because
        `.gitignore` does not untrack a committed file;
      - Claude path-scoped `.claude/rules/`: to `AGENTS.md` or a skill;
      - `.claude/settings.json` Bash rules, including the legacy `Bash(cmd:*)` form, to policy entries;
      - a hand-made policy with its own renderer (top-level groups such as `shared` and `worker`, raw Claude
        patterns as quoted strings, MCP tool allows) to version 2;
      - a version 1 `.agents/policy.yaml`.
- [ ] `references/investigate.md` is a table of symptom, likely cause, check command and fix, covering at least:
      - Claude ignores `AGENTS.md`;
      - an allowed command still prompts;
      - a denied command ran;
      - a hand edit to an adapter came back;
      - a teammate's Codex files disappeared;
      - an unattended run stalls or refuses;
      - links arrive as text files on Windows (`core.symlinks`);
      - personal approvals leaked into a commit.
- [ ] `references/design.md` explains:
      - the axes (knowledge, policy, harness, mode, role, model, environment, person);
      - what is committed and what each run chooses;
      - why links are committed;
      - why roles only deny;
      - why mode is not a policy field;
      - why the policy holds no model and no personal policy exists;
      - that the policy is a guardrail, not enforcement, linking ADR 0028 by its GitHub URL, since the skill is
        installed in other repositories.
- [ ] Every command the skill cites exists in the installed `claude --help`, `codex --help`, `codex exec --help` or
      `./cook --help`, or is a recipe `./cook help io.github.lmarqs:agents:0.0.0` lists.
- [ ] Unverified behavior is labelled as such; spike outcomes are stated as found.
- [ ] `tasks/agents-layout-draft/` is deleted in this story's commit.

### US-013: Seeded README and project docs

**Description:** As a reader of the repository's docs, I want them to describe the layout as it now is, so that no
page sends people to `#instructions`, `.agents/adapters/claude/CLAUDE.md` or the old policy path.

**Acceptance Criteria:**
- [ ] `cookbook/agents/README.template.md` (seeded as `.agents/README.md`) covers what lives where, the ways of
      working, launching per harness, mode and role (with the spike outcomes), and personal settings, as in
      `cooked/agentic/seed/.agents/README.md`, without its mock-up note and its `.claude/rules/` row.
- [ ] `README.md`, `cookbook/README.md`, `docs/architecture.md`, `docs/features/cli.md`, `docs/features/cook.md`,
      `docs/features/discard.md`, `docs/features/glossary.md` and `docs/ux/messages-and-docs.md` use recipes and paths
      that exist; examples that used `#instructions` use `#seed` or `#connect`.
- [ ] The examples in `docs/adrs/0025-recipes.md` (line 29) and `docs/adrs/0029-cookbook-distribution.md` (line 105)
      that name `#instructions` are corrected in place, as `docs/adrs/README.md` requires; no status label is added.
- [ ] `grep -rnE 'agents(:[^#[:space:]]*)?#instructions|adapters/claude/CLAUDE\.md|\.agents/policy\.yaml' README.md
      cookbook docs --include='*.md' | grep -v '^cookbook/agents/skills/'` returns nothing.
- [ ] Each command in `README.md` and `cookbook/README.md` was run as written in a temporary kitchen.

## Functional Requirements

- FR-1: Cook must read the policy only from `.agents/permission/policy.yaml`, and only version 2.
- FR-2: Cook must render `commands` and `tools` to Claude settings and `commands` to Codex rules.
- FR-3: A role must only add denies; Cook must refuse `allow` or `ask` in a role.
- FR-4: A Claude-only entry must appear in Codex's rules as a comment naming it.
- FR-5: Cook must not write `defaultMode`, a model, or any instruction file for Claude.
- FR-6: The `harnesses` section of `.gitignore` must ignore everything in `.claude/` and `.codex/` except the links
  Cook makes, and must ignore `CLAUDE.md`, `CLAUDE.local.md` and `**/.claude/CLAUDE.md`.
- FR-7: `#connect` must install the `agents-layout` skill as owned whole files.
- FR-8: Cook must never write `~/.claude/`, `~/.codex/` or `.claude/settings.local.json`.

## Non-Goals

- Adopting the layout in this repository (root `CLAUDE.md`, `.agents/policy.interactive.yaml`,
  `policy.unattended.yaml`, `docs/features/agentic-dogfood.md`); the user does that.
- A new ADR. The layout is cookbook content ([ADR 0013](../docs/adrs/0013-organization-content.md)), and
  `docs/adrs/README.md` excludes features and file fields from the corpus; the reasoning lives in the skill's
  `references/design.md`.
- A sandbox field in the policy, a hook declaration shared by both harnesses, `cook --check`, the
  `#connect/instructions` marker format, and a generated launch table; each is separate work.
- Enforcing anything on a person's machine: no hooks or checks against personal files.
- Fixing the hard-coded `0.1.0` in recipe help and yield text.
- Changing the engine (`core/src/main`).

## Technical Considerations

- The engine's tests use the agents cookbook as a fixture (`CookbookCookTest`); the `kitchen()` helper is a plain
  temporary folder, not a git repository.
- `File.deleteDir()` follows symbolic links into `.agents/`; tests that clean a kitchen with links use the
  `Files.walk` reverse delete already in `CookbookCookTest`.
- The Groovy formatter on Java 17 cannot parse `!in` or a GString nested in another; write `!(a in b)` and build such
  strings with `+` (`README.md`).
- `!` commands in Claude Code have no terminal, so manual cooks there need `--batch-approve`, placed before the first
  recipe operand.
- A `cook.toml` path to `cookbook/` serves any version, so `0.0.0` coordinates cook the working tree.
- Releases start only by hand (`.github/workflows/release.yml`, `workflow_dispatch`), so the several `!` commits
  make one major release as long as nobody starts a release before the last story lands.
- Checked on 2026-10-19, Claude Code 2.1.303: Claude reads `AGENTS.md` only while no `CLAUDE.md`, `.claude/CLAUDE.md`
  or `CLAUDE.local.md` exists in the directory or above, and `~/.claude/CLAUDE.md` does not count, even for a
  repository under `$HOME`.
- Checked with codex-cli 0.160.1: Codex ignores `.codex/AGENTS.md`; `codex exec` has no rules option, and
  `--ignore-rules` skips user and project rules; `codex execpolicy check` takes `--rules` more than once and prints
  JSON with a top-level `decision`. With the mock-up's files, `cook.rules` plus `roles/worker.rules` gives
  `forbidden` for `git commit -m x`, so the role files are right once a run can load them.

## Success Metrics

- A fresh clone of a cooked repository gives Claude and Codex the same instructions and the team's permissions with
  no cooking.
- `../medprev-rest-api/.agents/permissions/policy.yaml` translates to version 2 by following `references/adopt.md`,
  losing nothing except rules marked Claude-only.
- A person can answer "where does this change go?" from `SKILL.md` alone.

## Open Questions

- Whether Codex can choose a role per run (US-002); if not, roles are Claude-only and the skill says so.
- Whether cooking should refuse when `.agents/policy.yaml` exists and `.agents/permission/policy.yaml` does not.
  Without that, a kitchen upgrading to this release gets the starter, which grants nothing and also drops the
  team's denies; the batch review shows it, but nothing stops an approval. Recommendation: refuse, naming the move
  and `references/adopt.md`. That does not mean reading version 1.
