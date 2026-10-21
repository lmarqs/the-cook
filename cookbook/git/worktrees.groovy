@help('''
Gives the repository one ignored root for its linked worktrees, `.worktrees/`, with a README that states where each
worktree goes and how a new one is made runnable, so people, agents and loop runners put worktrees in the same place.

## API

```sh
./cook io.github.lmarqs.cookbook:git:<version>#worktrees
```

No parameters. What it publishes for people and other recipes:

| Name | What it is |
| --- | --- |
| `.worktrees/<branch>` | The rule: branch `<branch>` is checked out there, so `ralph/<name>` is at `.worktrees/ralph/<name>` |
| `.worktrees/setup` | An optional executable the team writes and commits; a person or a runner runs it inside each new worktree to install dependencies, run `mise trust` and create local files |
| `.worktrees/README.md` | The rule, the `git worktree add`, `remove` and `prune` commands, the setup script and the traps, for people and agents to read |

## Consumes

Nothing: it reads no file in the draft and needs no tool to cook. People and runners use `git worktree` at run time.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.gitignore` | The section `worktrees`: a comment naming the recipe, then `/.worktrees/*` and `!/.worktrees/README.md` | Always | owned |
| `.worktrees/README.md` | The rule, the commands, `.worktrees/setup` and the traps | Always | seed |

Never touches: `.worktrees/setup`, the worktrees and branches themselves, the rest of `.gitignore`, `AGENTS.md`.

## Connections

| Contact | Made by |
| --- | --- |
| A runner's worktrees, such as `io.github.lmarqs.cookbook:ralph#ralph-tui` at `.worktrees/ralph/<name>` | The runner, by following the rule; cook the two in either order |
| How a new worktree is set up | The team, in `.worktrees/setup`, which this recipe never writes |
| The Worktrees line of the Quick reference in `AGENTS.md` | The team, pointing at `.worktrees/README.md` |
| Which branch a worktree takes, and how it is delivered | The session that creates it |

## Re-cooking

Cook it again with no arguments. An upgrade updates the `worktrees` section; the README is never rewritten, so delete
it and cook again to take a newer one. No part is conditional, so `--check` reports drift only when the section was
edited by hand or the README deleted.

## Traps

- Tools that walk the directory without reading `.gitignore`, such as a linter given a glob, see every worktree's copy
  of the sources: exclude `.worktrees/` in their own settings.
- `/.worktrees/*` ignores `.worktrees/setup` too: commit it with `git add -f`.
- Claude Code in a worktree reads that worktree's `AGENTS.md`, and a `CLAUDE.local.md` in the main checkout reaches it
  and turns that `AGENTS.md` off.
''')
@yield('''
Commit `.gitignore` and `.worktrees/README.md`. When a new worktree needs more than a checkout to run, write
`.worktrees/setup`, make it executable and commit it with `git add -f .worktrees/setup`. With an `AGENTS.md`, point
its Worktrees line at `.worktrees/README.md`.
''')
import io.github.lmarqs.cookbook.git.Git

@ingredient(Git) def git

git.worktrees('io.github.lmarqs.cookbook:git#worktrees')
