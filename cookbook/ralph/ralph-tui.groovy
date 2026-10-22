@help('''
Sets ralph-tui up to work through a tracker's tasks with unattended coding agents, each run in a worktree of its own:
a configuration with one agent entry per harness, the prompts it renders for each task, and a launcher that creates,
sets up and cleans the run's worktree.

## API

```sh
# Claude Code workers, limited by the project's settings
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude
# Claude Code and Codex workers, Claude by default
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --harness=codex
# Claude workers bound to the agents layout's role worker
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-role=worker
# Claude workers with a settings file the team writes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-settings=ralph-worker.json
# Claude workers in run worktrees the launcher sets up and tears down with the team's scripts
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worktree-setup=.worktrees/setup \
--worktree-teardown=.worktrees/teardown
# Also giving agents and people the launcher's commands, in the files they read
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --instructions=AGENTS.md
```

The first `--harness` is the default agent. `--worker-role` and `--worker-settings` reach only the Claude entry, and
only one of them is given. `--worktree-setup` and `--worktree-teardown` each name a committed executable by its path
from the repository root, outside `.ralph-tui/`; without them, `bin/run` runs no setup and `bin/clean` no teardown.
`--instructions` names a Markdown file by its path from the repository root, and repeats for each file; the file must
exist. What it publishes for people, sessions and other recipes:

| Name | What it is |
| --- | --- |
| `.ralph-tui/bin/run <name> (--epic <id> \\| --prd <file>) [--base <ref>] [ralph-tui run arguments…]` | \
Runs ralph-tui in the run's worktree, as Run-time effects says |
| `.ralph-tui/bin/clean <name> [--force]` | Removes the run's worktree and branch, and every \
`ralph-parallel/*` worktree and branch |
| `.worktrees/ralph/<name>` on branch `ralph/<name>` | The rule for a run's worktree, following `.worktrees/<branch>` |
| `.ralph-tui/templates/beads.hbs`, `json.hbs` | The prompts ralph-tui renders for a run with `--epic` and \
with `--prd` |

## Consumes

- ralph-tui 0.12.0 and Git at run time; Claude Code 2.1.306 and codex-cli 0.160.1 as workers. `bd` 1.3.1 only for a
  run with `--epic`, which reads the Beads database; a run with `--prd` needs no `bd`.
- `.agents/adapters/claude/roles/<name>.json` in the draft, with `--worker-role=<name>`: written by
  `io.github.lmarqs.cookbook:agents#connect --harness=claude` from `roles.<name>` in `.agents/permission/policy.yaml`.
  Without it, cooking is refused and names both.
- The `--worker-settings` path as given: written into the configuration, never read or written at cook time. Workers
  resolve it in the run worktree, so it names a committed file by its path from the repository root, outside
  `.ralph-tui/`, where Git ignores all but the configuration, prompts and launcher.
- The `--worktree-setup` file, at run time, only with `--worktree-setup`: `bin/run` runs it in each run worktree it
  creates and fails the run when it is missing there, not executable or fails. Never read or written at cook time.
- The `--worktree-teardown` file, at run time, only with `--worktree-teardown`: `bin/clean` runs it in the run worktree
  before removing anything, and stops when it is missing there, not executable or fails. Never read or written at cook
  time.
- The files `--instructions` names, which must exist, written by the team or by a recipe cooked earlier in the same
  invocation.
- By hand first: nothing. By hand after: installing ralph-tui and the runtimes it needs, and for `--epic`, a Beads
  database with the epic, as After cooking says.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.ralph-tui/config.toml` | Config version 2.1, `tracker = "beads"`, the first harness as `defaultAgent`, \
`maxIterations = 10`, `autoCommit = true`, \
`[errorHandling] strategy = "skip"`, `[parallel] directMerge = true`, \
and one `[[agents]]` entry per harness | Always, when absent | seed |
| `.ralph-tui/templates/beads.hbs`, `json.hbs` | The prompts: stay in this worktree, \
leave committing to ralph-tui, never push, print `<promise>COMPLETE</promise>` only after the checks pass and \
`BLOCKED:` otherwise; never close or update the bead, or never edit the PRD file | Always, when absent | seed |
| `.ralph-tui/bin/run`, `.ralph-tui/bin/clean` | The launcher scripts, executable | Always | owned |
| `.gitignore` | The section `ralph-tui`: the argument line, `/.ralph-tui/*` but the configuration, \
prompts and launcher, and `/.worktrees/ralph/` | Always | owned |

The Claude entry runs `claude --permission-prompts none --permission-mode acceptEdits --print`: edits are accepted, and
any command the settings do not allow is refused rather than asked, with `skipPermissions = false`. `--worker-role` or
`--worker-settings` adds `--settings <file>` to it. The Codex entry runs `codex -c approval_policy=never -c
sandbox_workspace_write.network_access=false exec --sandbox workspace-write`: no approvals, no network, writes only in
the worktree. Neither sets a model or an effort.

Never touches: `.agents/permission/policy.yaml`, any settings or role file, version pins, remotes, pull requests, the
tracker's state, and the rest of each `--instructions` file.

## Connections

| Contact | Made by |
| --- | --- |
| What a Claude worker may do | The person, by `--worker-role` or `--worker-settings`; the team, \
by adding entries to the configuration; the session, by `ralph-tui run --agent <entry>` |
| What a Codex worker may do | The team, in the seeded entry, which sets Codex's `workspace-write` sandbox |
| The tracker a run takes tasks from | The session: `--epic <id>` runs the Beads epic, `--prd <file>` the JSON PRD |
| The run worktree's setup | The person, by `--worktree-setup`; the team writes the file |
| The run worktree's teardown | The person, by `--worktree-teardown`; the team writes the file |
| The branch a run starts from | The session, by `--base`, `HEAD` by default |
| Model and effort | The session, by `ralph-tui run --model` and `CLAUDE_CODE_EFFORT_LEVEL`; the team, per entry |
| The launcher's commands in the files agents and people read | The person, by `--instructions`; the section is \
appended at the file's end, and stays where the team moves it |
| The agents layout's Launching table | The team, from After cooking |
| Delivering the run branch | The session: the launcher never pushes, merges or opens a pull request |

`io.github.lmarqs.cookbook:git#worktrees` ignores and documents `.worktrees/`; cook the two in either order.
`io.github.lmarqs.cookbook:beads#tracker` gives the Beads side; cook the two in either order. With `--worker-role`,
cook `io.github.lmarqs.cookbook:agents#connect --harness=claude` first, or before this recipe in the same invocation,
with the role defined in the policy.

## Run-time effects

`bin/run <name>` refuses while `git status --porcelain .ralph-tui` in the main checkout lists anything, because a
worktree holds only what is committed. It creates `.worktrees/ralph/<name>` on a new branch `ralph/<name>` from
`--base`, `HEAD` by default, or reuses the worktree or the branch when they exist. With `--worktree-setup`, it runs that
file from the root of each worktree it creates, never one it reuses, and stops the run when the file is missing, not
executable or fails, leaving the worktree for `bin/clean`; without `--worktree-setup`, it runs no setup. It prints the
worktree's path and branch, then runs `ralph-tui run --epic <id>`, or `--prd` with the file's absolute path, in the
worktree, with the remaining arguments, such as `--headless`, `--agent codex` or `--model opus`. It sets and strips no
environment variable, so `CLAUDE_CODE_EFFORT_LEVEL` reaches Claude workers.

`bin/clean <name>` refuses while `ralph/<name>` has commits `HEAD` does not, which includes a branch delivered by a
squash merge, or while its worktree has changes; `--force` removes them anyway. With `--worktree-teardown`, it then
runs that file from the root of the run worktree, when one exists, and stops when the file is missing, not executable
or fails, removing nothing. It removes the worktree, every worktree on a `ralph-parallel/*` branch with that branch,
then the branch `ralph/<name>`.

## Re-cooking

Repeat the arguments on the opening line of the `ralph-tui` section in `.gitignore`, such as `#
io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worktree-setup=.worktrees/setup`. Cooking
with another `--worktree-setup` or `--worktree-teardown`, or without one, rewrites the launcher, and cooking without a
file's `--instructions` removes its section. The seeds are never rewritten: to take other harnesses, a worker role or a
newer version's configuration or prompts, edit them or delete them and cook again. A newer version rewrites the
launcher and the sections.

## Traps

- Claude Code ignores the project's `permissions.allow` in a workspace nobody has trusted, and a fresh clone or a new
  worktree may be one: an unattended Claude worker's allows come through `--worker-role` or `--worker-settings`.
- Deny rules are prefix rules: `git -C . push` gets past `git push`. They are a guardrail; the structural limit is a
  worker without push credentials.
- A Codex `workspace-write` worker can neither read nor write the Beads database, nor write `.git`: ralph-tui commits
  and closes beads for it, and the prompts ask for neither.
- Codex runs no project hooks in a worktree it has not trusted, so a Codex worker gets no session-start context unless
  started with `--dangerously-bypass-hook-trust`.
- ralph-tui 0.12's Claude plugin accepts only `sonnet`, `opus` and `haiku` as `--model`.
- ralph-tui 0.12's parallel mode starts children that are still blocked, and refuses fewer than three tasks.
- `bin/clean` removes every `ralph-parallel/*` worktree, so cleaning one run while another runs in parallel mode stops
  the other's workers.
''')
@yield('''
Install ralph-tui and the runtimes it needs with `mise use -g node@22 bun@1.3 npm:ralph-tui@0.12.0`: npm needs node to
install it, and its launcher runs on bun. Without `-g`, mise pins them in the project's `mise.toml` instead, which is
the team's choice. Review `.ralph-tui/config.toml` and the prompts, and commit `.gitignore` and `.ralph-tui/`:
`bin/run` refuses while `.ralph-tui` has uncommitted changes. Then run `.ralph-tui/bin/run <name> --epic <id>`, with a
Beads database holding the epic, or `.ralph-tui/bin/run <name> --prd <file>`, and `.ralph-tui/bin/clean <name>` once
the branch `ralph/<name>` is delivered. Cooked without `--instructions`, cook again with it, such as
`--instructions=AGENTS.md`, to give agents these commands, and with the agents layout, add them to the Launching
table in `.agents/README.md`.
''')
import io.github.lmarqs.cookbook.ralph.RalphTui

@param(value = ['harness'], choices = ['claude', 'codex'], many = true,
    doc = 'Harnesses ralph-tui drives, one agent entry each; the first is the default')
    String[] harness
@param(value = ['worker-role'], default = [],
    doc = 'A role of the agents layout whose rendered Claude settings the claude entry passes with --settings')
    String workerRole
@param(value = ['worker-settings'], default = [],
    doc = 'A Claude settings file the claude entry passes with --settings')
    String workerSettings
@param(value = ['worktree-setup'], default = [], path = true,
    doc = 'An executable, by its path from the repository root, that bin/run runs in each run worktree it creates')
    String worktreeSetup
@param(value = ['worktree-teardown'], default = [], path = true,
    doc = 'An executable, by its path from the repository root, that bin/clean runs in a run worktree before removal')
    String worktreeTeardown
@param(value = ['instructions'], many = true, default = [], path = true,
    doc = "A Markdown file, from the repository root, where the recipe gives the launcher's commands")
    String[] instructions

@ingredient(RalphTui) def ralph

List<String> harnesses = (harness as List<String>).unique(false)
ralph.harnesses(harnesses)

if (workerSettings != null) {
  ralph.workerSettings(workerSettings)
}

if (workerRole != null) {
  ralph.workerRole(workerRole)
}

if (worktreeSetup != null) {
  ralph.worktreeSetup(worktreeSetup)
}

if (worktreeTeardown != null) {
  ralph.worktreeTeardown(worktreeTeardown)
}

List<String> files = (instructions as List<String>).unique(false).sort(false)
ralph.instructions(files)

def arguments = harnesses.collect { "--harness=${it}" } +
(workerRole != null ? ["--worker-role=${workerRole}"] : []) +
(workerSettings != null ? ["--worker-settings=${workerSettings}"] : []) +
(worktreeSetup != null ? ["--worktree-setup=${worktreeSetup}"] : []) +
(worktreeTeardown != null ? ["--worktree-teardown=${worktreeTeardown}"] : []) +
files.collect { "--instructions=${it}" }
ralph.install((['io.github.lmarqs.cookbook:ralph#ralph-tui'] + arguments).join(' '))
