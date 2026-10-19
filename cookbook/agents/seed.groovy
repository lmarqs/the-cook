@help('''
Creates the shared agent layout: `AGENTS.md` from a section template for an agent to fill in, the shared policy at
`.agents/policy.yaml`, which grants nothing until edited, and the shared directories for skills, commands, hooks and
memory, which `.agents/README.md` describes.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:0.1.0#seed
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `AGENTS.md` | Section template, written the first time the recipe is cooked; never owned | Always |
| `.agents/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `.agents/README.md` | What each shared path holds, seeded when absent; never owned | Always |
| `.agents/skills/`, `commands/`, `hooks/`, `memory/` | An empty `.gitkeep` each, seeded when absent; never owned | Always |

The first time the recipe is cooked, it fails when `AGENTS.md` already exists. Run with `--force` to replace it with
the template, or with `--skip` to keep it as it is.

## With other recipes

Cook `io.github.lmarqs.cookbook:agents:0.1.0#connect` with the harnesses to use, to have them read this layout.
''')
@yield('''
Have an agent fill in `AGENTS.md`, edit `.agents/policy.yaml`, then cook `io.github.lmarqs.cookbook:agents:0.1.0#connect`.
''')
import io.github.lmarqs.cookbook.agents.Agents

@ingredient(Agents) def agents

agents.seed()
