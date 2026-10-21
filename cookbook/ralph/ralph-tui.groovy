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
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --role=worker
# Claude workers with a settings file the team writes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --settings=ralph-worker.json
```

The first `--harness` is the default agent. `--role` and `--settings` reach only the Claude entry, and only one of them
is given. What it publishes for people, sessions and other recipes:

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
- `.agents/adapters/claude/roles/<name>.json` in the draft, with `--role=<name>`: written by
  `io.github.lmarqs.cookbook:agents#connect --harness=claude` from `roles.<name>` in `.agents/permission/policy.yaml`.
  Without it, cooking is refused and names both.
- The `--settings` path as given: written into the configuration, never read or written at cook time. Workers
  resolve it in the run worktree, so it names a committed file by its path from the repository root, outside
  `.ralph-tui/`, where Git ignores all but the configuration, prompts and launcher.
- `.worktrees/setup`, at run time, when it is executable in a new run worktree.
- By hand first: `bun install -g ralph-tui@0.12.0`, and for `--epic`, a Beads database with the epic.

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
any command the settings do not allow is refused rather than asked, with `skipPermissions = false`. `--role` or
`--settings` adds `--settings <file>` to it. The Codex entry runs `codex -c approval_policy=never -c
sandbox_workspace_write.network_access=false exec --sandbox workspace-write`: no approvals, no network, writes only in
the worktree. Neither sets a model or an effort.

Never touches: `AGENTS.md`, `.agents/permission/policy.yaml`, any settings or role file, version pins, remotes, pull
requests, and the tracker's state.

## Connections

| Contact | Made by |
| --- | --- |
| What a Claude worker may do | The person, by `--role` or `--settings`; the team, \
by adding entries to the configuration; the session, by `ralph-tui run --agent <entry>` |
| What a Codex worker may do | Codex's `workspace-write` sandbox, in the seeded entry |
| The tracker a run takes tasks from | The session: `--epic <id>` runs the Beads epic, `--prd <file>` the JSON PRD |
| The run worktree's setup | The team, in `.worktrees/setup`, which `bin/run` runs in each worktree it creates |
| The branch a run starts from | The session, by `--base`, `HEAD` by default |
| Model and effort | The session, by `ralph-tui run --model` and `CLAUDE_CODE_EFFORT_LEVEL`; the team, per entry |
| A line in `AGENTS.md` and the agents layout's Launching table | The team, from After cooking |
| Delivering the run branch | The session: the launcher never pushes, merges or opens a pull request |

`io.github.lmarqs.cookbook:git#worktrees` ignores and documents `.worktrees/`; cook the two in either order.
`io.github.lmarqs.cookbook:beads#tracker` gives the Beads side; cook the two in either order. With `--role`, cook
`io.github.lmarqs.cookbook:agents#connect --harness=claude` first, or before this recipe in the same invocation, with
the role defined in the policy.

## Run-time effects

`bin/run <name>` refuses while `git status --porcelain .ralph-tui` in the main checkout lists anything, because a
worktree holds only what is committed. It creates `.worktrees/ralph/<name>` on a new branch `ralph/<name>` from
`--base`, `HEAD` by default, or reuses the worktree or the branch when they exist. In a worktree it creates, it runs
`.worktrees/setup` when that is executable. It prints the worktree's path and branch, then runs
`ralph-tui run --epic <id>`, or `--prd` with the file's absolute path, in the worktree, with the remaining arguments,
such as `--headless`, `--agent codex` or `--model opus`. It sets and strips no environment variable, so
`CLAUDE_CODE_EFFORT_LEVEL` reaches Claude workers.

`bin/clean <name>` refuses while `ralph/<name>` has commits `HEAD` does not, which includes a branch delivered by a
squash merge, or while its worktree has changes; `--force` removes them anyway. It removes the worktree, every worktree
on a `ralph-parallel/*` branch with that branch, then the branch `ralph/<name>`.

## Re-cooking

Repeat the arguments on the opening line of the `ralph-tui` section in `.gitignore`, such as
`# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex`. The seeds are never rewritten: to take
other harnesses, a role or a newer version's configuration or prompts, edit them or delete them and cook again. A newer
version rewrites the launcher and the section.

## Traps

- Claude Code ignores the project's `permissions.allow` in a workspace nobody has trusted, and a fresh clone or a new
  worktree may be one: an unattended Claude worker's allows come through `--role` or `--settings`.
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
Install ralph-tui with `bun install -g ralph-tui@0.12.0`, review `.ralph-tui/config.toml` and the prompts, and commit
`.gitignore` and `.ralph-tui/`: `bin/run` refuses while `.ralph-tui` has uncommitted changes. Then run
`.ralph-tui/bin/run <name> --epic <id>` or `.ralph-tui/bin/run <name> --prd <file>`, and `.ralph-tui/bin/clean <name>`
once the branch `ralph/<name>` is delivered. With an `AGENTS.md`, add these commands to it, and with the agents layout,
to the Launching table in `.agents/README.md`.
''')
import io.github.lmarqs.cookbook.ralph.RalphTui

@param(value = ['harness'], choices = ['claude', 'codex'], many = true,
    doc = 'Harnesses ralph-tui drives, one agent entry each; the first is the default')
    String[] harness
@param(value = ['role'], default = [],
    doc = 'A role of the agents layout whose rendered Claude settings the claude entry passes with --settings')
    String role
@param(value = ['settings'], default = [], doc = 'A Claude settings file the claude entry passes with --settings')
    String settings

@ingredient(RalphTui) def ralph

List<String> harnesses = (harness as List<String>).unique(false)
ralph.harnesses(harnesses)

if (settings != null) {
  ralph.settings(settings)
}

if (role != null) {
  ralph.role(role)
}

def arguments = harnesses.collect { "--harness=${it}" } +
(role != null ? ["--role=${role}"] : []) + (settings != null ? ["--settings=${settings}"] : [])
ralph.install((['io.github.lmarqs.cookbook:ralph#ralph-tui'] + arguments).join(' '))
