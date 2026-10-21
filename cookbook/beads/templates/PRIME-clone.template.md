# Beads

This project tracks its work as Beads issues, with `bd`. The issues live in this clone only: Git ignores them, and no
remote syncs them. `bd prime` prints this file at session start; run it again after the context is compacted or
cleared.

## Work

- `bd ready` lists the issues nothing blocks; `bd show <id>` shows one with its dependencies.
- `bd update <id> --claim` before starting on an issue.
- `bd create --title="<summary>" --description="<why, and what to do>" --type=task|bug|feature --priority=2` records
  new work before the code for it; `--parent=<id>` puts it under an epic, and `bd dep add <issue> <depends-on>` orders
  it.
- `bd close <id> --reason="<what was done>"` once the work is done and the project's checks pass.
- Track work here, not in Markdown files or a to-do tool. Never run `bd edit`: it opens an editor and waits.

## Rules

- A worker never closes its bead or pushes. A loop runner, such as ralph-tui, runs workers unattended and closes the
  bead itself once the worker reports the task complete; delivery is the choice of the person who started the session.
- The issues have no remote: never run `bd sync`, `bd dolt push` or `bd dolt pull`.
