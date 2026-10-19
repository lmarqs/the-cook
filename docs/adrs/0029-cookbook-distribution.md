# 0029. Cookbooks release as jars in Maven-layout repositories, Maven Central by default

## Objective

**Problem.** A cookbook author shares models and recipes with other repositories. People who cook a public cookbook
should not need an account or credentials to fetch it, and an organization needs to serve its own cookbooks. A
repository that cooks a release needs to know that the content it cooks is the content it reviewed.

**Goal.** Anyone can cook a public cookbook release with no setup, an organization can serve its own releases, and a
repository cooking a release can tell when the content under that release changes.

## Context

- A [cookbook](../features/glossary.md) is a repository, or a directory in one, with `cookbook.toml` at its root
  naming its `group`; each topic directory is one artifact holding models, recipes, resources and a README
  ([cookbook](../../cookbook/README.md)).
- The engine compiles an artifact's sources when one of its recipes is cooked. The jars that the cookbook Gradle
  plugin builds hold those sources, resources and README, not compiled classes
  ([`CookbookPlugin`](../../plugin/src/main/groovy/thecook/gradle/CookbookPlugin.groovy)). The build that publishes
  them sets their version.
- Maven-layout repositories are what build tools publish to and package hosts serve: Maven Central, Maven local at
  `~/.m2/repository`, and the package repositories organizations run. Maven Central serves artifacts over `https://`
  to anyone, without credentials, and never replaces a released version. Its `io.github.<user>` namespaces are
  verified through the GitHub account of that name.
- GitHub Packages' Maven registry needs an access token to install any package, public ones included.
- Two builds of one jar can differ in bytes, such as entry timestamps, while holding the same sources.
- This repository publishes its `agents` cookbook artifact and the cookbook Gradle plugin to Maven Central under
  `io.github.lmarqs` when the maintainer starts a release.

## Decision

1. A cookbook release is one jar per artifact in a Maven-layout repository, at `<group>:<artifact>:<version>`. The
   cookbook's `cookbook.toml` names its group, and the build that publishes the release sets the version.
2. A repository entry in the kitchen's `cook.toml` is an `https://` URL or a path to a Maven-layout repository, or a
   path to a cookbook's source directory, the one holding `cookbook.toml`. A source directory serves each artifact of
   its group, at any version, from the files of its top-level directory. A path starting with `~/` is relative to the
   user's home, any other path to the kitchen. Entries are tried in order.
3. Without a `cook.toml`, the engine resolves from Maven Central.
4. `cook.lock` records the sha256 of the artifact's sources: each path and its content, in path order. A different
   hash under a version the lock records is shown as a changed source before the diff.
5. Coordinates name exact releases. Branches, commits, snapshots and version ranges are refused.

## Reasons

- **1.** The jar carries the sources the engine compiles, and Maven-layout repositories are what existing tools
  publish to and hosts serve, so one resolution path covers public, private and local cookbooks. The version comes
  from the release, so no commit exists only to change a version number. The trade is a build to package a cookbook.
- **2.** Listing repositories in the kitchen keeps the sources a repository trusts in one reviewed file. A `~/` path
  lets a committed `cook.toml` name Maven local, `~/.m2/repository`, where a developer publishes their own cookbooks,
  without writing a personal home directory into the file. The trade is an entry that resolves differently on each
  machine. A source directory entry lets a cookbook's author cook an edit without building or publishing it. The trade
  is an entry that ignores the version, so the lock shows each edit as a changed source under the same version.
- **3.** Maven Central serves public artifacts to anyone without credentials, so a kitchen without a `cook.toml` cooks
  published cookbooks with no setup.
- **4.** A jar's bytes identify more than the artifact: two builds of one release can differ in bytes. A hash of the
  artifact's sources identifies exactly what the engine compiles. A repository other than Maven Central can replace a
  jar under its version, so comparing that hash with the lock is how a repository notices that a release's content
  changed. The trade is a hash that no other tool computes.
- **5.** A branch or a range selects different content over time under the same coordinates, so the lock could not
  say which content produced the ownership it records ([0025](0025-recipes.md)).

## Rejected alternatives

### Git tags as releases

At its best, an author releases with a tag on the repository that already holds the source, with no build and no
package hosting. It loses because it is a second resolution path beside Maven-layout repositories, with a tag naming
rule per cookbook directory, a fetch cache and a dependency on `git` on every machine, while Maven Central and Maven
local already serve versioned sources to anyone who publishes a jar.

### GitHub Packages

At its best, a cookbook's packages live beside its repository, with no other account. It loses because its Maven
registry needs an access token even for public packages, so every kitchen cooking a public cookbook would need
credentials.

### A branch or commit as the version

At its best, an author shares work without a release step. It loses because a branch moves under the same name, and a
raw commit says nothing about compatibility; decision 5 keeps coordinates to releases the author chose.

### A recipe at an `https://` URL

At its best, sharing a recipe is serving one file, with no cookbook or build. It loses because a URL carries no
release identity: its short name would change with a version written into the URL, or two hosts serving one path
would collide under one name; each template the recipe references would be fetched one by one; and Maven-layout
repositories already distribute versioned sources, which a developer can publish to Maven local without hosting. A
recipe operand is therefore a path inside the repository or coordinates.

## Consequences

Costs:

- Publishing a cookbook needs a build that writes Maven-layout jars, such as the cookbook Gradle plugin.
- The lock's source hash differs from the jar sha256 that package tools report.
- Publishing to Maven Central needs a verified namespace, a signing key and the Central Portal credentials.

Must be maintained:

- The source hash definition.
- The `io.github.lmarqs` namespace and the release workflow's credentials.

## Confirmation

- Review a kitchen without a `cook.toml`: does `io.github.lmarqs:agents:<version>#instructions` cook from Maven
  Central?
- Review a jar republished under the same version in Maven local: does the next run show a changed source before the
  diff?
- Review a kitchen that lists `~/.m2/repository`: does a cookbook its developer published to Maven local cook by its
  coordinates?
- Review a kitchen that lists a cookbook's source directory: does an edit to one of its recipes cook on the next run
  with no build?
