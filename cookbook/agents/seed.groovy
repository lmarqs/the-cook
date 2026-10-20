@help('''
Creates the shared agent layout: `AGENTS.md` from a section template for an agent to fill in, the shared policy at
`.agents/permission/policy.yaml`, which grants nothing until edited, and the shared directories for skills, commands,
hooks and memory, which `.agents/README.md` describes, with the `agents-layout` skill that explains the layout.

## Cook

```sh
./cook io.github.lmarqs.cookbook:agents:<version>#seed
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `AGENTS.md` | Section template, written the first time the recipe is cooked; never owned | Always |
| `.agents/permission/policy.yaml` | Starter policy, seeded when absent; never owned | Always |
| `.agents/README.md` | What each shared path holds, seeded when absent; never owned | Always |
| `.agents/skills/`, `commands/`, `hooks/`, `memory/` | An empty `.gitkeep` each, seeded when absent; never owned | Always |
| `.agents/skills/agents-layout/` | Whole files of the skill that explains the layout, with the templates of the seeded files under `templates/`, updated each time the recipe is cooked | Always |

The first time the recipe is cooked, it fails when `AGENTS.md` already exists. Run with `--force` to replace it with
the template, or with `--skip` to keep it as it is.

## With other recipes

Cook `io.github.lmarqs.cookbook:agents:<version>#connect` with the harnesses to use, to have them read this layout.
''')
@yield('''
Have an agent fill in `AGENTS.md`, edit `.agents/permission/policy.yaml`, then cook
`io.github.lmarqs.cookbook:agents:<version>#connect`.
''')
import io.github.lmarqs.cookbook.agents.Agents

@ingredient(Agents) def agents

agents.seed()
agents.skills('agents-layout')
