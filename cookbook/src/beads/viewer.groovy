@help('''
Sets up an independent Beads Viewer export ignore and workflow guidance in the instruction files you name.
Cooking needs no Beads or Viewer binary and creates no database, metadata or export.

## API

```sh
# Add Viewer to an existing native Beads project, with or without Cook tracker setup
cook io.github.lmarqs.cookbook:beads:<version>#viewer
# Publish guidance where people and agents read
cook io.github.lmarqs.cookbook:beads:<version>#viewer --instructions=AGENTS.md --instructions=README.md
# Replay the recorded Viewer recipe in another checkout
cook replicate io.github.lmarqs.cookbook:beads#viewer
```

`--instructions` repeats, naming existing Markdown files from the repository root; the default is no targets.
No tracker recipe, agent layout, native settings, workspace file or persistent environment setting is detected
or installed. Runtime store selection uses native BEADS_DIR and explicit --db, separately from recipe arguments.

## Consumes

- At runtime: Git, `bd` 1.3.1, `bv` 0.25.2 and an initialized native Beads store. After cooking gives installation
  and bootstrap steps. Embedded Dolt needs no separate Dolt CLI or server for this workflow.
- Only existing instruction files explicitly named by `--instructions`; missing paths are refused. Create them
  first, by hand or by cooking their recipe earlier in the invocation.
- At runtime, an absolute native `BEADS_DIR` selects an external store; otherwise Git's common directory locates
  the main checkout's `.beads`, including from linked worktrees. Export and Viewer use the same selected directory.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| Root `.gitignore` | `viewer` section with argument line and `/.beads/issues.jsonl` | Always | owned |
| Each `--instructions` file | `viewer` section with argument line and runtime guidance | `--instructions` | owned |

Cook alone maintains `cook.lock`. The native exporter creates the compatibility snapshot at runtime.
Never touches: tracker settings or sections, native metadata, tasks, runtime exports, team seeds, tool pins,
hooks, `.git/info/exclude`, environment files, agent/runner settings or unrelated ignores and instructions.

## Connections

| Contact | Made by |
| --- | --- |
| Guidance in team or agent instruction files | The person, through repeatable `--instructions` paths |
| Native local or external task store | The person, through native BEADS_DIR and explicit --db at runtime |
| Tracker policy or loop runner | The team, through separately selected native setup or recipes |

No Cook tracker setup is required. Tracker and Viewer sections have separate ownership and can be discarded
independently. This repository's ignores do not control an external repository's staging or sync policy.

## Re-cooking

Repeat the arguments on the opening line of the Viewer ignore section, for example
`# io.github.lmarqs.cookbook:beads#viewer --instructions=AGENTS.md`. New versions reconcile only owned Viewer
sections. Dropping an instruction path withdraws its Viewer section. `cook check` compares owned output without
accessing native stores. Discard removes the Viewer sections and keeps tracker configuration, tasks, native
metadata, runtime exports, team-owned seeds and unrelated text. It restores no previous content.

## Traps

- A worktree-local `.beads/config.yaml` can shadow main-store discovery. Explicit Viewer --db overrides BEADS_DB,
  which otherwise overrides BEADS_DIR; always export and triage the same selected directory.
- Viewer can automatically refresh but exit 0 with stale data after export failure. Inspect source authority and
  warnings; require successful explicit export before presenting current recommendations. Stale output is diagnostic.
  A missing export plus failed refresh exits 1. After cooking names the measured authority fields.
- Fresh local bootstrap may omit native metadata.json. Viewer still ranks but reports diagnostic_top_pick and a
  message instead of an actionable top-level ID. Ralph 0.12.0 beads-bv falls back to bd ready in this case.
  Native remote bootstrap can create metadata. Keep diagnostics visible; do not manufacture metadata or upgrade
  the tool pins to hide this limit. A selection never reserves or claims a task.
''')
@yield('''
Install the tested Beads runtime with `mise use -g aqua:steveyegge/beads@1.3.1`, then check `bd --version`.
Without -g, mise pins the tool in this project's mise.toml instead; the team chooses that. Embedded Dolt needs
no separate Dolt install. Install Viewer 0.25.2 from its pinned release:
https://github.com/Dicklesworthstone/beads_viewer/releases/tag/v0.25.2

For Linux amd64, with curl, tar and sha256sum available, download and verify the release archive:

```sh
(
  viewer_install="$(mktemp -d)" && cd "$viewer_install" &&
  viewer_release=https://github.com/Dicklesworthstone/beads_viewer/releases/download/v0.25.2 &&
  curl -fLO "$viewer_release/bv_0.25.2_linux_amd64.tar.gz" &&
  curl -fLO "$viewer_release/checksums.txt" &&
  sha256sum -c --ignore-missing checksums.txt &&
  tar -xzf bv_0.25.2_linux_amd64.tar.gz &&
  mkdir -p "$HOME/.local/bin" && install -m 755 bv "$HOME/.local/bin/bv"
)
export PATH="$HOME/.local/bin:$PATH"
bv --version
```

For another platform, select its 0.25.2 archive on that release page and verify checksums.txt before extracting
and placing bv on PATH. Confirm bd 1.3.1 and bv 0.25.2 before using the workflow.

Use an existing initialized native Beads project. If its store is absent, review that project's sharing and sync
policy first; in the tested embedded Dolt workflow run `bd bootstrap --yes`, then `bd dolt status` there.
Bootstrap creates or restores the store, can read team history and persist sync.remote, and can emit metadata,
role and permission diagnostics. It preserves hooks, instruction files and .git/info/exclude. Keep diagnostics
visible. Code checkout alone does not provide the task store. Cook's optional #tracker recipe documents team
and clone policy setup; Viewer does not configure or initialize either mode.

In the checkout or linked worktree, resolve one store. To select an external native store, first set BEADS_DIR
to its absolute directory for this shell. Run export, then Viewer only if export succeeds:

```sh
beads_dir="${BEADS_DIR:-$(dirname -- "$(git rev-parse --path-format=absolute --git-common-dir)")/.beads}"
bd --db "$beads_dir" export -o "$beads_dir/issues.jsonl" &&
  bv --db "$beads_dir" --robot-triage
```

Explicit Viewer --db overrides BEADS_DB, which otherwise overrides BEADS_DIR. A team worktree's local config.yaml
can shadow automatic main-store discovery. Exports stay in the selected store; this repository's ignores do
not control an external repository's staging or sync policy. Review that policy separately.

Keep export failures and Viewer diagnostics visible. Failed automatic refresh with an existing export can exit 0
with source_authority.state=partial, claim_safe=false, readiness=provisional, source stale=true and export warnings.
Missing export plus failed refresh exits 1. Inspect authority and warnings, require successful export before
presenting current recommendations, and retain stale output only as diagnostic information.

Without native metadata.json, Viewer can rank but emits diagnostic_top_pick and a message instead of an actionable
top-level ID; ralph 0.12.0 beads-bv then falls back to bd ready. Native remote bootstrap can create metadata.
Do not fabricate metadata or promise graph selection when the response cannot support it.

Agents use robot commands. People can replace --robot-triage with no robot flag for `bv --db "$beads_dir"` TUI use
after successful export. Selection never claims a task: check current state and claim explicitly with
`bd --db "$beads_dir" update <id> --claim`. Ordinary task mutation stays with bd; runner worker restrictions stay
in ralph prompts. Cook again with --instructions=AGENTS.md, repeated for each existing file needing this guidance.
Commit the owned ignores, instruction sections and cook.lock; keep runtime exports and task data under native policy.
''')
import io.github.lmarqs.cookbook.beads.BeadsViewer

@param(value = ['instructions'], many = true, sorted = true, default = [], path = true,
    doc = 'An existing Markdown file, from the repository root, where the recipe states the Viewer workflow')
    String[] instructions

@recipe(arguments = true) String cookedWith
@ingredient(BeadsViewer) def viewer

viewer.viewer(instructions as List<String>, cookedWith)
