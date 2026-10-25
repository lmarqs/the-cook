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

    def recipes = LockedRecipe.select(CookLock.parse(text), kitchen, operands, 'is cooked again from it')
    new CookbookCook(destination, repositories).propose(recipes*.replay())
  }
}
