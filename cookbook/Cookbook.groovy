package thecook

/** This cookbook's artifacts, published into a test kitchen through `Kitchens` at version `0.1.0`. */
class Cookbook {
  /** The cookbook source directory, `cookbook/` in this repository. */
  static final File COOKBOOK = new File(System.getProperty('cook.cookbook'))

  /** Publishes `cookbook/<name>/` as `io.github.lmarqs.cookbook:<name>:0.1.0`, with `extra` files beside its own. */
  static File publishDirectory(File kitchen, String name, Map<String, Object> extra = [:]) {
    Kitchens.publishDirectory(kitchen, "io.github.lmarqs.cookbook:${name}:0.1.0", new File(COOKBOOK, name), extra)
  }

  /** The sha256 `cook.lock` records for the artifact `cookbook/<name>/`. */
  static String sourcesSha256(String name) {
    Kitchens.sourcesSha256(new File(COOKBOOK, name))
  }
}
