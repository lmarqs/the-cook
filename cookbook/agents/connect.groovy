@help('''
Connects each selected harness to the shared agent layout: renders the shared policy into its settings under
`.agents/adapters/<harness>/`, links its own directory, `.claude/` or `.codex/`, to those files and to the shared
directories, and runs every script in `.agents/hooks/session-start.d/` when a session starts. With a worktree root, it
also has Claude Code create its worktrees there, set them up and tear them down with the team's scripts. Each harness
reads `AGENTS.md` natively and reaches hooks and memory by their paths. The links are committed, so a fresh clone
needs no cooking; Git ignores everything else in the harness directories.

## API

```sh
# A team that uses Claude Code only
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
# A team that uses Claude Code and Codex
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
# A new repository: the layout and its connection in one invocation
./cook io.github.lmarqs.cookbook:agents:<version>#seed io.github.lmarqs.cookbook:agents:<version>#connect \
--harness=claude
# Claude Code's worktrees at .worktrees/<name>, set up and torn down by the team's scripts
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --worktree-root=.worktrees \
--worktree-setup=.worktrees/setup --worktree-teardown=.worktrees/teardown
```

`--worktree-root` names a directory by its path from the repository root, and needs `--harness=claude`.
`--worktree-setup` and `--worktree-teardown` each name an executable by its path from a worktree's root, and need
`--worktree-root`; without them, the hook runs no setup or teardown.

Paths other recipes, launchers and people rely on:

| Path | What it is |
| --- | --- |
| `.agents/adapters/<harness>/` | Each harness's settings; Cook owns only the keys it renders, and the team writes the \
rest |
| `.agents/adapters/claude/roles/<role>.json` | A role's denies, added to one Claude run with `claude --settings \
<file>` |
| `.agents/hooks/session-start.d/<name>.sh` | Session context: each script's standard output reaches every connected \
harness when a session starts, in name order, with the harness name, `claude` or `codex`, as `$1`. A tool recipe \
writes its script there when the person names a path in it by a parameter of that recipe; the team writes its own \
beside them |
| `.agents/hooks/worktree` | Run as `worktree create` or `worktree remove` with Claude Code's `WorktreeCreate` or \
`WorktreeRemove` input on standard input; `create` prints the worktree's absolute path as its last line |
| `.claude/settings.json`, `.claude/skills`, `.claude/commands` | Where Claude Code reads the layout |
| `.codex/config.toml`, `.codex/rules`, `.codex/hooks.json` | Where Codex reads the layout |

## Consumes

- Claude Code and Codex read what it writes; written for Claude Code 2.1.304 and Codex 0.160.1, with the session-start
  hooks tested on Claude Code 2.1.306.
- `.agents/permission/policy.yaml` in the draft, the team's: its `commands` render into each harness's settings, its
  `tools` and `roles` into Claude's, and its `bypass` decides whether Claude's bypass mode is refused. When absent, the
  starter is seeded, which grants nothing.
- `.agents/hooks/session-start.d/*.sh` at run time, from tool recipes and the team: the dispatcher runs each with `sh`.
  `sh`, `awk` and, for Codex, `git` must be on the path.
- The `--worktree-setup` and `--worktree-teardown` executables at run time, committed by the team; the worktree hook
  needs `sh`, `sed` and `git`. The worktree hooks are written for Claude Code 2.1.308 and tested with it.
- A file or directory already at a link's path, such as a committed `.codex/config.toml` or `.claude/skills/`, is
  refused, and nothing is written: move its contents where the message says, delete it, and cook again. A link there
  is replaced.
- By hand first: cook `io.github.lmarqs.cookbook:agents:<version>#seed` to create `AGENTS.md` and the policy, and
  edit the policy.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.agents/permission/policy.yaml` | Starter policy | Always, when absent | seed |
| `.agents/README.md`, `.agents/<directory>/.gitkeep` | The shared directories | Always, when absent | seed |
| `.agents/hooks/session-start` | The session-start dispatcher, mode 0755 | Always | owned |
| `.agents/adapters/claude/settings.json` | `permissions`, with `disableBypassPermissionsMode` while the policy's \
`bypass` is `refuse` | `--harness=claude` | owned |
| `.agents/adapters/claude/settings.json` | `hooks.SessionStart`, running \
`"$CLAUDE_PROJECT_DIR"/.agents/hooks/session-start claude` | `--harness=claude` | owned |
| `.agents/hooks/worktree` | The worktree hook, mode 0755, with the root, setup and teardown paths | \
`--worktree-root` | owned |
| `.agents/adapters/claude/settings.json` | `hooks.WorktreeCreate` and `hooks.WorktreeRemove`, running \
`"$CLAUDE_PROJECT_DIR"/.agents/hooks/worktree create` and `... remove` | `--worktree-root` | owned |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--harness=claude` | link |
| `.agents/adapters/claude/roles/<role>.json` | The role's `permissions.deny`, added to a run with `claude --settings \
<file>` | `--harness=claude` and the policy has roles | owned |
| `.claude/skills`, `.claude/commands` | Links to the directories of the same names in `../.agents/`, where Claude \
finds skills and commands | `--harness=claude` | link |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write.network_access` | \
`--harness=codex` | owned |
| `.agents/adapters/codex/hooks.json` | `hooks.SessionStart`: one entry for `startup\\|resume\\|clear` running the \
dispatcher with `codex`, found from the root that `git rev-parse --show-toplevel` prints | `--harness=codex` | owned |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands`: the argument line, a comment on the runs Codex \
cannot refuse while `bypass` is `refuse`, then one rule per command entry | `--harness=codex` and the policy has \
command rules | owned |
| `.codex/config.toml`, `.codex/hooks.json`, `.codex/rules` | Links to the files of the same names in \
`../.agents/adapters/codex/` | `--harness=codex` | link |
| `.gitignore` | The section `harnesses`: the argument line; each selected harness's directory re-included over an \
earlier rule, then its contents ignored except the links above, all committed; and `CLAUDE.md`, `CLAUDE.local.md` and \
`**/.claude/CLAUDE.md`, which would make Claude skip `AGENTS.md` | Always | owned |

Cooking with fewer harnesses removes the outputs of the ones left out, and cooking without `--worktree-root` removes
the worktree hook and its keys.

`.claude/` and `.codex/` hold only the links, and the links are committed: a clone, a worktree, a cloud session or a CI
job reads the layout with no cooking and no Cook installed. Everything else a harness writes there for one machine,
such as `.claude/settings.local.json`, stays out of Git, so a file the team shares goes under `.agents/`. The
`agents-layout` skill's `references/design.md` gives the reason and its cost.

Never touches: `AGENTS.md`; every other key in the adapter files, such as the other events under `hooks`, `env` or
`[mcp_servers]`, which the team writes in the file and cooking keeps; the scripts in `.agents/hooks/session-start.d/`;
the `--worktree-setup` and `--worktree-teardown` executables;
`.claude/settings.local.json` and anything else a harness writes in its own directory.

## Connections

| Contact | Made by |
| --- | --- |
| The layout to each harness | The person, by `--harness` |
| The policy to each harness's settings | Cook, rendering the team's policy |
| A tool's session context to every harness | The person, naming a path in `.agents/hooks/session-start.d/` by a \
parameter of the tool's recipe; the team, writing its own there |
| A role to a Claude run | The session, launching `claude --settings .agents/adapters/claude/roles/<role>.json`; the \
Launching table in `.agents/README.md` lists the commands |
| The team's other hooks to each harness | The team, under `hooks` in `.claude/settings.json` and \
`.agents/adapters/codex/hooks.json` |
| Shared skills and commands to Claude | Cook, by the `.claude/skills` and `.claude/commands` links |
| Where Claude Code's worktrees go | The person, by `--worktree-root` |
| What makes a new worktree ready to run | The person, by `--worktree-setup`, naming the team's executable |
| What stops and deletes a worktree's local state | The person, by `--worktree-teardown`, naming the team's \
executable |

`io.github.lmarqs.cookbook:agents:<version>#seed` creates the layout this recipe connects: cook it first, or before
this recipe in the same invocation. Every cook names every harness the team uses. Scripts in `session-start.d/` are
read when a session starts, so a recipe that writes one can be cooked before or after this one.

## Run-time effects

`.agents/hooks/session-start <harness>` runs every `.agents/hooks/session-start.d/*.sh` with `sh`, in name order, with
the harness name as `$1`, no standard input, and standard error discarded. It joins their standard output and prints
it once as `{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"<text>"}}`, which both
harnesses add to the session. It prints nothing when the directory is missing or the scripts print nothing, and
always exits 0. Name a script with a number first, such as `20-<tool>.sh`, to place it in the order.

`.agents/hooks/worktree create` reads `name` from its input and adds the worktree `<root>/<name>` on the branch
`<name>`. The root is under the main checkout, the first entry of `git worktree list`, and a missing branch is made from
the main checkout's `HEAD`. With `--worktree-setup`, the hook then runs that executable from the worktree's root, with
its output on standard error. If the executable is missing, not executable or fails, the hook removes the worktree,
and the branch if it made it, and exits 1, so Claude Code creates nothing. Otherwise it prints the worktree's path last.

`.agents/hooks/worktree remove` reads `worktree_path`. With `--worktree-teardown`, it runs that executable from the
worktree's root; if the executable is missing, not executable or fails, the hook exits 1 and keeps the worktree. It
then removes the worktree and deletes its branch with `git branch -d`, which keeps a branch not merged into the main
checkout's `HEAD` and says so on standard error.

## Re-cooking

Repeat the arguments on the opening line of the `harnesses` section in `.gitignore`, or of the `commands` section in
`cook.rules`, such as `# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex \
--worktree-root=.worktrees`. Cook again after editing the policy, and after deleting `.claude/` or `.codex/` to recreate
the links. A newer version rewrites the keys, links, files and sections it owns; the team's keys, the scripts in
`session-start.d/` and the seeded files stay as they are. Under `--check`, a role file or `cook.rules` shows as a change
once the policy gains roles or command rules, until the recipe is cooked again.

## Traps

- Claude Code applies the project's allow rules only in a workspace someone has trusted, and Codex loads `.codex/rules`
  only in a trusted project: start each once, interactively, in the main checkout.
- Codex trusts hooks per absolute path of `.codex/hooks.json`, recorded in `~/.codex/config.toml`, so a worktree runs
  none of them, the session start included, without `--dangerously-bypass-hook-trust`, and Codex prints no warning
  when it skips them.
- A `CLAUDE.md`, `.claude/CLAUDE.md` or `CLAUDE.local.md` in the directory of an `AGENTS.md` or one above makes Claude
  Code skip that `AGENTS.md`. Claude Code older than 2.1.277 reads no `AGENTS.md`.
- Codex has no roles: a role's denies reach only Claude runs. `codex exec --ignore-rules`, `--approve-for-me` or an
  untrusted project skip or approve the Codex rules, and no setting refuses them.
- The worktree hooks cover only worktrees Claude Code itself creates: a `git worktree add` an agent runs through Bash
  bypasses them, and Codex's own worktrees go where Codex puts them.
- Claude Code keeps the worktrees of subagents and workflows and never runs `WorktreeRemove` for them: remove them with
  `git worktree remove`, after running the teardown by hand.
- Claude Code 2.1.308's `ExitWorktree` tool refuses `remove` for a worktree the hook created, even a clean one,
  saying it could not verify the worktree's state, until it is given `discard_changes: true` (tested).
  `claude --worktree -p` keeps the worktree on exit.
- Once `hooks.WorktreeCreate` is set, Claude Code no longer copies the files `.worktreeinclude` lists: the
  `--worktree-setup` executable copies what a worktree needs.
- `hooks.SessionStart` is Cook's in both harnesses, and `hooks.WorktreeCreate` and `hooks.WorktreeRemove` Claude's
  with `--worktree-root`: a hook the team writes there is replaced on the next cook. Write a session-start hook as a
  script in `.agents/hooks/session-start.d/` instead.
- With `core.symlinks` off, as is common on Windows, Git checks the links out as text files.

The `agents-layout` skill's `references/investigate.md` lists more symptoms, with their checks and fixes.
''')
@yield('''
Review the rendered permissions against each harness's permission model. Edit `.agents/permission/policy.yaml` and
cook again to update them; after deleting `.claude/` or `.codex/`, cook again to recreate the links.
''')
import io.github.lmarqs.cookbook.agents.Agents
import io.github.lmarqs.cookbook.agents.Claude
import io.github.lmarqs.cookbook.agents.Codex

@param(value = ['harness'], choices = ['claude', 'codex'], many = true,
    doc = 'Harnesses to connect to the shared layout')
    String[] harness
@param(value = ['worktree-root'], default = [], path = true,
    doc = "The directory, from the repository root, where Claude Code's worktrees go, each on the branch of its name")
    String worktreeRoot
@param(value = ['worktree-setup'], default = [], path = true,
    doc = 'An executable, by its path from a worktree root, that the worktree hook runs in each worktree it creates')
    String worktreeSetup
@param(value = ['worktree-teardown'], default = [], path = true,
    doc = 'An executable, by its path from a worktree root, that the worktree hook runs before removing a worktree')
    String worktreeTeardown

@recipe String self
@ingredient(Agents) def agents
@ingredient(Claude) def claude
@ingredient(Codex) def codex

def selected = [claude, codex].findAll { it.name in harness }
def worktreePaths = [root: worktreeRoot, setup: worktreeSetup, teardown: worktreeTeardown].findAll { it.value != null }
def cookedWith = ([self] + selected.collect { "--harness=${it.name}" } +
worktreePaths.collect { name, path -> "--worktree-${name}=${path}" }).join(' ')
agents.seedLayout()
agents.permit(selected, cookedWith)
agents.connect(selected)
agents.worktrees(selected, worktreeRoot, worktreeSetup, worktreeTeardown)
agents.ignoreLocalDirectories(selected, cookedWith)
