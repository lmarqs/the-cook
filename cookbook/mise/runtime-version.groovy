@help('''
Pins a runtime version in mise and removes that runtime's separate version file.

## Before cooking

Choose the runtime and its version explicitly. This recipe does not read the version from `.nvmrc` or
`.python-version`; the selected file is deleted. mise must be installed to use the pin.

## Cook

```sh
./cook io.github.lmarqs.cookbook:mise:<version>#runtime-version --runtime=node --version=22.12.0
./cook io.github.lmarqs.cookbook:mise:<version>#runtime-version --runtime=python --version=3.13.2
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `mise.toml` | `tools.<runtime>`, owned whole | Always |
| `.nvmrc` | Deleted | `--runtime=node` |
| `.python-version` | Deleted | `--runtime=python` |

mise holds the selected runtime's version for the project.

## With other recipes

Other recipes can declare tasks and different tools in the same `mise.toml`. A recipe that also pins the
selected runtime shares its `tools.<runtime>` target; cook this recipe last for `--version` to own that pin.
''')
@yield('''
Run `mise install` to install the pinned runtime.
''')
import io.github.lmarqs.cookbook.mise.Mise
import io.github.lmarqs.cookbook.node.Node
import io.github.lmarqs.cookbook.python.Python

@param(value = ['runtime'], choices = ['node', 'python'],
    doc = 'The runtime whose version moves to mise')
    String runtime
@param(value = ['version'], doc = 'The version mise pins') String version

@ingredient(Mise) def mise
@ingredient(Node) def node
@ingredient(Python) def python

mise.tool(runtime, version)
[node: node, python: python][runtime].dropVersionFile()
