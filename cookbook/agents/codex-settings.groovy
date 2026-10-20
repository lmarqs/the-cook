@help('''
Writes chosen Codex configuration, hooks and rule files from files you supply.

## Before cooking

The optional configuration input is a TOML file and the optional hooks input is a JSON file in the destination;
each top-level key becomes one owned key. Each optional rule input is a rule file in the destination, written under
its own file name, which may hold only letters, digits, underscores, dots and hyphens. Every input is read as the
recipes before this one in the invocation left it, and a missing input is refused.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:<version>#codex-settings --config=setup/codex.toml --rule=setup/git.rules
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `.agents/adapters/codex/config.toml` | Each top-level key of the config file, owned whole | `--config` supplies keys |
| `.agents/adapters/codex/hooks.json` | Each top-level key of the hooks file, owned whole | `--hooks` supplies keys |
| `.agents/adapters/codex/rules/<name>` | One whole file per rule file, under its file name | `--rule` supplies files |
| `.codex/config.toml`, `.codex/hooks.json`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | Each one written |

Codex reads the project configuration, hooks and rules; it reads `AGENTS.md` natively.

## With other recipes

Cook `io.github.lmarqs.cookbook:agents:<version>#permissions` after this recipe with `--harness=codex` to use the shared policy for
`sandbox_mode`, `approval_policy` and `sandbox_workspace_write.network_access`; other configuration keys remain separate
targets. Use other rule-file names when that recipe owns the `commands` section in `cook.rules`.
''')
@yield('''
Review sandbox settings and command rules against Codex's permission model. Cook again with updated
inputs to change them.
''')
import io.github.lmarqs.cookbook.agents.Codex

@param(value = ['config'], default = [], doc = 'TOML file whose top-level keys are written into .agents/adapters/codex/config.toml')
String config

@param(value = ['hooks'], default = [], doc = 'JSON file whose top-level keys are written into .agents/adapters/codex/hooks.json')
String hooks

@param(value = ['rule'], default = [], many = true, doc = 'Rule file written as .agents/adapters/codex/rules/<its name>; repeat for more')
List<String> rule

@ingredient(Codex) def codex

if (config) codex.configFrom(config)
if (hooks) codex.hooksFrom(hooks)
rule.each { codex.ruleFrom(it) }
