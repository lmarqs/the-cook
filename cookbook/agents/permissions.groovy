@help('''
Renders the shared agent policy into each selected harness's native permissions. An absent policy is seeded
with the starter, which grants nothing.

## Before cooking

Reads `.agents/permission/policy.yaml` when present. The policy uses version 2;
other versions, unknown fields, invalid entries and roles that do more than deny are rejected.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:0.1.0#permissions --harness=claude --harness=codex
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/permission/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `.agents/adapters/claude/settings.json` | `permissions` | `--harness=claude` |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--harness=claude` |
| `.agents/adapters/claude/roles/<role>.json` | The role's `permissions.deny`, added to a run with `claude --settings <file>` | `--harness=claude` and the policy has roles |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write` | `--harness=codex` |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands` | `--harness=codex` and the policy has command rules |
| `.codex/config.toml`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | `--harness=codex` |

Cooking with fewer harnesses removes the outputs of the ones left out.

## With other recipes

`io.github.lmarqs.cookbook:agents:0.1.0#connect` renders the same policy and also links the shared directories.
''')
@yield('''
Edit the shared policy and cook again to render the permissions.
''')
import io.github.lmarqs.cookbook.agents.Agents
import io.github.lmarqs.cookbook.agents.Claude
import io.github.lmarqs.cookbook.agents.Codex

@param(value = ['harness'], choices = ['claude', 'codex'], many = true, doc = 'Harnesses that receive the shared policy')
String[] harness

@ingredient(Agents) def agents
@ingredient(Claude) def claude
@ingredient(Codex) def codex

agents.permit([claude, codex].findAll { it.name in harness })
