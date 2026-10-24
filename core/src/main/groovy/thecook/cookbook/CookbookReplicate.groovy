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

  /** Replays lock names, coordinates or paths; only `Proposal.commit` writes the proposed changes. */
  Proposal propose(List<String> operands) {
    if (!operands) {
      throw new IllegalArgumentException(
      Diagnostic.text('replicate needs a recipe', null, 'name a recipe from cook.lock'))
    }

    def kitchen = destination.toPath().toRealPath()
    def lock = CookLock.parse(new Draft(destination).read(CookLock.PATH))
    Set<String> replicated = []
    List<RecipeRequest> requests = operands.collect { operand ->
      def name = RecipeLoader.name(kitchen, operand)

      if (!replicated.add(name)) {
        throw new IllegalArgumentException(
        Diagnostic.text("${name} is replicated twice", null, 'replicate it once per invocation'))
      }

      if (!(name in lock.names)) {
        throw new IllegalArgumentException(Diagnostic.text(
        "${CookLock.PATH} lists no recipe ${name}; nothing was written",
        "replicate cooks only what ${CookLock.PATH} records for a recipe",
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
