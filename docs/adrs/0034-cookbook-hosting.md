---
description: Records why this repository's cookbooks release as GitHub release assets that kitchens read by default.
audience: Whoever releases this repository's cookbook or changes where a kitchen fetches cookbooks from
---
# 0034. Cookbooks release as GitHub release assets, which kitchens read by default

People cook this repository's [cookbook](../features/glossary.md) from kitchens that name no source of their own,
and ask for its newer releases. Its maintainer releases it from one workflow, and a host whose free publishing can
stop, or a credential the workflow does not already hold, is one more way a release fails. Anyone can cook a release,
and find its newer releases, with no account and no setup, and a release reaches them whenever the workflow runs.

## Context

- Each release of this repository is a GitHub release tagged `v<version>`, and its workflow authenticates with the
  `GITHUB_TOKEN` GitHub Actions gives every run ([release workflow](../../.github/workflows/release.yml)).
- GitHub.com, checked 2026-11-18: an asset attached to a release of a public repository downloads without credentials
  from `https://github.com/<owner>/<repo>/releases/download/<tag>/<name>`, and
  `https://github.com/<owner>/<repo>/releases/latest/download/<name>` serves the asset of that name on the latest
  release. `gh release upload --clobber` replaces an asset under its name.
- GitHub's changelog of 2025-05-08,
  [Updated rate limits for unauthenticated requests](https://github.blog/changelog/2025-05-08-updated-rate-limits-for-unauthenticated-requests/),
  tightens the limits on cloning over HTTPS, the REST API and downloads from `raw.githubusercontent.com` without
  authentication, and publishes no numbers. It does not name release asset downloads.
- GitHub Packages' Maven registry needs an access token to install any package, public ones included.
- Maven Central tracks three monthly publishing metrics per organization, file count, release size and release
  count, against thresholds its Usage Center shows; past a 30-day grace period, a release that keeps an organization
  over a threshold is refused
  ([Maven Central publishing limits](https://central.sonatype.org/publish/maven-central-publishing-limits/), checked
  2026-11-18). Publishing there also needs a verified namespace, a signing key and Central Portal credentials.
- Maven Central holds this repository's cookbook artifacts, `io.github.lmarqs.cookbook:<artifact>`, from 2.0.0 to
  12.3.1. The GitHub releases v12.2.0, v12.3.0 and v12.3.1 carry each artifact's jar; earlier releases carry none,
  and 8.1.1 and 9.1.0 have no GitHub release.
- `update` lists releases from a Maven metadata listing ([0032](0032-recipe-updates.md)), and the jar at a version is
  all cooking reads ([0029](0029-cookbook-distribution.md)).

## Decision

1. Each release of this repository attaches to its GitHub release, for each released artifact,
   `<artifact>-<version>.jar`, `<artifact>-<version>.jar.sha256` and `<artifact>-maven-metadata.xml`, a Maven
   metadata listing of every version whose release carries the jar, this one included.
2. A `cook.toml` entry `github:<owner>/<repo>` reads the jar at
   `https://github.com/<owner>/<repo>/releases/download/v<version>/<artifact>-<version>.jar`, and lists versions from
   `https://github.com/<owner>/<repo>/releases/latest/download/<artifact>-maven-metadata.xml`.
3. Without a `cook.toml`, or with one that lists no `repositories`, the only repository is `github:lmarqs/the-cook`.
4. This repository publishes nothing to Maven Central. A kitchen that cooks a release Maven Central alone holds lists
   `https://repo.maven.apache.org/maven2/` in its `cook.toml`.

## Reasons

- **1, 2.** A release asset downloads anonymously at a URL built from the version, so a jar needs no index to be
  found, and the release workflow attaches assets with the `GITHUB_TOKEN` it already has. Each release rewrites the
  listing from the releases before it, and `releases/latest/download/` always serves the newest one, so `update`
  reads one file per artifact through the listing it already understands. The trade is a listing that is only as
  current as the latest release, and none at all until the first release that attaches one.
- **2.** The jar URLs are on `github.com`, not the REST API, whose unauthenticated limit and `raw.githubusercontent.com`
  are what GitHub tightened in 2025, so a kitchen reads them with no token and no API quota. The trade is a host whose
  download limits GitHub does not publish.
- **3.** A kitchen without a `cook.toml` cooks the official cookbook with no setup, and the commands `cook --help`
  prints work as written. The trade is that a kitchen depends on GitHub.com being reachable, as installing Cook from
  its release already does ([0017](0017-engine-version.md)).
- **4.** Maven Central's limits can refuse a release in a month the workflow publishes many files, and each release
  there needs a signing key and Portal credentials the workflow must hold. Without it, a release needs only GitHub
  and the `GITHUB_TOKEN`. The trade is the releases before 12.2.0, which reach a kitchen only from Maven Central.

## Rejected alternatives

### Keep publishing to Maven Central

At its best, Maven Central keeps every version in one place, never replaces one, and every Maven-aware tool reads it.
It loses because its monthly publishing limits can refuse a release, it needs a signing key and Portal credentials,
and a release reaches its mirrors only after a delay.

### A Maven repository rebuilt on the project's GitHub Pages site

At its best, the site serves a full Maven layout at `https://lmarqs.github.io/the-cook/maven/`, which a plain `https://`
entry reads. It loses because the site has to rebuild that layout from every release on each deploy, a release reaches
it only once the site deploys, and the default would depend on the site's build as well as the release.

### A `maven` branch served from raw.githubusercontent.com

At its best, a branch holds the Maven layout, and a commit per release publishes it with the token the workflow has. It
loses because GitHub tightened the unauthenticated limits on `raw.githubusercontent.com` in May 2025 without publishing
them, so a kitchen without a token could be refused at a limit nobody can plan for.

### GitHub Packages

At its best, a cookbook's packages live beside its repository, with no other account. It loses because its Maven
registry needs an access token even for public packages, so every kitchen cooking a public cookbook would need
credentials.

### A jar URL as the recipe operand

At its best, a person cooks a release asset by pasting its URL, with no `cook.toml` and no listing. It loses because a
URL carries no listing for `update` to choose a newer release from, the name `cook.lock` records would come from
parsing the URL, and the operand would bypass the repositories `cook.toml` lists for review
([0029](0029-cookbook-distribution.md)).

## Consequences

Costs:

- Versions before 12.2.0 have no jar on GitHub until their releases are given one, and 8.1.1 and 9.1.0 have no GitHub
  release at all. A kitchen whose `cook.lock` records one of them lists `https://repo.maven.apache.org/maven2/` in its
  `cook.toml`.
- `update` finds no newer release through `github:` until the first release that attaches the listings.
- A GitHub release asset can be replaced under its name; the lock's source hash shows that as a changed source
  ([0029](0029-cookbook-distribution.md), decision 4).

Must be maintained:

- The asset names and URLs of decisions 1 and 2, which every `github:` entry and every kitchen without a `cook.toml`
  depend on.
- The release workflow step that writes each listing from the releases before it.

## Confirmation

- `GithubReleaseRepositoryTest` reads a jar and a listing at the URLs of decision 2, and `CookTomlTest` reads a
  `github:` entry and the default of decision 3.
- `./gradlew -p cookbook check` runs `releaseAssetsTest`, which checks that the release assets are exactly each
  released jar and its sha256.
- Review a release: does it carry, for each released artifact, the jar, its `.sha256` and
  `<artifact>-maven-metadata.xml`, and no upload to Maven Central?
- Review a kitchen without a `cook.toml`: does `io.github.lmarqs.cookbook:hello:<version>#world` cook at the newest
  release?
