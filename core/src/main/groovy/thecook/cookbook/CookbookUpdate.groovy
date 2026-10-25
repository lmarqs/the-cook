package thecook.cookbook

import thecook.model.Diagnostic

/**
 * Cooks recipes from `cook.lock` again at the newest release its repositories list, with the arguments the lock records.
 */
final class CookbookUpdate {
  private final File destination
  private final CookbookRepositories repositories

  CookbookUpdate(File destination, CookbookRepositories repositories) {
    this.destination = destination
    this.repositories = repositories
  }

  /** A recipe the lock records at `from` and the repositories list at the newer `to`. */
  static final class Bump {
    final String name
    final String from
    final String to

    Bump(String name, String from, String to) {
      this.name = name
      this.from = from
      this.to = to
    }
  }

  /** The recipes named, or every coordinates recipe in the lock, that have a newer release. */
  List<Bump> bumps(List<String> operands) {
    def kitchen = destination.toPath().toRealPath()
    def text = new Draft(destination).read(CookLock.PATH)

    if (text == null) {
      throw new IllegalArgumentException(Diagnostic.text("there is no ${CookLock.PATH}",
      "${CookLock.PATH} records the recipes to update", 'run in the kitchen that holds it'))
    }

    def lock = CookLock.parse(text)
    def names = operands ? operands.collect { RecipeLoader.lockName(kitchen, it) } : lock.names as List
    names.findResults { name ->
      if (!(name in lock.names)) {
        throw new IllegalArgumentException(Diagnostic.text("${CookLock.PATH} lists no recipe ${name}", null,
        "check the recipe's name; ${CookLock.PATH} lists ${lock.names.join(', ')}"))
      }

      def source = lock.source(name)

      if (!Coordinates.written(source)) {
        return null
      }

      def recorded = Coordinates.parse(source)
      def latest = repositories.latest(recorded.group, recorded.artifact)
      latest && Coordinates.compareVersions(latest, recorded.version) > 0 ?
      new Bump(name, recorded.version, latest) : null
    }
  }

  /** Cooks each bump at its newer release with its recorded arguments; only `Proposal.commit` writes. */
  Proposal propose(List<Bump> bumps) {
    def lock = CookLock.parse(new Draft(destination).read(CookLock.PATH))
    def requests = bumps.collect { bump ->
      def recorded = lock.arguments(bump.name)

      if (recorded == null) {
        throw new IllegalArgumentException(Diagnostic.text("${bump.name} has no recorded arguments in ${CookLock.PATH}",
        null, 'cook it once with its arguments'))
      }

      def source = Coordinates.parse(lock.source(bump.name)).at(bump.to).toString()
      new RecipeRequest(source, recorded.collectMany { option, values ->
        values.collect { Argument.inline(option, it) }
      }, true)
    }
    new CookbookCook(destination, repositories).propose(requests)
  }
}
