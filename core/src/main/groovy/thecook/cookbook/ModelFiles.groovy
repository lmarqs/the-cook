package thecook.cookbook

import thecook.model.Diagnostic

import java.nio.file.Files
import java.nio.file.Path

/** The files a model reads besides the draft: its artifact's packaged files, or those of its recipe's directory. */
interface ModelFiles {
  /** The text of the file at `path`, or `null` when there is none. */
  String resource(String path)

  /** Every file under the directory `path`, keyed by its path below it. */
  Map<String, String> resources(String path)
}

/**
 * The files of a recipe's directory in the kitchen, which a model beside a recipe path reads as a model in a jar reads
 * the jar's files. A file that resolves outside the kitchen, through a symbolic link, is refused.
 */
final class KitchenDirectory implements ModelFiles {
  private final Path kitchen
  private final Path directory

  KitchenDirectory(Path kitchen, Path directory) {
    this.kitchen = kitchen
    this.directory = directory
  }

  /** The refusal of a `what`, such as a recipe or a model, that resolves outside the kitchen. */
  static IllegalArgumentException outside(String what) {
    def message = Diagnostic.text("the ${what} is outside the kitchen", null, 'move it into the kitchen')
    new IllegalArgumentException(message)
  }

  String resource(String path) {
    def file = directory.resolve(path).normalize()
    file.startsWith(directory) && Files.isRegularFile(file) ? text(file) : null
  }

  Map<String, String> resources(String path) {
    def root = directory.resolve(path).normalize()

    if (!root.startsWith(directory) || !Files.isDirectory(root)) {
      return [:]
    }

    List<Path> files = Files.walk(root).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) }.toList()
    }
    files.collectEntries { [(root.relativize(it).join('/')): text(it)] }.sort()
  }

  private String text(Path file) {
    if (!file.toRealPath().startsWith(kitchen)) {
      throw outside('model\'s file')
    }

    new String(Files.readAllBytes(file), 'UTF-8')
  }
}
