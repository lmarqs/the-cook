- Personal Beads tasks: after installing `bd` 1.3.1, run `bd bootstrap --yes`, then `bd dolt status` here.
  Before any personal writes, require the effective `Remote sync: disabled` indication. Resolve overrides in
  `BD_DOLT_LOCAL_ONLY` or `.beads/config.local.yaml` if it is absent; ignoring files alone proves no sync policy.
  Bootstrap can read team history, adopt its prefix and persist `sync.remote` even with local-only enabled.
  Push/pull skip with exit 0 while remote addition is refused. Use `bd ready`, `bd create`, `bd update <id> --claim`
  and `bd close <id>` for ordinary task work. Linked worktrees share the main checkout's store without `BEADS_DIR`.
  Discard removes the owned local-only gate while preserving native remotes, so synchronization can resume.
  Before switching modes, review effective configuration and already tracked files. Neither switching nor discard
  deletes tasks or rolls back published history. See `cook help io.github.lmarqs.cookbook:beads#tracker` for setup.
