package thecook.infra

import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookRepository
import thecook.cookbook.Coordinates

/**
 * The cookbook jars attached to the releases of a GitHub repository, one release `v<version>` per version, and the
 * `<artifact>-maven-metadata.xml` listing attached to its latest release.
 */
final class GithubReleaseRepository implements CookbookRepository {
  private static final String EMPTY_SHA256 = CookbookArtifact.sourcesSha256([:])

  private final String owner
  private final String repo
  private final URI base

  GithubReleaseRepository(String owner, String repo) {
    this(owner, repo, URI.create("https://github.com/${owner}/${repo}/releases/"))
  }

  GithubReleaseRepository(String owner, String repo, URI base) {
    this.owner = owner
    this.repo = repo
    this.base = base
  }

  CookbookArtifact artifact(Coordinates coordinates) {
    def jar = MavenRepository.read(base.resolve(
        "download/v${coordinates.version}/${coordinates.artifact}-${coordinates.version}.jar"))
    def artifact = jar == null ? null : CookbookArtifact.fromZip(coordinates, jar, coordinates.packagePath)
    // A release carries one jar per artifact, so a jar with no files below the group's package is another group's.
    artifact?.sha256 == EMPTY_SHA256 ? null : artifact
  }

  List<String> versions(String group, String artifact) {
    def metadata = MavenRepository.read(base.resolve("latest/download/${artifact}-maven-metadata.xml"))
    metadata == null || MavenRepository.parseGroupId(metadata) != group ? [] : MavenRepository.parseVersions(metadata)
  }

  @Override String toString() {
    "github:${owner}/${repo}"
  }
}
