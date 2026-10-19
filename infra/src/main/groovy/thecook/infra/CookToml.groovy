package thecook.infra

import org.tomlj.Toml
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository

import java.nio.file.Path

/**
 * The kitchen's `cook.toml`, which lists the repositories cookbook artifacts resolve from, or Maven Central when the
 * kitchen has none. An entry is an `https://` URL or a path to a Maven-layout repository, or a path to a cookbook's
 * source directory, the one holding `cookbook.toml`. A path starting with `~/` is relative to the user's home, any
 * other path to the kitchen.
 */
final class CookToml {
  static final URI CENTRAL = URI.create('https://repo.maven.apache.org/maven2/')

  private CookToml() {
  }

  /** The repositories that `cook.toml` in `kitchen` lists, or Maven Central without that file; `~/` is `home`. */
  static CookbookRepositories repositories(File kitchen, File home) {
    def manifest = new File(kitchen, 'cook.toml')
    def central = [new MavenRepository(CENTRAL)] as List<CookbookRepository>

    if (!manifest.isFile()) {
      return new CookbookRepositories(central, "Maven Central; no cook.toml in ${kitchen} lists other repositories")
    }

    def toml = Toml.parse(manifest.toPath())

    if (toml.hasErrors()) {
      throw new IllegalArgumentException("Cannot read ${manifest}: ${toml.errors().first()}")
    }

    if (!toml.contains('repositories')) {
      return new CookbookRepositories(central, "Maven Central; ${manifest} lists no repositories")
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
    new IllegalArgumentException("${manifest}: repositories must be a list of https:// URLs and paths")
  }

  private static CookbookRepository repository(String entry, File kitchen, File home, File manifest) {
    def (Path directory, String path) = base(entry, kitchen, home)
    def uri = directory.toUri().resolve(path.endsWith('/') ? path : "${path}/")

    if (!(uri.scheme in ['https', 'file'])) {
      throw new IllegalArgumentException("${manifest}: repository ${entry} must be an https:// URL or a path")
    }

    if (uri.scheme == 'file' && CookbookDirectory.isCookbook(new File(uri))) {
      return new CookbookDirectory(uri)
    }

    new MavenRepository(uri)
  }

  // A path entry's directory and its path from there: the user's home for `~/`, as shells read it, otherwise the
  // kitchen.
  private static Tuple2<Path, String> base(String entry, File kitchen, File home) {
    entry.startsWith('~/')
        ? new Tuple2<>(home.toPath().toAbsolutePath(), entry.substring(2))
        : new Tuple2<>(kitchen.toPath().toAbsolutePath(), entry)
  }
}
