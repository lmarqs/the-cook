package thecook.cookbook

import thecook.model.Diagnostic

/** Cooks named recipes again from the sources and arguments recorded in `cook.lock`. */
final class CookbookReplicate {
  private final File kitchen
  private final CookbookRepositories repositories

  CookbookReplicate(File kitchen, CookbookRepositories repositories) {
    this.kitchen = kitchen
    this.repositories = repositories
  }

  /**
   * Replays recipes by their lock names, or every recipe the lock lists, in its order, when `operands` is empty; only
   * `Proposal.commit` writes the proposed changes.
   */
  Proposal propose(List<String> operands) {
    def kitchenPath = kitchen.toPath().toRealPath()
    def text = new Draft(kitchen).read(CookLock.PATH)

    if (!operands && text == null) {
      throw new IllegalArgumentException(Diagnostic.text("there is no ${CookLock.PATH}",
      "${CookLock.PATH} records the recipes to cook again", 'run in the kitchen that holds it'))
    }

    def recipes = LockedRecipe.select(CookLock.parse(text), kitchenPath, operands, 'is cooked again from it')
    new CookbookCook(kitchen, repositories).propose(recipes*.replay())
  }
}
