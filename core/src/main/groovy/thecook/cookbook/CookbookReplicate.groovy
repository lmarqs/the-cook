package thecook.cookbook

import thecook.model.Diagnostic

/** Cooks named recipes again from the sources and arguments recorded in `cook.lock`. */
final class CookbookReplicate {
  private final File destination
  private final CookbookRepositories repositories

  CookbookReplicate(File destination, CookbookRepositories repositories) {
    this.destination = destination
    this.repositories = repositories
  }

  /**
   * Replays recipes by their lock names, or every recipe the lock lists, in its order, when `operands` is empty; only
   * `Proposal.commit` writes the proposed changes.
   */
  Proposal propose(List<String> operands) {
    def kitchen = destination.toPath().toRealPath()
    def text = new Draft(destination).read(CookLock.PATH)

    if (!operands && text == null) {
      throw new IllegalArgumentException(Diagnostic.text("there is no ${CookLock.PATH}",
      "${CookLock.PATH} records the recipes to cook again", 'run in the kitchen that holds it'))
    }

    def lock = CookLock.parse(text)
    Set<String> replicated = []
    List<RecipeRequest> requests = (operands ?: lock.names as List).collect { operand ->
      def name = RecipeLoader.lockName(kitchen, operand)

      if (!replicated.add(name)) {
        throw new IllegalArgumentException(
        Diagnostic.text("${name} is named twice", null, 'name it once per invocation'))
      }

      if (!(name in lock.names)) {
        throw new IllegalArgumentException(Diagnostic.text(
        "${CookLock.PATH} lists no recipe ${name}; nothing was written",
        "only a recipe ${CookLock.PATH} records is cooked again from it",
        "check the recipe's name; ${CookLock.PATH} lists ${lock.names ? lock.names.join(', ') : 'none'}"))
      }

      def recorded = lock.arguments(name)

      if (recorded == null) {
        throw new IllegalArgumentException(Diagnostic.text("${name} has no recorded arguments in ${CookLock.PATH}",
        null, 'cook it once with its arguments'))
      }

      new RecipeRequest(lock.source(name), recorded.collectMany { option, values ->
        values.collect { Argument.inline(option, it) }
      }, true)
    }
    new CookbookCook(destination, repositories).propose(requests)
  }
}
