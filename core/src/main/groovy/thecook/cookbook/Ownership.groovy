package thecook.cookbook

import thecook.model.Diagnostic

/**
 * Applies what recipes declare to one draft of the kitchen against what `cook.lock` records they own: a recipe
 * withdraws what it owned and no longer declares, writes what it declares and takes ownership from the recipes that
 * held it, and a discarded recipe withdraws everything it owned. `save` writes the lock into the draft.
 */
final class Ownership {
  /** The existing files that seeds replaced or kept so far. */
  final SeedOutcomes seeds = new SeedOutcomes()
  private final Draft draft
  private final CookLock lock
  private final ExistingSeed existing

  /**
   * Reads `cook.lock` from `draft`; `existing` decides what cooking a recipe the first time does with a file its
   * `existing = 'fail'` seed writes.
   */
  Ownership(Draft draft, ExistingSeed existing = ExistingSeed.REFUSE) {
    this.draft = draft
    this.lock = CookLock.parse(draft.read(CookLock.PATH))
    this.existing = existing
  }

  /** The recipes the lock lists, by short name. */
  Set<String> getNames() {
    lock.names
  }

  /** The sha256 the recipe `name` was last cooked with from `source`, or `null` when it was not cooked from it. */
  String sha256(String name, String source) {
    lock.sha256(name, source)
  }

  /**
   * Cooks what the recipe `name` declares into the draft, and records it as cooked from `source` with `sha256` and
   * `arguments`. Fails when two of its parts overlap, or when it deletes a file a recipe owns.
   */
  void cook(String name, Declared declared, String source, String sha256, Map<String, List<String>> arguments) {
    def owned = declared.owned
    refuseOverlaps(owned)
    declared.seeds.each { seed(it, name) }
    def targets = owned*.target
    lock.owned(name)
        .findAll { !(it in targets) }
        *.withdraw(draft)
    owned.each { it.target.write(draft, it.value) }
    declared.deletes.each { delete(it, name, targets) }
    lock.record(name, source, sha256, arguments, targets)
  }

  /** Withdraws everything the recipe `name` owned and drops it from the lock. */
  void discard(String name) {
    lock.forget(name)*.withdraw(draft)
  }

  /**
   * Writes the lock into the draft, or deletes it once it lists no recipe. Fails first when seeds refused existing
   * files.
   */
  void save() {
    seeds.refuse()

    if (lock.names) {
      draft.write(CookLock.PATH, lock.render())
    } else {
      draft.delete(CookLock.PATH)
    }
  }

  private static void refuseOverlaps(List<Declaration> owned) {
    owned.eachWithIndex { first, index ->
      def second = owned.drop(index + 1).find { it.target.overlaps(first.target) }

      if (second) {
        throw new IllegalArgumentException("${first.member} and ${second.member} both declare ${first.target}")
      }
    }
  }

  private void seed(Seeding seed, String name) {
    if (draft.read(seed.file) == null) {
      draft.write(seed.file, seed.text, seed.mode)
    } else if (seed.failsOnExisting && !lock.cooked(name)) {
      seeds.existing(existing, seed, name, draft)
    }
  }

  private void delete(Deletion deletion, String name, List<Target> targets) {
    def owner = lock.ownerOf(new FileTarget(deletion.file), name) ?:
        (targets.any { it.file == deletion.file } ? name : null)

    if (owner) {
      throw new IllegalArgumentException("${deletion.member} deletes ${deletion.file}, which ${owner} owns")
    }

    draft.delete(deletion.file)
  }
}

/** What cooking a recipe the first time does with a file its `existing = 'fail'` seed writes. */
enum ExistingSeed {
  /** Fails, naming the file. */
  REFUSE,
  /** Writes the seed over the file, or over the link at its path. */
  REPLACE,
  /** Leaves the file as it is. */
  KEEP
}

/** The existing files that seeds refused, replaced or kept during one invocation. */
final class SeedOutcomes {
  final Set<String> replaced = new TreeSet<>()
  final Set<String> kept = new TreeSet<>()
  private final Map<String, String> refused = new TreeMap<>()

  void existing(ExistingSeed choice, Seeding seed, String recipe, Draft draft) {
    if (choice == ExistingSeed.REPLACE) {
      draft.write(seed.file, seed.text, seed.mode)
      replaced.add(seed.file)
    } else if (choice == ExistingSeed.KEEP) {
      kept.add(seed.file)
    } else {
      refused[seed.file] = recipe
    }
  }

  /** Fails when seeds refused existing files, naming each file, its recipe and the options that resolve them. */
  void refuse() {
    if (refused.size() == 1) {
      def only = refused.entrySet().first()
      throw new IllegalArgumentException(Diagnostic.text("${only.key} already exists",
      "${only.value} writes it from a template the first time it is cooked",
      'run with --force to replace it, or --skip to keep it'))
    }

    if (refused) {
      def header = 'These files already exist, and their recipes write them from a template ' +
          'the first time they are cooked:'
      def files = refused.collect { file, recipe -> "  ${file} (${recipe})" }
      def footer = '  run with --force to replace them, or --skip to keep them'
      throw new IllegalArgumentException(([header] + files + [footer]).join('\n'))
    }
  }
}
