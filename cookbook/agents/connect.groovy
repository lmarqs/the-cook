@help('''
Connects each selected harness to the shared agent layout: renders the shared policy into its settings under
`.agents/adapters/<harness>/`, points it at `AGENTS.md`, and links its own directory, `.claude/` or `.codex/`, to
those files and to the shared directories. The harness directories hold only links and local files and are ignored in Git, so they can be deleted
and cooked again.

## Before cooking

Reads `.agents/policy.yaml` when present, or seeds the starter, and seeds the shared directories that are absent.
Cook `io.github.lmarqs:agents:0.1.0#seed` first to create `AGENTS.md` too.

## Cook

```sh
./cook io.github.lmarqs:agents:0.1.0#connect --harness=claude
./cook io.github.lmarqs:agents:0.1.0#connect --harness=claude --harness=codex
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `.agents/README.md`, `.agents/<directory>/.gitkeep` | The shared directories, seeded when absent; never owned | Always |
| `.agents/adapters/claude/settings.json` | `permissions` | `--harness=claude` |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--harness=claude` |
| `.claude/skills`, `commands`, `hooks`, `memory` | Links to the directories of the same names in `../.agents/` | `--harness=claude` |
| `CLAUDE.md` | The section `instructions`, containing `@AGENTS.md` | `--harness=claude` |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write` | `--harness=codex` |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands` | `--harness=codex` and the policy has command rules |
| `.codex/config.toml`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | `--harness=codex` |
| `.codex/hooks` | A link to `../.agents/hooks`; Codex reads `.agents/skills` natively | `--harness=codex` |
| `.gitignore` | The section `harnesses`: `/.claude/`, `/.codex/` for the selected harnesses | Always |

Cooking with fewer harnesses removes the outputs of the ones left out. When `CLAUDE.md` already imports
`@AGENTS.md` outside the recipe's section, cooking fails and names the line; remove that line and cook again.

## With other recipes

`io.github.lmarqs:agents:0.1.0#seed` creates the layout this recipe connects. `#claude-settings` and
`#codex-settings` write further keys into the same adapter files.
''')
@yield('''
Review the rendered permissions against each harness's permission model. Edit `.agents/policy.yaml` and cook again
to update them; after deleting `.claude/` or `.codex/`, cook again to recreate the links.
''')
import io.github.lmarqs.agents.Agents
import io.github.lmarqs.agents.Claude
import io.github.lmarqs.agents.Codex

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
