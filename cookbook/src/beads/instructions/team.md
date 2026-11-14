- Tracker policy: cook, recook and change policy in the main checkout; one policy applies to all linked worktrees
  of this physical clone. In a normal Git checkout, return there before setup or a policy change:

  ```sh
  main_checkout="$(dirname -- "$(git rev-parse --path-format=absolute --git-common-dir)")"
  cd "$main_checkout"
  ```

  If you cooked in a linked worktree, return here and recook with the recorded arguments from that cook's root
  `.gitignore` tracker section; preserve `--share` and every `--instructions` target. Separate team and private
  policies require separate physical clones or explicitly selected native stores. Tracker configures only its
  kitchen's .beads; it does not configure external stores. A branch-local cook does not establish independent policy.
- Beads tasks: after installing `bd` 1.3.1, run `bd bootstrap --yes`, then `bd dolt status` in the main checkout.
  Code checkout alone has no task store. Bootstrap can read published team history and persist `sync.remote`.
  Use `bd ready`, `bd create`, `bd update <id> --claim` and `bd close <id>` for ordinary task work.
  For first publication, review the intended `origin` with `git remote get-url origin`, publish the source branch,
  and review a clean index/worktree with `git status` before `bd dolt push --yes`. This consents to publishing the
  complete task history, persists `sync.remote` and creates a source commit under your Git identity. Inspect
  `git diff --check` and `git log -1 --oneline`, then `git push origin <branch>` to share that configuration.
  Later sync uses `bd dolt pull` and `bd dolt push`. Linked worktrees share the main checkout's store without
  `BEADS_DIR`. Follow `cook help io.github.lmarqs.cookbook:beads#tracker` for setup warnings and mode changes.
