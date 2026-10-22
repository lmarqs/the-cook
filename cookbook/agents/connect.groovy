@help('''
Connects each selected harness to the shared agent layout: renders the shared policy into its settings under
`.agents/adapters/<harness>/`, links its own directory, `.claude/` or `.codex/`, to those files and to the shared
directories, and runs every script in `.agents/hooks/session-start.d/` when a session starts. With a `--worktree-*`
param, it also has Claude Code create its worktrees under one directory, set them up and tear them down with the team's
scripts. Each harness reads `AGENTS.md` natively and reaches hooks and memory by their paths. The links are committed,
so a fresh clone needs no cooking; Git ignores everything else in the harness directories.

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
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude \
--worktree-setup=.worktrees/setup --worktree-teardown=.worktrees/teardown
# Worktrees on branches named worktree-<name>, each starting at the session's own HEAD, fetching only on request
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --worktree-branch-prefix=worktree- \
--worktree-base=head --worktree-fetch=never
```

Any `--worktree-*` param writes the worktree hook, and needs `--harness=claude`; the others take their defaults.

| Param | Value | Default |
| --- | --- | --- |
| `--worktree-root` | The directory holding the worktrees, by its path from the repository root | `.worktrees` |
| `--worktree-setup`, `--worktree-teardown` | An executable, by its path from a worktree's root | None: no setup \
or teardown |
| `--worktree-base` | `fresh`, the remote's default branch, or `head`, the `HEAD` of the checkout the session runs in, \
the hook input's `cwd` | `fresh` |
| `--worktree-fetch` | `stale`, fetching the remote once its last fetch is a day old; `always`; or `never` | `stale` |
| `--worktree-remote` | The remote whose default branch `fresh` starts from and `remove` checks against | `origin` |
| `--worktree-branch-prefix` | What a worktree's branch adds before its name, such as `worktree-` | None |

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
  needs `sh`, `sed`, `find` and `git`. The worktree hooks are written for Claude Code 2.1.308 and tested with it.
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
| `.agents/hooks/worktree` | The worktree hook, mode 0755, with the `--worktree-*` settings | A `--worktree-*` \
param | owned |
| `.agents/adapters/claude/settings.json` | `hooks.WorktreeCreate` and `hooks.WorktreeRemove`, running \
`"$CLAUDE_PROJECT_DIR"/.agents/hooks/worktree create` and `... remove` | A `--worktree-*` param | owned |
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

Cooking with fewer harnesses removes the outputs of the ones left out, and cooking without a `--worktree-*` param
removes the worktree hook and its keys.

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
| Where a new worktree's branch starts | The person, by `--worktree-base` and `--worktree-remote` |

`io.github.lmarqs.cookbook:agents:<version>#seed` creates the layout this recipe connects: cook it first, or before
this recipe in the same invocation. Every cook names every harness the team uses. Scripts in `session-start.d/` are
read when a session starts, so a recipe that writes one can be cooked before or after this one.

## Run-time effects

`.agents/hooks/session-start <harness>` runs every `.agents/hooks/session-start.d/*.sh` with `sh`, in name order, with
the harness name as `$1`, no standard input, and standard error discarded. It joins their standard output and prints
it once as `{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"<text>"}}`, which both
harnesses add to the session. It prints nothing when the directory is missing or the scripts print nothing, and
always exits 0. Name a script with a number first, such as `20-<tool>.sh`, to place it in the order.

`.agents/hooks/worktree create` reads `name` from its input; the branch is `<branch-prefix><name>`, and its worktree
`<root>/<branch>`, under the main checkout, the first entry of `git worktree list`. When that worktree exists, the hook
prints its path and does nothing else, as Claude Code does when a name is used again; a worktree whose directory was
deleted is pruned and added again. An existing branch is checked out as it is. A new branch starts, without tracking
anything, at the remote's default branch under `fresh`, after fetching as `--worktree-fetch` says, and at the `HEAD` of
the checkout named by the input's `cwd` under `head`. With no remote, or a remote whose default branch is unknown, it
starts at the main checkout's `HEAD`, and the hook says so on standard error. With `--worktree-setup`, the hook then
runs that executable from the worktree's root, with its output on standard error. If the executable is missing, not
executable or fails, the hook removes the worktree, and the branch if it made it, and exits 1, so Claude Code creates
nothing. Otherwise it prints the worktree's path last.

`.agents/hooks/worktree remove` reads `worktree_path`. With `--worktree-teardown`, it runs that executable from the
worktree's root; if the executable is missing, not executable or fails, the hook exits 1 and keeps the worktree. It
then removes the worktree and the directories under the root it leaves empty. It deletes the branch the worktree's path
names, whichever branch the worktree had checked out, once `git cherry` finds every patch on it on the main checkout's
`HEAD` or on the remote's default branch, fetched as `--worktree-fetch` says, so a branch merged, rebased or
cherry-picked there counts; otherwise it keeps the branch and says so on standard error.

A setup or teardown committed on the main checkout's `HEAD` but missing from the worktree is skipped with a line on
standard error: the branch predates it. One missing from the main checkout's `HEAD` too still fails.

A fetch and the lookup of the remote's default branch never wait for a password, a passphrase or a host key: they
run with `GIT_TERMINAL_PROMPT=0` and, unless `GIT_SSH_COMMAND`, `GIT_SSH` or `core.sshCommand` names another, with
`ssh -o BatchMode=yes`, and a failed fetch leaves the branches as last fetched.

## Re-cooking

Repeat the arguments on the opening line of the `harnesses` section in `.gitignore`, or of the `commands` section in
`cook.rules`, such as `# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex \
--worktree-setup=.worktrees/setup`. Cook again after editing the policy, and after deleting `.claude/` or `.codex/` to
recreate the links. A newer version rewrites the keys, links, files and sections it owns; the team's keys, the scripts
in `session-start.d/` and the seeded files stay as they are. Under `--check`, a role file or `cook.rules` shows as a
change once the policy gains roles or command rules, until the recipe is cooked again.

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
- A reused worktree keeps its old tip, and its setup does not run again; Claude Code's own `fresh` reuse resets a
  clean worktree with no commits to the default branch instead.
- A fetch has no time limit: a slow remote delays creating and removing a worktree, up to the hook's 600 s timeout.
  `--worktree-fetch=never` keeps the hook off the network.
- `git cherry` matches patches, so a branch squash-merged, or whose commits changed while being delivered, counts as
  not merged and is kept.
- Once `hooks.WorktreeCreate` is set, Claude Code no longer copies the files `.worktreeinclude` lists: the
  `--worktree-setup` executable copies what a worktree needs.
- `hooks.SessionStart` is Cook's in both harnesses, and `hooks.WorktreeCreate` and `hooks.WorktreeRemove` Claude's
  with a `--worktree-*` param: a hook the team writes there is replaced on the next cook. Write a session-start hook as
  a script in `.agents/hooks/session-start.d/` instead.
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

@param(value = ['harness'], choices = ['claude', 'codex'], many = true, sorted = true,
    doc = 'Harnesses to connect to the shared layout')
    String[] harness
@param(value = ['worktree-root'], default = [], path = true,
    doc = "The directory, from the repository root, holding Claude Code's worktrees; .worktrees by default")
    String worktreeRoot
@param(value = ['worktree-setup'], default = [], path = true,
    doc = 'An executable, by its path from a worktree root, that the worktree hook runs in each worktree it creates')
    String worktreeSetup
@param(value = ['worktree-teardown'], default = [], path = true,
    doc = 'An executable, by its path from a worktree root, that the worktree hook runs before removing a worktree')
    String worktreeTeardown
@param(value = ['worktree-base'], choices = ['fresh', 'head'], default = [],
    doc = "Where a new worktree's branch starts: fresh, the remote's default branch, by default, or head")
    String worktreeBase
@param(value = ['worktree-fetch'], choices = ['stale', 'always', 'never'], default = [],
    doc = 'When the worktree hook fetches the remote: stale, a day after its last fetch, by default, always or never')
    String worktreeFetch
@param(value = ['worktree-remote'], default = [],
    doc = 'The remote whose default branch the worktree hook uses; origin by default')
    String worktreeRemote
@param(value = ['worktree-branch-prefix'], default = [],
    doc = "What a worktree's branch adds before its name, such as worktree-; nothing by default")
    String worktreeBranchPrefix

@recipe(arguments = true) String cookedWith
@ingredient(Agents) def agents
@ingredient(Claude) def claude
@ingredient(Codex) def codex

def selected = [claude, codex].findAll { it.name in harness }
agents.seedLayout()
agents.permit(selected, cookedWith)
agents.connect(selected)
agents.worktrees(selected, [root: worktreeRoot, setup: worktreeSetup, teardown: worktreeTeardown, base: worktreeBase,
  fetch: worktreeFetch, remote: worktreeRemote, 'branch-prefix': worktreeBranchPrefix])
agents.ignoreLocalDirectories(selected, cookedWith)
