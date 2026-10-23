---
objective:
  problem: >-
    Teams reuse setup across repositories: agent instructions and permissions, editor settings, ignore rules,
    build and CI files. Copied by hand, the copies drift. Those files are shared: a setup that replaces one whole loses
    what teammates and other tools wrote in it, and nobody can tell which part came from where or remove a part cleanly.
    Coding agents widen the problem. Several agents and people work in one repository at once, each agent harness reads
    its own files for instructions, permissions, hooks and skills, and each agent's worktree needs the same setup to run.
  goal: >-
    A setup learned once reaches every repository through one reviewed change, lives beside hand-written content
    in the same files, and is withdrawn the same way. Contributors who do not run Cook need nothing to contribute: their
    programs read the native files directly, and the lock and markers Cook leaves are reviewed like any other change.
    Every agent harness and every worktree reads the same reviewed setup from what its branch commits.
---
# the-cook

[![release](https://github.com/lmarqs/the-cook/actions/workflows/release.yml/badge.svg)](https://github.com/lmarqs/the-cook/actions/workflows/release.yml)
[![version](https://img.shields.io/github/v/release/lmarqs/the-cook)](https://github.com/lmarqs/the-cook/releases/latest)

<p align="center"><img src="docs/assets/the-lab.png" alt="the-cook: pixel-art lab where workers in yellow hazmat suits each work at their own station while a man in a black pork-pie hat watches from a glass office marked THE COOK" width="960"></p>

**The cook: cook the best product without any source of contamination.**

The lab is a repository worked by coding agents. Each agent works at its own station, a worktree of its own. Each
recipe keeps to the parts of a file it owns, and one person approves one diff before anything is written.

## Current implementation

This section describes what runs on `main`. The [architecture](docs/architecture.md) defines the intended design, and
[issue #15](https://github.com/lmarqs/the-cook/issues/15) tracks implementing it.

### Repositories worked by agents

The `agents`, `git`, `beads` and `ralph` artifacts set a repository up for several coding agents and people at once.
Each recipe's help, read with `./cook help <coordinates>`, says what it writes.

| Need | What runs | Read |
| --- | --- | --- |
| One set of instructions and permissions for every harness | `agents#seed` creates `AGENTS.md` and `.agents/permission/policy.yaml`. `agents#connect` renders the policy into Claude Code's `permissions` and Codex's sandbox and approval settings and `cook.rules`, with roles that narrow it for Claude Code | [agents](cookbook/src/agents/README.md) |
| A fresh clone, a cloud session or CI that reads the setup without cooking | The `.claude/` and `.codex/` links into `.agents/` are committed, so a fresh checkout already holds each harness's settings, hooks and skills, and each harness reads `AGENTS.md` natively | [agents](cookbook/src/agents/README.md) |
| Context when a session starts | Both harnesses run `.agents/hooks/session-start`, which adds the output of each `session-start.d/*.sh` to the session; `beads#tracker` can write one that prints the Beads prime text | [beads](cookbook/src/beads/README.md) |
| A worktree for each agent | `git#worktrees` ignores `.worktrees/`, states the rule `.worktrees/<branch>` in `AGENTS.md` and seeds the `setup` and `teardown` scripts the team fills in. `agents#connect --worktree-setup=...` wires Claude Code's worktree hooks: a new worktree runs setup and is removed when setup fails, and removing one runs teardown and deletes its branch only once every commit on it has landed | [worktrees](cookbook/src/git/worktrees.groovy), [connect](cookbook/src/agents/connect.groovy) |
| Unattended loop runs | `ralph#ralph-tui` writes `.ralph-tui/bin/run`, which runs ralph-tui in `.worktrees/ralph/<name>` on the branch `ralph/<name>`, and `bin/clean`, which without `--force` refuses to delete commits that have not landed. With `--worker-check`, a task completes only once the team's check passes | [ralph](cookbook/src/ralph/README.md) |
| Output kept to one clone | With `--share=clone`, `ralph#ralph-tui` keeps ralph-tui's configuration out of commits and `beads#tracker` keeps the Beads issues out of them. `replicate` cooks ralph-tui's configuration again in a fresh clone from the source and arguments `cook.lock` records | [replicate](docs/features/replicate.md) |
| A variant for one branch | Cooking a recipe with other arguments, or editing a seeded file, on a branch changes committed files and `cook.lock`, so the variant reaches every worktree of that branch and merges with it | [the lock](docs/architecture.md#ownership-and-the-lock) |
| Drift caught in CI | `--check` exits 1 when cooking would change anything, such as a policy edited and not cooked | [batch options](docs/features/cli.md#batch-options-and-approval) |

Every change in this table goes through the review, the lock and the recovery that [What runs](#what-runs) lists.

### Quick start

With a Java 17 JDK and access to obtain Gradle and build dependencies, from the checkout root, build the CLI:

```sh
./gradlew cliBuild
```

Make a scratch kitchen whose `cook.toml` resolves from this checkout's cookbook sources:

```sh
kitchen=$(mktemp -d)
printf 'repositories = ["%s/cookbook"]\n' "$PWD" > "$kitchen/cook.toml"
```

Read the `agents` artifact's front page and one of its recipes, without running anything:

```sh
./cook -C "$kitchen" help io.github.lmarqs.cookbook:agents:0.0.0
./cook -C "$kitchen" help io.github.lmarqs.cookbook:agents:0.0.0#connect
```

Cook two recipes in it:

```sh
./cook -C "$kitchen" io.github.lmarqs.cookbook:agents:0.0.0#seed io.github.lmarqs.cookbook:agents:0.0.0#connect --harness=claude
```

Cook shows the diff of every file it would write, `cook.lock` included, and asks `Write these changes? [y/N]`. Answer
`y`: it prints `Created` for `AGENTS.md`, `.agents/README.md`, `.agents/permission/policy.yaml`, the shared
directories, the `agents-layout` skill, `.agents/adapters/claude/settings.json`, the `.claude/` links into `.agents/`,
`.gitignore` and `cook.lock`, then what to do next. Run the same command again: nothing changed, so it prints `Already cooked.` and asks nothing.

The repository's [`cook` script](cook) launches the local distribution, and the directory it runs from is the kitchen
Cook cooks in. `-C <dir>` or `--kitchen=<dir>` before the first recipe cooks in `<dir>` as if Cook had started there.
`./cook --help` prints the usage.

### What runs

`./cook` cooks recipes left to right in one invocation, each named by coordinates,
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
- `./cook discard [--dry-run] [--batch-approve] <recipe>...`, which removes what `cook.lock` lists for each recipe,
  without running it, through the same review: each section with its markers, each key, each whole file and link,
  deleting a file left empty, and the recipe's lock entry ([discard](docs/features/discard.md)).
- `./cook replicate [--dry-run] [--batch-approve] <recipe>...`, which cooks named recipes again from the sources and
  arguments recorded in `cook.lock`, including ignored output missing from a fresh clone
  ([replicate](docs/features/replicate.md)).
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
Beside `recipes/Notes.groovy`, which declares the model `acme.notes.Notes`, `recipes/notes.groovy` reads:

```groovy
@help('Adds a line to NOTES.md.')
import acme.notes.Notes

@ingredient(Notes) def notes

notes.add('Run the tests before committing.')
```

A path outside the kitchen, through a symbolic link too, is refused.

Of the [cookbook](cookbook/README.md), the `agents`, `git`, `beads` and `ralph` artifacts run; the other directories
are a design sketch that has not been compiled. `./gradlew -p cookbook publishToMavenLocal` packages the cookbook with
plain Gradle: a jar and a POM per directory under `cookbook/src/`. Packaging compiles nothing; the engine compiles an
artifact's sources when one of its recipes is cooked. `./gradlew -p cookbook test` runs the recipes' tests, under
`cookbook/test/`.

### Development tooling

`./gradlew check` includes `spotlessCheck`, which enforces the format of Groovy and Gradle (Groovy-Eclipse, [settings](gradle/spotless/greclipse.properties)) and plain-text files, with 2-space indentation per [`.editorconfig`](.editorconfig). `./gradlew spotlessApply` fixes violations. On first use the formatter downloads Groovy-Eclipse from the Eclipse mirror that `build.gradle` names, into `~/.gradle/caches/p2-data`; when that mirror is down, name another Eclipse mirror there. The Groovy formatter on Java 17 cannot parse the `!in` operator or a GString nested inside another; write `!(a in b)` and build such strings with `+`.

`./gradlew check` also lints Groovy and Gradle files with CodeNarc (`./gradlew codenarc`), using its `basic`, `braces`,
`imports`, `unused` and `formatting` rulesets ([rules](gradle/lint/codenarc.groovy)). Every `if`/`else`/`for`/`while`
body has braces, and Groovy-Eclipse moves braced bodies onto their own line. A statement with a block, such as `if`,
`for` or `try`, has a blank line before and after it, except at the start or end of its own block
([rule](gradle/lint/BlankLinesAroundBlocksRule.groovy)). A chain of more than two method calls puts each call on its own
line ([rule](gradle/lint/ChainedCallsRule.groovy)). Both cover `cookbook/`, the uncompiled sketches included. A line
over 120 characters inside a `'''` string, such as a Markdown table row in `@help`, continues on the next source line
after a trailing `\`, which leaves the string's value unchanged.

`docs/` is its own build. `./gradlew -p docs check`, which `./gradlew check` includes, fails on a relative link in
`docs/` that points to a missing file or heading anchor; [`docs/build.gradle`](docs/build.gradle) defines the check.

See [the core implementation](core/README.md) for how the engine cooks a recipe and how it differs from the
intended architecture.
[Infra](infra/README.md) reads the kitchen's `cook.toml` and fetches cookbook releases from Maven-layout repositories;
the CLI hands its repositories to the core.

## Read next

| To learn | Read |
| --- | --- |
| Why the project exists, what it is trying to achieve, and the tenets behind every decision | [Philosophy](docs/philosophy.md) |
| The intended design: responsibilities, operations, commands and distribution | [Architecture](docs/architecture.md) |

The same pages, as of the latest release, are published at [lmarqs.github.io/the-cook](https://lmarqs.github.io/the-cook/).

## Install

Each [release](https://github.com/lmarqs/the-cook/releases/latest) publishes Cook for Linux x64 and arm64 and macOS
arm64 as `the-cook-<version>-<platform>.tar.gz`, where the platform is `linux-x64`, `linux-arm64` or `macos-arm64`: a
complete package with its own Java runtime. It also publishes the `cook` wrapper, and each file has a `.sha256`. Commit the wrapper to pin the engine version for a repository:

```sh
curl -fsSLO https://github.com/lmarqs/the-cook/releases/latest/download/cook
chmod +x cook
./cook --help
git add cook
```

On first use the wrapper downloads the package for the machine, refuses it unless its sha256 matches the one written
in the wrapper, and caches it under `${XDG_CACHE_HOME:-~/.cache}/the-cook`. Later runs start the cached package. To
upgrade, commit the wrapper of another release ([wrapper](docs/features/wrapper.md)).

To run a package without the wrapper, unpack the **whole** image and run its launcher from the repository you want to
cook, or name the repository with `-C`. On Linux the launcher is `cook/bin/cook`, on macOS
`cook.app/Contents/MacOS/cook`:

```sh
v=<version>
curl -fsSLO "https://github.com/lmarqs/the-cook/releases/download/v$v/the-cook-$v-linux-x64.tar.gz"
tar -xzf "the-cook-$v-linux-x64.tar.gz"
cook/bin/cook -C /path/to/repository "io.github.lmarqs.cookbook:agents:$v#connect" --harness=claude
```

[mise](https://mise.jdx.dev) installs a release package and puts its `cook` launcher on `PATH`:

```sh
mise use github:lmarqs/the-cook@<version>
cook --help
```

Without a `cook.toml`, coordinates such as `io.github.lmarqs.cookbook:agents:<version>#connect` resolve from Maven
Central.

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

## License

the-cook is licensed under the [Apache License 2.0](LICENSE).
