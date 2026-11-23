---
description: Records why Cook installs on PATH through package and tool managers, and how a team pins the engine release.
audience: Whoever changes how Cook is installed, released or upgraded
---
# 0017. Cook installs on PATH, and a team pins its release with the tool manager it already uses

A newcomer should run Cook with the install command they already know, without Java, Gradle or a script of Cook's own in
the repository. A team whose developers and CI must run the same engine release needs a way to say which one, reviewed
like any other dependency change.

## Context

- The [architecture](../architecture.md#engine-distribution) retains JVM distribution using `jpackage`: a complete
  platform package containing Java ([0004](0004-runtime-dependencies.md)). The release workflow builds it on Linux x64
  and arm64 and macOS arm64 and publishes each image as `the-cook-<version>-<platform>.tar.gz` with its sha256.
- Because the package carries its own runtime, a package manager or tool manager installs it like any other
  self-contained binary: unpack it and put its launcher on `PATH`.
- Developers work in several repositories, and some teams already pin their tools in a committed file, such as
  `mise.toml`.
- Recipe ownership lives in the committed `cook.lock`, the only file of Cook's own in a destination
  ([0014](0014-directory-layout.md)).
- This source repository's own [`cook` script](../../cook) launches a locally built JVM distribution for development.

## Decision

1. Each release publishes the complete platform package for every supported platform, with its sha256.
2. People install `cook` on `PATH` through Homebrew, mise or the release package ([install](../features/install.md)).
   The Homebrew formula lives in this repository at [`Formula/cook.rb`](../../Formula/cook.rb), and the release
   workflow commits each new version and its package checksums to it.
3. Documentation and messages invoke Cook as `cook`.
4. A team that wants every machine and CI on one release pins it in its tool manager's committed file, such as
   `mise.toml`. Cook does not check which engine release runs it.
5. An upgrade is the installer's own: `brew upgrade cook`, a new version in `mise.toml`, or another release package.
   An engine upgrade does not rewrite project files, update cookbooks or choose a recipe version; requested recipe
   updates follow [0032](0032-recipe-updates.md).
6. The invocation's current working directory, or the directory `-C` names, is the destination, regardless of where
   the executable is installed.

## Reasons

- **1, 2.** A self-contained package needs no installer of Cook's own. Homebrew and mise download it, check it and
  put it on `PATH` the way people already install their tools. Keeping the formula in this repository needs no
  second repository or token; the cost is a two-line `brew tap` and a release commit on `main`.
- **3.** One spelling for every machine: examples copy and paste the same way whichever installer put `cook` there.
- **4.** A tool manager already records versions for a team's other tools, and its file is reviewed like any other
  change. A team that does not pin lets each person keep their own release, which the engine tolerates because
  `cook.lock` records recipe versions, not engine versions.
- **5.** Engine and cookbook releases evolve separately; moving one never moves the other.
- **6.** Locating an executable must not change output paths.

## Rejected alternatives

### A committed wrapper script in each repository

At its best, every clone carries its engine version and package checksums, and a newcomer needs nothing installed but
`curl`. It loses because Cook then maintains its own download, verify and cache script per platform, duplicating what
package managers do; each repository commits a second file of Cook's own beside `cook.lock`; and every example is
written for a path, `./cook`, that only works from the repository root.

### A separate Homebrew tap repository

At its best, `brew install lmarqs/tap/cook` is one line and the tap stays small. It loses because publishing to
another repository needs a token with push access stored as a secret, and a second repository to maintain.

### Container as the required entry point

At its best, an image carries the engine and its runtime together and suits an existing container-based CI workflow. It
loses because requiring it for every invocation adds a container runtime and working-directory integration to every
user's setup. The jpackage distribution supplies Java directly without that requirement.

## Consequences

Costs:

- A repository does not, by itself, say which engine release runs it; a team that needs that pins it in its tool
  manager.
- First install needs access to the release, through Homebrew, mise or a download.
- Maintainers distribute complete packages and checksums for the platforms they support, and keep the formula's
  release commit working.

Must be maintained:

- `Formula/cook.rb` names the latest release and its package checksums.
- Installed packages are protected from unintended changes.

## Confirmation

- Does `brew install cook` from the tap, `mise use github:lmarqs/the-cook` and the unpacked release package each put a
  `cook` on `PATH` that prints the release's version?
- After a release, does `Formula/cook.rb` on `main` name that release and the sha256 of each package?
- Does a `cook` installed anywhere on `PATH` write to the directory it is called in, or the one `-C` names?
