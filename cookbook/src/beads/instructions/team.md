- Beads tasks: after installing `bd` 1.3.1, run `bd bootstrap --yes`, then `bd dolt status` in this repository.
  Code checkout alone has no task store. Bootstrap can read published team history and persist `sync.remote`.
  Use `bd ready`, `bd create`, `bd update <id> --claim` and `bd close <id>` for ordinary task work.
  For first publication, review the intended `origin` with `git remote get-url origin`, publish the source branch,
  and review a clean index/worktree with `git status` before `bd dolt push --yes`. This consents to publishing the
  complete task history, persists `sync.remote` and creates a source commit under your Git identity. Inspect
  `git diff --check` and `git log -1 --oneline`, then `git push origin <branch>` to share that configuration.
  Later sync uses `bd dolt pull` and `bd dolt push`. Linked worktrees share the main checkout's store without
  `BEADS_DIR`. Follow `cook help io.github.lmarqs.cookbook:beads#tracker` for setup warnings and mode changes.
