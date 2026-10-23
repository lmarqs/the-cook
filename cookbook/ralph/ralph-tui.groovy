@help('''
Sets ralph-tui up to work through a tracker's tasks with unattended coding agents, each run in a worktree of its own:
a configuration with one agent entry per harness, the prompts it renders for each task, and a launcher that creates,
sets up and cleans the run's worktree.

## API

```sh
# Claude Code workers, limited by the project's denies, and its allows where the checkout is trusted
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude
# Claude Code and Codex workers, Claude by default
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --harness=codex
# Claude workers bound to the agents layout's role worker
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-role=claude:worker
# Claude and Codex workers with a settings file each, which the team writes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --harness=codex \
--worker-settings=claude:ralph-claude.json --worker-settings=codex:ralph-codex.toml
# Claude workers in run worktrees under worktrees/ralph/ instead of .worktrees/ralph/
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worktree-root=worktrees
# Claude workers in run worktrees the launcher sets up and tears down with the team's scripts
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worktree-setup=.worktrees/setup \
--worktree-teardown=.worktrees/teardown
# Claude workers whose tasks complete only once the team's check passes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-check=scripts/check
# Only JSON PRDs: seeds only their prompt, and the launcher offers only --prd
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --tracker=json
# Also giving agents and people the launcher's commands, in the files they read
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --instructions=AGENTS.md
```

The first `--harness` is the default agent. `--worker-role` and `--worker-settings` each take `<harness>:<x>`, once
per harness, and the harness is one `--harness` gives: `claude:<path>` makes the Claude entry pass `--settings <path>`,
and `codex:<path>` makes the Codex entry pass one `-c <key>=<value>` for each key of the TOML file at `<path>`, which
is read when cooking. A role and a settings file do not bind the same harness; to combine them, write a settings file
that holds both. `--worktree-root` names the directory, by its path from the repository root, under whose
`ralph/` run worktrees go, as `io.github.lmarqs.cookbook:agents#connect --worktree-root` does for Claude Code's;
`.worktrees` by default. `--worktree-setup` and `--worktree-teardown` each name a committed executable by its path
from the repository root, outside `.ralph-tui/`; without them, `bin/run` runs no setup and `bin/clean` no teardown.
`--worker-check` names one too, the check a task must pass to complete; without it, the worker refuses every task.
`--tracker` is `beads`, run with `--epic`, or `json`, run with `--prd`, and repeats; both by default. It narrows the
seeded prompts, the configuration's `tracker` line and the run flag the launcher offers. `--instructions` names a
Markdown file by its path from the repository root, and repeats for each file; the file must exist. What it publishes
for people, sessions and other recipes:

| Name | What it is |
| --- | --- |
| `.ralph-tui/bin/run <name> (--epic <id> \\| --prd <file>) [--base <ref>] [ralph-tui run arguments…]` | \
Runs ralph-tui in the run's worktree, as Run-time effects says |
| `.ralph-tui/bin/clean <name> [--force]` | Removes the run's worktree and branch, and its \
parallel workers' worktrees and branches |
| `.ralph-tui/bin/worker <agent> [agent arguments…]` | Runs the agent for ralph-tui, \
as Run-time effects says |
| `<worktree root>/ralph/<name>` on branch `ralph/<name>` | The rule for a run's worktree, following \
`<worktree root>/<branch>` |
| `.ralph-tui/templates/beads.hbs`, `json.hbs` | The prompts ralph-tui renders for a run with `--epic` and \
with `--prd` |

## Consumes

- ralph-tui 0.12.0 and Git at run time; Claude Code 2.1.306 and codex-cli 0.160.1 as workers. `bd` 1.3.1 only for a
  run with `--epic`, which reads the Beads database; a run with `--prd` needs no `bd`.
- `.agents/adapters/<harness>/roles/<name>.json` for `claude`, `.toml` for `codex`, in the draft, with
  `--worker-role=<harness>:<name>`: written by `io.github.lmarqs.cookbook:agents#connect --harness=<harness>` from
  `roles.<name>` in `.agents/permission/policy.yaml`. Without it, cooking is refused and names both.
- The `--worker-settings=claude:<path>` path as given: written into the configuration, never read or written at cook
  time. Workers resolve it in the run worktree, so it names a committed file by its path from the repository root,
  without `..` segments and outside `.ralph-tui/`, where Git ignores all but the configuration, prompts and launcher.
- The `--worker-settings=codex:<path>` file, and a Codex role's file, in the draft: read at cook time, and its keys
  written into the configuration as `-c` flags. Without the file, or when it is not TOML, or holds a value `-c` cannot
  carry, cooking is refused.
- The `--worktree-setup` file, at run time, only with `--worktree-setup`: `bin/run` runs it in each run worktree it
  creates and fails the run when it is missing there, not executable or fails. Never read or written at cook time.
- The `--worktree-teardown` file, at run time, only with `--worktree-teardown`: `bin/clean` runs it in the run worktree
  before removing anything, and stops when it is missing there, not executable or fails. Never read or written at cook
  time.
- The `--worker-check` file, at run time, only with `--worker-check`: the worker runs it in the worker's worktree,
  with the commit the agent started from as its argument, and a task completes only when it exits 0. Never read or
  written at cook time.
- The files `--instructions` names, which must exist, written by the team or by a recipe cooked earlier in the same
  invocation.
- By hand first: nothing. By hand after: installing ralph-tui and the runtimes it needs, and for `--epic`, a Beads
  database with the epic, as After cooking says.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.ralph-tui/config.toml` | Config version 2.1, `tracker = "beads"` with the `beads` tracker, \
the first harness as `defaultAgent`, \
`maxIterations = 10`, `autoCommit = true`, \
`[errorHandling] strategy = "skip"`, or with `--worker-check` `strategy = "retry"`, `maxRetries = 2` and \
`continueOnNonZeroExit = false`, `[parallel] directMerge = true`, and one `[[agents]]` entry per harness, with \
`--worker-check` running `.ralph-tui/bin/worker` with the harness as its first argument | Always, when absent | seed |
| `.ralph-tui/templates/beads.hbs`, `json.hbs` | The prompts of the chosen trackers: stay in this worktree, \
leave committing to ralph-tui, never push, print `<promise>COMPLETE</promise>` only after the checks pass and \
`BLOCKED:` otherwise; never close or update the bead, or never edit the PRD file | Always, when absent | seed |
| `.ralph-tui/bin/run`, `.ralph-tui/bin/clean` | The launcher scripts, executable | Always | owned |
| `.ralph-tui/bin/worker` | The worker wrapper, executable | Always | owned |
| `.gitignore` | The section `ralph-tui`: the argument line, `/.ralph-tui/*` but the configuration, \
prompts and launcher, and `/<worktree root>/ralph/` | Always | owned |

The Claude entry runs `claude --permission-prompts none --permission-mode acceptEdits --print`: edits are accepted, and
any command the settings do not allow is refused rather than asked, with `skipPermissions = false`.
`--worker-role=claude:<role>` or `--worker-settings=claude:<path>` adds `--settings <file>` to it. The Codex entry runs
`codex -c approval_policy=never -c sandbox_workspace_write.network_access=false exec --sandbox workspace-write`: no
approvals, no network, writes only in the worktree; `--worker-role=codex:<role>` or `--worker-settings=codex:<path>`
adds one `-c <key>=<value>` for each key of the file. Neither sets a model or an effort.

Never touches: `.agents/permission/policy.yaml`, any settings or role file, version pins, remotes, pull requests, the
tracker's state, and the rest of each `--instructions` file.

## Connections

| Contact | Made by |
| --- | --- |
| What a Claude worker may do | The person, by `--worker-role=claude:<role>` or `--worker-settings=claude:<path>`; the \
team, by adding entries to the configuration; the session, by `ralph-tui run --agent <entry>` |
| What a Codex worker may do | The person, by `--worker-role=codex:<role>` or `--worker-settings=codex:<path>`; the \
team, in the seeded entry, which sets Codex's `workspace-write` sandbox |
| The tracker a run takes tasks from | The session: `--epic <id>` runs the Beads epic, `--prd <file>` the JSON PRD |
| Where a run's configuration and prompts come from | The team, by committing `.ralph-tui/`; otherwise each person, \
by ignoring it in their clone, and `bin/run` copies it into each run |
| Whether a task completes only once the checks pass | The person, by `--worker-check`; the team writes the \
file, or points an entry's `command` at a wrapper of its own |
| Where run worktrees go | The person, by `--worktree-root` |
| The run worktree's setup | The person, by `--worktree-setup`; the team writes the file |
| The run worktree's teardown | The person, by `--worktree-teardown`; the team writes the file |
| The branch a run starts from | The session, by `--base`, `HEAD` by default |
| Model and effort | The session, by `ralph-tui run --model` and `CLAUDE_CODE_EFFORT_LEVEL`; the team, per entry |
| The launcher's commands in the files agents and people read | The person, by `--instructions`; the section is \
appended at the file's end, and stays where the team moves it |
| The agents layout's Launching table | The team, from After cooking |
| Delivering the run branch | The session: the launcher never pushes, merges or opens a pull request |

`io.github.lmarqs.cookbook:git#worktrees` ignores and documents `.worktrees/`; cook the two in either order.
`io.github.lmarqs.cookbook:beads#tracker` gives the Beads side; cook the two in either order. With
`--worker-role=<harness>:<role>`, cook `io.github.lmarqs.cookbook:agents#connect --harness=<harness>` first, or before
this recipe in the same invocation, with the role defined in the policy.

## Run-time effects

`bin/run <name>` refuses while `git status --porcelain .ralph-tui` in the main checkout lists anything, because a
worktree holds only what is committed. It creates `<worktree root>/ralph/<name>` on a new branch `ralph/<name>` from
`--base`, `HEAD` by default, or reuses the worktree or the branch when they exist. With `--worktree-setup`, it runs that
file from the root of each worktree it creates, never one it reuses, and stops the run when the file is missing, not
executable or fails, leaving the worktree for `bin/clean`; without `--worktree-setup`, it runs no setup. When
`ralph/<name>` has no committed `.ralph-tui/config.toml`, it copies the main checkout's `config.toml` and `templates/`
into the worktree on every run, because ralph-tui reads them only from there. It first checks that Git ignores them
and `session.json` in the worktree, because ralph-tui commits each task with `git add -A`; otherwise it refuses and
prints the line that ignores `.ralph-tui/` in the clone. With a copy, it passes `--prompt` with the copied prompt of
the run's tracker, unless the run gives `--prompt`, because ralph-tui copies only `config.toml` into a parallel
worker. It prints the worktree's path and branch, then runs `ralph-tui run --epic <id>`, or `--prd` with the file's
absolute path, in the worktree, with the remaining arguments, such as `--headless`, `--agent codex` or
`--model opus`. It sets and strips no environment variable, so `CLAUDE_CODE_EFFORT_LEVEL` reaches Claude workers.

An agent entry whose `command` is `.ralph-tui/bin/worker`, as `--worker-check` seeds, has ralph-tui run
`.ralph-tui/bin/worker <agent> [arguments…]` for each task, from the run worktree or a parallel worker's. The worker
refuses every task without `--worker-check`, and in the main checkout, and passes `--version` alone to every seeded
harness. It runs the agent with ralph-tui's stdin and holds the agent's stdout in `.ralph-tui/worker-output.log`; an
agent that exits non-zero ends the worker with its exit code, its output withheld. When that output holds
`<promise>COMPLETE</promise>`, it runs the check with the commit the agent started from, writing the check's output to
`.ralph-tui/worker-check.log`, never to stderr. A failed check prints one line to stderr and exits 1, so the task stays
open and ralph-tui retries it, then skips it. Otherwise it writes the held output for ralph-tui. Stopping the worker
stops the agent's or the check's processes. `bin/run` copies the worker with a copied configuration.

`bin/clean <name>` refuses while `ralph/<name>` or a parallel worker's branch has a commit whose change neither `HEAD`
nor `origin/HEAD` has. A merge, rebase or cherry-pick counts as delivered, except where it loses a change: a squash
merge, a commit changed while delivering it, and a rebase or cherry-pick that drops a merge commit whose result differs
from Git's own merge, such as a resolved conflict. It also refuses while its worktree or a worker's has changes.
`--force` removes them anyway. With `--worktree-teardown`, it then runs that file from the root of the run worktree,
when one exists, and stops when the file is missing, not executable or fails, removing nothing. It removes the worktrees
ralph-tui made for the run's parallel workers, in `.ralph-worktrees/<last part of the name>/` beside the run worktree,
such as `<worktree root>/ralph/.ralph-worktrees/<name>/`, with their branches, then the worktree and the branch
`ralph/<name>`.

## Re-cooking

Repeat the arguments on the opening line of the `ralph-tui` section in `.gitignore`, such as `#
io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worktree-setup=.worktrees/setup`. Cooking
with another `--worktree-setup` or `--worktree-teardown`, or without one, rewrites the launcher, cooking with another
`--worker-check`, or without one, rewrites the worker, and cooking without a file's `--instructions` removes its
section. Cooking with another `--worktree-root` rewrites the launcher and the `.gitignore` section, which then look only
under the new root, so clean every run before. The seeds are never rewritten or removed, so a narrower `--tracker`
leaves a prompt seeded earlier in place. A cook whose arguments the existing `.ralph-tui/config.toml` lacks is refused
and writes nothing: an agent entry for each `--harness`, the flags of each `--worker-role` or `--worker-settings`, the
worker as the `command` of each entry for `--worker-check`, and a beads `tracker` for `--tracker=beads`. Edit the file
or delete it and cook again; a Codex settings file is copied into the configuration when seeding, so editing one means
deleting `config.toml` and cooking again. A newer version's prompts are taken by deleting them and cooking again. A
newer version rewrites the launcher and the sections. Every cook rewrites `.ralph-tui/bin/`, so a team never edits it:
to use scripts of its own, it runs its own launcher instead of `bin/run` and `bin/clean`, or points an agent entry's
`command` at its own worker, such as `scripts/worker.py`, in the configuration, which is never rewritten.

## Traps

- Claude Code ignores the project's `permissions.allow` in a workspace nobody has trusted, and a fresh clone or a new
  worktree may be one: an unattended Claude worker's allows come through `--worker-role=claude:<role>` or
  `--worker-settings=claude:<path>`.
- Deny rules are prefix rules: `git -C . push` gets past `git push`. They are a guardrail; the structural limit is a
  worker without push credentials.
- A Codex `workspace-write` worker can neither read nor write the Beads database, nor write `.git`: ralph-tui commits
  and closes beads for it, and the prompts ask for neither.
- Codex runs no project hooks in a worktree it has not trusted, so a Codex worker gets no session-start context unless
  started with `--dangerously-bypass-hook-trust`.
- ralph-tui 0.12's Claude plugin accepts only `sonnet`, `opus` and `haiku` as `--model`.
- ralph-tui 0.12's parallel mode starts children that are still blocked, and refuses fewer than three tasks.
- ralph-tui 0.12's Claude plugin adds `--dangerously-skip-permissions` unless the entry has `skipPermissions = false`
  under `[agents.options]`; at the top of the entry, as `ralph-tui setup` would place it, the key is dropped silently.
- ralph-tui 0.12 completes and commits a task when the agent's output holds `<promise>COMPLETE</promise>` anywhere,
  even in a sentence saying it is not done, whatever the checks say. `--worker-check` gates it on the team's check;
  the artifact's README, under Gating completion, says how a team writes a wrapper of its own.
- With `.ralph-tui/` not committed, parallel workers find no `.ralph-tui/bin/worker`, because ralph-tui copies only
  `config.toml` into them: run serially, or commit `.ralph-tui/`.
- ralph-tui 0.12 treats stderr naming a rate limit, `429` or `overloaded` as rate limiting, and retries or switches
  agent: a worker's checks that print such words to stderr look rate-limited.
- ralph-tui 0.12 copies only `config.toml` into a parallel worker's worktree and runs no setup there.
- A `[parallel] worktreeDir` in the configuration moves the parallel workers where `bin/clean` does not look: it then
  removes the run and leaves its workers' worktrees and branches.
- ralph-tui commits with `git add -A`, so whatever a worker changes in the run worktree, `.beads/` included, rides in
  the run branch.
- ralph-tui 0.12 marks a task done before committing it, and reports a failed commit nowhere, headless or in the TUI.
  A commit refused by signing with no key or by a hook leaves the task done and its changes staged in the run
  worktree, where the next task's commit takes them in under its own message. When `ralph/<name>` has fewer commits
  than the run completed tasks, run `git status` in the run worktree and check the commit settings; `bin/clean`
  refuses while the changes remain.
- A tracked PRD run with `--parallel` leaves its completion marks uncommitted: ralph-tui 0.12 writes them after merging
  the workers' commits, and commits nothing after that.
- A run branch with a committed `.ralph-tui/config.toml` takes the configuration and prompts committed there, so a
  `--base` from before a change to them runs the earlier ones.
''')
@yield('''
Install ralph-tui and the runtimes it needs with `mise use -g node@22 bun@1.3 npm:ralph-tui@0.12.0`: npm needs node to
install it, and its launcher runs on bun. Without `-g`, mise pins them in the project's `mise.toml` instead, which is
the team's choice. Review `.ralph-tui/config.toml` and the prompts. Commit `.gitignore` and `.ralph-tui/` to share them
with the team, or keep `.ralph-tui/` to this clone by ignoring it with
`echo /.ralph-tui/ >> "$(git rev-parse --git-path info/exclude)"`, and `bin/run` copies it into each run. `bin/run`
refuses while `.ralph-tui` has changes Git neither commits nor ignores. Then run
`.ralph-tui/bin/run <name> --epic <id>`, with a Beads database holding the epic, or
`.ralph-tui/bin/run <name> --prd <file>`, and `.ralph-tui/bin/clean <name>` once the branch `ralph/<name>` is delivered.
Cooked without `--instructions`, cook again with it, such as `--instructions=AGENTS.md`, to give agents these commands,
and with the agents layout, add them to the Launching table in `.agents/README.md`.
''')
import io.github.lmarqs.cookbook.ralph.RalphTui

@param(value = ['harness'], choices = ['claude', 'codex'], many = true,
    doc = 'Harnesses ralph-tui drives, one agent entry each; the first is the default')
    String[] harness
@param(value = ['worker-role'], many = true, default = [],
    doc = '<harness>:<role>, a role of the agents layout whose rendered settings that harness\'s entry passes')
    String[] workerRole
@param(value = ['worker-settings'], many = true, default = [],
    doc = '<harness>:<path>, a settings file of the team\'s that harness\'s entry passes')
    String[] workerSettings
@param(value = ['worktree-root'], default = ['.worktrees'], path = true,
    doc = 'The directory, from the repository root, under whose ralph/ bin/run puts each run worktree')
    String worktreeRoot
@param(value = ['worktree-setup'], default = [], path = true,
    doc = 'An executable, by its path from the repository root, that bin/run runs in each run worktree it creates')
    String worktreeSetup
@param(value = ['worktree-teardown'], default = [], path = true,
    doc = 'An executable, by its path from the repository root, that bin/clean runs in a run worktree before removal')
    String worktreeTeardown
@param(value = ['worker-check'], default = [], path = true,
    doc = 'An executable, by its path from the repository root, that must pass before a worker completes a task')
    String workerCheck
@param(value = ['tracker'], choices = ['beads', 'json'], many = true, sorted = true, default = ['beads', 'json'],
    doc = 'Trackers whose prompts are seeded and whose run flag the launcher offers: beads (--epic), json (--prd)')
    String[] tracker
@param(value = ['instructions'], many = true, sorted = true, default = [], path = true,
    doc = "A Markdown file, from the repository root, where the recipe gives the launcher's commands")
    String[] instructions

@recipe(arguments = true) String cookedWith
@ingredient(RalphTui) def ralph

ralph.harnesses(harness as List<String>)
ralph.trackers(tracker as List<String>)
ralph.workerRoles(workerRole as List<String>)
ralph.workerSettings(workerSettings as List<String>)
ralph.worktreeRoot(worktreeRoot)
ralph.worktreeSetup(worktreeSetup)
ralph.worktreeTeardown(worktreeTeardown)
ralph.workerCheck(workerCheck)
ralph.instructions(instructions as List<String>)
ralph.install(cookedWith)
