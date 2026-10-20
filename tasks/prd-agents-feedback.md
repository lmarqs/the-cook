# PRD: Agents layout, what the medprev adoption runs found

Tracking issue: #41. Source: the "the-cook 3.1.1 Issues" doc. Three runs adopted the layout in medprev-rest-api
with v3.1.1, and the doc lists what they found. Letters A to R match that doc.

## Overview

Each of the three adoption runs ended with a working layout and green CI, but none passes the skill's own review.
Every run fails `check.sh` (A). Two fail the `#connect` dry-run check, because the settings recipes and `#connect`
each claim the same links in `cook.lock` (B). The rest are gaps in the CLI (no `--version` and no drift check for CI)
and in the adopt procedure: retiring a renderer, `.claude/` content that isn't a Cook link, seed modes, the `bypass`
decision, and the `(not run: ask first)` marker.

Every claim below was checked against the `v3.1.1` code. `cookbook/agents/` has not changed since that tag.

## Goals

- `check.sh` exits 0 on a correctly adopted repository that has dependencies installed.
- After the settings recipes and `#connect` are cooked, `#connect --dry-run` prints `Already cooked.`, whatever
  order the recipes were cooked in.
- CI can detect drift with `cook --check`, using a documented step, and `cook --version` reports the binary.
- An agent following `adopt.md` keeps what the repository already had: team content in `.claude/` or `.codex/`, code
  that imported the old renderer, contract tests and drift checks.
- A Quick reference marker says one thing only.

## Quality Gates

These commands must pass for every user story:
- `./gradlew check cliBuild jpackageTest`: tests, CodeNarc, Spotless, the CLI build and the packaged-CLI test.
- `git diff --check`

Delivery for every story: one or more small signed commits titled `<type>(<area>): <sentence>`, pushed to `main`,
and a comment on #41 with the commit and what was verified. Work test-first: a failing test in
`core/src/test/groovy/thecook/CookbookCookTest.groovy`, `cli/src/test/...` or the `check.sh` test before each
behaviour change.

## User Stories

### US-001: Spike which CLAUDE.md placements hide AGENTS.md (A)
**Description:** As a maintainer, I want to know which `CLAUDE.md` placements make Claude Code skip `AGENTS.md`, so
that `check.sh` fails exactly those and the docs state the tested rule.

**Acceptance Criteria:**
- [ ] A scratch repository under the session scratchpad has a root `AGENTS.md` and a nested `sub/AGENTS.md`, each
  with a unique marker line such as "Reply with the word ORCHID when asked for the marker".
- [ ] One headless `claude -p` run per placement asks for the marker from the root, and one asks from `sub/`. The
  placements are: none, root `CLAUDE.md`, `.claude/CLAUDE.md`, `CLAUDE.local.md`, `sub/CLAUDE.md`,
  `other/CLAUDE.md` (no `AGENTS.md` beside it), `node_modules/x/CLAUDE.md` with `node_modules/` git-ignored, and a
  root `CLAUDE.md` that is git-ignored.
- [ ] The results go in a table on #41 with the Claude Code version and the exact command.
- [ ] Nothing outside the scratchpad changes. No file in `~/.claude/` is edited.

### US-002: check.sh flags only the CLAUDE.md files that hide AGENTS.md (A)
**Description:** As an agent running the review, I want `check.sh` to FAIL only on a `CLAUDE.md` that hides an
`AGENTS.md`, so that installed dependencies don't fail the review.

**Acceptance Criteria:**
- [ ] `check_claude_files` in `cookbook/agents/skills/agents-layout/scripts/check.sh` no longer lists git-ignored
  files from the repository scan (`git ls-files -co --exclude-standard`).
- [ ] The placements US-001 showed to hide `AGENTS.md` FAIL, including a git-ignored root `CLAUDE.md`,
  `.claude/CLAUDE.md` or `CLAUDE.local.md` if the spike showed it still takes effect. The placements US-001 showed
  harmless are not reported.
- [ ] The test in `CookbookCookTest.groovy` that runs `check.sh` gains a fixture with a git-ignored
  `node_modules/x/CLAUDE.md` and exits 0, plus one fixture per FAILing placement that exits 1.
- [ ] The scope in `Claude.groovy:56`, `SKILL.md` (Review, Guidance), `templates/README.template.md` (Personal
  settings) and `references/investigate.md` states the rule US-001 found, in the same words everywhere.

### US-003: Adopt moves team content out of .claude/ and .codex/ (H)
**Description:** As a team adopting the layout, I want the skills, hooks, memory and other files we kept in
`.claude/` or `.codex/` moved into the `.agents/` shared directories, so that every harness reads them and Cook's
`.gitignore` section never hides them.

**Acceptance Criteria:**
- [ ] `references/adopt.md` "An existing setup" gains a section for content in `.claude/` or `.codex/` that Cook does
  not make. It sets the inventory: `git ls-files .claude .codex`, minus Cook's link names.
- [ ] The section maps each kind to its directory: skills to `.agents/skills/`, slash commands to
  `.agents/commands/`, hook scripts to `.agents/hooks/` (wired through `#claude-settings` and `#codex-settings`), and
  durable facts to `.agents/memory/`. A file that fits none of these is a question for the developer. The section
  uses medprev's `REVIEW.md` and `eslintrc.hook.cjs` as worked examples.
- [ ] Moving a link means moving its target, so the section says to `git mv` the target and delete the old link.
- [ ] `check.sh` WARNs on each entry in `.claude/` or `.codex/` that is not a link Cook makes (`settings.json`,
  `skills`, `commands`; `config.toml`, `hooks.json`, `rules`), with next step "move it into .agents/ as adopt.md's
  section says". Only Cook's own links get the ignored, broken and untracked FAIL checks.
- [ ] A `CookbookCookTest` fixture with a tracked `.claude/hooks` link shows the WARN and exits 0.

### US-004: #connect owns every .claude/ and .codex/ link (B)
**Description:** As a team that uses a settings recipe, I want the `#connect` dry-run to print `Already cooked.`, so
that the review's drift check works and `cook.lock` stops flipping between recipes.

**Acceptance Criteria:**
- [ ] `#claude-settings` and `#codex-settings` declare no `@Links`. They write adapter keys only.
- [ ] `#connect` links `.claude/settings.json` when it writes the Claude adapter or the adapter exists as the
  earlier recipes in the invocation left it (`current(path)`). It links each Codex adapter file (`config.toml`,
  `hooks.json`, `rules`) the same way.
- [ ] A test cooks `#claude-settings`, `#codex-settings` and `#connect --harness=claude --harness=codex` in one
  invocation. A second invocation of `#connect` alone with `--dry-run` reports nothing to change, and `cook.lock`
  lists every link under `#connect` only.
- [ ] A test cooks `#connect` in a second invocation after the settings recipes were cooked in an earlier one, and
  reports nothing to change.
- [ ] The help of `claude-settings.groovy` and `codex-settings.groovy` moves the link rows out of Writes. It says to
  cook `#connect` after the recipe, in the same command, which also fixes O's `#permissions` pointer. The help of
  `connect.groovy` lists the links.
- [ ] `SKILL.md` "Where a change goes" and `adopt.md` put the settings recipes before `#connect` in every command.

### US-005: cook --version (F)
**Description:** As an agent finding Cook, I want `cook --version` to print the binary's version, so that I can tell
a stale `cook` on `PATH` from the one the repository pins.

**Acceptance Criteria:**
- [ ] `cook --version` prints `cook <version>` and exits 0. The version is the release version the CLI was built
  with (`releaseVersion`, `0.0.0` in a local build).
- [ ] A test in `cli/src/test` covers the output, and `jpackageTest` runs it against the packaged binary.
- [ ] `docs/features/cli.md` and `docs/ux/cli-standards.md` list the option.

### US-006: cook --check for CI (G)
**Description:** As a team whose CI checked the rendered settings against the policy, I want `cook --check`, so that
adoption keeps a drift check.

**Acceptance Criteria:**
- [ ] `cook --check <recipes>` writes nothing. It prints the same diff as `--dry-run` and exits 1 when cooking would
  change any file or `cook.lock`. It prints `Already cooked.` and exits 0 otherwise.
- [ ] Cook refuses `--check` combined with `--batch-approve`, and the message names both options.
- [ ] Tests in `cli/src/test` cover exit 0, exit 1, and the refusal.
- [ ] `docs/features/cli.md` and `docs/ux/cli-standards.md` describe the option and its exit codes.

### US-007: A documented CI drift step, and finding Cook by its pin (F, G, Q)
**Description:** As an agent adopting the layout, I want the skill to run the Cook the repository pins and to give a
CI step for the drift check, so that a stale binary doesn't win and CI keeps the guarantee.

**Acceptance Criteria:**
- [ ] `adopt.md` step 1 ("Find Cook") uses `mise exec -- cook` when `mise.toml` pins the-cook, and checks the version
  with `cook --version` against the one `cook.lock` names.
- [ ] Commands in `SKILL.md`, `references/adopt.md`, `references/investigate.md` and
  `templates/README.template.md` are written as `cook`, with one line saying to run Cook the way step 1 found it.
  `grep -rn '\./cook' cookbook/agents/skills` returns nothing.
- [ ] `adopt.md` gains a step that adds a CI drift check: install the pinned Cook (`mise install`, or the release
  tarball with its `.sha256`), then run `cook --check` with the same recipes and harnesses the team cooks. The step
  shows a GitHub Actions example.
- [ ] `SKILL.md` Review replaces the `--dry-run` drift row with `cook --check`.

### US-008: Two Quick reference markers (P)
**Description:** As an agent reading `AGENTS.md`, I want `(ask first)` to mean only "ask a person every time", so
that I don't stop before my own Done check.

**Acceptance Criteria:**
- [ ] `templates/AGENTS.template.md` defines two markers. `(ask first)` is a team rule: get approval every time.
  `(unverified)` means the writer could not run the command here; later agents run it normally and fix the line if
  it fails.
- [ ] The template says a Done-check command may carry `(unverified)` but never `(ask first)`.
- [ ] `SKILL.md` Review and `references/agents-md.md` check the two markers by name. `(not run: ask first)` appears
  nowhere: `grep -rn 'not run: ask first' cookbook/` returns nothing.
- [ ] The template's fixed text stays at or under 300 words, as `wc -w` counts it outside the fill blanks.

### US-009: Renderer retirement rewires instead of deleting (D, J, R)
**Description:** As an agent retiring a hand-made renderer, I want the procedure to find every caller and say what
replaces each one, so that I don't delete tests and CI checks the team relied on.

**Acceptance Criteria:**
- [ ] `adopt.md` "A hand-made policy with its own renderer", step 3, searches for the module name as well as the
  file: `git grep -n -e render.py -e 'import render' -e 'from render'`, with a note to adapt it to the renderer's
  language.
- [ ] Step 5 deletes only the renderer and its outputs. A new step lists the replacements: code that read the
  renderer's role output reads `.agents/adapters/claude/roles/<role>.json`; contract tests point at
  `.agents/adapters/codex/rules/cook.rules` and `.agents/adapters/claude/settings.json`; a `--check` step is
  replaced by US-007's CI step.
- [ ] The step says to list every assertion or deny that is dropped, with its reason, in the commit message or the
  PR. Codex roles do not exist, so those denies are reported, not silently lost.
- [ ] The `git mv` lines at `adopt.md:250` and `adopt.md:314` overwrite the seeded policy (`git mv -f`), and the text
  says the seeded file is replaced.
- [ ] "A new repository" starts with one line: when the repository has code, tests, CI or hooks that read an old
  agent setup, follow the matching "An existing setup" sections first.

### US-010: Adopt covers --force seeds, the bypass decision and staging (I, K, M)
**Description:** As an agent following `adopt.md`, I want every decision and every changed file covered, so that
the procedure fits the seed mode I used and no behaviour changes silently.

**Acceptance Criteria:**
- [ ] `adopt.md` requires `--skip` when `AGENTS.md` exists. If a seed already ran with `--force`, it says to read the
  old file with `git show HEAD:AGENTS.md` for the line map.
- [ ] The decisions at the start of "An existing setup" include `bypass`. `refuse` adds
  `disableBypassPermissionsMode: "disable"`. The agent states whether the old setup refused bypass mode and asks the
  developer before changing it.
- [ ] Step 9 stages from `git status --short`, reviewing each path, instead of a fixed `git add` list, and the
  `SKILL.md` example matches.

### US-011: An over-budget AGENTS.md when no line may be dropped (L)
**Description:** As an agent merging a long `AGENTS.md`, I want to know what to do when every line must stay, so
that the word budget is not an impossible target.

**Acceptance Criteria:**
- [ ] `references/agents-md.md` says that when no line may be dropped, sections move first to a skill in
  `.agents/skills/` or a file in `.agents/memory/`, each named under Deep dive. A file left over 800 words is
  reported to the developer with the remaining count, not cut further.
- [ ] `templates/AGENTS.template.md` points to that paragraph in one line.

### US-012: #seed skips .gitkeep in a directory that has files (N)
**Description:** As a team adopting with existing `.agents/` content, I want no `.gitkeep` added beside tracked
files, so that the seed diff holds only what is needed.

**Acceptance Criteria:**
- [ ] `Agents.groovy` seeds `.agents/<dir>/.gitkeep` only when that directory has no files, as the earlier recipes
  left it.
- [ ] A `CookbookCookTest` case seeds into `.agents/skills/` holding `x/SKILL.md` and writes no
  `.agents/skills/.gitkeep`. An empty `.agents/hooks/` still gets one.
- [ ] If core gives a recipe no way to list a directory, the story reports that on #41 and leaves behaviour as it
  is, rather than adding a core API.

### US-013: End-to-end greenfield and adoption check
**Description:** As a maintainer, I want one fresh run of the final skill, so that the fixes hold together.

**Acceptance Criteria:**
- [ ] A scratch repository with a git-ignored `node_modules/x/CLAUDE.md`, a tracked `.claude/hooks` link and an
  `AGENTS.md` is adopted by a headless agent with `/agents-layout adopt`. It cooks `#claude-settings`,
  `#codex-settings` and `#connect` from the locally built CLI and the locally published cookbook.
- [ ] `check.sh` exits 0, and `cook --check` with the same recipes exits 0.
- [ ] The `.claude/hooks` content ends up in `.agents/hooks/`.
- [ ] Results, cost and word count go on #41.

### US-014: Release and reply to the doc
**Description:** As the doc's author, I want to know which items shipped in which release, so that the next adoption
uses it.

**Acceptance Criteria:**
- [ ] The release workflow runs on `main` once US-001 to US-013 are in, and the new version is on GitHub releases.
- [ ] A comment on the "the-cook 3.1.1 Issues" doc lists each letter as fixed (with the release) or out of scope (C,
  E), in one table.
- [ ] #41 is closed with a link to the release.

## Functional Requirements

- FR-1: `check.sh` must not report a git-ignored file found by its repository scan.
- FR-2: `check.sh` must FAIL on each `CLAUDE.md` placement that US-001 showed hides `AGENTS.md`, and on nothing
  else of that kind.
- FR-3: `check.sh` must WARN, not FAIL, on an entry in `.claude/` or `.codex/` that is not a link Cook makes.
- FR-4: Only `#connect` may declare links in `.claude/` and `.codex/`.
- FR-5: `cook --version` must print `cook <version>` and exit 0.
- FR-6: `cook --check` must write nothing. It exits 1 when a cook would change anything and 0 on `Already cooked.`.
- FR-7: Cook must refuse `--check` together with `--batch-approve`.
- FR-8: The skill's commands must not assume `./cook`.
- FR-9: `(ask first)` and `(unverified)` must be the only Quick reference markers, each with one meaning.
- FR-10: `adopt.md` must never tell the agent to delete a caller of the renderer without naming its replacement.
- FR-11: `#seed` must not write `.gitkeep` into a directory that has files.

## Non-Goals

- C: a `{codex: …}` policy entry, or per-role Codex rules. Codex having no roles stays the documented design.
- E: recording the settings recipes' input paths in `cook.lock`, or default input paths.
- Shared ownership of one target by several recipes in `cook.lock`.
- A minimum Cook version declared per cookbook.
- Signing releases or publishing checksums outside the release.
- Changes to medprev-rest-api or to its PRs.
- Anything outside the repository: `~/.claude/`, `~/.codex/`, user settings.

## Technical Considerations

- `Claude.groovy:114` and `Codex.groovy:148` hold the `@Links` that `#claude-settings` and `#codex-settings` reach
  through the shared ingredient. US-004 moves link declaration behind a flag that only `connect.groovy` sets, or into
  a method only `#connect` calls. `Codex.linkNames` already lists all three names for the `.gitignore` section.
- `current(path)` returns a file as the earlier recipes in the invocation left it. `Codex.input` uses it.
- `cook --check` can reuse the `--dry-run` path in `cli/src/main/groovy/thecook/cli/terminal/CookCommand.groovy`,
  with an exit code taken from whether the plan is empty.
- `cli/build.gradle` sets `version` from `releaseVersion`. The CLI needs it at run time, through a generated resource
  or the jar manifest.
- `check.sh` stays POSIX `sh` with only git, grep and awk.
- US-001 and US-013 run Claude Code and Codex headless in the scratchpad. A spike that touches
  `~/.codex/config.toml` trust entries records them and reverts them, proved with `cmp`.

## Success Metrics

- On a repository adopted with the new skill: `check.sh` exits 0 with `node_modules` installed, and
  `cook --check` exits 0 after settings recipes and `#connect`.
- `grep -rn '\./cook\|not run: ask first' cookbook/agents/skills` returns nothing.
- Every letter in the doc except C and E is marked fixed in the reply.

## Open Questions

- Whether a git-ignored root `CLAUDE.md` still hides `AGENTS.md`. US-001 answers this, and US-002 follows it.
- Whether core lets a recipe list a directory's files (US-012). If it doesn't, N stays open.
