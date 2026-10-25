package thecook.infra

import org.tomlj.Toml
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookRepository

/**
 * A cookbook's source directory at a `file:` URI, where `cookbook.toml` names its `group`. It serves each artifact of
 * that group at any version from the files of its directory under `src/`, as they are, so an edit cooks with no build.
 * Hidden files are left out.
 */
final class CookbookDirectory implements CookbookRepository {
  final URI uri
  final String group
  private final File directory

  CookbookDirectory(URI uri) {
    this.uri = uri
    this.directory = new File(uri)
    def manifest = new File(directory, 'cookbook.toml')
    def toml = Toml.parse(manifest.toPath())

    if (toml.hasErrors()) {
      throw new IllegalArgumentException("Cannot read ${manifest}: ${toml.errors().first()}")
    }

    if (!toml.isString('group')) {
      throw new IllegalArgumentException("${manifest} must name the cookbook's group")
    }

    this.group = toml.getString('group')
  }

  /** Whether `directory` holds a cookbook's `cookbook.toml`. */
  static boolean isCookbook(File directory) {
    new File(directory, 'cookbook.toml').isFile()
  }

  CookbookArtifact artifact(Coordinates coordinates) {
    def name = coordinates.artifact
    def root = new File(directory, "src/${name}")

    if (coordinates.group != group || !root.isDirectory()) {
      return null
    }

    Map<String, byte[]> files = [:]
    read(root, '', files)
    new CookbookArtifact(coordinates, files)
  }

  List<String> versions(String group, String artifact) {
    group == this.group && new File(directory, "src/${artifact}").isDirectory() ? null : []
  }

  @Override String toString() {
    uri.toString()
  }

  private static void read(File folder, String prefix, Map<String, byte[]> files) {
    folder.listFiles()
        .findAll { !hidden(it.name) }
        .each { file ->
          if (file.isDirectory()) {
            read(file, "${prefix}${file.name}/", files)
          } else {
            files["${prefix}${file.name}".toString()] = file.bytes
          }
        }
  }

  private static boolean hidden(String name) {
    name.startsWith('.')
  }
}
