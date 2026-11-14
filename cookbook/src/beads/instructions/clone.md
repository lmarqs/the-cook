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
- Personal Beads tasks: after installing `bd` 1.3.1, run `bd bootstrap --yes`, then `bd dolt status` in the main
  checkout. Before any personal writes, require the effective `Remote sync: disabled` indication. Resolve overrides in
  `BD_DOLT_LOCAL_ONLY` or `.beads/config.local.yaml` if it is absent; if cooking happened in a linked worktree,
  return to main and recook with the recorded arguments, then repeat the status check. Ignoring files alone proves
  no sync policy.
  Bootstrap can read team history, adopt its prefix and persist `sync.remote` even with local-only enabled.
  Push/pull skip with exit 0 while remote addition is refused. Use `bd ready`, `bd create`, `bd update <id> --claim`
  and `bd close <id>` for ordinary task work. Linked worktrees share the main checkout's store without `BEADS_DIR`.
  Discard removes the owned local-only gate while preserving native remotes, so synchronization can resume.
  Before switching modes, review effective configuration and already tracked files. Neither switching nor discard
  deletes tasks or rolls back published history. See `cook help io.github.lmarqs.cookbook:beads#tracker` for setup.
