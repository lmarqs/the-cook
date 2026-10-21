# Beads

This project tracks its work as Beads issues, with `bd`. The team shares them: bd's configuration in `.beads/` is
committed, and the issues sync through the Dolt remote that `bd config get sync.remote` names. `bd prime` prints this
file at session start; run it again after the context is compacted or cleared.

## Work

- `bd ready` lists the issues nothing blocks; `bd show <id>` shows one with its dependencies.
- `bd update <id> --claim` before starting on an issue, so teammates see who holds it.
- `bd create --title="<summary>" --description="<why, and what to do>" --type=task|bug|feature --priority=2` records
  new work before the code for it; `--parent=<id>` puts it under an epic, and `bd dep add <issue> <depends-on>` orders
  it.
- `bd close <id> --reason="<what was done>"` once the work is done and the project's checks pass.
- Track work here, not in Markdown files or a to-do tool. Never run `bd edit`: it opens an editor and waits.
- Issue text reaches every teammate: never paste secrets, credentials or personal data into it.

## Rules

- A worker never closes its bead or pushes. A loop runner runs workers unattended and closes the bead itself once
  the worker reports the task complete; delivery is the choice of the person who started the session.
- `bd sync` pulls the team's changes to the issues, resolves conflicts and pushes; only the person who started the
  session runs it, never a worker.
