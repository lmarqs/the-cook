# the-cook

[![release](https://github.com/lmarqs/the-cook/actions/workflows/release.yml/badge.svg)](https://github.com/lmarqs/the-cook/actions/workflows/release.yml)
[![version](https://img.shields.io/github/v/release/lmarqs/the-cook)](https://github.com/lmarqs/the-cook/releases/latest)

<p align="center"><img src="docs/assets/the-lab.png" alt="the-cook: pixel-art lab where workers in yellow hazmat suits each work at their own station while a man in a black pork-pie hat watches from a glass office marked THE COOK" width="960"></p>

**The cook: cook the best product without any source of contamination.**

People and coding agents work in your repository at once, each with their own editor, harness and working copy.
Onboard on day one: a fresh clone already carries how the project is built and checked and what an agent may run. Work
in parallel: every branch gets its own worktree with that setup. Make it yours: your own settings sit beside the
team's, never in a commit. Do it again: the same recipes cook the next repository. Kept by hand, that setup drifts
between tools, working copies and repositories. the-cook writes the decisions, made once, into the native files each
tool reads, touches only the parts each recipe owns, and writes nothing until one person approves one diff. Recipes are
Groovy, and the one your team is missing is two files you [write yourself](docs/write-a-recipe.md).
[Philosophy](docs/philosophy.md) says why it exists, and [the site](https://lmarqs.github.io/the-cook/) shows the
docs of the latest release.

## Quick start

[Get started](docs/get-started.md) walks through a first cook with real output. In short, with `cook`
[installed](#install), in a Git repository:

```sh
agents=io.github.lmarqs.cookbook:agents:$(cook --version | cut -d' ' -f2)
cook $agents#seed $agents#connect --harness=claude
```

Cook shows the diff of every file it would write and asks `Write these changes? [y/N]`. Run it again and it prints
`Already cooked.`. To cook this checkout's recipes instead of a release, see [core verification](core/README.md#verification).

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

The [jpackage task](cli/gradle/jpackage.gradle) builds the same self-contained package for the machine that runs it,
with a Java 17 JDK (including `jpackage`), and `jpackageArchive` packs it as the release's `.tar.gz` with its `.sha256`
in `cli/build/distributions`:

```sh
./gradlew jpackage
cli/build/jpackage/cook/bin/cook --help
```

The paths above are for Linux. The build defines `cook.app/Contents/MacOS/cook` on macOS and
`cook/cook.exe` on Windows; releases ship Linux x64 and arm64 and macOS arm64 packages.

`./gradlew jpackageTest` builds the image and tests the native launcher with Java and Gradle absent from `PATH`: help,
cooking coordinates and a recipe path in one invocation, the refusal to write without a terminal, a repeated run, `-C`
and a usage error. On Linux x64 and arm64 and macOS arm64 it also checks the archive: every entry under the image
directory, a launcher that runs once the archive is unpacked, and a matching `.sha256`.

For development, `./gradlew cliBuild` builds the regular JVM distribution used by this repository's `./cook` entry
point; that distribution requires Java 17 on the user's machine.

## Current implementation

This section describes what runs on `main`. The [architecture](docs/architecture.md) defines the intended design, and
[issue #15](https://github.com/lmarqs/the-cook/issues/15) tracks implementing it. What each cookbook sets up is in
[the cookbook](cookbook/README.md#what-runs-today).

### What runs

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
`recipes/Notes.groovy` declares the model `acme.notes.Notes`:

```groovy
package acme.notes

/** The team's notes, kept in NOTES.md. */
@Ingredient
class Notes {
  private final List<String> lines = []

  /** Adds a line to the notes. */
  void add(String line) {
    lines << line
  }

  /** NOTES.md, one bullet per line added. */
  @File(path = 'NOTES.md')
  String file() {
    lines ? lines.collect { "- ${it}\n" }.join() : null
  }
}
```

and `recipes/notes.groovy`, cooked with `cook recipes/notes.groovy`, reads:

```groovy
@help('Adds a line to NOTES.md.')
import acme.notes.Notes

@ingredient(Notes) def notes

notes.add('Run the tests before committing.')
```

A path outside the kitchen, through a symbolic link too, is refused.
[Write a recipe](docs/write-a-recipe.md) grows this example step by step, into a cookbook other repositories cook.

Of the [cookbook](cookbook/README.md), the `agents`, `git`, `docs`, `ralph` and `hello` artifacts run. `./gradlew -p cookbook publishToMavenLocal` packages the
cookbook with plain Gradle: a jar and a POM per directory under `cookbook/src/`. Packaging compiles nothing; the
engine compiles an artifact's sources when one of its recipes is cooked. `./gradlew -p cookbook test` runs the recipes'
tests, under `cookbook/test/`.

### Development tooling

`./gradlew check` includes `spotlessCheck`, which enforces the format of Groovy and Gradle (Groovy-Eclipse, [settings](gradle/spotless/greclipse.properties)) and plain-text files, with 2-space indentation per [`.editorconfig`](.editorconfig). `./gradlew spotlessApply` fixes violations. On first use the formatter downloads Groovy-Eclipse from the Eclipse mirror that [`thecook.lint.gradle`](gradle/build-conventions/src/main/groovy/thecook.lint.gradle) names, into `~/.gradle/caches/p2-data`; when that mirror is down, name another Eclipse mirror there. The Groovy formatter on Java 17 cannot parse the `!in` operator or a GString nested inside another; write `!(a in b)` and build such strings with `+`.

`./gradlew check` also lints Groovy and Gradle files with CodeNarc (`./gradlew codenarc`), using its `basic`, `braces`,
`imports`, `unused` and `formatting` rulesets ([rules](gradle/lint/codenarc.groovy)). Every `if`/`else`/`for`/`while`
body has braces, and Groovy-Eclipse moves braced bodies onto their own line. A statement with a block, such as `if`,
`for` or `try`, has a blank line before and after it, except at the start or end of its own block
([rule](gradle/lint/BlankLinesAroundBlocksRule.groovy)). A chain of more than two method calls puts each call on its own
line ([rule](gradle/lint/ChainedCallsRule.groovy)). Both cover `cookbook/`, the uncompiled sketches included. A line
over 120 characters inside a `'''` string, such as a Markdown table row in `@help`, continues on the next source line
after a trailing `\`, which leaves the string's value unchanged.

`docs/` is a project of the build. `./gradlew -p docs check`, which `./gradlew check` includes, fails on a relative link in
`docs/` that points to a missing file or heading anchor; [`docs/build.gradle`](docs/build.gradle) defines the check.

See [the core implementation](core/README.md) for how the engine cooks a recipe and how it differs from the
intended architecture.
[Infra](infra/README.md) reads the kitchen's `cook.toml` and fetches cookbook releases from Maven-layout repositories;
the CLI hands its repositories to the core.

## Read next

| To learn | Read |
| --- | --- |
| How to cook a first recipe, step by step | [Get started](docs/get-started.md) |
| How to write a recipe of your own and move it into a cookbook | [Write a recipe](docs/write-a-recipe.md) |
| Why the project exists, what it is trying to achieve, and the tenets behind every decision | [Philosophy](docs/philosophy.md) |
| The intended design: responsibilities, operations, commands and distribution | [Architecture](docs/architecture.md) |

## License

the-cook is licensed under the [Apache License 2.0](LICENSE).
