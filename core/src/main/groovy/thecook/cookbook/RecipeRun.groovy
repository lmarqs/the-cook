package thecook.cookbook

// Lower-case names would read as variables in Groovy expressions.
import thecook.cookbook.api.recipe.ingredient as Injected
import thecook.cookbook.api.recipe.param as Param
import thecook.cookbook.api.recipe.recipe as Self
import thecook.cookbook.api.recipe.resource as Resource
import thecook.cookbook.api.ingredient.Ingredient
import thecook.filesystem.TargetPath
import thecook.model.Diagnostic

import java.lang.reflect.Field

/**
 * Runs a compiled recipe: binds its `@param` fields from the arguments, sets its `@resource` fields and its `@recipe`
 * fields from its name and the arguments as bound, injects its ingredients, runs its body, then reads what the
 * ingredients declare.
 */
final class RecipeRun {
  private RecipeRun() { }

  /**
   * Runs `recipe` with `arguments` and returns what its ingredients declare, and the arguments given, normalized: each
   * param's first name with its values, in declaration order.
   * A bare `--name` is `true` for a yes-or-no param and an error for any other.
   * `resources` holds the text of each `@resource` path.
   * `name` is the recipe's own name, for its `@recipe` fields and markers. Each ingredient reads `files` as its
   * artifact's and `draft` as the destination. A model that declares anything on a new instance is refused.
   */
  static Evaluation run(CompiledRecipe recipe, String name, List<Argument> arguments, Map<String, String> resources,
      ModelFiles files, Draft draft) {
    def script = (Script) recipe.type.getDeclaredConstructor().newInstance()
    List<Field> fields = recipe.fields.collect { recipe.type.getDeclaredField(it) }
    def params = fields.findAll { it.getAnnotation(Param) }
    def names = params.collectMany {
      it.getAnnotation(Param)
          .value()
          .toList()
    }
    Map<String, List<String>> given = [:]
    def takes = names ? 'its arguments are ' + names.collect { "--${it}" }.join(', ') : 'it takes no arguments'
    def unknown = arguments*.name.find { !(it in names) }

    if (unknown) {
      throw new ArgumentBindingException("Unknown argument --${unknown}", takes, true)
    }

    try {
      params.each { field ->
        def bound = bind(field, arguments, recipe)
        set(script, field, bound.value)

        if (bound.given) {
          def declared = field.getAnnotation(Param)
          given[declared.value().first()] = bound.values
        }
      }
    } catch (IllegalArgumentException error) {
      throw new ArgumentBindingException(error.message, takes, false, error)
    }

    fields.findAll { it.getAnnotation(Resource) }.each { field ->
      if (!(field.type in [String, CharSequence, Object])) {
        throw new IllegalArgumentException(
        "${field.name} has the type ${field.type.simpleName}, which a resource cannot have")
      }

      set(script, field, resources[field.getAnnotation(Resource).value()])
    }
    fields.findAll { it.getAnnotation(Self) }.each { field ->
      if (!(field.type in [String, CharSequence, Object])) {
        throw new IllegalArgumentException(
        "${field.name} has the type ${field.type.simpleName}, which the recipe name cannot have")
      }

      def line = [name] + given.collectMany { option, values ->
        values.collect {
          "--${option}=${it}".toString()
        }
      }
      set(script, field, field.getAnnotation(Self).arguments() ? line.join(' ') : name)
    }
    def ingredients = fields.findAll { it.getAnnotation(Injected) }.collect { field ->
      def injected = field.getAnnotation(Injected)
      def instance = model(injected.value(), injected.at(), files, draft)
      set(script, field, instance)
      instance
    }
    script.run()
    ingredients*.getClass()
        .unique()
        .each { checkPristine(it, files, draft, name) }
    new Evaluation(Declarations.of(ingredients, name), given)
  }

  private static CookbookModel model(Class type, String at, ModelFiles files, Draft draft) {
    if (!type.getAnnotation(Ingredient) || !CookbookModel.isAssignableFrom(type)) {
      throw new IllegalArgumentException(
      Diagnostic.text("${type.simpleName} is not a model", 'a model class is marked @Ingredient'))
    }

    if (at != '.') {
      TargetPath.of(at)
    }

    def instance = (CookbookModel) type.getDeclaredConstructor().newInstance()
    CookbookModel.attach(instance, at, files, draft)
    instance
  }

  private static void checkPristine(Class type, ModelFiles files, Draft draft, String name) {
    def declared = Declarations.of([model(type, '.', files, draft)], name).all

    if (declared) {
      throw new IllegalArgumentException(Diagnostic.text("${declared.first()} on a new instance",
      'a model declares only what a recipe asks for'))
    }
  }

  /**
   * The value of `field`, its option, the values it was bound from, and whether they were given rather than defaulted.
   */
  private static Map bind(Field field, List<Argument> arguments, CompiledRecipe recipe) {
    def declared = field.getAnnotation(Param)
    def name = "--${declared.value().first()}"
    def given = arguments.findAll { it.name in declared.value() }
    boolean yesOrNo = field.type in [boolean, Boolean]
    // As in Go's flag package, a yes-or-no value comes only after `=`, so a bare --name means true whatever follows.
    def separate = given.find { it.form == Argument.Form.SEPARATE }

    if (yesOrNo && separate) {
      throw new IllegalArgumentException(Diagnostic.text("--${separate.name} takes no separate value", null,
      "write --${separate.name}=${separate.value}"))
    }

    if (!yesOrNo && given.any { it.form == Argument.Form.BARE }) {
      throw new IllegalArgumentException(Diagnostic.text("${name} needs a value", null, "write ${name}=<value>"))
    }

    List<String> values = given.collect { it.form == Argument.Form.BARE ? 'true' : it.value }
    boolean defaulted = !values

    if (defaulted) {
      if (!recipe.hasDefault(field.name)) {
        throw new IllegalArgumentException("Missing ${name}: ${declared.doc()}")
      }

      values = declared."default"().toList()
    }

    if (!declared.many() && values.size() > 1) {
      throw new IllegalArgumentException(Diagnostic.text("${name} takes one value", "it was given ${values.size()}"))
    }

    def choices = declared.choices().toList()
    values.each { value ->
      if (choices && !(value in choices)) {
        throw new IllegalArgumentException("${name}=${value} is not one of: ${choices.join(', ')}")
      }
    }

    if (declared.path()) {
      values = values.collect { inRepository(name, it) }
    }

    values = values.unique(false)

    if (declared.sorted()) {
      values = values.sort(false)
    }

    [value: convert(name, field.type, values), option: name, values: values, given: !defaulted]
  }

  private static String inRepository(String name, String value) {
    def next = 'name the file by its path from the repository root'
    def segments = value.split('/').findAll { it && it != '.' }

    if (!value.trim() || !segments) {
      throw new IllegalArgumentException(Diagnostic.text("${name} needs a path", null, next))
    }

    if (value.startsWith('/') || '..' in segments) {
      throw new IllegalArgumentException(Diagnostic.text("${name}=${value} is outside the repository",
      'a recipe reads and writes only inside the repository', next))
    }

    segments.join('/')
  }

  private static Object convert(String name, Class type, List<String> values) {
    if (type.array) {
      return values as String[]
    }

    if (Collection.isAssignableFrom(type)) {
      return values
    }

    String value = values ? values.first() : null

    if (value == null || type in [String, Object, CharSequence]) {
      return value
    }

    if (type in [boolean, Boolean]) {
      if (!(value in ['true', 'false'])) {
        throw new IllegalArgumentException("${name}=${value} must be true or false")
      }

      return value == 'true'
    }

    if (type in [int, Integer, long, Long]) {
      if (!(value ==~ /-?[0-9]+/)) {
        throw new IllegalArgumentException("${name}=${value} must be a whole number")
      }

      return type in [int, Integer] ? value.toInteger() : value.toLong()
    }

    throw new IllegalArgumentException("${name} has the type ${type.simpleName}, which a param cannot have")
  }

  private static void set(Script script, Field field, Object value) {
    field.accessible = true
    field.set(script, value)
  }
}

/** What a recipe declares, and the arguments it was given, each param's first name with its values. */
final class Evaluation {
  final Declared declared
  final Map<String, List<String>> arguments

  Evaluation(Declared declared, Map<String, List<String>> arguments) {
    this.declared = declared
    this.arguments = arguments
  }
}

/**
 * An argument rejected while binding params, before the recipe's body runs: the `reason`, and the arguments the recipe
 * `accepted`, which the message adds when `listAccepted`.
 */
final class ArgumentBindingException extends IllegalArgumentException {
  final String reason
  final String accepted

  ArgumentBindingException(String reason, String accepted, boolean listAccepted, Throwable cause = null) {
    super(listAccepted ? Diagnostic.text(reason, accepted) : reason, cause)
    this.reason = reason
    this.accepted = accepted
  }
}
