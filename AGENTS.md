# the-cook

This repository builds the-cook, a recipe driven configuration tool, and the `agents`, `beads`, `git`, `docs` and `ralph`
cookbook artifacts it cooks. [Philosophy](docs/philosophy.md) says who it is for, and
[Quick start](README.md#quick-start) how to try it.

Stack: Groovy 4.0 on Java 17 (`mise.toml`), Gradle 9.8 (`gradle/wrapper/gradle-wrapper.properties`), JUnit 4,
Spotless and CodeNarc (`gradle/build-conventions/`), semantic-release (`.github/workflows/release.yml`).

## Quick reference

- Install or add a dependency: `mise install` for Java 17; add a library to the `dependencies` block of the module's
  `build.gradle`.
- Run: `./cook <recipe> [arguments]` once the CLI is built; `./cook --help` lists the options.
- Build: `./gradlew cliBuild -PreleaseVersion=<version>`; without the property, `./cook --version` prints `0.0.0`.
- Test one file: `./gradlew -p <module> test --tests <class>`, as
  `./gradlew -p core test --tests thecook.CoordinatesTest`.
- Lint or format one file: `./gradlew spotlessApply -PspotlessIdeHook=<absolute path>` formats one file;
  `./gradlew codenarc` lints every Groovy and Gradle file, `./gradlew -p <module> lint` one module.
- Generate code: none.
- Local services: none.
<!-- cook:begin io.github.lmarqs.cookbook:git#worktrees/worktrees -->
<!-- io.github.lmarqs.cookbook:git#worktrees --instructions=AGENTS.md -->
- Worktrees: branch `<branch>` is checked out at `.worktrees/<branch>`; `.worktrees/README.md` says how to
  create one ready to run and remove it.
<!-- cook:end io.github.lmarqs.cookbook:git#worktrees/worktrees -->
- Quick check, seconds on what changed: `./gradlew -p <module> check` for each module changed, about 13 s for `core`.
- Done check, all that must pass before a change is done: `./gradlew check cliBuild jpackageTest`, then
  `git diff --check`; about a minute with a warm Gradle daemon, no services, network access to fetch Gradle dependencies.

## Boundaries

**Always**

- Run the Quick check before moving on. A change is done when the Done check passes.
- Report each check's result, or that it did not run.
- Update this file in the change that alters a command, path or rule it names.
- Keep this file under 800 words; past 1,200, move a section into a skill or memory file before adding. All
  `AGENTS.md` files together stay under 16 KiB.
- After changing a recipe, review the configuration it generates and its ownership records in `cook.lock`.

**Never**

- Commit secrets or credentials, or put personal data in logs, fixtures or commits.
- Edit generated files by hand: `cook.lock`; `.agents/skills/agents-layout/` and `docs-layout/`,
  cooked from `cookbook/src/<artifact>/skills/`; the keys Cook renders from `.agents/permission/policy.yaml` into
  `.agents/adapters/`; `build/` directories.
- Make a check pass by weakening it: skipping a test, suppressing a warning, loosening an assertion.

**Stop and ask** when a check fails again after a fix, a command is refused, or a decision is left open.

Commands agents may run are set in `.agents/permission/policy.yaml`. A refusal is the rule working.

## Where things go

- New code: in its existing module: `core/` the engine, `infra/` the kitchen's `cook.toml` and cookbook fetching,
  `cli/` the command line, `cookbook/src/<artifact>/` recipes, `gradle/build-conventions/` shared build setup. The
  design is [docs/architecture.md](docs/architecture.md).
- Tests: `<module>/src/test/groovy/thecook/`, one `<Subject>Test.groovy` per subject, as
  `core/src/test/groovy/thecook/CoordinatesTest.groovy`; recipe tests in `cookbook/test/<artifact>/`, never in core.
- Settings: build options are Gradle `-P` properties, as `releaseVersion`; release secrets are the GitHub Actions
  secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` and `SIGNING_PASSWORD`.
- Decisions, feature behavior, standards and pitfalls: where [docs/README.md](docs/README.md) says, by the
  `docs-layout` skill.
- A procedure an agent repeats: a skill in `.agents/skills/`.
- A trap a correction reveals in one area: `.agents/memory/<area>.md`, listed under Deep dive.

## Rules

- A behavior change ships with a test; a bug fix with one that failed before the fix.
- Comments record only reasons the code cannot show.
- Change only what the task needs, matching the surrounding code.
- Before changing a workflow or the command line, read its page under `docs/features/` and the matching standard
  under `docs/ux/`.
- Format and lint follow [Development tooling](README.md#development-tooling); the Done check enforces them.
- Commits: subjects are `<type>(<area>): <sentence>`, where `<area>` is the module or topic, such as `core`, `cli` or
  `docs`. The type decides the next release: `feat` a minor, `fix` and `docs` a patch, and `refactor`, `test`, `build`,
  `ci` and `chore` none. A `!` after the area, as in `feat(core)!:`, releases a major version. Lint-only changes use
  `chore(lint)`.

## Gotchas

- The Quick check skips Spotless and CodeNarc; the Done check runs them, and `./gradlew -p <module> lint`
  for `core`, `infra` and `cli`.
- The Groovy formatter rejects some syntax, and a long line in a recipe's `@help` continues after a trailing `\`; see
  [Development tooling](README.md#development-tooling).

## Deep dive (read on demand)

- How the engine cooks a recipe: [core/README.md](core/README.md); how cookbooks are fetched:
  [infra/README.md](infra/README.md).
- Known traps by feature: [docs/features/pitfalls.md](docs/features/pitfalls.md).
- Agent permissions, roles and launch commands: [.agents/README.md](.agents/README.md) and the `agents-layout` skill.
<!-- cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui -->
<!-- io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worktree-setup=.worktrees/setup --worktree-teardown=.worktrees/teardown --tracker=beads --tracker=json --instructions=AGENTS.md -->
- Loop runs: `.ralph-tui/bin/run <name> --agent <entry> --epic <id>`, or `--prd <file>`, runs ralph-tui in
  `.worktrees/ralph/<name>` on branch `ralph/<name>`; `.ralph-tui/bin/clean <name>` removes both once the
  branch is delivered.
<!-- cook:end io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui -->
<!-- cook:begin io.github.lmarqs.cookbook:beads#tracker/tracker -->
<!-- io.github.lmarqs.cookbook:beads#tracker --instructions=AGENTS.md -->
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
<!-- cook:end io.github.lmarqs.cookbook:beads#tracker/tracker -->
<!-- cook:begin io.github.lmarqs.cookbook:beads#viewer/viewer -->
<!-- io.github.lmarqs.cookbook:beads#viewer --instructions=AGENTS.md -->
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
<!-- cook:end io.github.lmarqs.cookbook:beads#viewer/viewer -->
