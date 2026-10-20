# PRD: Agents layout, what the medprev adoption runs found

Tracking issue: #41. Source: the "the-cook 3.1.1 Issues" doc. Letters match that doc.

## Overview

Three adoption runs with v3.1.1 ended with working layouts that fail the skill's own review. `check.sh` FAILs on
files no harness reads (A, H). `cook.lock` flips link ownership between `#connect` and the settings recipes (B).
CI has no drift check and the CLI cannot report its version (G, F). And one Quick reference marker means two things
(P).

The settings recipes turn out to be unnecessary. Cook owns only the keys it renders from the policy and keeps every
other key. In a scratch repository, `hooks` and `env` written into `.claude/settings.json`, and `[features]` and
`[mcp_servers.*]` written into `.codex/config.toml`, left `#connect --dry-run` at `Already cooked.` Removing
`#claude-settings`, `#codex-settings` and `#permissions` fixes B, E and O, and leaves `#seed` and `#connect`.

## Goals

- `check.sh` exits 0 on a correctly adopted repository with dependencies installed.
- `#connect` is the only recipe that renders the policy or makes links. The review's drift check passes whatever
  the team adds to the harness files.
- CI can fail on drift with `cook --check`, and `cook --version` names the binary.

## Quality Gates

These commands must pass for every user story:
- `./gradlew check cliBuild jpackageTest`
- `git diff --check`

Each story ships as small signed commits titled `<type>(<area>): <sentence>`. They are pushed to `main` with a
comment on #41, written test-first.

## User Stories

### US-001: check.sh FAILs only what hides AGENTS.md or blocks a link (A, H)
**Description:** As an agent running the review, I want `check.sh` to fail only on setups a harness cannot read,
so that installed dependencies and the team's own tracked links don't fail it.

**Acceptance Criteria:**
- [ ] `check_claude_files` tests `CLAUDE.md`, `CLAUDE.local.md` and `.claude/CLAUDE.md` in the directory of each
  tracked `AGENTS.md` and in every directory above it, up to the root, ignored or not. It no longer scans the whole
  repository. A git-ignored `node_modules/x/CLAUDE.md` is not reported.
- [ ] A link with mode 120000 in the index is never reported as ignored. A link that is not tracked FAILs once, and
  its next step names `git check-ignore -v --no-index <link>` for when `git add` skipped it.
- [ ] The `check.sh` tests in `CookbookCookTest.groovy` pass on these fixtures: an ignored
  `node_modules/x/CLAUDE.md` and a tracked `.claude/hooks` link under `/.claude/*` (both exit 0), and a root
  `CLAUDE.md` (exits 1).
- [ ] `SKILL.md` and `templates/README.template.md` state the scope as `adopt.md:113` does ("in the directory or
  one above").

### US-002: #connect is the only recipe (B, E, O)
**Description:** As a team, I want one recipe to render the policy and make every link, and our hooks, env and MCP
keys written straight into the harness files, so that `cook.lock` never flips and no input paths need recording.

**Acceptance Criteria:**
- [ ] `claude-settings.groovy`, `codex-settings.groovy` and `permissions.groovy` are deleted. Methods only they
  used are deleted too (`settingsFrom`, `configFrom`, `hooksFrom`, `ruleFrom` and their helpers), along with the
  `@Links(instead=…)` texts that name them.
- [ ] `#connect` links `.codex/hooks.json` when `.agents/adapters/codex/hooks.json` exists, as the earlier recipes
  left it (`current(path)`).
- [ ] A `CookbookCookTest` case cooks `#seed` and `#connect` for both harnesses, then writes `hooks` and `env` into
  `.claude/settings.json`, `[features]` into `.codex/config.toml`, and an `.agents/adapters/codex/hooks.json`. A
  second `#connect` adds only the `.codex/hooks.json` link. A third prints `Already cooked.`, and `cook.lock` lists
  every link under `#connect`.
- [ ] Every mention of the three recipes is gone: `grep -rn 'claude-settings\|codex-settings\|#permissions'
  cookbook docs README.md` returns nothing. This covers `cookbook/README.md`, `docs/features/cli.md` and the
  `RecipeSourceTest` examples.
- [ ] The commit is `feat(cookbook)!:`.
- [ ] The step that upgrades a repository whose `cook.lock` lists a deleted recipe, keeping its keys, is found in a
  test kitchen and recorded on #41.

### US-003: The skill says who owns which key
**Description:** As a team, I want the skill to say that Cook owns only the keys it renders from the policy, so that
we write hooks, env and MCP servers in the harness's own file.

**Acceptance Criteria:**
- [ ] `SKILL.md` (owners table, Where a change goes, Guidance) and `templates/README.template.md` say Cook owns
  `permissions` in `.claude/settings.json`, `sandbox_mode`, `approval_policy` and
  `sandbox_workspace_write.network_access` in `.codex/config.toml`, and `.codex/rules/cook.rules`. Every other key,
  and `.codex/hooks.json`, belongs to the team.
- [ ] The "Run a shared hook script" and "Change another shared harness setting" rows name the native file. The
  Codex hooks row adds "cook `#connect` once, to link it".
- [ ] "Edit the policy, not what is rendered from it" is limited to the keys Cook owns. "generated, never edited by
  hand" appears nowhere.
- [ ] `references/adopt.md`, `investigate.md` and `design.md` name no deleted recipe.

### US-004: cook --version and cook --check (F, G)
**Description:** As a team, I want `cook --version` and a drift check that fails CI, so that a stale binary is
visible and a policy edit without a cook is caught.

**Acceptance Criteria:**
- [ ] The build writes the version into the CLI at build time (`releaseVersion`, `0.0.0` locally). `cook --version`
  prints `cook <version>` and exits 0, in the jar and in the jpackage image (`jpackageTest`).
- [ ] `cook --check <recipes>` writes nothing. It prints the `--dry-run` output, exits 1 when anything would change,
  and exits 0 on `Already cooked.`. Cook refuses `--check` together with `--batch-approve`, naming both.
- [ ] Tests in `cli/src/test` cover the version output, check exit 0, check exit 1 and the refusal.
- [ ] `docs/features/cli.md` and `docs/ux/cli-standards.md` describe both options.

### US-005: Skill fixes: drift check, finding Cook, one marker, adopt one-liners (D, F, G, I, J, K, P)
**Description:** As an agent following the skill, I want each step to be right the first time.

**Acceptance Criteria:**
- [ ] The `SKILL.md` Review drift row and `adopt.md` use `cook --check …#connect --harness=…`. `adopt.md` gives a
  CI step of 5 lines or fewer that runs it with the committed `./cook` launcher.
- [ ] `adopt.md` step 1 tries `mise exec -- cook` before `cook` when `mise.toml` pins the-cook.
- [ ] `templates/AGENTS.template.md` has one marker, `(unverified)`: the writer could not run the command, and later
  agents run it. Approval lives only under `ask` in `.agents/permission/policy.yaml`. `grep -rn 'ask first'
  cookbook/agents` returns nothing. The template's fixed text stays at 300 words or fewer.
- [ ] `adopt.md` renderer section: step 3 also greps the module name (`-e 'import render' -e 'from render'`), step 5
  says rewire what still reads the renderer's outputs and delete only the renderer, and `git mv` becomes
  `git mv -f` (lines 250, 314).
- [ ] `adopt.md` names `bypass` among the decisions to confirm with the developer.
- [ ] `adopt.md` step 9 stages from `git status --short`, and the `SKILL.md` example matches.

### US-006: Greenfield run, release, reply
**Description:** As the doc's author, I want the fixes released and mapped to the letters, so that the next
adoption uses them.

**Acceptance Criteria:**
- [ ] One headless `/agents-layout adopt` run in a scratch repository with an ignored `node_modules/x/CLAUDE.md`
  ends with `check.sh` exit 0 and `cook --check` exit 0. The result and cost go on #41.
- [ ] The release workflow publishes the next major version.
- [ ] One comment on the doc gives a table of each letter: fixed in the release, or not done with a one-line reason.
  It includes US-002's upgrade step. #41 is closed with the release link.

## Functional Requirements

- FR-1: `check.sh` checks `CLAUDE.md` names only beside or above a tracked `AGENTS.md`.
- FR-2: `check.sh` never reports a tracked link as ignored.
- FR-3: The `agents` cookbook has two recipes, `#seed` and `#connect`.
- FR-4: `#connect` keeps every key it does not render from the policy.
- FR-5: `cook --version` prints the version set at build time.
- FR-6: `cook --check` exits 1 on any pending change, 0 otherwise, and writes nothing.
- FR-7: The Quick reference has one marker, `(unverified)`.

## Non-Goals

- C: Codex-only denies or Codex roles.
- L, M, N, Q, R: the existing `check.sh` WARN covers L; the runs that hit M and R deliberately left the procedure;
  N is cosmetic; `./cook` is the documented install, so Q is not a defect.
- A spike on `CLAUDE.md` placements, team links moved by adopt, a minimum Cook version, release signing.
- Making `.codex/` one link, or a `.codex/rules` link for hand rules when the policy has no commands.
- Changes to medprev-rest-api.

## Technical Considerations

- `Claude.permit` always sets `permissions`, and `Codex.permit` always sets `config.toml` keys, so `#connect`
  already makes `.claude/settings.json` and `.codex/config.toml` for every selected harness.
- The connect.groovy `.gitignore` section already re-includes `.codex/hooks.json`.
- `CookCommand.write` (`cli/src/main/groovy/thecook/cli/terminal/CookCommand.groovy:352`) returns true on both
  `Already cooked.` and a dry-run diff. `--check` needs the action count.

## Success Metrics

- `check.sh` and `cook --check` exit 0 after a greenfield adoption with dependencies installed.
- The cookbook has 2 recipes, down from 5.

## Open Questions

- The upgrade step for a `cook.lock` that lists a deleted recipe (US-002 finds it).
