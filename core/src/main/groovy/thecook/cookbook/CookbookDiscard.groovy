package thecook.cookbook

import thecook.model.Diagnostic

/**
 * Discards recipes without evaluating them: withdraws everything `cook.lock` lists for each one, a marked section with
 * its markers, a key, a whole file or a link, and drops its entry. A file left empty is deleted, and so is the lock
 * once it lists no recipe. Earlier content is not restored; the kitchen's version control holds it.
 */
final class CookbookDiscard {
  private final File destination

  CookbookDiscard(File destination) {
    this.destination = destination
  }

  /** The changes that discard the recipes `operands` name as cooking does; only `Proposal.commit` writes them. */
  Proposal propose(List<String> operands) {
    def kitchen = destination.toPath().toRealPath()
    def draft = new Draft(destination)
    def lock = CookLock.parse(draft.read(CookLock.PATH))
    Set<String> discarded = []
    operands.each { operand ->
      def name = RecipeLoader.name(kitchen, operand)

      if (!discarded.add(name)) {
        throw new IllegalArgumentException(
        Diagnostic.text("${name} is discarded twice", null, 'discard it once per invocation'))
      }

      if (!(name in lock.names)) {
        throw new IllegalArgumentException(Diagnostic.text(
        "${CookLock.PATH} lists no recipe ${name}; nothing was written",
        "discard removes only what ${CookLock.PATH} records for a recipe",
        "check the recipe's name; ${CookLock.PATH} lists ${lock.names ? lock.names.join(', ') : 'none'}"))
      }

      lock.forget(name).each { it.withdraw(draft) }
    }

    if (lock.names) {
      draft.write(CookLock.PATH, lock.render())
    } else {
      draft.delete(CookLock.PATH)
    }

    new Proposal(draft, [], [], new SeedOutcomes())
  }
}
