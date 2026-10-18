# Wrapper: `cook`

## Objective

**Problem.** Developers and CI need to run the same engine version, and a download must match the version the
repository reviewed before it runs.

**Goal.** The engine version is a repository decision: every machine runs the pinned platform package, verifies its
checksum before execution, and upgrades only through an explicit, reviewed change.

This page owns the `./cook` wrapper of the [intended architecture](../architecture.md#engine-distribution): its pin,
checksum verification, first-use download, cache and upgrades. This source repository's own [`cook` script](../../cook)
is a development launcher for a local build; see [source repository launcher](#source-repository-launcher).

## Overview

Each [release](https://github.com/lmarqs/the-cook/releases) publishes a `cook` [wrapper](glossary.md): a POSIX `sh`
script with the release's version, its download location and the sha256 of its Linux x64 and arm64 packages written
in it. A repository commits that file as its executable `./cook`. The wrapper selects the package for the machine's
architecture. Cook is JVM-based: the package is produced with `jpackage` and contains Java. The wrapper caches and
launches the complete package.

```sh
curl -fsSLO https://github.com/lmarqs/the-cook/releases/latest/download/cook
chmod +x cook
git add cook
```

The engine pin belongs in committed repository content, separate from [`cook.lock`](glossary.md), which records
recipe ownership.

## How it works

1. Select the package from `uname -s` and `uname -m`: `x86_64` or `amd64` is `linux-x64`, and `aarch64` or `arm64` is
   `linux-arm64`. Any other platform is refused, named, with exit status 1.
2. If `${XDG_CACHE_HOME:-$HOME/.cache}/the-cook/<version>/linux-<arch>/the-cook/bin/the-cook` exists, launch it.
3. Otherwise download `the-cook-<version>-linux-<arch>.tar.gz` from the release with `curl`, or `wget` when `curl` is
   absent, and compute its sha256 with `sha256sum`, or `shasum -a 256` when `sha256sum` is absent.
4. Refuse a download whose sha256 differs from the one in the wrapper: print the file and both hashes, exit with
   status 1, and cache nothing.
5. Unpack the verified package beside the cache and move it in with one rename, so the cache never holds a partial
   package, then launch it with the supplied CLI arguments.

First use of an uncached version needs access to the release, `tar`, one of `curl` and `wget`, and one of `sha256sum`
and `shasum`; without them the wrapper names the tool it needs and exits with status 1. Later use of the cached
package requires no download. The cache keeps each version and platform apart; a package built for one architecture
is not a substitute for another.

The wrapper passes its CLI arguments to the engine. The [CLI reference](cli.md) defines cooking, discard and help, recipe
operands, and batch options. The invocation directory is the destination for declared file
operations, regardless of the wrapper's location.

## One pin

The committed version and platform checksums identify the engine packages the repository intends to run. A
cached package is reused for that pin. Review changes to the wrapper and pin as executable code and dependency
changes.

A checksum detects whether a download matches the reviewed pin. It does not by itself authenticate a publisher.
[FR-26](../architecture.md#requirements) requires checksum pinning.

## Upgrading

Upgrade by replacing `./cook` with the wrapper of another release, which changes the version and its package
checksums together. Review the pin change; on the next use, the wrapper obtains the new package through the same
verified download/cache flow. An ordinary
invocation does not move the pin or select a newer engine release.

Changing the engine pin does not manage recipe versions, fetch cookbook updates, or automatically cook recipes into
project files. Cookbooks ship ingredients and recipes through their repositories and can have separate builds. Users
supply the recipe version to cook.

## CI

A CI job that invokes Cook uses the repository's committed wrapper and pin through the same verification flow as a
developer. A fresh machine needs the pinned package downloaded; a machine with that package cached can reuse it.
Configured programs consume their native files directly.

## Air-gapped machines

A locally cached pinned package can run without downloading again. A machine without the complete package needs
it transferred from elsewhere, with its checksum verified against the committed pin. Transfer the package including
Java and use one built for that machine's OS and architecture. Without the package or access to obtain it, the
wrapper cannot run the engine.

## Security

Review the wrapper, engine version, and checksums together. A checksum mismatch prevents the downloaded package
from running. Protect cached executable packages from unintended changes. Package verification is separate from
recipe trust: Groovy recipes are executable code, and batch approval is not a sandbox for recipe side effects.

## Source repository launcher

This repository's [`cook` script](../../cook) runs the local build, `cli/build/install/the-cook/bin/the-cook`, from the
directory it is called in, so that directory is the kitchen. If that local distribution is absent, it prints
`Build the CLI first: ./gradlew cliBuild`. The root README's [install section](../../README.md#install) describes
installing a release.

## Troubleshooting

| Situation | Required response |
| --- | --- |
| An uncached package cannot be downloaded | Obtain the pinned platform package; a cached complete package runs offline |
| A download does not match its checksum | Refuse execution; resolve the download or pin discrepancy |
| The wrapper names a tool it needs | Install `curl` or `wget`, and `sha256sum` or `shasum` |
| The wrapper has no package for the platform | Run on Linux x64 or arm64 |
| A copied launcher cannot find its runtime | Transfer the complete jpackage image, including Java |
| This repository's script prints `Build the CLI first: ./gradlew cliBuild` | The local CLI distribution is absent; run `./gradlew cliBuild`; see the [quick start](../../README.md#quick-start) |
