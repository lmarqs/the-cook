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

  /** Chooses a newer release from all listings, unless any repository cannot list this artifact's releases. */
  ReleaseSelection selectRelease(Coordinates recorded, boolean major) {
    def listings = repositories.collectEntries { [(it): it.versions(recorded.group, recorded.artifact)] }
    def unlisted = listings.findAll { it.value == null }*.key

    if (unlisted) {
      return new ReleaseSelection(unlisted, null, null)
    }

    def releases = listings.values()
        .flatten()
        .findAll { version ->
          !version.contains('#') && Coordinates.valid("${recorded.group}:${recorded.artifact}:${version}")
        }
        .collect { recorded.at(it) }
        .findAll { Coordinates.compareVersions(it.version, recorded.version) > 0 }
        .sort { left, right -> Coordinates.compareVersions(right.version, left.version) }
    def newest = releases ? releases.first() : null
    def target = releases.find { major || compatible(recorded.version, it.version) }
    def newerOverall = !major && newest &&
        Coordinates.compareVersions(newest.version, target?.version ?: recorded.version) > 0 ? newest : null
    new ReleaseSelection([], target, newerOverall)
  }

  private static boolean compatible(String recorded, String candidate) {
    def currentParts = recorded.tokenize('.')*.toBigInteger()
    def candidateParts = candidate.tokenize('.')*.toBigInteger()
    currentParts[0] == candidateParts[0] &&
        (currentParts[0] > 0 || (currentParts[1] > 0 && currentParts[1] == candidateParts[1]))
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

  /** A known listing with an optional update target, or the repositories that cannot list, with neither release. */
  static final class ReleaseSelection {
    /** The repositories that hold the artifact but cannot list its releases; empty when the listing is known. */
    final List<CookbookRepository> unlisted
    /** The chosen newer coordinates, or `null` when there is no newer release in the requested range. */
    final Coordinates target
    /** A release above the selected target (or recorded version), available only outside the compatible range. */
    final Coordinates newestOverall

    private ReleaseSelection(List<CookbookRepository> unlisted, Coordinates target, Coordinates newestOverall) {
      this.unlisted = unlisted
      this.target = target
      this.newestOverall = newestOverall
    }

    boolean isKnown() {
      !unlisted
    }
  }
}

/** A repository that cookbook artifacts resolve from. Its `toString` names it in messages. */
interface CookbookRepository {
  /** The artifact that `coordinates` name, or `null` when this repository does not hold it. */
  CookbookArtifact artifact(Coordinates coordinates)

  /**
   * The releases listed for `group` and `artifact`, an empty list when this repository does not hold the artifact, or
   * `null` when it holds the artifact but cannot list its releases.
   */
  List<String> versions(String group, String artifact)
}
