package thecook.infra

import org.tomlj.Toml
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository

import java.nio.file.Path
import java.util.regex.Pattern

/**
 * The kitchen's `cook.toml`, which lists the repositories cookbook artifacts resolve from, or the GitHub releases of
 * `lmarqs/the-cook` when the kitchen has none. An entry is `github:<owner>/<repo>`, the releases of that GitHub
 * repository, an `https://` URL or a path to a Maven-layout repository, or a path to a cookbook's source directory,
 * the one holding `cookbook.toml`. A path starting with `~/` is relative to the user's home, any other path to the
 * kitchen.
 */
final class CookToml {
  private static final String DEFAULT_OWNER = 'lmarqs'
  private static final String DEFAULT_REPO = 'the-cook'
  private static final String FORMS = 'an https:// URL, a path or github:<owner>/<repo>'
  private static final Pattern GITHUB = ~/^github:([A-Za-z0-9-]+)\/(?!\.\.?$)([A-Za-z0-9._-]+)$/

  private CookToml() {
  }

  /**
   * The repositories that `cook.toml` in `kitchen` lists, or the GitHub releases of lmarqs/the-cook without that file;
   * `~/` is `home`.
   */
  static CookbookRepositories repositories(File kitchen, File home) {
    def manifest = new File(kitchen, 'cook.toml')
    def github = [new GithubReleaseRepository(DEFAULT_OWNER, DEFAULT_REPO)] as List<CookbookRepository>
    def origin = "GitHub releases of ${DEFAULT_OWNER}/${DEFAULT_REPO}"

    if (!manifest.isFile()) {
      return new CookbookRepositories(github, "${origin}; no cook.toml in ${kitchen} lists other repositories")
    }

    def toml = Toml.parse(manifest.toPath())

    if (toml.hasErrors()) {
      throw new IllegalArgumentException("Cannot read ${manifest}: ${toml.errors().first()}")
    }

    if (!toml.contains('repositories')) {
      return new CookbookRepositories(github, "${origin}; ${manifest} lists no repositories")
    }

    if (!toml.isArray('repositories')) {
      throw misshapen(manifest)
    }

    def array = toml.getArray('repositories')
    List<CookbookRepository> repositories = (0..<array.size()).collect { index ->
      def entry = array.get(index)

      if (!(entry instanceof String)) {
        throw misshapen(manifest)
      }

      repository((String) entry, kitchen, home, manifest)
    }
    new CookbookRepositories(repositories, "the repositories in ${manifest}: ${repositories.join(', ')}")
  }

  private static IllegalArgumentException misshapen(File manifest) {
    new IllegalArgumentException(
        "${manifest}: repositories must be a list of https:// URLs, paths and github:<owner>/<repo>")
  }

  private static CookbookRepository repository(String entry, File kitchen, File home, File manifest) {
    if (entry.startsWith('github:')) {
      def matcher = GITHUB.matcher(entry)

      if (!matcher.matches()) {
        throw new IllegalArgumentException("${manifest}: repository ${entry} must be written github:<owner>/<repo>")
      }

      return new GithubReleaseRepository(matcher.group(1), matcher.group(2))
    }

    def uri = entry.contains('://') ? URI.create(slashed(entry)) : directory(entry, kitchen, home)

    if (!(uri.scheme in ['https', 'file'])) {
      throw new IllegalArgumentException("${manifest}: repository ${entry} must be ${FORMS}")
    }

    if (uri.scheme == 'file' && CookbookDirectory.isCookbook(new File(uri))) {
      return new CookbookDirectory(uri)
    }

    new MavenRepository(uri)
  }

  // A path entry's directory, from the user's home for `~/`, as shells read it, otherwise from the kitchen.
  private static URI directory(String entry, File kitchen, File home) {
    Path path = entry.startsWith('~/') ? home.toPath().resolve(entry.substring(2)) : kitchen.toPath().resolve(entry)
    def file = path.toAbsolutePath()
        .normalize()
        .toFile()
    URI.create(slashed(file.toURI().toString()))
  }

  private static String slashed(String uri) {
    uri.endsWith('/') ? uri : "${uri}/"
  }
}
