@help('''
Gives the repository one ignored root for its linked worktrees, `.worktrees/`, with a README that states where each
worktree goes, and a setup and a teardown script the team fills in, so people, agents and loop runners put worktrees in
the same place and prepare and clean them the same way.

## API

```sh
./cook io.github.lmarqs.cookbook:git:<version>#worktrees
# Also stating the rule where agents and people read
./cook io.github.lmarqs.cookbook:git:<version>#worktrees --instructions=AGENTS.md --instructions=README.md
```

`--instructions` names a Markdown file by its path from the repository root, and repeats for each file. The file must
exist, written by the team or by a recipe cooked earlier in the same invocation. What it publishes for people and
other recipes:

| Name | What it is |
| --- | --- |
| `.worktrees/<branch>` | The rule: branch `<branch>` is checked out there, \
so `fix/login` is at `.worktrees/fix/login` |
| `.worktrees/setup` | An executable, seeded doing nothing, that the team fills in and commits; \
a person or a runner runs it inside each new worktree to install dependencies and tools and create local files |
| `.worktrees/teardown` | An executable, seeded doing nothing, that the team fills in and commits; \
a person or a runner runs it inside a worktree before removing it, to stop its services and delete its local state |
| `.worktrees/README.md` | The rule, the `git worktree add`, `remove` and `prune` commands, \
the setup and teardown scripts and the traps, for people and agents to read |

## Consumes

Only the files `--instructions` names, which must exist; it needs no tool to cook. People and runners use
`git worktree` at run time.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.gitignore` | The section `worktrees`: a comment naming the recipe, \
then `/.worktrees/*` and re-includes of `.worktrees/README.md`, `setup` and `teardown` | Always | owned |
| `.worktrees/README.md` | The rule, the commands, `.worktrees/setup`, `.worktrees/teardown` and the traps | Always | \
seed |
| `.worktrees/setup`, `.worktrees/teardown` | Mode 0755: `sh` scripts that do nothing yet, \
with comments naming what a team adds | Always | seed |
| Each `--instructions` file | The section `worktrees`: the argument line as an HTML comment, then a list item \
stating the rule and pointing at `.worktrees/README.md` | `--instructions` | owned |

Never touches: the worktrees and branches themselves, the rest of `.gitignore` and of each `--instructions` file.
The seeds keep the team's edits.

## Connections

| Contact | Made by |
| --- | --- |
| A loop runner's worktrees | The runner, by following the rule |
| How a new worktree is set up | The team, in the seeded `.worktrees/setup` |
| How a worktree is torn down before removal | The team, in the seeded `.worktrees/teardown` |
| The rule in the files agents and people read | The person, by `--instructions`; the section is appended at the \
file's end, and stays where the team moves it |
| Which branch a worktree takes, and how it is delivered | The session that creates it |

## Re-cooking

Repeat the arguments on the opening line of the `worktrees` section in `.gitignore`, such as
`# io.github.lmarqs.cookbook:git#worktrees --instructions=AGENTS.md`. An upgrade updates the `worktrees` sections; the
README and the scripts are never rewritten, so delete one and cook again to take a newer one. Cooking without a file's
`--instructions` removes its section. What it writes follows from its arguments alone, so `--check` reports drift only
when a section was edited by hand or a seed deleted.

## Traps

- Tools that walk the directory without reading `.gitignore`, such as a linter given a glob, see every worktree's copy
  of the sources: exclude `.worktrees/` in their own settings.
''')
@yield('''
Fill in `.worktrees/setup` with what a new worktree needs to run, and `.worktrees/teardown` with what removing one
needs beyond `git worktree remove`, then commit `.gitignore` and `.worktrees/`. Cooked without `--instructions`, cook
again with it, such as `--instructions=AGENTS.md`, to state the rule where agents and people read.
''')
import io.github.lmarqs.cookbook.git.Git

@param(value = ['instructions'], many = true, default = [], path = true,
    doc = 'A Markdown file, from the repository root, where the recipe states the worktree rule')
    String[] instructions

@recipe String self
@ingredient(Git) def git

List<String> files = (instructions as List<String>).unique(false).sort(false)
git.instructions(files)
git.worktrees(([self] + files.collect { "--instructions=${it}" }).join(' '))
