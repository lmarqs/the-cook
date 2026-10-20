# PRD: Worktrees, loop runners and refusing Claude's bypass mode

## Overview

The `io.github.lmarqs.cookbook:agents` layout gives every coding agent in a repository the same `AGENTS.md`, policy
and skills. Teams run agents in git worktrees made by loop runners, by project scripts or by hand.
medprev-rest-api, for example, uses ralph-tui, `mise run worktree:create`, and a cleanup session that removes stale
worktrees. Today the layout says little about worktrees, and what it says names ralph-tui. Nothing in it stops a runner
from launching agents with flags that skip the policy.

A survey of 15 orchestrators and of the awesome-agent-orchestrators list (about 200 tools) found two things:

- **Worktree placement varies.** Worktrees go inside the repository, beside it, or under `~`.
- **Most runners skip the policy by default.** They launch Claude with `--dangerously-skip-permissions` and Codex with
  `--dangerously-bypass-approvals-and-sandbox` or `--full-auto`, and nearly all let the command be configured.

This PRD makes the layout compose with any runner and any worktree location without naming either. It also adds one
rendered setting, which refuses Claude's bypass mode unless the team's policy allows it.

The shape was settled in a grilling session on 2026-10-20. Two alternatives were dropped:

- **A generated launcher:** a stress test showed Claude already refuses bypass from a settings file.
- **Recommending in-repo worktrees:** Claude's docs say it also loads the parent checkout's `AGENTS.md`.

US-012 to US-015 fix four gaps outside the layout, found while delivering #38. Stale versions in recipe help.
Compact JSON on later writes. Diagnostics that miss the documented prefix and the reason and next lines.

Tracking issue: #39 (follow-up to #38). The working tree holds uncommitted drafts in
`cookbook/agents/skills/agents-layout/references/design.md`, `references/investigate.md` and
`cookbook/agents/seed/README.template.md`. They name ralph-tui as the example, and US-007, US-009 and US-010 rewrite
them. No story commits them as they are.

## Goals

- Any loop runner can rely on a documented contract (paths and launch lines) that `#connect` keeps stable, and the
  layout names no runner.
- `bypass: refuse | allow` in `policy.yaml` decides whether Claude may run in bypass mode. When the field is absent,
  bypass is refused.
- The `agents-layout` skill states, with how each fact was established, what a worktree carries, how trust and
  personal settings reach it, what each location implies, and every known way to step outside the policy.
- Facts still marked "docs" or "not tested" are tested where a scratch setup can do it.
- The reasoning behind `AGENTS.md`'s shape is seeded with the skill, where agents filling in the template read it.

## Quality Gates

These must pass for every user story:

- `./gradlew check cliBuild jpackageTest`: tests, CodeNarc, Spotless, the CLI build and the packaged-CLI test.
- `git diff --check`: no whitespace errors.

Run `./gradlew spotlessApply` first when Groovy or Gradle files changed, and include its changes.

**Every story that changes files ends with:**

- A commit whose subject follows `<type>(<area>): <sentence>` (see `AGENTS.md`). Commits are signed by default
  (`commit.gpgsign`); do not pass `--no-gpg-sign`. This PRD releases a minor version: use `feat(cookbook)` for a
  behaviour change, `docs(cookbook)` for skill and seed text, `fix(<area>)` for US-012 to US-015, and no `!`. The
  message ends with the session's `Co-Authored-By` line.
- `git push` to `main`.
- A comment on issue #39 naming the story, the commit, what changed and what was verified.

A spike story changes no repository files. It ends with its comment on #39 only, and the comment records:

- every command it ran;
- the Claude Code and Codex versions (`claude --version`, `codex --version`);
- each result as observed.

**Files to leave alone:**

- Never stage `.claude/settings.json`, the repository's own `.gitignore`, `.agents/policy.yaml`, `.beads/` or
  `cooked/`. They are the user's own uncommitted files. The drafts named in the Overview are staged only by the
  stories that rewrite them.
- Do not edit `~/.claude/`, `~/.claude.json` or `~/.codex/`. The one exception is a temporary change a spike makes:
  the spike records it in its comment, reverts it, and proves the revert with `cmp` against a backup.

**Help and tests:** each story updates, in the same commit, the tests it breaks and the `@help` and `@yield` texts of
every recipe whose writes it changes, so each help table lists exactly what its recipe writes.

## User Stories

### US-001: Spike - Claude instruction files and bypass refusal in worktrees

**Description:** As the maintainer, I want to know how Claude Code loads instructions in worktrees and whether
`disableBypassPermissionsMode` works from the project's own settings. The docs stories depend on both answers, and so
does the `bypass` field.

**Setup:**
- A scratch repository under the session scratchpad, seeded and connected with the local cookbook (`cook.toml`
  `repositories = ["/home/lmarqs/Work/the-cook/cookbook"]`, then `#seed` and `#connect --harness=claude`).
- One worktree inside the repository (`.wt/in`) and one beside it (`../wt-out`).
- The main checkout's `AGENTS.md` holds the marker word `PARENTMARK`. Each worktree's branch commits an `AGENTS.md`
  holding `CHILDMARK` instead.

**Acceptance Criteria:**
- [ ] In `.wt/in` and in `../wt-out`, run `claude -p "List every all-caps marker word in your project instructions,
  nothing else"` twice each. Record which markers appear.
- [ ] Create an ignored `CLAUDE.local.md` in the main checkout. Repeat the in-repo run, record whether `CHILDMARK`
  still appears, then delete the file.
- [ ] Add `"disableBypassPermissionsMode": "disable"` under `permissions` in the scratch repository's Claude adapter
  settings. That workspace is untrusted. Run `claude -p --dangerously-skip-permissions "Run: touch probe"` and record
  whether `probe` was created.
- [ ] Test the trusted case without editing `~/.claude.json`, because trust follows the main checkout:
  - `git -C /home/lmarqs/Work/the-cook worktree add --detach <scratchpad>/cook-wt HEAD`;
  - add the same key to that worktree's `.claude/settings.json`;
  - repeat the `touch` run;
  - `git worktree remove --force <scratchpad>/cook-wt`;
  - `git -C /home/lmarqs/Work/the-cook worktree list` shows no leftover.
- [ ] With the key in place (in whichever trust state honoured it), run each of these and record whether `probe` was
  created:
  - `--permission-mode bypassPermissions`;
  - `--permission-mode=bypassPermissions`;
  - `--setting-sources user --dangerously-skip-permissions`.
- [ ] The comment on #39 states, for each check, "tested" with the observed result. It names any check whose result
  contradicts the docs (code.claude.com/docs/en/memory, /permissions).

### US-002: Spike - Codex rules under each approval and sandbox flag

**Description:** As the maintainer, I want to know which Codex flags skip the rendered `cook.rules`, so the Codex gap
comment and the guardrail list state facts.

**Setup:**
- Back up `~/.codex/config.toml` to the scratchpad.
- Append `[projects."<scratchpad>/codex-main"]` with `trust_level = "trusted"`.
- In `codex-main`, write a policy with `commands.deny: [[gh]]` and `commands.ask: [[touch]]`, then seed and connect
  with `--harness=codex`.

**Acceptance Criteria:**
- [ ] For each of these, run `codex exec ... "Run exactly: gh --version"` and `codex exec ... "Run exactly: touch
  probe"`. Record whether each command was rejected by policy, refused for approval, or ran (`probe` exists, or `gh`
  printed its version):
  - no flag;
  - `-c approval_policy=never`;
  - `--full-auto`;
  - `--sandbox danger-full-access`;
  - `--dangerously-bypass-approvals-and-sandbox`;
  - `--ignore-rules`.
- [ ] Remove exactly the appended lines. `cmp` against the backup succeeds. If Codex changed other lines meanwhile,
  stop and report the diff instead of overwriting.
- [ ] The comment on #39 has a table: flag × command → observed result.

### US-003: Spike - Codex trust for a worktree outside the trusted checkout

**Description:** As the maintainer, I want to know whether Codex treats a worktree outside a trusted checkout as
trusted, so the location section states it instead of "not tested".

**Setup:** the same backup and trust entry pattern as US-002, trusting only `<scratchpad>/codex-main`. Add worktrees
at `codex-main/.wt/in` and at `<scratchpad>/codex-out`.

**Acceptance Criteria:**
- [ ] In each worktree, run `codex exec "Run exactly: gh --version"` and record whether the forbidden rule applied.
- [ ] Record any trust prompt or warning Codex prints.
- [ ] Remove the trust entry and prove the revert with `cmp`, as in US-002.
- [ ] The comment on #39 states, for each location, "tested" with the observed result.

### US-004: `bypass` field in the policy

**Description:** As a team, I want to state in `policy.yaml` whether Claude may run in bypass mode, so the decision is
reviewed with the rest of the policy.

**Acceptance Criteria:**
- [ ] Write the tests first, in `readsEveryFormOfThePolicyAndNamesTheFieldOfEachRefusal`
  (`core/src/test/groovy/thecook/CookbookCookTest.groovy`, around line 274):
  - `PolicyCheck`'s summary gains a fourth line, `bypass <value>`, and the existing expected strings are updated;
  - the starter template parses to `bypass refuse`;
  - a policy with `bypass: allow` parses to `bypass allow`;
  - the refusal table gains `'version: 2\nbypass: maybe\n'` → `bypass must be refuse or allow`.

  Run `./gradlew :core:test --tests '*CookbookCookTest*'` and see the tests fail.
- [ ] `cookbook/agents/Agents.groovy`, `Policy`:
  - a `String bypass` property, `'refuse'` when absent;
  - `parse` adds `'bypass'` to the known fields and fails with `bypass must be refuse or allow` for any other value;
  - the Javadoc of `parse` lists the new refusal.
- [ ] `cookbook/agents/seed/policy.template.yaml`: `bypass: refuse` after `version: 2`, with a comment saying `allow`
  is for teams that run every agent in a container.
- [ ] `cookbook/agents/skills/agents-layout/references/policy.md`:
  - a Top level row: `bypass`, `refuse` or `allow`, optional, default `refuse`, "Whether Claude may run in bypass
    mode";
  - a row in "Mistakes Cook refuses" with the exact message;
  - the sentence "Cook refuses another version or any other top-level field" still holds.
- [ ] The tests pass.

### US-005: Render `bypass` into the harness adapters

**Description:** As a team, I want `#connect` to turn `bypass: refuse` into Claude's own setting, so every launch
path refuses bypass mode with no wrapper.

Depends on US-001, for where the key takes effect, and on US-002, for the Codex comment's wording.

**Acceptance Criteria:**
- [ ] Write the tests first:
  - `core/src/test/resources/agents-layout/claude-settings.json` gains `"disableBypassPermissionsMode": "disable"`
    under `permissions`, after `deny`;
  - `rendersThePolicyIntoClaudeSettingsAndOneDenyFilePerRole` re-cooks with `bypass: allow` and asserts the key is
    absent;
  - `core/src/test/resources/agents-layout/codex-commands.rules` gains the gap comment as its first line;
  - every other test that compares a whole rendered Claude settings file or Codex rules file is updated.

  The tests fail.
- [ ] `cookbook/agents/Claude.groovy` `permit`: when `policy.bypass == 'refuse'`, `settings.permissions` gains
  `disableBypassPermissionsMode: 'disable'`. The class and method Javadoc say so.
- [ ] `cookbook/agents/Codex.groovy` `permit`: when `policy.bypass == 'refuse'`, the `commands` section starts with one
  `#` comment.
  - It names the flags US-002 found to skip `cook.rules`.
  - It says Codex has no project-level refusal, and that requirements or Agent Security refuse them at the
    organisation level.
  - Javadoc updated.
- [ ] The Writes tables in the help of `cookbook/agents/connect.groovy` and `cookbook/agents/permissions.groovy` say
  `permissions`, including `disableBypassPermissionsMode` while the policy's `bypass` is `refuse`.
- [ ] Commit `feat(cookbook): agents#connect refuses Claude's bypass mode unless policy.yaml says bypass: allow`. The
  body says re-cooking adds the key, and that teams running agents in containers write `bypass: allow`.

### US-006: Seed the AGENTS.md design note with the skill

**Description:** As an agent filling in or reviewing `AGENTS.md`, I want the reasons behind its sections next to the
other references, so I keep what works and cut what does not.

**Acceptance Criteria:**
- [ ] Write the tests first: both expected skill file lists in `CookbookCookTest.groovy` gain
  `.agents/skills/agents-layout/references/agents-md.md`. They are the lock listing around lines 74-78 and the list
  around lines 235-239. The tests fail.
- [ ] `git mv cookbook/agents/seed/AGENTS.template.design.md
  cookbook/agents/skills/agents-layout/references/agents-md.md`.
- [ ] In the moved file:
  - the title becomes "Why `AGENTS.md` holds what it holds";
  - the first paragraph says to read it when filling in `AGENTS.md` from the template or deciding what to add or cut;
  - the sentence naming the `instructions` recipe is replaced by the current in-scope check: a fresh session sets up,
    makes a small change and passes the Done check from `AGENTS.md` alone;
  - the Worktrees bullet adds that the rows are the project's, so any loop runner's worktrees fit them, and states the
    in-repo consequence from US-001.
- [ ] `cookbook/agents/skills/agents-layout/SKILL.md` "Pick the task" gains the row "Fill in or review `AGENTS.md`" →
  `references/agents-md.md`.
- [ ] The fill-in note at the top of `cookbook/agents/seed/AGENTS.template.md` gains one clause pointing to
  `.agents/skills/agents-layout/references/agents-md.md`.
- [ ] The Javadoc at `cookbook/agents/Agents.groovy` around line 128 names the new path.
- [ ] `grep -rn "template.design" --include='*.md' --include='*.groovy' .` (excluding `build/`) finds nothing.

### US-007: design.md - worktrees and loop runners

**Description:** As a team adding any loop runner, I want one section that says what the layout guarantees, what the
runner decides and what each worktree location implies.

**Acceptance Criteria:**
- [ ] `cookbook/agents/skills/agents-layout/references/design.md`: the header's tested versions are the ones the
  spikes recorded. `references/investigate.md`'s header matches.
- [ ] `## Parallel agents` (committed text and draft) is replaced by `## Worktrees and loop runners`. It holds:
  - the contract table (FR-1);
  - what a worktree carries (tested);
  - trust and local settings following the main checkout (tested);
  - both location consequences, with the US-001 and US-003 results and their labels, framed neutrally: in-repo
    loading of the parent's instruction files is a hazard when that checkout is on another branch, and an override
    point when a team wants one;
  - shared state across worktrees (stash, branches, an issue tracker kept in the clone, Docker), and `git worktree`
    in a worker role's deny;
  - the runner table from Technical Considerations, condensed to location, copies/setup, launch default and
    configurable, with each tool's primary source linked, and "not verified" where the survey says so.
- [ ] No sentence presents one runner as the example. `grep -n "such as ralph-tui" design.md` finds nothing.
- [ ] The Axes table and the text say "loop runner" where they meant the category, matching
  `docs/features/glossary.md`.

### US-008: design.md - bypass and the guardrail

**Description:** As a team, I want to know why bypass is refused by default, why there is no launcher, and every way
a person or runner can still step outside the policy.

**Acceptance Criteria:**
- [ ] A new section, `## Why bypass is refused by default`, contains:
  - the stress-test results (Technical Considerations) and the US-001 results, each labelled tested or docs;
  - why `bypass` is a policy field (container-only teams);
  - why there is no launcher: it reaches only runners already configured to call it, Claude already refuses from
    settings, and Codex has no roles to select.
- [ ] `## A guardrail, not enforcement` lists each route, each labelled tested, docs or not tested:
  - Claude bypass flags and modes, and what `bypass: refuse` does to them;
  - `--setting-sources user`;
  - under bypass, asks run and denies hold;
  - the Codex flags from US-002;
  - untrusted paths from US-003;
  - `-c` overrides;
  - prefix gaps (`git -C`, `env X=1`, `bash -c`, scripts);
  - runner defaults;
  - organisation controls (managed settings, requirements or Agent Security, branch protection, CI) as the only
    enforcement.

### US-009: investigate.md and SKILL.md

**Description:** As someone whose agent misbehaves in a worktree or under a runner, I want a row that names the check
and the fix.

**Acceptance Criteria:**
- [ ] `references/investigate.md` keeps the worktree trust row (draft), worded from the tested results, and gains
  three rows:
  - **A worker runs what would ask.**
    - Cause: `bypass: allow`, or `--setting-sources user`.
    - Check: `jq .permissions.disableBypassPermissionsMode .agents/adapters/claude/settings.json` and the runner's
      launch configuration.
    - Fix: `bypass: refuse` and re-cook, or the runner's command.
  - **A Codex worker runs outside the rules.**
    - Cause: a flag from US-002 in the runner's configuration.
    - Check: the runner's configuration.
    - Fix: the unattended line from `.agents/README.md`.
  - **In an in-repo worktree, Claude follows instructions the worktree's `AGENTS.md` lacks.**
    - Cause: the main checkout's `AGENTS.md`, `CLAUDE.md` or `CLAUDE.local.md`, per US-001.
    - Check: the existing walk-up loop.
    - Fix: move the worktree outside, or keep the parent's file deliberately.
- [ ] `SKILL.md`:
  - the "Three owners" table gains a row: a loop runner's artifact owns its configuration and worktrees, and consumes
    the contract in `references/design.md#worktrees-and-loop-runners`;
  - "Pick the task" gains "Set up a loop runner" → that section;
  - the guidance bullet "Treat the policy as an agreement" names `bypass`;
  - "orchestrator" becomes "loop runner" where it names the category.
- [ ] Every Markdown link in the skill resolves. `./gradlew :docs:linkCheck` covers `cookbook/`; if it does not, check
  the links with `grep -o '](references/[^)#]*' SKILL.md` and `ls`.

### US-010: Seeded `.agents/README.md`

**Description:** As a person in a seeded repository, I want the README to tell me how to point a loop runner at the
layout and what bypass does.

**Acceptance Criteria:**
- [ ] `cookbook/agents/seed/README.template.md`'s trust paragraph has a worktree sentence worded from the US-001 and
  US-003 results.
- [ ] The sentence "An orchestrator such as ralph-tui ..." is replaced by a `## Loop runners` section. The section:
  - says what a runner may rely on (the contract, in short);
  - says to configure the runner's command to the unattended line, with the role file for Claude;
  - says Claude refuses bypass flags while `bypass` is `refuse`, and states what Codex does per US-002;
  - says where worktrees go is the `AGENTS.md` Worktrees section.
- [ ] The section names no runner.

### US-011: End-to-end check

**Description:** As the maintainer, I want the released behaviour checked in a real kitchen before closing #39.

**Acceptance Criteria:**
- [ ] In a new scratch kitchen with the local cookbook, run `#seed` and then `#connect --harness=claude --harness=codex`.
  Then check:
  - `jq -r .permissions.disableBypassPermissionsMode .agents/adapters/claude/settings.json` prints `disable`;
  - `cook.rules` starts with the gap comment;
  - `.agents/skills/agents-layout/references/agents-md.md` exists;
  - `git ls-files -s .claude .codex` shows mode `120000` for every entry.
- [ ] In the trust state US-001 found effective, `claude -p --dangerously-skip-permissions "Run: touch probe"` in a
  worktree of the kitchen does not create `probe`.
- [ ] Set `bypass: allow` and re-cook. The key is gone, and the gap comment is gone.
- [ ] The comment on #39 lists every story's commit so far and what was verified.

### US-012: Examples name `<version>`, not 0.1.0

**Description:** As someone copying a command from a recipe's help or after-cooking text, I want it to name a version I
can use, so I don't cook a release that does not exist.

**Acceptance Criteria:**
- [ ] Every `./cook io.github.lmarqs.cookbook:<artifact>:0.1.0#...` and `` `...:0.1.0#...` `` in `cookbook/*/*.groovy`
  names `<version>` instead. `grep -rn ':0\.1\.0' cookbook --include='*.groovy'` finds nothing.
- [ ] Tests that compare help or after-cooking text are updated in the same commit.
- [ ] The coordinates error in `core/src/main/groovy/thecook/cookbook/Coordinates.groovy` gives an example that names a
  recipe that exists (`#seed`), not the removed `#instructions`.
- [ ] Commits: `fix(cookbook)` for the recipes and `fix(core)` for the coordinates message.

### US-013: A later write keeps a JSON file's indentation

**Description:** As a reviewer of a policy change, I want a re-cook to change only the lines that changed, so the
rendered settings stay readable in the diff.

Cause (read in the code): `KeyTarget.write` indents a new JSON file, but `JsonDocument.write` splices
`JsonOutput.toJson(value)` into an existing one, so the key's value becomes one compact line.

**Acceptance Criteria:**
- [ ] Write the test first: cook a key into a new JSON file, change the value, cook again. The file is indented as a new
  file with the new value would be, and the other keys keep their text. The test fails.
- [ ] A value written into an existing JSON file is indented to the depth of its key, using the same two-space form as
  a new file.
- [ ] A key that already holds the value is still left as it is.
- [ ] Commit `fix(core): a cook that changes a JSON key writes its value indented, as the first cook did`.

### US-014: Diagnostics start with `cook: `

**Description:** As someone reading stderr, I want every diagnostic to say it came from Cook, under the name I typed.

**Acceptance Criteria:**
- [ ] Write the tests first in `cli/src/test/groovy/thecook/cli/CookCommandTest.groovy`: each expected stderr starts
  with `cook: `; under `discard`, with `cook: discard: `; under `help`, with `cook: help: `. The tests fail.
- [ ] Every line `CookCommand` writes to stderr as a diagnostic starts with that prefix. The batch diff, prompts and
  stdout results are unchanged.
- [ ] `docs/ux/cli-standards.md` and `docs/ux/messages-and-docs.md` say `cook: ` wherever they said `the-cook: `.
  `grep -rn "the-cook: " docs` finds only the README image's alt text, if anything.
- [ ] Commit `fix(cli): diagnostics on stderr start with cook: , as the message standard says`.

### US-015: Errors say what failed, why and what to do next

**Description:** As someone whose cook failed, I want the cause and the next step on their own lines, as the message
standard's examples show.

**Acceptance Criteria:**
- [ ] List every user-facing error Cook can print (the `throw` sites in `core/src/main`, `cli/src/main` and
  `infra/src/main` whose message reaches stderr), and record the list in the #39 comment. Internal invariant errors
  that a user cannot act on are named as such and left alone.
- [ ] Each user-facing error has its first line naming what failed, then indented lines for why and for next, where
  the message has them. Wording keeps the facts it has now (found values, lists of recipes).
- [ ] The discard error for a recipe missing from `cook.lock` reads as the standard's example:
  `cook: discard: cook.lock lists no recipe <name>; nothing was written`, then `discard removes only what cook.lock
  records for a recipe`, then the next step naming the recipes the lock does list, or none.
- [ ] Tests first for each group of messages, one commit per group (`fix(core)` or `fix(cli)`).

### Final comment

- [ ] After US-015, the final comment on #39 lists every story's commit and what was verified.

## Functional Requirements

- FR-1: The layout's contract for loop runners is:
  - the root `AGENTS.md`;
  - `.claude/skills` and `.claude/commands`, with `.agents/skills/` for Codex;
  - `--settings .agents/adapters/claude/roles/<role>.json` for a Claude role;
  - `.codex/rules`, with no per-run Codex role;
  - the unattended lines `claude -p --permission-prompts none [--settings <role>]` and `codex exec -c
    approval_policy=never`.

  `#connect` keeps these paths stable. A runner decides its worktree location, copies and setup, merge flow,
  parallelism and model. The project decides the `AGENTS.md` Worktrees rows.
- FR-2: `policy.yaml` accepts a top-level `bypass` of `refuse` or `allow`, `refuse` when absent. Any other value is
  refused with `bypass must be refuse or allow`.
- FR-3: With `bypass: refuse`, `#connect --harness=claude` writes `permissions.disableBypassPermissionsMode: "disable"`
  into `.agents/adapters/claude/settings.json`. With `allow`, it writes no such key.
- FR-4: With `bypass: refuse`, `#connect --harness=codex` starts the `commands` section of `cook.rules` with one comment
  naming the flags that skip it and the organisation-level control.
- FR-5: The layout writes no launcher, runner configuration or worktree.
- FR-6: The skill labels every harness or runner fact it states as tested (with the version), docs (with the page) or
  not tested.
- FR-7: `references/agents-md.md` is seeded with the skill. No file named `AGENTS.template.design.md` remains.
- FR-8: Recipe help and after-cooking texts name `<version>` in example coordinates.
- FR-9: Changing a key in an existing JSON file writes its value indented to the key's depth.
- FR-10: Every stderr diagnostic starts with `cook: `, plus `discard: ` or `help: ` under those commands, and each
  user-facing error puts its cause and next step on indented lines.

## Non-Goals

- A generated launcher or wrapper for any harness. It was rejected after the stress test.
- Configuring, detecting or naming a specific loop runner in the layout, or writing a runner's configuration.
- Recommending a worktree location, or adding a worktree directory to the `harnesses` ignore section.
- Codex roles, or any project-level Codex refusal beyond the comment.
- Changing the `cookbook/ralph` sketch, or anything in medprev-rest-api.
- Running a release. The release workflow is started by hand.
- Imports of models across artifacts when cooking by coordinates. Nine artifacts import another's models and fail to
  compile that way, but nobody uses them; `agents`, the published one, imports nothing.
- Status icons in place of the prefix on a terminal. The standard allows them; it does not require them.

## Technical Considerations

**Stress test** (Claude Code 2.1.304, Codex 0.160.1, 2026-10-20, scratch repository, untrusted workspace):

| Test | Result |
| --- | --- |
| `claude -p --permission-prompts none`, `touch probe` | Denied: "requires approval, and this session has no approval surface" |
| `claude -p --dangerously-skip-permissions`, `touch probe` | Ran; file created |
| The same plus `--settings '{"permissions":{"disableBypassPermissionsMode":"disable"}}'` | Neutralised: `touch` needed approval; no file |
| The same key in the untrusted repository's own `.claude/settings.json` instead | Neutralised; a control without it ran `touch`. So US-005 renders the key into the shared settings only |
| `claude -p --dangerously-skip-permissions`, `git push origin main` with a project deny | Denied: denies hold under bypass |
| Claude in a sibling worktree with a deny in the main checkout's `.claude/settings.local.json` | Denied: local settings follow the main checkout |
| Untrusted worktree message | Names `projects["<main checkout>"]`: trust is keyed on the main checkout |
| `codex exec` in medprev's `.work/worktrees/recovery-password-lookup` (inside the trusted checkout), `gh --version` | Rejected: "policy forbids commands starting with `gh`" |

**Docs:**
- code.claude.com/docs/en/permissions: `disableBypassPermissionsMode` "works from any scope".
- code.claude.com/docs/en/memory: Claude loads "every `AGENTS.md` and `.claude/AGENTS.md` in your working directory and
  the directories above it", and a `CLAUDE.md`, `.claude/CLAUDE.md` or `CLAUDE.local.md` there or above turns
  `AGENTS.md` off.
- code.claude.com/docs/en/worktrees: approvals are saved to the main checkout's `.claude/settings.local.json`, and
  untracked `.claude/skills` is read through from the main checkout.
- Codex config reference (learn.chatgpt.com/docs/config-file/config-reference): a project's `.codex/config.toml` cannot
  cap sandbox or approval flags. Only requirements or Agent Security can. Untrusted projects skip `.codex/` layers.

**Runner survey.** A research agent checked each tool on 2026-10-20 against primary sources (docs or source code);
"nv" means not verified.

| Tool | Worktrees | Copies / setup | Launch default | Configurable | Source |
| --- | --- | --- | --- | --- | --- |
| Claude Code `--worktree` | `.claude/worktrees/<name>/`; a `WorktreeCreate` hook replaces it | `.worktreeinclude` | It is the agent | n/a | code.claude.com/docs/en/worktrees |
| Codex app / CLI `--worktree` | `$CODEX_HOME/worktrees` (`git-worktree-root`) | `.worktreeinclude` (app); setup scripts (key nv) | It is the agent | n/a | learn.chatgpt.com/docs/environments/git-worktrees; openai/codex `codex-rs/worktree/src/settings.rs` |
| ralph-tui 0.12 | Code default `../.ralph-worktrees/<project>/` (docs say `.ralph-tui/worktrees`); `[parallel].worktreeDir` | Its own config only | claude `--dangerously-skip-permissions`; codex `exec --full-auto` | `command`, `defaultFlags`, `skipPermissions`, `fullAuto` | ralph-tui.com/docs/parallel/configuration; `src/parallel/worktree-manager.ts` |
| Conductor | `~/conductor/workspaces/<repo>/<ws>` | `.worktreeinclude` or `file_include_globs`; `[scripts] setup` | nv | nv | conductor.build/docs/concepts/git-worktrees |
| claude-squad | `~/.claude-squad/worktrees/` | None | `claude` in tmux; `--autoyes` presses Enter | `-p <program>` | smtg-ai/claude-squad `session/git/worktree.go` |
| Gastown | `~/gt/<rig>/polecats/<name>/rig/` | `.runtime/setup-hooks/*` | claude `--dangerously-skip-permissions`; codex `--dangerously-bypass-approvals-and-sandbox` | `gt config agent set` | gastownhall/gastown `internal/config/agents.go` |
| Scion | `.scion/agents/<name>/workspace`, in a container | `provision.py` | claude `--dangerously-skip-permissions`; codex `danger-full-access` and the bypass flag | Harness bundles | GoogleCloudPlatform/scion `harnesses/*/config.yaml` |
| Orca | `~/orca/workspaces/<repo>/<name>` (`workspaceDir`) | `.worktreeinclude` (literal paths); `worktree.sharedDirectories` | Bypass flags; "Manual" clears them | Arguments field | stablyai/orca `src/shared/tui-agent-permissions.ts` |
| paperclip | `.paperclip/worktrees` (`worktreeParentDir`) | `provisionCommand` | Skip permissions and bypass, true by default | Strategy options | paperclipai/paperclip `server/src/services/workspace-runtime.ts` |
| vibe-kanban (sunsetting) | OS temp dir `/vibe-kanban/worktrees` | `copy_files`, `setup_script` | `dangerously_skip_permissions: true`; codex `danger-full-access` | Profiles | BloopAI/vibe-kanban `crates/executors/default_profiles.json` |
| Crystal (deprecated) | `<repo>/worktrees` | None found | claude `--dangerously-skip-permissions` | Per-project mode | stravu/crystal `main/src/services/claudeCodeManager.ts` |
| Symphony | No worktrees: one clone per issue under `<tmp>/symphony_workspaces` | `hooks.after_create` and others | `codex app-server`; the Elixir default rejects approvals | Config | openai/symphony `SPEC.md` |
| ForgeCode | `../<name>` | None | Its own agent; no parallelism | n/a | tailcallhq/forgecode `crates/forge_main/src/sandbox.rs` |
| Cursor | nv | `.cursor/worktrees.json` `setup-worktree` | nv | nv | cursor.com/docs/configuration/worktrees |

Field: github.com/andyrewlee/awesome-agent-orchestrators (third-party list) groups about 200 tools as terminal,
desktop/web, swarms, loop runners, task runners, infrastructure and assistants.

**Code anchors:**
- `Policy` in `cookbook/agents/Agents.groovy` (around line 236; `parse` around 255).
- `Claude.permit` (`cookbook/agents/Claude.groovy` around line 72).
- `Codex.permit` and `prefixRule` (`cookbook/agents/Codex.groovy` around lines 61 and 132).
- The policy tests in `core/src/test/groovy/thecook/CookbookCookTest.groovy` (around lines 274, 362 and 382), with
  fixtures in `core/src/test/resources/agents-layout/`.

**Trusted spike for Claude:** a temporary detached worktree of `/home/lmarqs/Work/the-cook` gives a trusted workspace
without editing `~/.claude.json`, because Claude keys trust on the main checkout (tested). Remove it with
`git worktree remove --force`.

## Success Metrics

- A team can point any runner at the layout by reading `.agents/README.md` alone, without the layout naming that
  runner.
- `claude --dangerously-skip-permissions` cannot run a command that would ask in a re-cooked kitchen with the default
  policy, in the trust states US-001 confirms.
- The skill has no unlabelled harness or runner claim, and only the claims that no setup here can test are marked
  "not tested".

## Open Questions

- If US-002 shows that `cook.rules` still holds under `--dangerously-bypass-approvals-and-sandbox`, the gap comment
  narrows to the flags that do skip it. US-005 words it from the results.
