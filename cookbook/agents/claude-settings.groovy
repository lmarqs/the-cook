@help('''
Writes chosen Claude Code settings and instructions from files and arguments you supply.

## Before cooking

The optional settings input is a JSON file in the destination. It is read as it exists before the invocation;
each top-level key becomes one owned setting.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:0.1.0#claude-settings --settings=setup/claude.json --instructions='Run tests before committing.'
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/adapters/claude/settings.json` | Each top-level key of the settings file, owned whole | `--settings` supplies keys |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--settings` supplies keys |
| `.agents/adapters/claude/CLAUDE.md` | The section `instructions`, linked from `.claude/CLAUDE.md` | `--instructions` supplies text |

Claude Code reads the project settings and instructions.

## With other recipes

Cook `io.github.lmarqs.cookbook:agents:0.1.0#instructions` after this recipe to use shared instructions in place of the
supplied instructions. Cook `io.github.lmarqs.cookbook:agents:0.1.0#permissions` after it to use the shared policy for
`permissions`; other settings keys remain separate targets. Select `--harness=claude` for either recipe.
''')
@yield('''
Review permission settings against Claude's permission model. Cook again with updated inputs to change
them.
''')
import io.github.lmarqs.cookbook.agents.Claude

@param(value = ['settings'], default = null, doc = 'JSON file whose top-level keys are written into .agents/adapters/claude/settings.json')
File settings

@param(value = ['instructions'], default = null, doc = 'Instruction text for .claude/CLAUDE.md')
String instructions

@ingredient(Claude) def claude

if (settings) claude.settings(new JsonSlurper().parse(settings))
if (instructions) claude.instruct(instructions)
