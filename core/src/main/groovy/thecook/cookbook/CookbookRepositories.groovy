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
    def listings = repositories.collect { it.versions(recorded.group, recorded.artifact) }

    if (listings.any { it == null }) {
      return new ReleaseSelection(false, null, null)
    }

    def releases = listings.flatten()
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
    new ReleaseSelection(true, target, newerOverall)
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

  /** A known listing with an optional update target, or an unknown listing with neither release populated. */
  static final class ReleaseSelection {
    final boolean known
    /** The chosen newer coordinates, or `null` when there is no newer release in the requested range. */
    final Coordinates target
    /** A release above the selected target (or recorded version), available only outside the compatible range. */
    final Coordinates newestOverall

    private ReleaseSelection(boolean known, Coordinates target, Coordinates newestOverall) {
      this.known = known
      this.target = target
      this.newestOverall = newestOverall
    }
  }
}
