package thecook

import thecook.cookbook.Argument
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookCook
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository
import thecook.cookbook.Coordinates
import thecook.cookbook.Proposal
import thecook.cookbook.RecipeRequest

import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Cooks this repository's cookbook artifacts in a test kitchen: each artifact is published as a jar under the
 * kitchen's `repository/` directory at version `0.1.0`, and coordinates resolve from there.
 */
class Kitchens {
  /** The cookbook source directory, `cookbook/` in this repository. */
  static final File COOKBOOK = new File(System.getProperty('cook.cookbook'))

  /** The version every artifact is published at. */
  static final String VERSION = '0.1.0'

  /** Publishes `cookbook/<name>/` as `io.github.lmarqs.cookbook:<name>:0.1.0`, with `extra` files beside its own. */
  static File publishDirectory(File kitchen, String name, Map<String, Object> extra = [:]) {
    def directory = new File(COOKBOOK, name)
    Map<String, Object> files = files(directory).collectEntries { [it, new File(directory, it).bytes] }
    publish(kitchen, "io.github.lmarqs.cookbook:${name}:${VERSION}", files + extra)
  }

  /** The paths of every file under `directory`, relative to it, in path order. */
  static List<String> files(File directory) {
    List<String> paths = []
    directory.eachFileRecurse(groovy.io.FileType.FILES) {
      paths << directory.toPath()
          .relativize(it.toPath())
          .toString()
    }
    paths.sort()
  }

  /** Publishes `files`, each a path in the artifact mapped to its text or bytes, at `address`. */
  static File publish(File kitchen, String address, Map<String, Object> files) {
    def coordinates = Coordinates.parse(address)
    def jar = new File(kitchen, "repository/${coordinates.jarPath}")
    jar.parentFile.mkdirs()
    new JarOutputStream(new FileOutputStream(jar)).withCloseable { out ->
      files.each { path, content ->
        out.putNextEntry(new JarEntry("${coordinates.packagePath}${path}"))
        out.write(content instanceof byte[] ? content : content.toString().getBytes('UTF-8'))
        out.closeEntry()
      }
    }
    jar
  }

  /** The repositories that resolve the jars `publish` wrote under the kitchen. */
  static CookbookRepositories repositories(File kitchen) {
    def directory = new File(kitchen, 'repository')
    def jars = { Coordinates coordinates ->
      def jar = new File(directory, coordinates.jarPath)
      jar.isFile() ? CookbookArtifact.fromZip(coordinates, jar.bytes, coordinates.packagePath) : null
    } as CookbookRepository
    new CookbookRepositories([jars], directory.path)
  }

  /** Proposes the recipes `operands` names, each with its arguments, in one invocation, as `./cook` does. */
  static Proposal cook(File kitchen, Map<String, Map<String, List<String>>> operands) {
    new CookbookCook(kitchen, repositories(kitchen)).propose(operands.collect { operand, arguments ->
      request(operand, arguments)
    })
  }

  /** Proposes one recipe with its arguments. */
  static Proposal cook(File kitchen, String operand, Map<String, List<String>> arguments = [:]) {
    cook(kitchen, [(operand): arguments])
  }

  /** A request for `operand`, with each argument given inline as `--name=value`. */
  static RecipeRequest request(String operand, Map<String, List<String>> arguments) {
    new RecipeRequest(operand, arguments.collectMany { name, values ->
      values.collect { Argument.inline(name, it) }
    })
  }

  /** The sha256 `cook.lock` records for the artifact `cookbook/<name>/`, as docs/architecture.md defines it. */
  static String sourcesSha256(String name) {
    def directory = new File(COOKBOOK, name)
    def lines = files(directory).collect { "${sha256(new File(directory, it).bytes)}  ${it}\n" }
    sha256(lines.join().getBytes('UTF-8'))
  }

  /** Whether Git ignores `path` in the repository at `kitchen`, by its ignore files alone. */
  static boolean ignored(File kitchen, String path) {
    ['git', '-C', kitchen.path, 'check-ignore', '-q', '--no-index', path].execute().waitFor() == 0
  }

  private static String sha256(byte[] bytes) {
    MessageDigest.getInstance('SHA-256')
        .digest(bytes)
        .encodeHex()
        .toString()
  }
}
