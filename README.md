---
description: Install Cook and try recipes that manage repository configuration.
audience: Developers familiar with Git who are evaluating Cook or trying it for the first time.
---

# the-cook

[![release](https://github.com/lmarqs/the-cook/actions/workflows/release.yml/badge.svg)](https://github.com/lmarqs/the-cook/actions/workflows/release.yml)
[![version](https://img.shields.io/github/v/release/lmarqs/the-cook)](https://github.com/lmarqs/the-cook/releases/latest)

<p align="center"><img src="docs/assets/the-lab.png" alt="the-cook: pixel-art lab where workers in yellow hazmat suits each work at their own station while a man in a black pork-pie hat watches from a glass office marked THE COOK" width="960"></p>

**The cook: cook the best product without any source of contamination.**

the-cook manages repository configuration for teams working with coding agents. Recipes are Groovy files that
write tools' native configuration files. Cook shows one diff for review before writing changes.

Reuse the same recipes across repositories, reviewing the changes each repository needs. The output remains plain
files that teammates, agents and CI can read without Cook. Recipes can also install skills: instructions that guide
an agent through work that needs judgment.

For developers familiar with Git, start below to install Cook and try a recipe. [Write a recipe](docs/write-a-recipe.md)
covers authoring; [Developing Cook](DEVELOPMENT.md) covers building and contributing code.
[Philosophy](docs/philosophy.md) explains why Cook exists, and [the site](https://lmarqs.github.io/the-cook/)
contains the docs of the latest release.

## Quick start

Set up repository instructions and a starter permission policy for Claude. With `cook` [installed](#install), run
these commands in a Git repository. The `seed` recipe creates the shared instructions and policy; `connect` connects
Claude to that setup. The starter policy grants no permissions.

```sh
agents=io.github.lmarqs.cookbook:agents:$(cook --version | cut -d' ' -f2)
cook $agents#seed $agents#connect --harness=claude
```

Cook shows the diff of every file it would write and asks `Write these changes? [y/N]`. Run it again and it prints
`Already cooked.`. [Get started](docs/get-started.md) walks through the result and the next steps.

## Install

Each [release](https://github.com/lmarqs/the-cook/releases/latest) ships Cook for Linux x64 and arm64 and macOS arm64,
with its own Java runtime. Pick one:

**Homebrew**

```sh
brew tap lmarqs/the-cook https://github.com/lmarqs/the-cook
brew install cook
```

**mise**

```sh
mise use github:lmarqs/the-cook
```

**Release package**: download `the-cook-<version>-<platform>.tar.gz` for `linux-x64`, `linux-arm64` or `macos-arm64`,
check it against its `.sha256`, unpack the **whole** image and put its launcher on `PATH`:

```sh
v=<version>
curl -fsSLO "https://github.com/lmarqs/the-cook/releases/download/v$v/the-cook-$v-linux-x64.tar.gz"
curl -fsSLO "https://github.com/lmarqs/the-cook/releases/download/v$v/the-cook-$v-linux-x64.tar.gz.sha256"
sha256sum -c "the-cook-$v-linux-x64.tar.gz.sha256"
tar -xzf "the-cook-$v-linux-x64.tar.gz" -C ~/.local/share
export PATH="$HOME/.local/share/cook/bin:$PATH"
```

Then `cook --help`. A team that wants every machine and CI on one release pins it in `mise.toml`;
[install](docs/features/install.md) covers pinning, upgrades and macOS paths. Without a `cook.toml`, coordinates such
as `io.github.lmarqs.cookbook:agents:<version>#connect` resolve from Maven Central.

### From source

See [Build from source](DEVELOPMENT.md#build-from-source) for Java requirements, build commands and package checks.

## Current implementation

This section describes what runs on `main`. The [architecture](docs/architecture.md) defines the intended design, and
[issue #15](https://github.com/lmarqs/the-cook/issues/15) tracks implementing it. What each cookbook sets up is in
[the cookbook](cookbook/README.md#what-runs-today).

### What runs

The **kitchen** is the directory Cook configures: the current directory by default, or the directory selected with
`-C <dir>` / `--kitchen=<dir>`.

`cook` cooks recipes left to right in one invocation, each named by coordinates,
`group:artifact:version#recipe`, or by a `.groovy` path inside the kitchen:

- One review before any write: a `Changed source` notice for a release whose sources differ from what `cook.lock`
  recorded under the same version, each recipe's `@confirm` text, a unified diff and one question; `--batch-approve` to
  write without asking, and nothing written without a terminal unless it is given
  ([batch options and approval](docs/features/cli.md#batch-options-and-approval)).
- One line per changed path after the writes, `Would` lines under `--dry-run`, then each recipe's `@yield` text
  ([recipe arguments and ordering](docs/features/cli.md#recipe-arguments-and-ordering)).
- `--force` and `--skip` for a file a recipe writes from a template the first time it is cooked
  ([existing files](docs/features/cli.md#existing-files)).
- `cook.lock`, which the kitchen keeps and commits: each recipe's short name, source, the source's sha256, normalized
  supplied arguments and what it owns, so a later run withdraws what a recipe no longer declares
  ([the lock](docs/architecture.md#ownership-and-the-lock)).
- A failed write rolls back the writes already made, so the files are as they were, and prints the error.
- `cook check [<name>...]`, which cooks the named recipes, or every recipe in `cook.lock`, again from the sources and
  arguments it records, writes nothing and exits 1 when anything would change ([check](docs/features/cli.md#check)).
- `cook discard [--dry-run] [--batch-approve] <name>...`, which removes what `cook.lock` lists for each recipe,
  without running it, through the same review: each section with its markers, each key, each whole file and link,
  deleting a file left empty, and the recipe's lock entry ([discard](docs/features/discard.md)).
- `cook replicate [--dry-run] [--batch-approve] [<name>...]`, which cooks the named recipes, or every recipe in
  `cook.lock`, again from the sources and arguments it records, including ignored output missing from a fresh clone
  ([replicate](docs/features/replicate.md)).
- `cook update [--dry-run] [--batch-approve] [--major] [<name>...]`, which moves coordinate recipes in `cook.lock`
  to newer compatible releases with their recorded arguments; `--major` allows releases beyond that range
  ([update](docs/features/update.md)).
- `help` on an artifact, a recipe or a recipe path, read without running it.

Coordinates resolve from the first repository of the kitchen's `cook.toml` `repositories` list that holds them, and
from Maven Central when the kitchen has no `cook.toml`. An entry is an `https://` URL or a path to a Maven-layout
repository of jars, where `~/` starts from the user's home as in `~/.m2/repository`, or a path to a cookbook's source
directory, which serves its artifacts at any version as they are, with no build ([cookbook](cookbook/README.md)).
Arguments follow their recipe as `--name=value` or `--name value`, and a yes-or-no argument as a bare `--name` or
`--name=false`. A word that contains `/` or `:` or ends with `.groovy` starts the next recipe, so a value shaped like
one is written `--name=value`.

A URL is not a recipe operand; a recipe shared beyond one repository is published in a cookbook and cooked by its
coordinates ([a cookbook outside this repository](cookbook/README.md#a-cookbook-outside-this-repository)).

### Recipe paths

A `.groovy` path inside the kitchen is a recipe like any other. Its short name, in section markers and `cook.lock`, is
its path in the kitchen without `.groovy`, so moving the file renames the recipe; the lock records its path and the
sha256 of the recipe, the models beside it and its `@resource` files. It compiles against the models in its own
directory, the upper-case `.groovy` files there, as a recipe in an artifact compiles against that artifact's models.

A path outside the kitchen, including through a symbolic link, is refused.
[Write a recipe](docs/write-a-recipe.md) walks through a complete example and moves it into a cookbook other
repositories can use. See [the cookbook guide](cookbook/README.md) for available recipes, packaging and tests.

### Development tooling

See [Development tooling](DEVELOPMENT.md#development-tooling) for formatting, lint rules and documentation checks.

## Read next

| To learn | Read |
| --- | --- |
| How to cook a first recipe, step by step | [Get started](docs/get-started.md) |
| How to write a recipe of your own and move it into a cookbook | [Write a recipe](docs/write-a-recipe.md) |
| Why the project exists, who it is for, and what it is and is not | [Philosophy](docs/philosophy.md) |
| The tenets a proposal for Cook is judged against | [Contributing](docs/contributing.md) |
| The intended design: responsibilities, operations, commands and distribution | [Architecture](docs/architecture.md) |

## License

the-cook is licensed under the [Apache License 2.0](LICENSE).
