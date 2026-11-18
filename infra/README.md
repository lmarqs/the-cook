# Infra

Infra connects the [core](../core/README.md) to the places cookbook releases come from. It implements the core's
`CookbookRepository`, and the CLI calls it to build the repositories for a kitchen.

- `CookToml.repositories` reads the `repositories` list of the kitchen's `cook.toml`, or uses `github:lmarqs/the-cook`
  when the kitchen has no `cook.toml` or it lists no `repositories`. An entry is `github:<owner>/<repo>`, an
  `https://` URL or a path to a Maven-layout repository, or a path to a cookbook's source directory, the one holding
  `cookbook.toml`. A path starting with `~/` is relative to the user's home, so `~/.m2/repository` reads Maven local;
  any other path is relative to the kitchen.
- `GithubReleaseRepository` reads an artifact's jar from
  `https://github.com/<owner>/<repo>/releases/download/v<version>/<artifact>-<version>.jar`, and its versions from
  `<artifact>-maven-metadata.xml` on the repository's latest release. It holds no artifact of another group.
- `MavenRepository` reads an artifact's jar from a Maven-layout repository over `https://` or from a path.
- `CookbookDirectory` reads an artifact of a cookbook's source directory from the files of `src/<artifact>/`, at any
  version and with no build, leaving out hidden files.

`./gradlew check` runs `CookTomlTest`, which resolves from jars built in the test, and `GithubReleaseRepositoryTest`,
which reads release assets laid out in a temporary directory.
