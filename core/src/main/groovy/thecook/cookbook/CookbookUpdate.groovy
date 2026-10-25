package thecook.cookbook

import thecook.model.Diagnostic

/** Selects newer releases for the recipes and arguments recorded in `cook.lock`. */
final class CookbookUpdate {
  private final File destination
  private final CookbookRepositories repositories

  CookbookUpdate(File destination, CookbookRepositories repositories) {
    this.destination = destination
    this.repositories = repositories
  }

  /**
   * Plans updates in operand order, or lock order without operands, omitting path recipes. Only `major` permits
   * crossing the compatible range. A path recipe or an unknown release listing is refused when its recipe was named
   * explicitly.
   */
  List<RecipeUpdate> plan(List<String> operands, boolean major = false) {
    def kitchen = destination.toPath().toRealPath()
    def recipes = LockedRecipe.select(readLock(), kitchen, operands, 'can be updated')
    def unversioned = recipes.find { !Coordinates.valid(it.source) }

    if (operands && unversioned) {
      throw new IllegalArgumentException(Diagnostic.text("${unversioned.name} has no version to update", null,
      'cook it again from its path'))
    }

    recipes.findAll { Coordinates.valid(it.source) }.collect { recipe ->
      def source = Coordinates.parse(recipe.source)
      def update = new RecipeUpdate(recipe, source, repositories.selectRelease(source, major))

      if (operands && !update.known) {
        throw new IllegalArgumentException(Diagnostic.text("${recipe.name} has no known releases; nothing was written",
        'its repository cannot list releases', 'use a repository that lists releases, or cook an exact version'))
      }

      update
    }
  }

  /** Cooks only moves, with their recorded arguments, in plan order; only `Proposal.commit` writes changes. */
  Proposal propose(List<RecipeUpdate> updates) {
    def requests = updates.findAll { it.to != null }.collect { it.recipe.replay(it.source.at(it.to).toString()) }
    new CookbookCook(destination, repositories).propose(requests)
  }

  private CookLock readLock() {
    def text = new Draft(destination).read(CookLock.PATH)

    if (text == null) {
      throw new IllegalArgumentException(Diagnostic.text("there is no ${CookLock.PATH}",
      "${CookLock.PATH} records the recipes to update", 'run in the kitchen that holds it'))
    }

    CookLock.parse(text)
  }
}

/**
 * One lock recipe's release selection: it moves when `to` is set, is current when known without `to`, and is unknown
 * when a repository cannot list its releases.
 */
final class RecipeUpdate {
  final LockedRecipe recipe
  final Coordinates source
  private final CookbookRepositories.ReleaseSelection selection

  RecipeUpdate(LockedRecipe recipe, Coordinates source, CookbookRepositories.ReleaseSelection selection) {
    this.recipe = recipe
    this.source = source
    this.selection = selection
  }

  String getName() {
    recipe.name
  }

  String getFrom() {
    source.version
  }

  /** The release to move to, or `null` when there is no newer release in the requested range. */
  String getTo() {
    selection.target?.version
  }

  /** A newer release outside the compatible range, when one is available. */
  String getNewestOverall() {
    selection.newestOverall?.version
  }

  boolean isKnown() {
    selection.known
  }

  /** The repositories that hold the artifact but cannot list its releases. */
  List<CookbookRepository> getUnlisted() {
    selection.unlisted
  }
}
