# PRD: `./cook update`, move recipes to newer cookbook releases with their recorded arguments

## Overview

After upgrading the CLI in a project, moving its recipes to newer cookbook releases is manual: an agent runs
`./cook help <recipe>` for each recipe, works out its arguments, and retypes
`./cook group:artifact:<new version>#recipe --arguments...`. `cook.lock` already records each recipe's source and
arguments, and `replicate` already cooks a recipe again from them. Only the version is missing.

This feature adds `./cook update`, which works like `npm update`. For each named recipe, or every coordinates recipe
in `cook.lock`, it finds the newest release that its kitchen's repositories list. It then cooks the recipe at that
release with the arguments the lock records, through one diff and one batch approval, and the lock records the new
version. Without `--major`, a recipe stays inside its compatible range, as npm keeps to `^`. `--major` moves it to the
newest release. `--dry-run` reports what would move and exits 1 when something would, like `npm outdated`.

Tracking issue: #86. A throwaway spike on branch `spike/update` (commit `c64eb80`) shows that the approach works
against Maven Central. It is a reference, not a base: build each story from `main`.

## Goals

- One command, with no recipe names, versions or arguments typed, moves every coordinates recipe in a kitchen to its
  newest compatible release.
- A release that may break recorded arguments, a new major version, is taken only when asked for with `--major`.
- An update whose recorded arguments no longer fit the new release writes nothing. It names the argument and the
  arguments the recipe accepts.
- A recipe whose newer releases Cook cannot see is reported, never described as up to date.
- The decision corpus and feature docs state the command without contradicting one another.

## Quality Gates

These commands must pass for every user story:
- `./gradlew check cliBuild jpackageTest`: build, tests, Spotless, CodeNarc and the docs check (the AGENTS.md Done
  check)
- `git diff --check`: no whitespace errors

## User Stories

### US-001: Record the decision that Cook picks a newer release on request
**Description:** As a maintainer, I want the decision corpus to state when Cook may choose a recipe version, so that
`update` does not contradict "users supply the recipe version".

**Acceptance Criteria:**
- [ ] Read `docs/adrs/README.md` and follow it: objective front matter (`problem` of at most four sentences, `goal`
      of at most two, plain text, naming no command, file or setting), present tense, one owning ADR per rule.
- [ ] New `docs/adrs/0032-recipe-updates.md` decides:
      1. Cook moves a recipe's recorded version only when asked, through the same review and approval as cooking.
      2. Without `--major`, the newest release inside the recorded version's compatible range: the same major version,
         or the same `0.minor` below 1.0.0, as npm's `^`. `--major` takes the newest release.
      3. Releases come from the listing a Maven-layout repository publishes (`maven-metadata.xml`, or
         `maven-metadata-local.xml` in Maven local).
      4. A recipe that a repository serves without listing releases, such as a cookbook source directory, has no
         known newer release.
      5. The lock keeps exact versions ([0029](0029-cookbook-distribution.md), decision 5).
      Rejected alternatives cover at least: version ranges in `cook.lock`, crossing major versions by default, and
      `--force` as the flag (it already means "replace a template file", `docs/ux/cli-standards.md:218`).
- [ ] The ADR index in `docs/adrs/README.md` lists 0032 with the question it answers.
- [ ] Statements that users alone supply recipe versions become consistent with 0032 and link to it, at
      `docs/architecture.md:332`, `docs/adrs/0013-organization-content.md:36-39,79-80,94`,
      `docs/adrs/0017-engine-version.md:47,51`, `docs/adrs/0026-cookbook-trust.md:28,92` and
      `docs/features/wrapper.md:73`. "An engine upgrade does not move recipe versions" stays true and stays.
- [ ] `docs/architecture.md` FR-17 and FR-19 add `./cook update [batch-options] [--major] [<name>]...`: `update` is
      a reserved first operand like `check`, takes lock names only, and takes kitchen selection, `--dry-run`,
      `--batch-approve` and `--major`.
- [ ] Commit: `docs(adrs): Cook moves a recipe to a newer release on request (#86)`

### US-002: Repositories list the releases of an artifact
**Description:** As the update engine, I want each repository to list the releases it holds of an artifact, or say it
cannot, so that I can find a newer version.

**Acceptance Criteria:**
- [ ] `CookbookRepository` (`core/src/main/groovy/thecook/cookbook/CookbookRepositories.groovy:35`) gains
      `List<String> versions(String group, String artifact)`, documented as follows. It returns the releases listed,
      `[]` when the repository does not hold the artifact, and `null` when it holds the artifact but cannot list its
      releases. It is a default method returning `[]`, so the closure-coerced fakes in
      `core/src/testFixtures/groovy/thecook/Kitchens.groovy:59` and `CookbookReplicateTest.groovy:52` still compile.
- [ ] `MavenRepository` (`infra/src/main/groovy/thecook/infra/MavenRepository.groovy`) reads
      `<group path>/<artifact>/maven-metadata.xml`, then `maven-metadata-local.xml`, and returns every
      `versioning/versions/version` text. With neither file present it returns `[]`. It parses with the JDK's
      `javax.xml.parsers` (no new dependency, ADR 0004), with external entities and DTD loading disabled.
- [ ] `CookbookDirectory` (`infra/src/main/groovy/thecook/infra/CookbookDirectory.groovy`) returns `null` for an
      artifact it serves (its group, with `src/<artifact>/` present) and `[]` otherwise.
- [ ] New `infra/src/test/groovy/thecook/infra/MavenRepositoryTest.groovy` covers: versions from
      `maven-metadata.xml`, versions from `maven-metadata-local.xml`, `[]` with no metadata, and a metadata file
      with a DOCTYPE declaring an external entity is read without resolving it.
- [ ] `CookbookDirectoryTest.groovy` covers `null` for a served artifact and `[]` for another group and for a missing
      artifact.
- [ ] Commit: `feat(infra): repositories list an artifact's releases (#86)`

### US-003: Choose the newest compatible release
**Description:** As the update engine, I want one place that chooses a recipe's target release, so that the
compatible-range rule is applied the same way everywhere.

**Acceptance Criteria:**
- [ ] `Coordinates` (`core/src/main/groovy/thecook/cookbook/Coordinates.groovy`) gains
      `static int compareVersions(String, String)`, numeric per component, and `Coordinates at(String version)`,
      the same coordinates at another version.
- [ ] `CookbookRepositories` gains a method that, given recorded `Coordinates` and a `major` flag, returns one of
      three outcomes:
      (a) a newer release;
      (b) no newer release;
      (c) unknown, when any repository returns `null` for the artifact.
      It considers only versions that `Coordinates.valid` accepts, from every repository, and never returns a
      version that is lower than or equal to the recorded one.
- [ ] Without `major`: from `8.0.0`, among `[8.0.1, 8.1.1, 9.0.1]`, the choice is `8.1.1`. From `0.3.0`, among
      `[0.3.2, 0.4.0, 1.0.0]`, it is `0.3.2`. From `0.0.3`, among `[0.0.4, 0.1.0]`, there is none.
      With `major`: `9.0.1`, `1.0.0` and `0.1.0` respectively.
- [ ] For case (b) or (a) without `major`, the outcome also carries the newest release overall when it is higher,
      so the CLI can say a newer major exists.
- [ ] Tests in `CoordinatesTest.groovy` and new `core/src/test/groovy/thecook/CookbookRepositoriesTest.groovy`
      cover every case above, plus `9.10.0 > 9.9.0`, a non-release version such as `9.1.0-SNAPSHOT` ignored, and the
      union of two repositories' listings.
- [ ] Commit: `feat(core): choose a recipe's newest compatible release (#86)`

### US-004: Update engine
**Description:** As a developer, I want the engine to propose cooking lock-recorded recipes at their newer releases
with their recorded arguments, so that the CLI can review and write them in one batch.

**Acceptance Criteria:**
- [ ] New `core/src/main/groovy/thecook/cookbook/CookbookUpdate.groovy`, shaped like `CookbookReplicate.groovy`. It
      takes the kitchen and `CookbookRepositories`. Its plan step, given lock names and `major`, returns for each
      recipe in scope one of: moves `from -> to` (with any newer major release noted), current, or unknown.
- [ ] With no names, scope is every entry in `cook.lock`, in lock order. Path recipes (sources that are not
      coordinates) are left out silently.
- [ ] With names, it uses `RecipeLoader.lockName` as replicate does. It refuses before writing when a name:
      - is given twice;
      - is absent from the lock (the message lists the lock's names);
      - is a path recipe ("has no version to update");
      - is versioned coordinates;
      - is unknown, meaning it was named explicitly and its repository cannot list releases.
- [ ] A kitchen without `cook.lock` is refused, with the replicate-style diagnostic naming `cook.lock`.
- [ ] Its propose step builds a `RecipeRequest(source.at(to).toString(), recorded arguments, true)` for each move, in
      scope order, and returns `CookbookCook.propose`'s `Proposal`. Recipes not moving are not cooked.
- [ ] A moving recipe without recorded arguments is refused with replicate's "cook it once with its arguments" text.
- [ ] When recorded arguments fail to bind at the new release, the error names the recipe at its new coordinates, the
      unknown or missing argument, the arguments it accepts and the recorded line. The next step is "read
      `./cook help <new coordinates>` and cook it with its new arguments", and nothing is written.
- [ ] New `core/src/test/groovy/thecook/CookbookUpdateTest.groovy` uses `Kitchens.publish` with a fixture repository
      that lists the versions published under it (extend `Kitchens.repositories`). It covers:
      - moving within the major version, with arguments kept in the lock;
      - holding at the major boundary without `major`, and crossing it with `major`;
      - the lock's `source` and `sha256` after the move;
      - a recipe already current;
      - a renamed argument that writes nothing;
      - each refusal above;
      - an unknown recipe in scope without names, reported as unknown.
- [ ] Commit: `feat(core): propose cooking lock recipes at their newer releases (#86)`

### US-005: `./cook update` command
**Description:** As a developer or agent, I want `./cook update` to show what moves and cook it after one approval, so
that bumping recipes takes one command.

**Acceptance Criteria:**
- [ ] Read `docs/features/cli.md` and `docs/ux/cli-standards.md` before changing the command line.
- [ ] `Grammar.groovy` adds `UpdateArgs extends KitchenScoped` with `--dry-run`, `--batch-approve`, `--major` and
      `NAME...` parameters. It is listed after `replicate` in the root `subcommands`, with the description
      `Cook recipes from cook.lock again at their newest release.`
- [ ] `CookCommand.groovy` routes `UpdateArgs` to an `update` action that calls `lockNames(args.recipes)` first, so
      recipe arguments and misplaced batch options get the existing errors.
- [ ] Output on stdout, one line per recipe in scope, before the review:
      - `Update <name> <from> -> <to>` for a move.
      - `Holding <name> <version>; <newer> needs --major`, when only a new major version is newer.
- [ ] An unknown recipe in scope without names prints a diagnostic on stderr that follows the messages standard.
      It names the recipe and the repository and says what to do: cook the release you want by its coordinates. The
      run continues with the others, and the exit status does not change.
- [ ] Nothing to move prints `Already at the newest releases.` and exits 0.
- [ ] Without `--dry-run`, it shows the moves, then the confirm texts and diff through the existing
      `write(proposal, false, batchApprove)`. Exit status is 0 when written, and 1 when approval is declined or
      missing.
- [ ] With `--dry-run`, it shows the moves and the diff the update would write, writes nothing, and exits 1 when at
      least one recipe would move and 0 when none would. Errors exit 2.
- [ ] `--dry-run`, `--batch-approve`, `--force` or `--skip` before `update` gets the existing "go before a recipe, not
      before a command" error. `--force` and `--skip` after `update` are unknown options.
- [ ] `./cook --help` lists `update`. The usage assertions in `CookCommandTest.groovy:12,205` and
      `JpackageTest.groovy:16,61` also assert the `update` line.
- [ ] `CookCommandTest.groovy` covers each output line, every exit status above, `--major`, a named recipe, and a
      lock name with a version refused.
- [ ] Commit: `feat(cli): ./cook update moves recipes to their newest releases (#86)`

### US-006: Feature page and CLI reference
**Description:** As a user, I want docs that show how to update recipes, so that I can use the command without reading
the source.

**Acceptance Criteria:**
- [ ] New `docs/features/update.md`, shaped like `docs/features/replicate.md`: `objective` front matter, an
      Overview, an example that moves a kitchen from `git:8.0.0#worktrees` to the newest 8.x and then, with
      `--major`, to the newest release, and a Reference. The Reference covers:
      - the synopsis;
      - scope, with and without names;
      - the compatible range, with the 0.x rule;
      - where releases come from;
      - unknown recipes;
      - arguments that no longer fit;
      - `--dry-run` exit status;
      - approval and recovery, by link to `cook.md#review-and-recovery`.
- [ ] `docs/features/cli.md` adds `update` to Synopsis, Commands, Lock names and Exit status. `docs/features/README.md`
      adds the page to the reading path and the Pages table. `README.md` lists the command next to `replicate`
      (around line 108).
- [ ] `docs/ux/cli-standards.md` "Command set" (line 187) names `update` as a reserved first operand.
- [ ] `docs/features/pitfalls.md` records the trap that a cookbook source directory in `cook.toml` makes its recipes
      unknown to `update`.
- [ ] Every command shown in the new page was run against a built CLI. Its output is pasted from that run, not written
      by hand.
- [ ] Commit: `docs(features): update page and CLI reference (#86)`

## Functional Requirements

- FR-1: `./cook update [-C <dir>] [--dry-run] [--batch-approve] [--major] [<name>]...` must cook each recipe in
  scope at its chosen newer release, with the arguments `cook.lock` records, in one batch with one approval.
- FR-2: Scope without names is every coordinates entry in `cook.lock`, in lock order. With names, it is those lock
  names in the order given.
- FR-3: Without `--major`, the chosen release is the newest one listed with the recorded major version, or with the
  same `0.minor` below 1.0.0. With `--major`, it is the newest listed release. A version lower than or equal to the
  recorded one is never chosen.
- FR-4: Releases are those every repository in the kitchen's `cook.toml` lists, or Maven Central without one, that
  are exact `X.Y.Z` versions.
- FR-5: A recipe that any repository serves without listing releases is unknown. Without names, it is reported and
  skipped. Named, it is an error that writes nothing.
- FR-6: If any recipe's recorded arguments do not bind at its new release, nothing is written. The error names the
  argument, the accepted arguments, the recorded line and `./cook help <new coordinates>`.
- FR-7: After a write, each moved entry in `cook.lock` records the new `source` and `sha256`, and its arguments
  unchanged.
- FR-8: Exit status is 0 when written or nothing moves, 1 when approval is declined or missing, or when `--dry-run`
  finds a move, and 2 on any error.
- FR-9: `update` takes no recipe arguments, `--force` or `--skip`, and no versioned coordinates.

## Non-Goals

- Version ranges in `cook.lock` or in recipe operands; the lock stays exact.
- Updating the engine wrapper `./cook` (ADR 0017).
- Updating path recipes.
- Choosing or prompting for new arguments during an update. An argument change is done by cooking the recipe at its
  new coordinates.
- Moving a recipe to a lower version, or to a chosen version other than the newest.
- `npm outdated`'s table and a JSON output.
- An `upgrade` alias. npm treats it as `update`; a second spelling breaks "one spelling" in `cli-standards.md`.

## Technical Considerations

- Resolution order is unchanged: the chosen version resolves from the first repository holding it. A source directory
  listed before Maven Central would serve the new version from its files. That is why such an artifact is unknown
  (FR-5) rather than taking its version from another repository.
- `RecipeRequest`'s `replayed = true` gives update replicate's argument-binding message. The changed-source notice
  compares hashes under the same address, so a moved recipe shows no notice. That is correct: the version changed,
  not the content under it.
- `MavenRepository.read` turns an I/O failure other than not-found into an error. A network failure while listing
  therefore exits 2, which is intended.
- The spike's `versions` default method on a Groovy interface compiled with Groovy 4.0 and kept the closure
  coercion in `Kitchens` working.

## Success Metrics

- Moving this repository's consumers to a new cookbook release takes `./cook update`, a review, and a commit. No
  `./cook help` or retyped arguments are needed.
- In the example kitchen, `./cook check` reports `Already cooked.` after `update` writes.

## Open Questions

- The 0.x rule follows npm's `^`, my design call. Since every published `io.github.lmarqs.cookbook` release is 2.0.0
  or later, a plain major-version rule would behave the same for them today.
