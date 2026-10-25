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
   * crossing the compatible range. An unknown release listing is refused when its recipe was named explicitly.
   */
  List<RecipeUpdate> plan(List<String> operands, boolean major = false) {
    def kitchen = destination.toPath().toRealPath()
    def lock = readLock()
    Set<String> named = []
    def names = (operands ?: lock.names as List).collect { operand ->
      def name = RecipeLoader.lockName(kitchen, operand)

      if (!named.add(name)) {
        throw new IllegalArgumentException(
        Diagnostic.text("${name} is named twice", null, 'name it once per invocation'))
      }

      if (!(name in lock.names)) {
        throw new IllegalArgumentException(Diagnostic.text(
        "${CookLock.PATH} lists no recipe ${name}; nothing was written",
        "only a recipe ${CookLock.PATH} records can be updated",
        "check the recipe's name; ${CookLock.PATH} lists ${lock.names ? lock.names.join(', ') : 'none'}"))
      }

      if (operands && !Coordinates.valid(lock.source(name))) {
        throw new IllegalArgumentException(Diagnostic.text("${name} has no version to update", null,
        'cook it again from its path'))
      }

      name
    }
    names.findAll { Coordinates.valid(lock.source(it)) }.collect { name ->
      def source = Coordinates.parse(lock.source(name))
      def selection = repositories.selectRelease(source, major)

      if (operands && !selection.known) {
        throw new IllegalArgumentException(Diagnostic.text("${name} has no known releases; nothing was written",
        'its repository cannot list releases', 'use a repository that lists releases, or cook an exact version'))
      }

      new RecipeUpdate(name, source, selection)
    }
  }

  /** Cooks only moves, with their recorded arguments, in plan order; only `Proposal.commit` writes changes. */
  Proposal propose(List<RecipeUpdate> updates) {
    def lock = readLock()
    List<RecipeRequest> requests = updates.findAll { it.to != null }.collect { update ->
      def recorded = lock.arguments(update.name)

      if (recorded == null) {
        throw new IllegalArgumentException(Diagnostic.text(
        "${update.name} has no recorded arguments in ${CookLock.PATH}", null, 'cook it once with its arguments'))
      }

      new RecipeRequest(update.source.at(update.to).toString(), recorded.collectMany { option, values ->
        values.collect { Argument.inline(option, it) }
      }, true)
    }
    new CookbookCook(destination, repositories).propose(requests, true)
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

/** One lock recipe's release selection: `move`, `current`, or `unknown`, with versions for a CLI to review. */
final class RecipeUpdate {
  final String name
  final Coordinates source
  final boolean known
  final String to
  /** A newer release outside the compatible range, when one is available. */
  final String newestOverall

  RecipeUpdate(String name, Coordinates source, CookbookRepositories.ReleaseSelection selection) {
    this.name = name
    this.source = source
    this.known = selection.known
    this.to = selection.target?.version
    this.newestOverall = selection.newestOverall?.version
  }

  String getFrom() {
    source.version
  }

  String getStatus() {
    !known ? 'unknown' : to != null ? 'move' : 'current'
  }
}
