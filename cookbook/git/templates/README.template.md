# Worktrees

This directory holds the repository's linked Git worktrees: extra checkouts of other branches that share this clone's
history, branches and stash. Git ignores everything here except this file, so a worktree never shows up in the main
checkout's `git status`.

## The rule

A worktree's path is its branch: branch `<branch>` is checked out at `.worktrees/<branch>`. People, agents and loop
runners all follow it, so anyone can find a branch's worktree, and a branch with a `/`, such as `ralph/<name>`, sits
in a subdirectory.

```sh
git worktree add .worktrees/<branch> -b <branch>   # a new branch, from the current commit
git worktree add .worktrees/<branch> <branch>      # an existing branch
git worktree remove .worktrees/<branch>            # refused while it has changes; --force removes them
git worktree prune                                 # forgets worktrees whose directory was deleted by hand
```

## Setup

A new worktree holds only the committed files. When a checkout needs more to run, the team writes `.worktrees/setup`,
an executable that makes the worktree it runs in ready: it installs dependencies, runs `mise trust` when the project
uses mise, and copies or creates the local files the checkout needs, such as an untracked `.env`. Everything here but
this file is ignored, so commit it with `git add -f .worktrees/setup`; each worktree then carries its own copy:

```sh
git worktree add .worktrees/<branch> -b <branch>
cd .worktrees/<branch> && .worktrees/setup
```

Loop runners that follow the rule run it too, in each worktree they create, when it is executable. Without it, a
worktree is used as checked out.

## Traps

- Branches, the stash and anything else kept in the clone are shared by every worktree: stashing or deleting a
  branch in one reaches all of them.
