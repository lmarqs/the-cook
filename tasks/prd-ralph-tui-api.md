# PRD: ralph#ralph-tui, a symmetric recipe API that refuses what it cannot apply

## Overview

A user-perspective review of `io.github.lmarqs.cookbook:ralph#ralph-tui` (issue #63) found an API that accepts
arguments it cannot apply, treats Codex as "whatever is not Claude", and picks an agent nobody chose:

- Re-cooking accepts `--harness`, `--worker-role`, `--worker-settings`, `--worker-check` and `--tracker`. It records
  them in the `.gitignore` section, but `.ralph-tui/config.toml` is a seed that is never rewritten. With
  `--worker-role` added later, the record says the workers are bound to a role while they run without its denies.
- The first `--harness` silently becomes the default agent. ralph-tui then falls back further: an `--agent` that
  matches no entry runs a bare plugin, which for Claude means `--dangerously-skip-permissions`.
- `--worker-role` and `--worker-settings` reach only Claude. Their syntax doesn't say so, and Codex has no way to take
  a team file.
- `bin/run` keeps only the last of a repeated `--epic`, `--prd` or `--base`, and ignores `--base` when the run branch
  already exists.
- The model `RalphTui` validates in its setters, so the recipe has to call them in a fixed order. It patches its shell
  scripts with text replacements that do nothing when they miss. It keeps the same facts in three places.

**What this delivers:**
- every run names its agent;
- the worker-limit params take a `<harness>:` prefix and work for both harnesses;
- a cook is refused when the existing configuration lacks what an argument needs;
- `bin/run` refuses flags it would ignore;
- the model is restructured so these changes stay small.

Facts the stories rely on:

| # | Fact | Where |
| --- | --- | --- |
| A1 | ralph-tui 0.12.0 picks the agent as follows: `--agent x` matches an `[[agents]]` entry by `name` or `plugin`. Failing that, if `x` is an installed plugin, it builds an entry with empty options, and for Claude that means `--dangerously-skip-permissions`. Without `--agent` it uses `defaultAgent`, then the first entry | `~/.bun/install/global/node_modules/ralph-tui/dist/cli.js:20655-20684` |
| A2 | `.ralph-tui/config.toml` is a `@Seed`: written when absent, never rewritten or removed | `cookbook/ralph/RalphTui.groovy`, `config()` |
| A3 | codex-cli 0.160.1 takes `-c <key=value>` per config key, and `-p <profile>` only from `$CODEX_HOME`. No option adds a rules file to one run | `codex exec --help`; `.agents/README.md:54` |
| A4 | Claude Code reads `--settings <file>` at run time | `cookbook/ralph/ralph-tui.groovy`, Consumes |
| A5 | The engine imports `groovy.toml` (`TomlSlurper`) into every recipe and model | `cookbook/README.md`, Names |
| A6 | `CookCommandTest.theCookbookReadmesRenderedHelpIsWhatHelpPrints` fails when `cookbook/README.md`'s rendered help differs from `./cook help` | `cli/src/test/groovy/thecook/cli/CookCommandTest.groovy:186` |

Decisions already made by the maintainer:

- **Breaking changes are fine; quality comes first.** Commits are `feat(cookbook)!:` where the API changes.
- **The API is symmetric across harnesses so it can evolve.** What a harness does with a value at run time is that
  harness's business, not the recipe's.
- **No default agent:** every run names one with `--agent`.
- **The `--worker-` prefix stays,** per the convention in `cookbook/README.md` Names: a param that applies to one
  party of a run names that party first.
- **`--worktree-root` keeps its `.worktrees` default,** and `--worktree-setup` and `--worktree-teardown` keep no
  default.
- **A Codex settings file is read at cook time.** Editing it later means deleting `config.toml` and cooking again.

## Goals

- No argument is accepted and recorded without its effect being present in what the cook writes, or in what already
  exists.
- No run starts without naming its agent, and no run starts with an agent the configuration has no entry for.
- `--worker-role` and `--worker-settings` read the same and validate the same for `claude` and `codex`.
- `bin/run` refuses every flag it would otherwise ignore.
- `RalphTui`'s setters can be called in any order. Every script variable the model sets is checked to exist.
- Each fact about upstream limits lives in one place: the recipe's help.

## Quality Gates

These commands must pass for every user story:

- `./gradlew check cliBuild jpackageTest` - tests, Spotless, CodeNarc, the CLI build and the packaged-binary test
- `git diff --check` - no whitespace errors

## User Stories

The stories run in order; each builds on the one before.

**Every story follows these rules:**
- **Tests come first.** Each named test is added to `core/src/test/groovy/thecook/RalphTuiCookTest.groovy`, seen to
  fail, and then made to pass.
- **Exact messages.** A refusal in a story is given exactly. Messages start with the recipe coordinates and `: `, as
  the existing refusals in that test file do.
- **Help and README stay in step.** A story that changes the help in `cookbook/ralph/ralph-tui.groovy` regenerates
  `cookbook/README.md` as Technical Considerations describes, so A6's test passes.

### US-001: RalphTui validates once, in install()

**Description:** As a maintainer, I want `RalphTui`'s setters to store values only and `install()` to validate the
whole state, so the recipe has no `if` chain and no call-order rules.

**Acceptance Criteria:**

- [ ] In `cookbook/ralph/RalphTui.groovy`, each setter only stores its argument and accepts `null`: `harnesses`,
  `trackers`, `workerRole`, `workerSettings`, `worktreeRoot`, `worktreeSetup`, `worktreeTeardown`, `workerCheck` and
  `instructions`. For `worktreeRoot`, `null` keeps `.worktrees`.
- [ ] `install(cookedWith)` runs every check the setters ran before, with the same messages, in this order:
  1. `cookedWith` is not blank;
  2. harness names and at least one harness;
  3. tracker names;
  4. role name syntax;
  5. Claude-only for role and settings;
  6. role and settings together;
  7. the role file is in the draft;
  8. settings path not blank;
  9. setup, teardown and check are not under `.ralph-tui/`.
- [ ] Every "Call after …" sentence is removed from the Javadoc. `install`'s Javadoc lists what it refuses.
- [ ] The body of `cookbook/ralph/ralph-tui.groovy` calls each setter once, unconditionally, then `install`, with no
  `if`.
- [ ] Every existing test in `RalphTuiCookTest` passes unchanged. This story changes no behavior, so it adds no test.

### US-002: One entry(harness) builds each agent entry

**Description:** As a maintainer, I want one method to build an `[[agents]]` entry from a per-harness record, so
harness differences live in data instead of string branches.

**Acceptance Criteria:**

- [ ] `FLAGS` and `OPTIONS` are replaced by one `private static final Map` from harness name to a record holding its
  `flags` and `options`.
- [ ] `private List<String> entry(String harness)` returns the entry's lines:
  `[[agents]]`, `name`, `plugin`, `command` (only with `workerCheck`), `defaultFlags`, a blank line,
  `[agents.options]`, then the options.
- [ ] `config()` no longer appends `"\ncommand = …"` to the `plugin` line. `flags()` has no `harness == 'claude'`
  ternary; the Claude `--settings` pair comes from one place.
- [ ] The rendered `config.toml` is byte-identical: `CONFIG`, `aWorkerCheckWritesTheWorkerAndPointsEachSeededEntryAtItWithRetries`,
  `aRoleAddsItsRenderedSettingsToTheClaudeEntryOnly` and every other existing test pass unchanged.

### US-003: Script variables are set by one checked helper

**Description:** As a maintainer, I want every value the model writes into a script set by one helper that fails when
the script's line is missing, so a reformatted script can never ship unconfigured.

**Acceptance Criteria:**

- [ ] `private static String assign(String script, String variable, String value)`:
  - It replaces the one line that starts with `<variable>=` with `<variable>=<shell-quoted value>`.
  - It throws `IllegalStateException("<variable>= is not in the script")` when no such line exists.
- [ ] `run()`, `clean()` and `worker()` set `worktrees`, `setup`, `teardown`, `check` and `agents` only through
  `assign`.
- [ ] `cookbook/ralph/bin/run.sh` gets the variable `flags='(--epic <id> | --prd <file>)'` beside `setup`.
  - `usage()` prints `"usage: .ralph-tui/bin/run <name> $flags [--base <ref>] [ralph-tui run arguments...]"`.
  - `run()` sets `flags` through `assign` instead of replacing the usage text.
- [ ] The output of `bin/run`'s usage is unchanged. `aJsonOnlyCookSeedsOnlyTheJsonPromptAndOffersOnlyPrd` passes
  unchanged.
- [ ] New test `everyVariableTheModelSetsIsInItsScript`: for each pair in `run.sh` (`worktrees`, `setup`, `flags`),
  `clean.sh` (`worktrees`, `teardown`) and `worker.sh` (`check`, `agents`), it asserts the source under
  `cookbook/ralph/bin/` has a line starting with `<variable>=`.

### US-004: --worker-role and --worker-settings take a harness prefix and serve both harnesses

**Description:** As a team using Claude and Codex workers, I want `--worker-role=<harness>:<role>` and
`--worker-settings=<harness>:<path>`, so each harness's workers get the team's limits with the same syntax.

**Acceptance Criteria:**

**Params** (`cookbook/ralph/ralph-tui.groovy`):
- [ ] Both params are declared `many = true, default = []` and are not `path`, because the value has a prefix.
- [ ] Their `doc` texts become:
  - `--worker-role`: `'<harness>:<role>, a role of the agents layout whose rendered settings that harness\'s entry passes'`
  - `--worker-settings`: `'<harness>:<path>, a settings file of the team\'s that harness\'s entry passes'`
- [ ] `RalphTui` takes `workerRoles(List<String>)` and `workerSettings(List<String>)`. Both resolve in `install()` to
  one map from harness to the extra `defaultFlags` of that harness's entry.

**How each value is resolved:**
- [ ] `claude:<path>` adds `"--settings", "<path>"`.
- [ ] `codex:<path>`:
  - It reads `<path>` from the draft with `current(path)`.
  - It parses the file with `new TomlSlurper().parseText(…)` and flattens nested tables into dotted keys, in file
    order.
  - It adds `"-c", "<key>=<value>"` for each key. A string is rendered with `groovy.json.JsonOutput.toJson`, a number
    or boolean with `toString()`, and a list of those as `[a, b]`.
- [ ] `--worker-role=<h>:<role>` resolves exactly as `--worker-settings=<h>:.agents/adapters/<h>/roles/<role>.<ext>`,
  where `ext` is `json` for `claude` and `toml` for `codex`.

**Refusals**, with lines after the first indented two spaces:
- [ ] A value without `:`, or with an empty part:
  `--worker-role=worker is not <harness>:<role>` /
  `name the harness the role binds, as --worker-role=claude:worker`.
  The same shape applies to `--worker-settings`, with `<harness>:<path>` and `--worker-settings=claude:ralph-claude.json`.
- [ ] A harness not in `--harness`:
  `--worker-settings=codex:x.toml names codex, which no --harness gives` / `add --harness=codex, or leave it out`.
- [ ] A harness given twice in one param:
  `--worker-settings names claude twice` / `give one file per harness`.
- [ ] A role and settings for one harness:
  `--worker-role=claude:worker and --worker-settings=claude:x.json both set claude's settings` /
  `give one of them; to combine a role with more settings, write a settings file that holds both`.
- [ ] A role name failing `[a-z][a-z0-9-]*`: today's message, with `--worker-role=claude:<name>`.
- [ ] A role file missing from the draft:
  `--worker-role=<h>:<role> needs .agents/adapters/<h>/roles/<role>.<ext>` / `the role file is not in the draft` /
  `define roles.<role> in .agents/permission/policy.yaml, then cook io.github.lmarqs.cookbook:agents#connect --harness=<h>`.
- [ ] A settings path that is absolute or has a `..` segment:
  `--worker-settings=claude:/x.json is not a path from the repository root` /
  `name a file inside the repository, without .. segments`.
- [ ] A Codex settings file missing from the draft:
  `--worker-settings=codex:x.toml needs x.toml in the draft` / `Codex settings are read when cooking` /
  `write the file, then cook again`.
- [ ] A Codex settings file that is not TOML: `x.toml is not valid TOML` / `<the parser's message>`.
- [ ] A value that is a list of tables, or any other value it can't render:
  `x.toml: <key> cannot be passed as -c <key>=<value>` / `use strings, numbers, booleans or lists of them`.

**Tests:**
- [ ] Rewritten to the prefixed syntax, with the same assertions:
  - `aRoleAddsItsRenderedSettingsToTheClaudeEntryOnly`, as `--worker-role=claude:worker`;
  - `refusesARoleWhoseRenderedSettingsAreNotInTheDraft`, with the new first line;
  - `takesTheRoleFileAgentsConnectRendersEarlierInTheInvocation`;
  - `aSettingsFileIsPassedAsGivenAndNeverWritten`;
  - `refusesARoleAndASettingsFileTogether`;
  - `refusesARoleOrSettingsFileWithoutAClaudeEntry`, renamed `refusesAWorkerFileForAHarnessNotGiven`.
- [ ] New tests:
  - `aCodexSettingsFileBecomesConfigFlagsOfTheCodexEntry`: a file with `model_reasoning_effort = "high"` and
    `[sandbox_workspace_write]` `writable_roots = ["/var/cache"]` gives the Codex entry the extra flags
    `"-c", "model_reasoning_effort=\"high\"", "-c", "sandbox_workspace_write.writable_roots=[\"/var/cache\"]"`;
  - `aCodexRoleReadsTheRoleTomlAgentsConnectWouldRender`, using a hand-written
    `.agents/adapters/codex/roles/worker.toml`;
  - `refusesACodexRoleWhoseFileIsNotInTheDraft`;
  - `refusesAWorkerValueWithoutAHarness`;
  - `refusesAHarnessGivenTwiceInOneWorkerParam`;
  - `refusesAWorkerSettingsPathOutsideTheRepository`;
  - `refusesACodexSettingsFileThatIsMissingOrNotToml`.

**Docs:**
- [ ] The help's API examples become `--worker-role=claude:worker` and
  `--worker-settings=claude:ralph-claude.json --worker-settings=codex:ralph-codex.toml`. Its paragraphs on these
  params, Consumes, Produces (the sentence on what each entry runs) and Connections ("What a Claude worker may do",
  "What a Codex worker may do") state the prefixed rule, and that a Codex file is read when cooking.
- [ ] `docs/ux/generated-content-standards.md:143`'s example message is updated to the new first line.
- [ ] The model's Javadoc (Use, With other models) uses the new calls.

### US-005: A cook is refused when the existing config.toml lacks what an argument needs

**Description:** As someone re-cooking the recipe, I want a cook refused when `.ralph-tui/config.toml` already exists
without an argument's effect, so the recorded arguments never claim what the configuration doesn't do.

**Acceptance Criteria:**

**When the check runs:** in `install()`, only when `current('.ralph-tui/config.toml')` is not null. The file is parsed
with `TomlSlurper`. Keys and entries the check doesn't name are the team's and are never refused.

**The checks, in this order, each refusing on the first failure:**
1. [ ] Each `--harness=<h>`: some `[[agents]]` entry has `plugin = "<h>"`.
2. [ ] Each harness with extra flags from US-004: some entry with that plugin has those flags as a contiguous run in
   its `defaultFlags`.
3. [ ] `--worker-check`: for each harness, some entry with that plugin has `command = ".ralph-tui/bin/worker"` and
   the harness as the first item of `defaultFlags`.
4. [ ] When `beads` is in `--tracker`: `tracker`, or failing that `defaultTracker`, is present and starts with
   `beads`.

**Refusal shape**, lines after the first indented two spaces:
```
<argument> is not applied in .ralph-tui/config.toml
<cause>
the configuration is seeded once and never rewritten, so a later cook cannot add it
add it there, or delete .ralph-tui/config.toml and cook again
```

The `<argument>` and `<cause>` for each check:

| Check | `<argument>` | `<cause>` |
| --- | --- | --- |
| 1 | `--harness=<h>` | `no [[agents]] entry there has plugin = "<h>"` |
| 2 | the param as given, such as `--worker-settings=claude:x.json` | `no [[agents]] entry there with plugin = "<h>" has <the flags, each JSON-quoted, joined by ", "> in its defaultFlags` |
| 3 | `--worker-check=<path>` | `no [[agents]] entry there with plugin = "<h>" has command = ".ralph-tui/bin/worker" and "<h>" first in its defaultFlags` |
| 4 | `--tracker=beads` | `it sets no tracker, and a run with --epic needs tracker = "beads"`, or, when a non-beads tracker is set, `its tracker is "<t>", and a run with --epic needs tracker = "beads"` |

**A configuration that isn't TOML:** `.ralph-tui/config.toml is not valid TOML` / `<the parser's message>` /
`fix it, or delete it and cook again`.

**A refused cook writes nothing:** `config.toml` and the `.gitignore` section stay byte-identical.

**Tests:**
- [ ] `keepsAHandEditedConfigurationAndPrompt` is changed: its second cook uses `harness: ['claude']` and
  `'worktree-root': ['wt']` instead of adding `codex`. It asserts the hand edits are kept, and that the `.gitignore`
  record line is `# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worktree-root=wt`.
- [ ] `aWorkerCheckLeavesAConfigurationSeededEarlierAsItIs` is replaced by
  `refusesAWorkerCheckOverAConfigurationSeededWithoutIt`.
- [ ] New tests:
  - `refusesARecookWhoseHarnessTheConfigurationLacks`
  - `refusesARecookWhoseWorkerFilesTheConfigurationLacks` (Claude role, Claude settings, Codex settings)
  - `refusesARecookWhoseBeadsTrackerTheConfigurationLacks` (seeded with `tracker: ['json']`, re-cooked with the
    default)
  - `acceptsARecookOverAHandWrittenConfigurationThatHasWhatTheArgumentsNeed`: an entry `name = "shim"`,
    `plugin = "claude"`, `command = "scripts/ralph/claude"`,
    `defaultFlags = ["--permission-prompts", "none", "--settings", "x.json"]`, and `tracker = "beads-rust"`, cooked
    with `--worker-settings=claude:x.json`, is accepted and left unchanged
  - `acceptsARecookWithTheWorkerCheckTheConfigurationWasSeededWith`: re-cooked with another check path, the worker
    gets the new path
  - `refusesAConfigurationThatIsNotToml`

**Docs:** the help's Re-cooking paragraph replaces "The seeds are never rewritten … edit them or delete them and cook
again" with the refusal rule. It keeps that a narrower `--tracker` leaves an earlier prompt in place. The Trap "A
configuration seeded before `--worker-check` keeps its entries" is removed.

### US-006: bin/run refuses a repeated flag and a --base it would ignore

**Description:** As someone starting a run, I want `bin/run` to refuse flags it would ignore, so the run starts from
what I asked for.

**Acceptance Criteria:**

- [ ] In `cookbook/ralph/bin/run.sh`, a second `--epic`, `--prd` or `--base`, in either the space or the `=` form,
  fails with exit 1: `run: <flag> is given twice` / `  a run takes one <flag>`.
- [ ] When `--base` is given and `refs/heads/ralph/<name>` exists, it fails with exit 1 before touching any worktree:
  - `run: --base does not apply: ralph/<name> exists`
  - `  the run continues the branch where it is`
  - `  run without --base, or run .ralph-tui/bin/clean <name> first`
- [ ] Without `--base`, an existing branch or worktree is reused as today.
- [ ] Tests:
  - `runRefusesARepeatedEpicPrdOrBase`, which covers the three flags and the `=` form;
  - `runRefusesABaseWhenTheRunBranchExists`, which runs once, then runs again with `--base HEAD`, and asserts exit 1,
    the three lines and an unchanged branch.
- [ ] Docs: the help's Run-time effects states both refusals.

### US-007: Every run names its agent; no default agent

**Description:** As someone starting a run, I want to name the agent every time, and be refused one the configuration
has no entry for, so no run uses an agent I didn't choose, or Claude without permission checks.

**Acceptance Criteria:**

**Cook:**
- [ ] `--harness` is declared with `sorted = true`. Its doc becomes
  `'Harnesses ralph-tui drives, one agent entry each; each run picks one with --agent'`.
- [ ] The seeded `config.toml` has no `defaultAgent` line: the `CONFIG` test constant drops it.
- [ ] The help, the param doc and the model's Javadoc no longer say the first harness is the default.

**`bin/run`:**
- [ ] It parses `--agent <entry>` and `--agent=<entry>` like `--epic`, including US-006's refusal of a repeat.
- [ ] Without `--agent` it prints the usage and exits 2. The usage becomes
  `usage: .ralph-tui/bin/run <name> --agent <entry> $flags [--base <ref>] [ralph-tui run arguments...]`.
- [ ] After the worktree is ready, and after any copy of the configuration, it checks the entry:
  - It looks for a line matching `^(name|plugin) = "<entry>"$` in `<worktree>/.ralph-tui/config.toml`.
  - With no match, it fails with exit 1:
    - `run: --agent <entry> matches no entry in .ralph-tui/config.toml`
    - `  ralph-tui would run an installed plugin of that name with no options, and Claude without permission checks`
    - `  give one of: <the entries' name values, joined by ", ">`
- [ ] It passes the agent as `exec ralph-tui run --epic "$epic" --agent "$agent" "$@"`, and the same with `--prd`.
  A copied prompt keeps coming after `--agent`, as in `ralph-tui run --epic ep-1 --agent claude --prompt <path>
  --headless`.

**The instructions section** (`instructionsSections()`):
- [ ] With both trackers it reads `- Loop runs: \`.ralph-tui/bin/run <name> --agent <entry> --epic <id>\`, or
  \`--prd <file>\`, runs ralph-tui in`.
- [ ] With one tracker it reads `- Loop runs: \`.ralph-tui/bin/run <name> --agent <entry> <flag>\` runs ralph-tui in`.

**Tests:**
- [ ] Every existing `launch(…, '.ralph-tui/bin/run', …)` call passes `--agent`, `'claude'`, and its expected output
  includes `--agent claude`. `givesTheLauncherCommandsInEachInstructionsFile` and
  `aJsonOnlyCookSeedsOnlyTheJsonPromptAndOffersOnlyPrd` expect the new text.
- [ ] New tests:
  - `runRequiresAnAgent`
  - `runRefusesAnAgentTheConfigurationHasNoEntryFor`: a `--harness=codex` cook, run with `--agent claude`
  - `runTakesTheAgentByEntryNameOrPluginInEitherForm`: `--agent claude` and `--agent=claude`

**Docs:**
- [ ] The help's published-names table, After cooking and Run-time effects show `--agent <entry>`.
- [ ] Connections states that `--agent codex` runs with only what `--worker-*=codex:` gave the Codex entry.
- [ ] A new Trap: ralph-tui 0.12 runs an installed plugin with no options for an `--agent` that matches no entry, and
  for Claude that means no permission checks. `bin/run` refuses it; `ralph-tui run` started by hand does not.

### US-008: Each fact lives in one place; Traps ordered by likelihood

**Description:** As a reader of the recipe's help and model, I want each upstream limit stated once, and the traps I'm
most likely to hit listed first.

**Acceptance Criteria:**

**The model's Javadoc** (`RalphTui.groovy`):
- [ ] It drops the "Known limits of those versions: …" paragraph.
- [ ] It keeps the entries to add, the model and effort paragraph (with `sonnet`, `opus` and `haiku`), and the other
  sections required by `cookbook/README.md` "Model class doc comment".

**The doc test:**
- [ ] `theModelDocumentsTheEntriesToAddAndTheKnownLimits` becomes `theModelDocumentsTheEntriesToAdd`, keeping the
  assertions on `skipPermissions = true`, `"--settings", ".agents/adapters/claude/roles/worker.json"`,
  `ralph-tui run --model`, `CLAUDE_CODE_EFFORT_LEVEL`, `model_reasoning_effort` and `` `sonnet`, `opus` and `haiku` ``.
- [ ] New test `theHelpStatesTheKnownLimits` asserts the help text of `cookbook/ralph/ralph-tui.groovy` contains
  `fewer than three tasks`, `--dangerously-bypass-hook-trust`, `neither read nor write the Beads database`,
  `permissions.allow` and `prefix rules`. Assert against the text with line continuations removed.

**Traps:**
- [ ] The two items saying ralph-tui copies only `config.toml` into a parallel worker become one item. It also says
  that without a committed `.ralph-tui/`, parallel workers find no `bin/worker`.
- [ ] The items appear in this order:
  1. the trust and allows item
  2. the `<promise>COMPLETE</promise>` item
  3. the bare-plugin `--agent` item
  4. `git add -A`
  5. the failed-commit item
  6. the merged parallel-copy item
  7. the Claude plugin `skipPermissions` placement
  8. model aliases
  9. the parallel minimum
  10. the Codex hooks item
  11. the Codex `.git` and Beads item
  12. prefix denies
  13. rate-limit words
  14. `worktreeDir`
  15. a tracked PRD with `--parallel`
  16. a committed config at an earlier `--base`

**Other docs:**
- [ ] `cookbook/ralph/README.md` Gating completion stays true. Its config example has no `defaultAgent`, and it says
  `--worker-check`, unchanged.
- [ ] `theHelpFollowsTheRecipeDocumentationStandard` passes.

## Functional Requirements

- FR-1: Every run must name its agent with `--agent`, and `bin/run` must refuse an agent that matches no entry.
- FR-2: The seeded configuration must have no default agent.
- FR-3: `--worker-role` and `--worker-settings` must take `<harness>:<value>` and apply to that harness's entry.
  - Claude gets `--settings <file>`.
  - Codex gets `-c key=value` for each key in the file, read when cooking.
  - A role resolves to `.agents/adapters/<harness>/roles/<role>.<json|toml>`.
- FR-4: A cook must be refused, writing nothing, when an existing `.ralph-tui/config.toml` lacks an argument's effect.
- FR-5: `bin/run` must refuse a repeated `--agent`, `--epic`, `--prd` or `--base`, and `--base` when `ralph/<name>`
  exists.
- FR-6: `RalphTui`'s setters must be callable in any order, and its refusals must run in `install()`.
- FR-7: Every script variable the model sets must exist in its source, or the cook must fail.

## Non-Goals

- **Rendering Codex roles in `agents#connect`.** A Codex role works here once connect writes
  `.agents/adapters/codex/roles/<role>.toml`.
- **Matching runtime behavior across harnesses,** such as Claude's sandbox or network defaults, or which of two
  Codex `-c` values for one key wins.
- **Reading a Codex settings file at run time.**
- **Resolving the Codex trust conflict** between `.agents/README.md:57-59` and the help's Codex hooks Trap. That is a
  separate issue.
- **Defaults for `--worktree-setup` or `--worktree-teardown`,** and any change to `--worktree-root`.
- **`bin/clean` and `bin/worker` behavior.**
- **This repository's own cooked ralph-tui files** (`.ralph-tui/`, `.gitignore`, the `AGENTS.md` section,
  `cook.lock`). They update when the maintainer cooks a released version.
- **The `release` workflow, GitHub issue comments, pushing and opening pull requests.**

## Technical Considerations

- **Files:**
  - `cookbook/ralph/RalphTui.groovy`
  - `cookbook/ralph/ralph-tui.groovy`
  - `cookbook/ralph/bin/run.sh`
  - `cookbook/ralph/README.md`
  - `cookbook/README.md`
  - `docs/ux/generated-content-standards.md`
  - `core/src/test/groovy/thecook/RalphTuiCookTest.groovy`
- **Regenerating `cookbook/README.md`'s rendered help:**
  1. Run `./gradlew cliBuild`.
  2. Replace the body of each fenced block that starts with ` ````text ` and `$ ./cook help <address>` with the output
     of `./cook -C . help <address>`. The affected blocks are lines 226 and 299, for
     `io.github.lmarqs.cookbook:ralph:0.0.0` and `…#ralph-tui`.
  3. Never edit the rendered text by hand. A6's test checks the result.
- **Shared test helpers** in `RalphTuiCookTest`: `kitchen()`, `repository()`, `cook(...)`, `launch(...)`,
  `commitAll(...)`, `git(...)`, `write(...)`, `executable(...)` and `shouldFail(...)`. The fake `ralph-tui` on the test
  `PATH` prints its arguments, which is how run tests assert the `ralph-tui run …` line.
- **Help text:** a long line in a recipe's `@help` continues after a trailing `\`. See `README.md`, Development
  tooling. Spotless and CodeNarc run only in the Done check.
- **Never** hand-edit `cook.lock` or `build/`, and never weaken a test. Change a test's assertions only where a story
  names the change.
- **Commits:**
  - `refactor(cookbook)` for US-001 to US-003;
  - `feat(cookbook)!` for US-004, US-005 and US-007;
  - `fix(cookbook)` for US-006;
  - `docs(cookbook)` for US-008.

  Each subject ends with `(#63)`.

## Success Metrics

- Every new test fails before its story's change and passes after.
- The Done check passes after every story.
- Without `--agent`, no `ralph-tui run` line is produced. Without its effect in `config.toml`, no worker argument is
  recorded.

## Open Questions

- None blocking. A follow-up issue covers the Codex trust conflict (Non-Goals).
