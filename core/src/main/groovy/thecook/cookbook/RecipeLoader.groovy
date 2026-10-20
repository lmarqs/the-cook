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
  private final Map<ClassLoader, CookbookArtifact> modelArtifacts = [:]
  private final Map<ClassLoader, Path> modelDirectories = [:]

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
   * without their version, or the path's place in `kitchen` without `.groovy`.
   */
  static String name(Path kitchen, String operand) {
    Coordinates.written(operand) ?
        Coordinates.parse(operand).recipeName :
        relative(kitchen, operand).replaceAll(/\.groovy$/, '')
  }

  /**
   * What a model of `type` reads: its own artifact's packaged files, the files of its directory for a model beside a
   * recipe path, and `draft` as the current content.
   */
  ModelReads reads(Class type, Draft draft) {
    def artifact = modelArtifacts[type.classLoader]
    def directory = modelDirectories[type.classLoader]
    [
      resource: { String path -> directory ? directoryFile(directory, path) : artifact?.resource(path) },
      resources: { String path -> directory ? directoryFiles(directory, path) : artifact?.resources(path) },
      current: { draft.read(it) },
    ] as ModelReads
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
    new LoadedRecipe(coordinates.recipeName, coordinates.recipeAddress(recipe),
        artifact.sha256, source, recipe, models(coordinates), resources)
  }

  // A path's short name is its place in the kitchen, so moving the file renames the recipe.
  private LoadedRecipe fromPath(String operand) {
    def place = relative(kitchen, operand)
    def path = kitchen.resolve(place)

    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Recipe file not found: ${operand}")
    }

    if (!path.toRealPath().startsWith(kitchen)) {
      throw outsideTheKitchen('recipe')
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
    new LoadedRecipe(name(kitchen, operand), place, CookbookArtifact.sourcesSha256(sources), source,
        file.replaceAll(/\.groovy$/, ''), models(path.parent, siblings), resources)
  }

  /** The upper-case `.groovy` files in `directory`, the models of the recipes there, keyed by their kitchen path. */
  private Map<String, byte[]> modelFiles(Path directory) {
    List<Path> files = Files.list(directory).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) && model(it.fileName.toString()) }.toList()
    }
    files.collectEntries { model ->
      if (!model.toRealPath().startsWith(kitchen)) {
        throw outsideTheKitchen('model')
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
      throw outsideTheKitchen('resource')
    }

    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Resource file not found: ${kitchen.relativize(path).join('/')}")
    }

    if (!path.toRealPath().startsWith(kitchen)) {
      throw outsideTheKitchen('resource')
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
      throw outsideTheKitchen('recipe')
    }

    kitchen.relativize(path)
        .collect { it.toString() }
        .join('/')
  }

  private CookbookArtifact artifact(Coordinates coordinates) {
    artifacts.computeIfAbsent(coordinates.artifactAddress) { repositories.artifact(coordinates) }
  }

  private ClassLoader models(Coordinates coordinates) {
    models.computeIfAbsent(coordinates.artifactAddress) {
      def artifact = artifact(coordinates)
      def loader = CookbookCompiler.models(artifact)
      modelArtifacts[loader] = artifact
      loader
    }
  }

  private ClassLoader models(Path directory, Map<String, byte[]> files) {
    directoryModels.computeIfAbsent(directory) {
      Map<String, byte[]> byName = files.collectEntries { place, bytes -> [(place.tokenize('/').last()): bytes] }
      def loader = CookbookCompiler.models(new CookbookArtifact(null, byName))
      modelDirectories[loader] = directory
      loader
    }
  }

  // A model beside a recipe path reads the files of its directory as a model in a jar reads the jar's files.
  private String directoryFile(Path directory, String path) {
    def file = directory.resolve(path).normalize()
    file.startsWith(directory) && Files.isRegularFile(file) ? kitchenText(file) : null
  }

  private Map<String, String> directoryFiles(Path directory, String path) {
    def root = directory.resolve(path).normalize()

    if (!root.startsWith(directory) || !Files.isDirectory(root)) {
      return [:]
    }

    List<Path> files = Files.walk(root).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) }.toList()
    }
    files.collectEntries { [(root.relativize(it).join('/')): kitchenText(it)] }.sort()
  }

  private static IllegalArgumentException outsideTheKitchen(String what) {
    def message = Diagnostic.text("the ${what} is outside the kitchen", null, 'move it into the kitchen')
    new IllegalArgumentException(message)
  }

  private String kitchenText(Path file) {
    if (!file.toRealPath().startsWith(kitchen)) {
      throw outsideTheKitchen('model\'s file')
    }

    new String(Files.readAllBytes(file), 'UTF-8')
  }
}

/**
 * A resolved recipe: its short name for markers and `cook.lock`, the address the lock records as its source and the
 * review shows, the sha256 of its sources, its text and file name, the class loader its model imports resolve from,
 * and the text of each file its `@resource` lines name, keyed by the path as written.
 */
final class LoadedRecipe {
  final String name
  final String address
  final String sha256
  final String source
  final String fileName
  final ClassLoader models
  final Map<String, String> resources

  LoadedRecipe(String name, String address, String sha256, String source, String fileName, ClassLoader models,
  Map<String, String> resources) {
    this.name = name
    this.address = address
    this.sha256 = sha256
    this.source = source
    this.fileName = fileName
    this.models = models
    this.resources = resources
  }
}
