# Infra

Infra connects the [core](../core/README.md) to the places cookbook releases come from. It implements the core's
`CookbookRepository`, and the CLI calls it to build the repositories for a kitchen.

- `CookToml.repositories` reads the `repositories` list of the kitchen's `cook.toml`, or uses Maven Central when the
  kitchen has none. An entry is an `https://` URL or a path to a Maven-layout repository, or a path to a cookbook's
  source directory, the one holding `cookbook.toml`. A path starting with `~/` is relative to the user's home, so
  `~/.m2/repository` reads Maven local; any other path is relative to the kitchen.
- `MavenRepository` reads an artifact's jar from a Maven-layout repository over `https://` or from a path.
- `CookbookDirectory` reads an artifact of a cookbook's source directory from the files of its top-level directory,
  at any version and with no build, leaving out hidden files and `build`.

`./gradlew check` runs `CookTomlTest`, which resolves from jars built in the test.
