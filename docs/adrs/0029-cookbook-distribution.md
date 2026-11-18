---
description: Records why cookbooks release as versioned jars that resolve from the repositories a kitchen lists.
audience: Whoever publishes a cookbook or changes how one is fetched
---
# 0029. Cookbooks release as versioned jars resolved from the kitchen's repositories

A cookbook author shares models and recipes with other repositories. People who cook a public cookbook should not need
an account or credentials to fetch it, and an organization needs to serve its own cookbooks. A repository that cooks a
release needs to know that the content it cooks is the content it reviewed. Anyone can cook a public cookbook release
with no setup, an organization can serve its own releases, and a repository cooking a release can tell when the content
under that release changes.

## Context

- A [cookbook](../features/glossary.md) is a repository, or a directory in one, with `cookbook.toml` at its root
  naming its `group`; each directory under its `src/` is one artifact holding models, recipes, resources and a
  README, and its tests sit apart under `test/` ([cookbook](../../cookbook/README.md)).
- The engine compiles an artifact's sources when one of its recipes is cooked. An artifact's jar holds those sources,
  resources and README under the artifact's package path, `<group path>/<artifact>/`, not compiled classes. A jar of
  files and a POM is what any build tool writes: `cookbook/build.gradle` writes them with Gradle's `Jar` task and its
  `maven-publish` plugin ([cookbook](../../cookbook/README.md)). The build that publishes them sets their version.
- Maven-layout repositories are what build tools publish to and package hosts serve: Maven Central, Maven local at
  `~/.m2/repository`, and the package repositories organizations run. Maven Central serves artifacts over `https://`
  to anyone, without credentials, and never replaces a released version.
- Maven Central tracks three monthly publishing metrics per organization, file count, release size and release
  count, and past a 30-day grace period refuses a release that keeps an organization over a threshold
  ([Maven Central publishing limits](https://central.sonatype.org/publish/maven-central-publishing-limits/), checked
  2026-11-18). Publishing there needs a verified namespace, a signing key and Central Portal credentials.
- GitHub.com, checked 2026-11-18: an asset attached to a release of a public repository downloads without credentials
  from `https://github.com/<owner>/<repo>/releases/download/<tag>/<name>`, and
  `https://github.com/<owner>/<repo>/releases/latest/download/<name>` serves the asset of that name on the latest
  release. Both redirect to `release-assets.githubusercontent.com`. GitHub's changelog of 2025-05-08,
  [Updated rate limits for unauthenticated requests](https://github.blog/changelog/2025-05-08-updated-rate-limits-for-unauthenticated-requests/),
  tightens unauthenticated limits on the REST API and on `raw.githubusercontent.com`, publishes no numbers and does
  not name release assets. A release workflow uploads assets with the `GITHUB_TOKEN` every run already has.
- GitHub Packages' Maven registry needs an access token to install any package, public ones included.
- Two builds of one jar can differ in bytes, such as entry timestamps, while holding the same sources.

## Decision

1. A cookbook release is one jar per artifact, at `<group>:<artifact>:<version>`, in a Maven-layout repository or
   attached to a GitHub release as `<artifact>-<version>.jar`. The cookbook's `cookbook.toml` names its group,
   and the build that publishes the release sets the version.
2. A repository entry in the kitchen's `cook.toml` is an `https://` URL or a path to a Maven-layout repository, or a
   path to a cookbook's source directory, the one holding `cookbook.toml`, or `github:<owner>/<repo>`, the releases
   of that GitHub repository: it reads the jar from the release tagged `v<version>`, and lists releases from the
   `<artifact>-maven-metadata.xml` its latest release carries. A source directory serves each artifact of its group,
   at any version, from the files of `src/<artifact>/`. A path starting with `~/` is relative to the user's home,
   any other path to the kitchen. Entries are tried in order.
3. Without a `cook.toml`, the engine resolves from `github:lmarqs/the-cook`, where this repository releases its
   cookbook.
4. `cook.lock` records the sha256 of the artifact's sources: each path and its content, in path order. A different
   hash under a version the lock records is shown as a changed source before the diff.
5. Coordinates name exact releases. Branches, commits, snapshots and version ranges are refused.

## Reasons

- **1.** The jar carries the sources the engine compiles, and Maven-layout repositories are what existing tools
  publish to and hosts serve, so one resolution path covers public, private and local cookbooks. The version comes
  from the release, so no commit exists only to change a version number. The trade is a build to package a cookbook,
  though a jar of files needs only the packaging steps every build tool has.
- **2.** Listing repositories in the kitchen keeps the sources a repository trusts in one reviewed file. A `~/` path
  lets a committed `cook.toml` name Maven local, `~/.m2/repository`, where a developer publishes their own cookbooks,
  without writing a personal home directory into the file. The trade is an entry that resolves differently on each
  machine. A source directory entry lets a cookbook's author cook an edit without building or publishing it. The trade
  is an entry that ignores the version, so the lock shows each edit as a changed source under the same version.
  Serving `src/<artifact>/` keeps the cookbook's tests, under `test/`, and its build files out of every artifact, so an
  edit cooks exactly what a release of it would hold. The trade is one more directory level in every cookbook path.
  A GitHub release asset downloads anonymously at a URL built from the version, so a `github:` entry finds a
  jar with no index, and reads its listing through the Maven metadata format `update` already reads
  ([0032](0032-recipe-updates.md)). It uses download URLs, not the REST API, so a kitchen needs no token and spends
  no API quota. The trades are a listing only as current as the latest release, and a download limit GitHub does not
  publish.
- **3.** A kitchen without a `cook.toml` cooks this repository's cookbook with no setup, and the commands
  `cook --help` prints work as written. This repository releases on GitHub because Maven Central's publishing limits
  can refuse a release and need a signing key and Portal credentials, while a GitHub release needs only the token the
  workflow has. The trades are a kitchen that depends on GitHub.com, as installing Cook already does
  ([0017](0017-engine-version.md)), and the releases before 12.2.0, which only Maven Central holds
  ([cookbook](../../cookbook/README.md#units)).
- **4.** A jar's bytes identify more than the artifact: two builds of one release can differ in bytes. A hash of the
  artifact's sources identifies exactly what the engine compiles. A repository other than Maven Central, such as a
  GitHub release whose asset is uploaded again, can replace a jar under its version, so comparing that hash with the
  lock is how a repository notices that a release's content changed. The trade is a hash that no other tool computes.
- **5.** A branch or a range selects different content over time under the same coordinates, so the lock could not
  say which content produced the ownership it records ([0025](0025-recipes.md)).

## Rejected alternatives

### Git tags as releases

At its best, an author releases with a tag on the repository that already holds the source, with no build and no
package hosting. It loses because it is a second resolution path beside Maven-layout repositories, with a tag naming
rule per cookbook directory, a fetch cache and a dependency on `git` on every machine, while Maven Central and Maven
local already serve versioned sources to anyone who publishes a jar.

### A Gradle plugin that packages a cookbook

At its best, a cookbook author applies one plugin and every artifact is packaged and published the same way. It loses
because an artifact's jar is files under a path with a POM, which a build writes in a few lines, while a published
plugin is a second release to version, sign and keep working across Gradle versions, and ties cookbook authors to
Gradle.

### A branch or commit as the version

At its best, an author shares work without a release step. It loses because a branch moves under the same name, and a
raw commit says nothing about compatibility; decision 5 keeps coordinates to releases the author chose.

### Maven Central by default

At its best, Maven Central keeps every version in one place, never replaces one, and every Maven-aware tool reads it.
It loses because its monthly publishing limits can refuse a release, and publishing there needs a signing key and
Portal credentials that a GitHub release does not.

### A Maven repository rebuilt on the project's GitHub Pages site

At its best, the site serves a full Maven layout that a plain `https://` entry reads. It loses because the site has to
rebuild that layout from every release on each deploy, and a release reaches kitchens only once the site deploys.

### A `maven` branch served from raw.githubusercontent.com

At its best, a branch holds the Maven layout, and one commit per release publishes it. It loses because GitHub
tightened unauthenticated limits on `raw.githubusercontent.com` in 2025 without publishing them, so a kitchen
without a token could be refused at a limit nobody can plan for.

### GitHub Packages

At its best, a cookbook's packages live beside its repository, with no other account. It loses because its Maven
registry needs an access token even for public packages, so every kitchen cooking a public cookbook would need
credentials.

### A recipe at an `https://` URL

At its best, sharing a recipe is serving one file, with no cookbook or build. It loses because a URL carries no
release identity: its short name would change with a version written into the URL, or two hosts serving one path
would collide under one name; each template the recipe references would be fetched one by one; and Maven-layout
repositories already distribute versioned sources, which a developer can publish to Maven local without hosting. A
recipe operand is therefore a path inside the repository or coordinates. A jar URL, such as a release asset's, loses
too: it carries no listing for `update` to find a newer release in, and it bypasses the repositories `cook.toml`
lists for review.

## Consequences

Costs:

- Publishing a cookbook needs a build that writes Maven-layout jars, such as the Gradle build
  [A cookbook outside this repository](../../cookbook/README.md#a-cookbook-outside-this-repository) shows.
- The lock's source hash differs from the jar sha256 that package tools report.

- `update` finds no newer release through a `github:` entry until a release carries the listing.

Must be maintained:

- The source hash definition.
- The release asset names of decisions 1 and 2, and the release workflow step that writes each listing from the
  releases before it.

## Confirmation

- Review a kitchen without a `cook.toml`: does `io.github.lmarqs.cookbook:hello:<version>#world` cook at the newest
  release?

- Review a jar republished under the same version in Maven local: does the next run show a changed source before the
  diff?
- Review a kitchen that lists `~/.m2/repository`: does a cookbook its developer published to Maven local cook by its
  coordinates?
- Review a kitchen that lists a cookbook's source directory: does an edit to one of its recipes cook on the next run
  with no build?
