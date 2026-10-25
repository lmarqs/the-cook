package thecook.cookbook

import thecook.model.Diagnostic

/**
 * The repositories that cookbook artifacts resolve from, in the order they are tried: the first that holds an artifact
 * supplies it. `origin` names where the list came from in messages.
 */
final class CookbookRepositories {
  final List<CookbookRepository> repositories
  private final String origin

  CookbookRepositories(List<CookbookRepository> repositories, String origin) {
    this.repositories = repositories
    this.origin = origin
  }

  /** The artifact that `coordinates` name, read from the first repository holding it. */
  CookbookArtifact artifact(Coordinates coordinates) {
    for (CookbookRepository repository : repositories) {
      def artifact = repository.artifact(coordinates)

      if (artifact != null) {
        return artifact
      }
    }

    throw new IllegalArgumentException(
    Diagnostic.text("${coordinates.artifactAddress} not found in ${origin}", null,
    'publish it there, or list its repository in cook.toml'))
  }

  /** The newest release of `group:artifact` that any repository lists, or `null` when none lists a release. */
  String latest(String group, String artifact) {
    repositories.collectMany { it.versions(group, artifact) }
    .findAll { Coordinates.valid("${group}:${artifact}:${it}") }
    .max { a, b -> Coordinates.compareVersions(a, b) }
  }
}

/** A repository that cookbook artifacts resolve from. Its `toString` names it in messages. */
interface CookbookRepository {
  /** The artifact that `coordinates` name, or `null` when this repository does not hold it. */
  CookbookArtifact artifact(Coordinates coordinates)

  /** The releases of `group:artifact` this repository lists; none when it cannot list them. */
  default List<String> versions(String group, String artifact) {
    []
  }
}
