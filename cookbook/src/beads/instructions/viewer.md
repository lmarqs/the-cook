Use Beads Viewer (`bv` 0.25.2) with `bd` 1.3.1 for graph inspection and recommendations. An initialized native
Beads store is required. Install and bootstrap by the recipe's After cooking instructions before using Viewer.
Exports are compatibility snapshots; the native tracker remains authoritative.

Before triage, select one store and refresh successfully. For an external store, supply an absolute native
`BEADS_DIR` in this shell. Otherwise resolve the main checkout's store through Git's common directory, including
from a linked worktree:

```sh
beads_dir="${BEADS_DIR:-$(dirname -- "$(git rev-parse --path-format=absolute --git-common-dir)")/.beads}"
bd --db "$beads_dir" export -o "$beads_dir/issues.jsonl" &&
  bv --db "$beads_dir" --robot-triage
```

Explicit Viewer `--db` overrides `BEADS_DB`, which otherwise overrides `BEADS_DIR`. A team worktree's local
`.beads/config.yaml` can shadow automatic main-store discovery; pass the same selected directory to both tools.
External exports stay in that store. This repository's ignore rules do not control the external repository's
staging or sync policy. Review its policy separately.

Keep export failures and Viewer diagnostics visible. A failed automatic refresh with an existing export can exit 0
while reporting `source_authority.state=partial`, `claim_safe=false`, `readiness=provisional`, source `stale=true`
and export warnings. Missing export plus failed refresh exits 1. Require successful export before presenting current
recommendations, inspect source authority and warnings, and retain stale output only as diagnostic information.

Fresh local bootstrap can omit native `metadata.json`: Viewer can rank the snapshot but returns
`diagnostic_top_pick` and a message instead of an actionable top-level ID. Ralph 0.12.0 `beads-bv` then falls back
to `bd ready`. Native remote bootstrap can create metadata; do not fabricate it or promise graph selection when
the response cannot support it.

Agents use robot commands such as `--robot-triage`; people can use `bv --db "$beads_dir"` for the interactive TUI
after a successful export. Selection never claims or reserves a task. Check current tracker state and use
`bd --db "$beads_dir" update <id> --claim` to claim; use `bd` for ordinary task creation, updates and closure.
Worker restrictions in ralph prompts apply to that runner's workers.
