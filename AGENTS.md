# the-cook

This repository builds the-cook, a recipe driven configuration tool, and the `agents`, `git`, `beads` and `ralph`
cookbook artifacts it cooks. The [README](README.md#objective) says who it is for, and
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
- Edit generated files by hand: `cook.lock`; `.agents/skills/agents-layout/`, cooked from
  `cookbook/src/agents/skills/agents-layout/`; the keys Cook renders from `.agents/permission/policy.yaml` into
  `.agents/adapters/`; `build/` directories.
- Make a check pass by weakening it: skipping a test, suppressing a warning, loosening an assertion.

**Stop and ask** when a check fails again after a fix, a command is refused, or a decision is left open.

Enforced by `.agents/permission/policy.yaml`. A refusal is the rule working.

## Where things go

- New code: in its existing module: `core/` the engine, `infra/` the kitchen's `cook.toml` and cookbook fetching,
  `cli/` the command line, `cookbook/src/<artifact>/` recipes, `gradle/build-conventions/` shared build setup. The
  design is [docs/architecture.md](docs/architecture.md).
- Tests: `<module>/src/test/groovy/thecook/`, one `<Subject>Test.groovy` per subject, as
  `core/src/test/groovy/thecook/CoordinatesTest.groovy`; recipe tests in `cookbook/test/<artifact>/`, never in core.
- Settings: build options are Gradle `-P` properties, as `releaseVersion`; release secrets are the GitHub Actions
  secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` and `SIGNING_PASSWORD`.
- Decisions and domain rules: [docs/adrs/](docs/adrs/README.md); feature behavior in `docs/features/`, interface
  standards in `docs/ux/`.
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
  `docs`. The type decides the next release: `feat` a minor, `fix` a patch, and `docs`, `refactor`, `test`, `build`,
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
