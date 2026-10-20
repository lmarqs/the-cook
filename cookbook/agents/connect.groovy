@help('''
Connects each selected harness to the shared agent layout: renders the shared policy into its settings under
`.agents/adapters/<harness>/` and links its own directory, `.claude/` or `.codex/`, to those files and to the shared
directories. Each harness reads `AGENTS.md` natively, and hooks and memory are reached by their paths. The links are
committed, so a fresh clone needs no cooking; everything else in the harness directories is ignored in Git.

## Before cooking

Reads `.agents/permission/policy.yaml` when present, or seeds the starter, and seeds the shared directories that are
absent.
Cook `io.github.lmarqs.cookbook:agents:<version>#seed` first to create `AGENTS.md` too.

A file or directory already at a link's path, such as a committed `.codex/config.toml` or `.claude/skills/`, is
refused, and nothing is written: move its contents where the message says, delete it, and cook again. A link there is
replaced.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/permission/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `.agents/README.md`, `.agents/<directory>/.gitkeep` | The shared directories, seeded when absent; never owned | Always |
| `.agents/adapters/claude/settings.json` | `permissions`, with `disableBypassPermissionsMode` while the policy's `bypass` is `refuse` | `--harness=claude` |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--harness=claude` |
| `.agents/adapters/claude/roles/<role>.json` | The role's `permissions.deny`, added to a run with `claude --settings <file>` | `--harness=claude` and the policy has roles |
| `.claude/skills`, `.claude/commands` | Links to the directories of the same names in `../.agents/`, where Claude finds skills and commands | `--harness=claude` |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write` | `--harness=codex` |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands`, opening with a comment on the runs Codex cannot refuse while `bypass` is `refuse` | `--harness=codex` and the policy has command rules |
| `.codex/config.toml`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | `--harness=codex` |
| `.gitignore` | The section `harnesses`: each selected harness's directory ignored except the links above, which are committed, and `CLAUDE.md`, `CLAUDE.local.md` and `**/.claude/CLAUDE.md`, which would make Claude skip `AGENTS.md` | Always |

Cooking with fewer harnesses removes the outputs of the ones left out.

## With other recipes

`io.github.lmarqs.cookbook:agents:<version>#seed` creates the layout this recipe connects. `#claude-settings` and
`#codex-settings` write further keys into the same adapter files.
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
agents.seedLayout()
agents.permit(selected)
agents.connect(selected)
agents.ignoreLocalDirectories(selected)
