package thecook.cookbook

import thecook.model.Diagnostic

import java.lang.reflect.InvocationTargetException

/**
 * Cooks recipes, by coordinates or by path, left to right, into one draft of the destination: each recipe withdraws
 * what it owned and no longer declares, then writes what it declares, taking ownership from the recipes that held it.
 * `cook.lock` records the result.
 */
final class CookbookCook {
  private final File destination
  private final CookbookRepositories repositories
  private final ExistingSeed existing

  /**
   * Coordinates resolve from `repositories`; `existing` decides what cooking a recipe the first time does with a
   * file its `existing = 'fail'` seed writes.
   */
  CookbookCook(File destination, CookbookRepositories repositories, ExistingSeed existing = ExistingSeed.REFUSE) {
    this.destination = destination
    this.repositories = repositories
    this.existing = existing
  }

  /** Evaluates `requests` in order and returns the proposed changes, written only by `Proposal.commit`. */
  Proposal propose(List<RecipeRequest> requests) {
    def loader = new RecipeLoader(destination, repositories)
    def draft = new Draft(destination)
    def ownership = new Ownership(draft, existing)
    Set<String> named = []
    List<CookedRecipe> recipes = []
    List<ChangedSource> changedSources = []
    requests.eachWithIndex { request, index ->
      def label = requests.size() > 1 ? "recipe ${index + 1} (${request.operand})" : request.operand

      try {
        def recipe = loader.load(request.operand)
        String name = recipe.name

        if (!named.add(name)) {
          throw new IllegalArgumentException(
          Diagnostic.text("${name} is named twice", null, 'name it once per invocation'))
        }

        def evaluation = recipe.evaluate(request.arguments, draft)
        def recorded = ownership.sha256(name, recipe.address)
        ownership.cook(name, evaluation.declared, recipe.address, recipe.sha256, evaluation.arguments)

        if ((request.replayed || recipe.released) && recorded != null && recorded != recipe.sha256) {
          changedSources.add(new ChangedSource(recipe.address, recorded, recipe.sha256))
        }

        def texts = RecipeSource.texts(recipe.source, "${recipe.fileName}.groovy")
        recipes.add(new CookedRecipe(recipe.address, texts?.confirm, texts?.yieldText))
      } catch (Exception error) {
        def cause = error instanceof InvocationTargetException && error.cause instanceof Exception ? error.cause : error
        def message = cause.message ?: cause.class.simpleName

        if (request.replayed && cause instanceof ArgumentBindingException) {
          def line = request.arguments.collect { "--${it.name}=${it.value}" }.join(' ')
          message = Diagnostic.text(message, "arguments recorded in ${CookLock.PATH}: ${line}",
              'cook it with its arguments')
        }

        throw new IllegalArgumentException("${label}: ${message}", cause)
      }
    }
    ownership.save()
    new Proposal(draft, recipes, changedSources, ownership.seeds)
  }
}

/**
 * One recipe to cook: its operand as written, coordinates or a path in the kitchen, and its arguments in order.
 * A request `replayed` from `cook.lock` reports a changed path source, and arguments that no longer bind as the ones
 * the lock recorded.
 */
final class RecipeRequest {
  final String operand
  final List<Argument> arguments
  final boolean replayed

  RecipeRequest(String operand, List<Argument> arguments, boolean replayed = false) {
    this.operand = operand
    this.arguments = arguments
    this.replayed = replayed
  }
}

/** A recipe that ran: its address, the consequence to confirm before the writes and what to do after them. */
final class CookedRecipe {
  final String address
  final String confirm
  final String yieldText

  CookedRecipe(String address, String confirm, String yieldText) {
    this.address = address
    this.confirm = confirm
    this.yieldText = yieldText
  }
}

/**
 * A recipe whose sources differ from those `cook.lock` recorded at the same address, as when a release's tag moved
 * or a replicated path was edited: its address and the sha256 the lock recorded and the one it has now.
 */
final class ChangedSource {
  final String address
  final String recorded
  final String current

  ChangedSource(String address, String recorded, String current) {
    this.address = address
    this.recorded = recorded
    this.current = current
  }
}

/** The changes a batch proposes, written by `commit`. */
final class Proposal {
  private final Draft draft
  private final SeedOutcomes seeds
  final List<CookedRecipe> recipes
  final List<ChangedSource> changedSources

  Proposal(Draft draft, List<CookedRecipe> recipes, List<ChangedSource> changedSources, SeedOutcomes seeds) {
    this.draft = draft
    this.recipes = recipes
    this.changedSources = changedSources
    this.seeds = seeds
  }

  /** The paths that change, in path order. */
  List<String> getChanges() {
    draft.changes
  }

  /**
   * Each changed or kept path, in path order, with what happens to it: `create`, `update`, `delete`, `replace` for an
   * existing file a seed wrote over, or `keep` for one it left.
   */
  Map<String, String> getActions() {
    def changes = draft.changes
    (changes + seeds.kept).unique()
        .sort()
        .collectEntries { path ->
          [(path): !(path in changes) ? 'keep' : path in seeds.replaced ? 'replace' : draft.action(path)]
        }
  }

  /** The text of a changed `path` before the batch; `null` when it is created. */
  String before(String path) {
    draft.before(path)
  }

  /** The text of a changed `path` after the batch; `null` when it is deleted. */
  String after(String path) {
    draft.after(path)
  }

  /** The unified diff of a changed `path`, as the review shows it. */
  List<String> patch(String path) {
    FilePatch.of(path, draft.before(path), draft.after(path))
  }

  /** Writes the changes; a failed write leaves the files as they were. */
  void commit() {
    draft.commit()
  }
}
