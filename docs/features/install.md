---
description: Explains how to install, pin and upgrade Cook with Homebrew, mise or the release package.
audience: Whoever installs Cook, upgrades it or runs it in CI
---
# Install: `cook`

Cook runs as `cook` on `PATH`. Each [release](https://github.com/lmarqs/the-cook/releases) publishes a complete
package for Linux x64 and arm64 and macOS arm64, `the-cook-<version>-<platform>.tar.gz` with its `.sha256`, where the
platform is `linux-x64`, `linux-arm64` or `macos-arm64`. The package is a `jpackage` image that carries its own Java
runtime, so the machine needs no Java or Gradle. [0017](../adrs/0017-engine-version.md) records why Cook installs
this way.

## Homebrew

The formula lives in this repository, so tap it by URL once:

```sh
brew tap lmarqs/the-cook https://github.com/lmarqs/the-cook
brew install cook
cook --version
```

The formula names the latest release and the sha256 of each package; Homebrew refuses a download that does not match.
The release workflow updates [`Formula/cook.rb`](../../Formula/cook.rb) with every release.

## mise

[mise](https://mise.jdx.dev) installs the release package from GitHub and puts its launcher on `PATH`:

```sh
mise use github:lmarqs/the-cook
cook --version
```

## Release package

Download the package for the machine, check it against its `.sha256`, unpack the **whole** image and put its launcher
on `PATH`. On Linux x64:

```sh
v=<version>
curl -fsSLO "https://github.com/lmarqs/the-cook/releases/download/v$v/the-cook-$v-linux-x64.tar.gz"
curl -fsSLO "https://github.com/lmarqs/the-cook/releases/download/v$v/the-cook-$v-linux-x64.tar.gz.sha256"
sha256sum -c "the-cook-$v-linux-x64.tar.gz.sha256"
tar -xzf "the-cook-$v-linux-x64.tar.gz" -C ~/.local/share
export PATH="$HOME/.local/share/cook/bin:$PATH"
```

On macOS the image is `cook.app` and its launcher `cook.app/Contents/MacOS/cook`; check the package with
`shasum -a 256 -c`. The launcher finds its runtime inside the image, so copying the launcher alone does not run Cook.

## Pinning a release for a team

`cook.lock` records recipe versions, not the engine's. A team that wants every developer and CI on one release pins
it in its tool manager's committed file. With mise:

```sh
mise use github:lmarqs/the-cook@<version>
git add mise.toml
```

A pin change is a dependency change: review it like one.

## Upgrading

Upgrade with the installer that put `cook` there: `brew upgrade cook`, a new version in `mise.toml`, or another
release package. An engine upgrade does not move recipe versions, fetch cookbook updates or cook anything. Recipe
versions move on request under [update](update.md).

## Where Cook writes

`cook` writes into the directory it is called in, or the one `-C` names, wherever it is installed. The [CLI
reference](cli.md) defines cooking, discard and help, recipe operands and batch options.

## CI

A CI job that runs Cook installs it like a developer does, with the team's pinned version when there is one, and
fails when it cannot. Configured programs read their native files directly, so a job that only builds or tests needs
no Cook ([0010](../adrs/0010-dependence-on-the-binary.md)).

## Air-gapped machines

Transfer the complete package built for the machine's OS and architecture, including Java, with its `.sha256`, and
check it before unpacking.

## Security

A matching sha256 shows the package is the one the release published. It does not prove who built it or that it is
free of malicious code ([0021](../adrs/0021-release-trust.md)). Package verification is separate from recipe trust:
Groovy recipes are executable code, and batch approval is not a sandbox for recipe side effects.

## Source repository launcher

This repository's [`cook` script](../../cook) runs the local build, `cli/build/install/cook/bin/cook`, from the
directory it is called in, so that directory is the kitchen. Run it as `./cook` from the repository root. If the
local distribution is absent, it prints `Build the CLI first: ./gradlew cliBuild`.

## Troubleshooting

| Situation | Response |
| --- | --- |
| `brew install cook` finds no formula | Tap it by URL first: `brew tap lmarqs/the-cook https://github.com/lmarqs/the-cook` |
| A download does not match its `.sha256` | Do not unpack it; download it again from the release |
| No package for the platform | Run on Linux x64 or arm64, or macOS arm64 |
| A copied launcher cannot find its runtime | Unpack the complete image, including Java, and put its launcher directory on `PATH` |
| Developers and CI run different releases | Pin the release in the team's tool manager, such as `mise.toml` |
| This repository's script prints `Build the CLI first: ./gradlew cliBuild` | Run `./gradlew cliBuild`; see the [quick start](../../README.md#quick-start) |
