@help('''
Sets up Beads tracker policy for a team or one clone: five native configuration keys, required root ignore rules,
and workflow guidance in the instruction files you name. Cooking needs no Beads binary and creates no task store.

## API

```sh
# Shared tracker configuration, with native task history published separately
cook io.github.lmarqs.cookbook:beads:<version>#tracker
# Personal tasks in this clone, with remote synchronization disabled
cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=clone
# State the selected workflow in the files agents and people read
cook io.github.lmarqs.cookbook:beads:<version>#tracker --instructions=AGENTS.md --instructions=README.md
# Replay the source and arguments recorded in cook.lock in a new code clone
cook replicate io.github.lmarqs.cookbook:beads#tracker
```

`--share` is `team` by default or `clone`. Team mode shares `.beads/config.yaml`; clone mode ignores all `.beads/`
and sets the native local-only gate. `--instructions` repeats, naming existing Markdown files by their paths from
the repository root. `.beads/` is the native store location; repository-linked worktrees share the main checkout's
store without `BEADS_DIR`. No bootstrap, initialization or synchronization command runs while cooking.

## Consumes

- Git and `bd` 1.3.1 at run time; embedded Dolt needs no separate Dolt CLI or server for this workflow.
- Existing `.beads/config.yaml`, merged through Cook's normal key edits, preserving `sync.remote` and unrelated keys.
- Only the instruction files `--instructions` explicitly names; each must exist, written by the team or a recipe
  cooked earlier in the invocation. Missing files are refused rather than created.
- By hand after: install Beads and bootstrap the store as After cooking says. Code checkout alone has no task store.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.beads/config.yaml` | `dolt.local-only=false` | `--share=team` | owned |
| `.beads/config.yaml` | `dolt.local-only=true` | `--share=clone` | owned |
| `.beads/config.yaml` | `dolt.auto-push=false`, `no-git-ops=true`, `export.auto=false`, \
`export.git-add=false`, as nested YAML keys | Always | owned |
| Root `.gitignore` | `tracker` section with argument line, `/.beads/*`, `!/.beads/config.yaml` | \
`--share=team` | owned |
| Root `.gitignore` | `tracker` section with argument line and `/.beads/` | `--share=clone` | owned |
| Root `.gitignore` | In that section: `.dolt/`, `*.db`, `.beads-credential-key`, `.beads/proxieddb/`, \
`*.gate.lock*` | Always | owned |
| Each `--instructions` file | `tracker` section with argument line and guidance for the selected mode | \
`--instructions` | owned |

The root rules exclude native database, export, credential and lock files, including `.beads.gate.lock`.
Native `.beads/.gitignore` is redundant bootstrap output. The engine alone records ownership in `cook.lock`.
Never touches: task data, native metadata, native remotes, hooks, `core.hooksPath`, `.git/info/exclude`, tool pins,
team-owned seeds, agent integrations, or unrelated ignore rules, YAML keys and instruction text.

## Connections

| Contact | Made by |
| --- | --- |
| Workflow guidance in the team's files | The person, by repeatable `--instructions` paths |
| Task store creation and optional team synchronization | The person, with native commands after cooking |
| Publication destination | The person, reviewing `origin` before consenting to first publication |
| Coding agent integration | The team, separately using native `bd setup` if desired; never detected or installed |
| Viewer or a loop runner | The person, separately selecting those tools and their store connections |

Cook a recipe that creates an instruction file before this one, or create the file by hand first.

## Re-cooking

Repeat the arguments on the opening line of the root `.gitignore` tracker section, such as
`# io.github.lmarqs.cookbook:beads#tracker --share=clone --instructions=AGENTS.md`. An upgrade reconciles the owned
keys and sections. Cooking without an instruction path withdraws only its tracker section. `cook check` reports
changes to owned policy or guidance without initializing or accessing the task store.

Changing mode reconciles the local-only key, ignores and guidance; it preserves task data, native remotes and seeds.
Before switching modes, review `bd dolt status`, configuration overrides and tracked files. Git keeps previously
tracked files tracked despite new ignore rules: review `git ls-files .beads`, and deliberately remove private files
from the index with `git rm --cached <path>` when appropriate, retaining their local copies. Changing mode never
deletes tasks, unpublishes history or changes a native remote's destination.

Discard removes only the five owned keys and tracker sections. Removing clone's local-only key can re-enable a
preserved native remote; review effective policy before using `bd` again. Discard restores no earlier values.

## Traps

- `BD_DOLT_LOCAL_ONLY` and `.beads/config.local.yaml` can override repository policy. In clone mode, require the
  effective `Remote sync: disabled` indication from `bd dolt status` before personal writes. Push/pull can exit 0
  because they skipped synchronization; remote addition is refused. An ignore rule alone proves no sync policy.
- Bootstrap can read team history, adopt its task prefix and persist `sync.remote` even in clone mode. Local-only
  controls subsequent synchronization; it does not promise bootstrap reads no remote data.
- Fresh local bootstrap can warn about absent native metadata and use the default database name `beads`. It also
  leaves `beads.role` unset, so writes can suggest `git config beads.role maintainer` or `contributor`. Choose the
  appropriate role yourself. Cloned directories can trigger `chmod 700 .beads` permission advice. Keep native
  diagnostics visible; this recipe invents no metadata seed and makes no user-specific role or permission choice.
- `bd init` and `--stealth` can add unmanaged excludes or stage/commit source files. Use `bd bootstrap --yes` here;
  it preserves existing hooks, instruction files and `.git/info/exclude`, including with both origin and upstream.
''')
@yield('''
Install the tested runtime: `mise use -g aqua:steveyegge/beads@1.3.1`, then check `bd --version`. Without `-g`, mise
pins it in this project's mise.toml instead; the team chooses that. Embedded Dolt needs no separate Dolt install.

In the repository, run `bd bootstrap --yes`, then `bd dolt status`. Bootstrap creates or restores the native store;
it can read origin's team history, adopt its prefix and persist sync.remote in either mode. It preserves hooks,
instructions and .git/info/exclude, including when both origin and upstream exist. Do not replace it with bd init
or --stealth. Code checkout alone has no task store; a new code clone must bootstrap too.

With --share=clone, require `Remote sync: disabled` in `bd dolt status` before any personal writes. If absent,
resolve BD_DOLT_LOCAL_ONLY and .beads/config.local.yaml overrides first. Push/pull skip with exit 0; remote addition
is refused. Ignoring files proves no sync policy. Discarding the owned local-only key may re-enable a native remote.

With --share=team, review `git remote get-url origin` as the intended destination and publish the source branch
there first. Review `git status` for a clean index and worktree before `bd dolt push --yes`. That first consent
publishes the complete task history, persists sync.remote and creates a source commit under your Git identity.
Inspect `git diff --check` and `git log -1 --oneline`, then `git push origin <branch>` to share the configuration.
Subsequent team synchronization uses `bd dolt pull` and `bd dolt push`.

Fresh local bootstrap may warn about absent metadata; writes may suggest setting beads.role, and cloned directories
may prompt chmod 700 .beads. Keep these diagnostics visible and choose your own role and permissions. No metadata
seed is needed. Repository-linked worktrees use the main checkout's store without an extra BEADS_DIR.

Use `bd ready`, `bd create`, `bd update <id> --claim` and `bd close <id>` for ordinary task work. To publish workflow
guidance where people and agents read, cook again with --instructions=AGENTS.md, or another existing Markdown file;
repeat the argument for each file. Commit the owned root ignores and cook.lock; team mode also shares config.yaml.
''')
import io.github.lmarqs.cookbook.beads.BeadsTracker

@param(value = ['share'], choices = ['team', 'clone'], default = ['team'],
    doc = 'Who shares the tracker: team shares configuration; clone keeps tasks local with native sync disabled')
    String share
@param(value = ['instructions'], many = true, sorted = true, default = [], path = true,
    doc = 'An existing Markdown file, from the repository root, where the recipe states the tracker workflow')
    String[] instructions

@recipe(arguments = true) String cookedWith
@ingredient(BeadsTracker) def beads

beads.tracker(share, instructions as List<String>, cookedWith)
