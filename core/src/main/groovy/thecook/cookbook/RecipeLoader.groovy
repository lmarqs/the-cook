package thecook.cookbook

import thecook.model.Diagnostic

import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves recipe operands for one invocation: coordinates from the kitchen's repositories, compiled against their
 * artifact's models, and a path inside the kitchen, compiled against the models in its directory. Each artifact and
 * directory has its models compiled once.
 */
final class RecipeLoader {
  private final Path kitchen
  private final CookbookRepositories repositories
  private final Map<String, CookbookArtifact> artifacts = [:]
  private final Map<String, ClassLoader> models = [:]
  private final Map<Path, ClassLoader> directoryModels = [:]

  RecipeLoader(File kitchen, CookbookRepositories repositories) {
    this.kitchen = kitchen.toPath().toRealPath()
    this.repositories = repositories
  }

  /** The recipe that `operand` names: coordinates when written as coordinates, otherwise a path in the kitchen. */
  LoadedRecipe load(String operand) {
    Coordinates.written(operand) ? fromCoordinates(Coordinates.parse(operand)) : fromPath(operand)
  }

  /**
   * The short name for markers and `cook.lock` of the recipe `operand` names, read from the operand alone: coordinates
   * without their version, a lock name already in that form, or the path's place in `kitchen` without `.groovy`.
   */
  static String name(Path kitchen, String operand) {
    if (operand ==~ /[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)*:[a-z][a-z0-9-]*#[a-z][a-z0-9-]*/) {
      return operand
    }

    Coordinates.written(operand) ?
        Coordinates.parse(operand).recipeName :
        relative(kitchen, operand).replaceAll(/\.groovy$/, '')
  }

  /**
   * `operand` as the lock name it is written as; refuses one that names the recipe another way, such as coordinates
   * with a version or a path with `.groovy`, so each recipe has one spelling.
   */
  static String lockName(Path kitchen, String operand) {
    def name = name(kitchen, operand)

    if (name != operand) {
      throw new IllegalArgumentException(
      Diagnostic.text("${operand} is not a name ${CookLock.PATH} writes", null, "write ${name}"))
    }

    name
  }

  private LoadedRecipe fromCoordinates(Coordinates coordinates) {
    def artifact = artifact(coordinates)
    def recipe = coordinates.recipe

    if (!recipe) {
      throw new IllegalArgumentException(Diagnostic.text("${coordinates} names an artifact, not a recipe", null,
      'name one: ' + artifact.recipes.collect { coordinates.recipeAddress(it) }.join(', ')))
    }

    def source = artifact.recipeSource(recipe)
    Map<String, String> resources = RecipeSource.resources(source, "${recipe}.groovy").collectEntries { path ->
      def text = artifact.resource(path)

      if (text == null) {
        throw new IllegalArgumentException("No file ${path} in ${coordinates.artifactAddress}")
      }

      [(path): text]
    }
    new LoadedRecipe(coordinates.recipeName, coordinates.recipeAddress(recipe), true,
        artifact.sha256, source, recipe, models(coordinates), artifact, resources)
  }

  // A path's short name is its place in the kitchen, so moving the file renames the recipe.
  private LoadedRecipe fromPath(String operand) {
    def place = relative(kitchen, operand)
    def path = kitchen.resolve(place)

    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Recipe file not found: ${operand}")
    }

    if (!path.toRealPath().startsWith(kitchen)) {
      throw KitchenDirectory.outside('recipe')
    }

    def bytes = Files.readAllBytes(path)
    def source = new String(bytes, 'UTF-8')
    def file = path.fileName.toString()
    Map<String, byte[]> sources = [(place): bytes]
    Map<String, String> resources = RecipeSource.resources(source, file).collectEntries { declared ->
      def resource = resource(path.parent, declared)
      def content = Files.readAllBytes(resource)
      sources[kitchen.relativize(resource).join('/')] = content
      [(declared): new String(content, 'UTF-8')]
    }
    def siblings = modelFiles(path.parent)
    sources.putAll(siblings)
    new LoadedRecipe(name(kitchen, operand), place, false, CookbookArtifact.sourcesSha256(sources), source,
        file.replaceAll(/\.groovy$/, ''), models(path.parent, siblings), new KitchenDirectory(kitchen, path.parent),
        resources)
  }

  /** The upper-case `.groovy` files in `directory`, the models of the recipes there, keyed by their kitchen path. */
  private Map<String, byte[]> modelFiles(Path directory) {
    List<Path> files = Files.list(directory).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) && model(it.fileName.toString()) }.toList()
    }
    files.collectEntries { model ->
      if (!model.toRealPath().startsWith(kitchen)) {
        throw KitchenDirectory.outside('model')
      }

      [(kitchen.relativize(model).join('/')): Files.readAllBytes(model)]
    }
  }

  // The rule CookbookArtifact applies to an artifact's files, so a directory and its jar hold the same models.
  private static boolean model(String fileName) {
    fileName.endsWith('.groovy') && Character.isUpperCase(fileName.charAt(0))
  }

  // A resource resolves from the recipe's directory and, like the recipe, stays inside the kitchen.
  private Path resource(Path directory, String declared) {
    def path = directory.resolve(declared).normalize()

    if (!path.startsWith(kitchen)) {
      throw KitchenDirectory.outside('resource')
    }

    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Resource file not found: ${kitchen.relativize(path).join('/')}")
    }

    if (!path.toRealPath().startsWith(kitchen)) {
      throw KitchenDirectory.outside('resource')
    }

    path
  }

  private static String relative(Path kitchen, String operand) {
    if (operand.contains('://')) {
      throw new IllegalArgumentException(
      Diagnostic.text('a URL is not a recipe', null,
      'list its repository in cook.toml and cook the recipe by its coordinates'))
    }

    def path = kitchen.resolve(operand).normalize()

    if (!path.startsWith(kitchen)) {
      throw KitchenDirectory.outside('recipe')
    }

    kitchen.relativize(path)
        .collect { it.toString() }
        .join('/')
  }

  private CookbookArtifact artifact(Coordinates coordinates) {
    artifacts.computeIfAbsent(coordinates.artifactAddress) { repositories.artifact(coordinates) }
  }

  private ClassLoader models(Coordinates coordinates) {
    models.computeIfAbsent(coordinates.artifactAddress) { CookbookCompiler.models(artifact(coordinates)) }
  }

  private ClassLoader models(Path directory, Map<String, byte[]> files) {
    directoryModels.computeIfAbsent(directory) {
      Map<String, byte[]> byName = files.collectEntries { place, bytes -> [(place.tokenize('/').last()): bytes] }
      CookbookCompiler.models(new CookbookArtifact(null, byName))
    }
  }
}

/**
 * A resolved recipe: its short name for markers and `cook.lock`, the address the lock records as its source and the
 * review shows, whether it is `released`, read by coordinates rather than from a path in the kitchen, the sha256 of its
 * sources, its text and file name, the class loader its model imports resolve from, the files its models read, and the
 * text of each file its `@resource` lines name, keyed by the path as written.
 */
final class LoadedRecipe {
  final String name
  final String address
  final boolean released
  final String sha256
  final String source
  final String fileName
  final ClassLoader models
  final ModelFiles modelFiles
  final Map<String, String> resources

  LoadedRecipe(String name, String address, boolean released, String sha256, String source, String fileName,
  ClassLoader models, ModelFiles modelFiles, Map<String, String> resources) {
    this.name = name
    this.address = address
    this.released = released
    this.sha256 = sha256
    this.source = source
    this.fileName = fileName
    this.models = models
    this.modelFiles = modelFiles
    this.resources = resources
  }

  /**
   * Runs the recipe with `arguments` against `draft`, which its models read as the recipes before it left the
   * destination, and returns what it declares; the draft does not change.
   */
  Evaluation evaluate(List<Argument> arguments, Draft draft) {
    RecipeRun.run(CookbookCompiler.recipe(source, fileName, models), name, arguments, resources, modelFiles, draft)
  }
}
