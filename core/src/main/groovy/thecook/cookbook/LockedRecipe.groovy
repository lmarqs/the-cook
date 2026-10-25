package thecook.cookbook

import thecook.model.Diagnostic

import java.nio.file.Path

/** A recipe `cook.lock` records: its lock name, the source it was cooked from and the arguments it was cooked with. */
final class LockedRecipe {
  final String name
  final String source
  private final Map<String, List<String>> arguments

  private LockedRecipe(String name, String source, Map<String, List<String>> arguments) {
    this.name = name
    this.source = source
    this.arguments = arguments
  }

  /**
   * The recipes `operands` name by their lock names, in operand order, or every recipe `lock` lists, in its order, when
   * `operands` is empty. `recordedFor` completes the reason a name the lock does not list is refused, as
   * `is cooked again from it`.
   */
  static List<LockedRecipe> select(CookLock lock, Path kitchen, List<String> operands, String recordedFor) {
    Set<String> named = []
    (operands ?: lock.names as List).collect { operand ->
      def name = RecipeLoader.lockName(kitchen, operand)

      if (!named.add(name)) {
        throw new IllegalArgumentException(
        Diagnostic.text("${name} is named twice", null, 'name it once per invocation'))
      }

      if (!(name in lock.names)) {
        throw new IllegalArgumentException(Diagnostic.text(
        "${CookLock.PATH} lists no recipe ${name}; nothing was written",
        "only a recipe ${CookLock.PATH} records ${recordedFor}",
        "check the recipe's name; ${CookLock.PATH} lists ${lock.names ? lock.names.join(', ') : 'none'}"))
      }

      new LockedRecipe(name, lock.source(name), lock.arguments(name))
    }
  }

  /** A request that cooks this recipe from `operand`, its recorded source unless given, with its recorded arguments. */
  RecipeRequest replay(String operand = source) {
    if (arguments == null) {
      throw new IllegalArgumentException(Diagnostic.text("${name} has no recorded arguments in ${CookLock.PATH}",
      null, 'cook it once with its arguments'))
    }

    new RecipeRequest(operand, arguments.collectMany { option, values ->
      values.collect { Argument.inline(option, it) }
    }, source)
  }
}
