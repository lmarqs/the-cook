@help('''
Creates the shared agent layout: `AGENTS.md` from a section template for an agent to fill in, the shared policy at
`.agents/permission/policy.yaml`, which grants nothing until edited, and the shared directories for skills, commands,
hooks and memory, which `.agents/README.md` describes, with the `agents-layout` skill that explains the layout.

## API

```sh
# A repository without AGENTS.md
./cook io.github.lmarqs.cookbook:agents:<version>#seed
# A repository with an AGENTS.md: replace it with the template, or keep it as it is
./cook --force io.github.lmarqs.cookbook:agents:<version>#seed
./cook --skip io.github.lmarqs.cookbook:agents:<version>#seed
```

Paths, scripts and commands other recipes, launchers and people rely on:

| Path or command | What it is |
| --- | --- |
| `AGENTS.md` | The instructions every harness reads |
| `.agents/permission/policy.yaml` | The shared policy, which `#connect` renders into each harness's settings |
| `.agents/skills/`, `.agents/commands/`, `.agents/hooks/`, `.agents/memory/` | The shared directories, each described \
in `.agents/README.md` |
| The Launching table in `.agents/README.md` | The launch command for each harness, mode and role |
| `sh .agents/skills/agents-layout/scripts/check.sh` | The layout check |
| `/agents-layout` in Claude Code, `$agents-layout` in Codex | The skill, taking `adopt`, `review` or `review --fix`; \
given nothing, it adopts until `cook.lock` records `#connect` |

## Consumes

- Claude Code and Codex read the skill and the instructions; written for Claude Code 2.1.304 and Codex 0.160.1.
- Whether `AGENTS.md`, the policy, `.agents/README.md` and each `.gitkeep` exist in the draft: each absent one is
  seeded. The first time the recipe is cooked, an existing `AGENTS.md` refuses the cook unless Cook runs with
  `--force` or `--skip`.
- By hand first: nothing.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `AGENTS.md` | Section template, written the first time the recipe is cooked | Always | seed |
| `.agents/permission/policy.yaml` | Starter policy | Always, when absent | seed |
| `.agents/README.md` | What each shared path holds | Always, when absent | seed |
| `.agents/skills/`, `commands/`, `hooks/`, `memory/` | An empty `.gitkeep` each | Always, when absent | seed |
| `.agents/skills/agents-layout/` | Whole files of the skill that explains the layout, with the templates of the \
seeded files under `templates/` and the layout check `scripts/check.sh` | Always | owned |

Never touches: `.claude/`, `.codex/`, `.agents/adapters/`, `.gitignore`, and a seeded file once it exists.

## Connections

| Contact | Made by |
| --- | --- |
| The layout to each harness | The person, cooking `io.github.lmarqs.cookbook:agents:<version>#connect` with the \
harnesses to use |
| What the instructions and the policy say | The team, in the seeded `AGENTS.md` and `.agents/permission/policy.yaml` |
| The skill to Claude Code | Cook, once `#connect --harness=claude` links `.claude/skills`; Codex reads \
`.agents/skills/` natively |

Cook this recipe before `#connect`, or before it in the same invocation.

## Run-time effects

`scripts/check.sh` checks the layout of the repository it runs in and writes nothing. It prints one line per check,
`ok`, `WARN` or `FAIL`, each gap with the step that closes it, and exits 1 when a harness cannot read the setup.

## Re-cooking

The recipe takes no arguments. A newer version rewrites `.agents/skills/agents-layout/` whole, including the
templates; the seeded files keep the team's edits, so merge the template changes that fit by hand, as
`git diff -- .agents/skills/agents-layout/templates/` shows them.
''')
@yield('''
Have an agent fill in `AGENTS.md`, edit `.agents/permission/policy.yaml`, then cook
`io.github.lmarqs.cookbook:agents:<version>#connect`, all in one commit: `$agents-layout adopt` in Codex does it, and
`/agents-layout adopt` in Claude Code once `#connect --harness=claude` has linked `.claude/skills`. Until that link
exists, ask Claude Code to follow `.agents/skills/agents-layout/SKILL.md` with the argument `adopt`.
''')
import io.github.lmarqs.cookbook.agents.Agents

@ingredient(Agents) def agents

agents.seed()
agents.skills('agents-layout')
