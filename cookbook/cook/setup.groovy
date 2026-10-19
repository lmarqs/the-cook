@help('''
Sets up this repository's shared policy for Claude and Codex, three skills, working instructions and an
ignore pattern for Claude's local settings file.

## Before cooking

Reads `.agents/permission/policy.yaml` when present, or uses the starter policy when absent.
The policy supplies command permissions for both harnesses.

## Cook

```sh
./cook io.github.lmarqs.cookbook:cook:0.1.0#setup
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/permission/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `AGENTS.md` | Title, seeded when absent; never owned | Always |
| `AGENTS.md` | The section `instructions`: working guidance | Always |
| `.agents/skills/<name>/` | Whole files for `solid`, `hexagonal-layout`, `effective-java-core` | Always |
| `.agents/adapters/claude/settings.json` | `permissions`, linked from `.claude/settings.json` | Always |
| `.claude/skills` | A relative link to `../.agents/skills` | Always |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write`, linked from `.codex/config.toml` | Always |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands`, linked through `.codex/rules` | The policy has command rules |
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

@ingredient(Agents) def agents
@ingredient(Claude) def claude
@ingredient(Codex) def codex
@ingredient(Git) def git

def harnesses = [claude, codex]

agents.skills('solid', 'hexagonal-layout', 'effective-java-core')
agents.instruct(
  '# Working on the-cook',
  '',
  'Use .agents/skills/solid for implementation and review.',
  'Use .agents/skills/hexagonal-layout for application boundaries and adapters.',
  'Use .agents/skills/effective-java-core for Java and Groovy API design.',
  '',
  'Core, infra, the CLI, and cookbook ingredients and recipes are Groovy.',
  'Build and verify changes with ./gradlew check cliBuild.')
agents.permit(harnesses)
agents.connect(harnesses)

git.ignore(claude.localFiles)
