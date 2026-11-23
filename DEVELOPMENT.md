---
description: Build Cook from source, verify changes and use the repository's formatting and lint tools.
audience: Contributors who know Git and Gradle and are new to building Cook.
---

# Developing Cook

Use Java 17 to build Cook, and run `./gradlew check cliBuild jpackageTest` before submitting a change.
This guide covers local builds, packaging and development tools. To install a release and try Cook, follow the
[quick start](README.md#quick-start).

## Build from source

The [jpackage task](cli/gradle/jpackage.gradle) builds a self-contained package for the machine that runs it,
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

## Development tooling

Tests that compare paths returned by Git with temporary fixture paths must account for symbolic links. Git resolves
the checkout's physical path, while JUnit's temporary directory can retain a symlink path on macOS. Derive the
expected discovered store from the fixture's `canonicalFile`; keep explicitly supplied paths as supplied. The
Beads Viewer test covers a symlinked checkout as well as a linked worktree so Linux runs catch this mismatch too.

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

See [the core implementation](core/README.md) for how Cook cooks a recipe and how it differs from the
intended architecture.
[Infra](infra/README.md) reads the kitchen's `cook.toml` and fetches cookbook releases from Maven-layout repositories;
the CLI hands its repositories to the core.

Run `git diff --check` after the build checks, then review the diff before committing.
