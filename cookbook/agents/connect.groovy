@help('''
Connects each selected harness to the shared agent layout: renders the shared policy into its settings under
`.agents/adapters/<harness>/` and links its own directory, `.claude/` or `.codex/`, to those files and to the shared
directories. Each harness reads `AGENTS.md` natively, and hooks and memory are reached by their paths. The links are
committed, so a fresh clone needs no cooking; everything else in the harness directories is ignored in Git.

## API

```sh
# A team that uses Claude Code only
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
# A team that uses Claude Code and Codex
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
# A new repository: the layout and its connection in one invocation
./cook io.github.lmarqs.cookbook:agents:<version>#seed io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
```

Paths other recipes, launchers and people rely on:

| Path | What it is |
| --- | --- |
| `.agents/adapters/<harness>/` | Each harness's settings; Cook owns only the keys it renders, and the team writes the rest |
| `.agents/adapters/claude/roles/<role>.json` | A role's denies, added to one Claude run with `claude --settings <file>` |
| `.claude/settings.json`, `.claude/skills`, `.claude/commands` | Where Claude Code reads the layout |
| `.codex/config.toml`, `.codex/rules`, `.codex/hooks.json` | Where Codex reads the layout |

## Consumes

- Claude Code and Codex read what it writes; written for Claude Code 2.1.304 and Codex 0.160.1.
- `.agents/permission/policy.yaml` in the draft, the team's: its `commands` render into each harness's settings, its
  `tools` and `roles` into Claude's, and its `bypass` decides whether Claude's bypass mode is refused. When absent, the
  starter is seeded, which grants nothing.
- `.agents/adapters/codex/hooks.json` in the draft, the team's: when present, `.codex/hooks.json` links to it.
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
| `.agents/adapters/claude/settings.json` | `permissions`, with `disableBypassPermissionsMode` while the policy's `bypass` is `refuse` | `--harness=claude` | owned |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--harness=claude` | link |
| `.agents/adapters/claude/roles/<role>.json` | The role's `permissions.deny`, added to a run with `claude --settings <file>` | `--harness=claude` and the policy has roles | owned |
| `.claude/skills`, `.claude/commands` | Links to the directories of the same names in `../.agents/`, where Claude finds skills and commands | `--harness=claude` | link |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write.network_access` | `--harness=codex` | owned |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands`: the argument line, a comment on the runs Codex cannot refuse while `bypass` is `refuse`, then one rule per command entry | `--harness=codex` and the policy has command rules | owned |
| `.codex/config.toml`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | `--harness=codex` | link |
| `.codex/hooks.json` | A link to `../.agents/adapters/codex/hooks.json` | `--harness=codex` and that file exists | link |
| `.gitignore` | The section `harnesses`: the argument line; each selected harness's directory re-included over an earlier rule, then its contents ignored except the links above, all committed; and `CLAUDE.md`, `CLAUDE.local.md` and `**/.claude/CLAUDE.md`, which would make Claude skip `AGENTS.md` | Always | owned |

Cooking with fewer harnesses removes the outputs of the ones left out.

Never touches: `AGENTS.md`; every other key in the adapter files, such as `hooks`, `env` or `[mcp_servers]`, which the
team writes in the file and cooking keeps; `.agents/adapters/codex/hooks.json`; `.claude/settings.local.json` and
anything else a harness writes in its own directory.

## Connections

| Contact | Made by |
| --- | --- |
| The layout to each harness | The person, by `--harness` |
| The policy to each harness's settings | Cook, rendering the team's policy |
| A role to a Claude run | The session, launching `claude --settings .agents/adapters/claude/roles/<role>.json`; the Launching table in `.agents/README.md` lists the commands |
| The team's Codex hooks to Codex | The team, writing `.agents/adapters/codex/hooks.json`; Cook links it |
| Shared skills and commands to Claude | Cook, by the `.claude/skills` and `.claude/commands` links |

`io.github.lmarqs.cookbook:agents:<version>#seed` creates the layout this recipe connects: cook it first, or before
this recipe in the same invocation. Every cook names every harness the team uses.

## Re-cooking

Repeat the arguments on the opening line of the `harnesses` section in `.gitignore`, or of the `commands` section in
`cook.rules`, such as `# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex`. Cook again after
editing the policy, and after deleting `.claude/` or `.codex/` to recreate the links. A newer version rewrites the
keys, links and sections it owns; the team's keys and the seeded files stay as they are. Under `--check`, a role file, `cook.rules` or the
`.codex/hooks.json` link shows as a change once the policy gains roles or command rules, or the team writes
`.agents/adapters/codex/hooks.json`, until the recipe is cooked again.

## Traps

- Claude Code applies the project's allow rules only in a workspace someone has trusted, and Codex loads `.codex/rules`
  only in a trusted project: start each once, interactively, in the main checkout.
- A `CLAUDE.md`, `.claude/CLAUDE.md` or `CLAUDE.local.md` in the directory of an `AGENTS.md` or one above makes Claude
  Code skip that `AGENTS.md`. Claude Code older than 2.1.277 reads no `AGENTS.md`.
- Codex has no roles: a role's denies reach only Claude runs. `codex exec --ignore-rules`, `--approve-for-me` or an
  untrusted project skip or approve the Codex rules, and no setting refuses them.
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

@param(value = ['harness'], choices = ['claude', 'codex'], many = true, doc = 'Harnesses to connect to the shared layout')
String[] harness

@ingredient(Agents) def agents
@ingredient(Claude) def claude
@ingredient(Codex) def codex

def selected = [claude, codex].findAll { it.name in harness }
def cookedWith = (['io.github.lmarqs.cookbook:agents#connect'] + selected.collect { "--harness=${it.name}" }).join(' ')
agents.seedLayout()
agents.permit(selected, cookedWith)
agents.connect(selected)
agents.ignoreLocalDirectories(selected, cookedWith)
