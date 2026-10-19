@help('''
Writes chosen Codex configuration, hooks and rule files from files you supply.

## Before cooking

The optional configuration input is a TOML file and the optional hooks input is a JSON file in the destination.
Both are read as they exist before the invocation; each top-level key becomes one owned key. The optional
rules input is a directory of rule files. Each file name may hold only letters, digits, underscores, dots
and hyphens, and may not be `.` or `..`.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:0.1.0#codex-settings --config=setup/codex.toml --rules=setup/codex-rules
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/adapters/codex/config.toml` | Each top-level key of the config file, owned whole | `--config` supplies keys |
| `.agents/adapters/codex/hooks.json` | Each top-level key of the hooks file, owned whole | `--hooks` supplies keys |
| `.agents/adapters/codex/rules/<name>` | One whole file per file in the rules directory | `--rules` supplies files |
| `.codex/config.toml`, `.codex/hooks.json`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | Each one written |

Codex reads the project configuration, hooks and rules; it reads `AGENTS.md` natively.

## With other recipes

Cook `io.github.lmarqs.cookbook:agents:0.1.0#permissions` after this recipe with `--harness=codex` to use the shared policy for
`sandbox_mode`, `approval_policy` and `sandbox_workspace_write`; other configuration keys remain separate
targets. Use other rule-file names when that recipe owns the `commands` section in `cook.rules`.
''')
@yield('''
Review sandbox settings and command rules against Codex's permission model. Cook again with updated
inputs to change them.
''')
import io.github.lmarqs.cookbook.agents.Codex

@param(value = ['config'], default = null, doc = 'TOML file whose top-level keys are written into .agents/adapters/codex/config.toml')
File config

@param(value = ['hooks'], default = null, doc = 'JSON file whose top-level keys are written into .agents/adapters/codex/hooks.json')
File hooks

@param(value = ['rules'], default = null, doc = 'Directory whose files become .agents/adapters/codex/rules/<name>')
File rules

@ingredient(Codex) def codex

if (config) codex.config(new TomlSlurper().parse(config))
if (hooks) codex.hooks(new JsonSlurper().parse(hooks))
if (rules) rules.eachFile { codex.rule(it.name, it.text) }
