Beads keeps a task graph in its native store and synchronizes task history separately from code commits. This artifact
offers two independent recipes: `#tracker` sets team or clone policy, and optional `#viewer` documents exporting a
compatibility snapshot for Beads Viewer to rank. Cook, recook and change tracker policy in the main checkout;
one policy applies to all linked worktrees of that physical clone. For normal Git checkouts, locate it with
`dirname -- "$(git rev-parse --path-format=absolute --git-common-dir)"` and return there before cooking or setup.
Separate team and private policies require separate physical clones or explicitly selected native stores;
tracker configures only its kitchen's `.beads` and does not configure external stores. Neither cooking nor
discarding operates on task data. Start with
`cook help` for the recipe you choose: its help names the tested runtimes, explicit connections, owned configuration,
after-cooking commands and removal limits. Tracker works without Viewer or a loop runner; Viewer also works with an
existing native Beads project that Cook did not configure.
