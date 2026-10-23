@help('''
Sets up this repository's shared policy for Claude and Codex, the `agents-layout` skill, working instructions and an
ignore pattern for Claude's local settings file.

## Before cooking

Reads `.agents/permission/policy.yaml` when present, or uses the starter policy when absent.
The policy supplies command permissions for both harnesses.

## Cook

```sh
./cook io.github.lmarqs.cookbook:cook:<version>#setup
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/permission/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `AGENTS.md` | Title, seeded when absent; never owned | Always |
| `AGENTS.md` | The section `instructions`: working guidance | Always |
| `.agents/skills/agents-layout/` | Whole files of the skill that explains the shared layout | Always |
| `.agents/adapters/claude/settings.json` | `permissions`, linked from `.claude/settings.json` | Always |
| `.claude/skills` | A relative link to `../.agents/skills` | Always |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write.network_access`, \
linked from `.codex/config.toml` | Always |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands`, linked through `.codex/rules` | \
The policy has command rules |
| `.gitignore` | The section `ignore`: `/.claude/settings.local.json` | Always |

Claude reads `AGENTS.md` natively and skills through `.claude/skills`.
Codex reads `AGENTS.md` and `.agents/skills/` natively.
''')
@yield('''
Review the rendered permissions against each harness's permission model; edit the shared policy and
cook again to update them.
''')
import io.github.lmarqs.cookbook.agents.Agents
import io.github.lmarqs.cookbook.agents.Claude
import io.github.lmarqs.cookbook.agents.Codex
import io.github.lmarqs.cookbook.git.Git

@recipe String self
@ingredient(Agents) def agents
@ingredient(Claude) def claude
@ingredient(Codex) def codex
@ingredient(Git) def git

def harnesses = [claude, codex]

agents.skills('agents-layout')
agents.instruct(
    '# Working on the-cook',
    '',
    'Core, infra, the CLI, and cookbook ingredients and recipes are Groovy.',
    'Build and verify changes with ./gradlew check cliBuild.')
agents.permit(harnesses, self)
agents.connect(harnesses)

git.ignoredBy(self)
git.ignore(claude.localFiles)
