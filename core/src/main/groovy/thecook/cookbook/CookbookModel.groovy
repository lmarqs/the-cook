package thecook.cookbook

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 * The superclass the engine gives every `@Ingredient` model that declares none: the instance's `directory` and the
 * reads a model may make, from its own artifact and from the destination as the recipes before it left it.
 */
@CompileStatic
abstract class CookbookModel {
  /** The instance's base inside the destination, with `.` at its root. */
  String directory = '.'

  // Private and read statically: dynamic lookup from a model, in another package, would not find these fields.
  // CodeNarc misses the field access through `this.@`.
  @SuppressWarnings('UnusedPrivateField')
  private ModelFiles engineFiles

  @SuppressWarnings('UnusedPrivateField')
  private Draft engineDraft

  /** Places `model` at `directory`, reading `files` as its artifact's and `draft` as the destination. */
  @PackageScope
  static void attach(CookbookModel model, String directory, ModelFiles files, Draft draft) {
    model.directory = directory
    model.@engineFiles = files
    model.@engineDraft = draft
  }

  /** The text of the file `name` packaged in this model's artifact, or `null` when the artifact has none. */
  String resource(String name) {
    this.@engineFiles.resource(name)
  }

  /** Every file packaged under `dir` in this model's artifact, keyed by its path below `dir`. */
  Map<String, String> resources(String dir) {
    this.@engineFiles.resources(dir)
  }

  /** The destination file at `path` below `directory`, as the recipes before this one left it; `null` when absent. */
  String current(String path) {
    this.@engineDraft.read(Declarations.join(directory, path))
  }
}
