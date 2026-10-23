package io.github.lmarqs.cookbook.git

/**
 * Models the files Git reads from a repository's working tree: its ignore rules and the root its linked worktrees sit
 * under.
 *
 * ## Writes for
 *
 * Git ignore syntax in `.gitignore`, with patterns relative to this instance's directory, and a Markdown
 * `.worktrees/README.md` for the worktrees `git worktree` makes at `.worktrees/<branch>`. A Markdown section stating
 * the worktree rule in each instruction file the recipe names, such as `AGENTS.md`.
 *
 * ## Instances
 *
 * One instance covers one directory. A second `.gitignore` is a second instance `at` another directory,
 * such as `@ingredient(Git, at = 'packages/api')`. Worktrees belong to the repository, so `worktrees` is called on the
 * instance at the repository's root.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.git.Git
 *
 * @ingredient(Git) def git
 *
 * git.instructions(['AGENTS.md'])
 * git.worktrees('io.github.lmarqs.cookbook:git#worktrees --instructions=AGENTS.md')
 * git.ignore('build/', '*.log')
 * ```
 *
 * ## With other models
 *
 * Git consumes the ignore patterns other models expose, such as `git.ignore(node.ephemeralFiles())`. The recipe
 * passes them to the Git instance for the same directory. Other artifacts name `.worktrees/<branch>`,
 * `.worktrees/setup` and `.worktrees/teardown` only as path conventions.
 */
@Ingredient
class Git {
  /** The directory each linked worktree sits under, at the path of its branch. */
  static final String WORKTREES = '.worktrees'

  private List<String> patterns = []
  private List<String> instructions = []
  private String worktreesCookedWith
  private String ignoreCookedWith

  /** Adds Git ignore patterns as individual strings, relative to this instance's directory. */
  void ignore(String... values) {
    patterns += values.toList()
  }

  /** Adds a collection of Git ignore patterns, relative to this instance's directory. */
  void ignore(Collection<String> values) {
    patterns += values
  }

  /**
   * Requests the worktree root: Git ignores everything under `.worktrees/` except the README, which states the rule
   * `.worktrees/<branch>`, and the `.worktrees/setup` and `.worktrees/teardown` scripts, all seeded. `cookedWith` names
   * the recipe and its arguments, which the section's first line records; it must not be blank.
   */
  void worktrees(String cookedWith) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('worktrees needs the recipe it is cooked with')
    }

    worktreesCookedWith = cookedWith
  }

  /**
   * Has the worktree rule stated in each Markdown file in `paths`, by its path from the repository root, in a
   * `worktrees` section. A file the draft lacks is refused when the sections are written: the rule goes into a file the
   * team keeps.
   */
  void instructions(List<String> paths) {
    instructions = paths.unique(false)
  }

  /** Names the recipe and its arguments, which the `ignore` section opens with as a comment. */
  void ignoredBy(String cookedWith) {
    ignoreCookedWith = cookedWith
  }

  /**
   * The `ignore` section of `.gitignore`, with duplicate patterns removed, when patterns are added; it opens with the
   * line `ignoredBy` names, when given.
   */
  @Section(path = '.gitignore', id = 'ignore')
  List<String> ignoreSection() {
    if (!patterns) {
      return null
    }

    (ignoreCookedWith ? ["# ${ignoreCookedWith}".toString()] : []) + patterns.unique()
  }

  /**
   * The `worktrees` section of `.gitignore`: a comment naming the recipe, then `/.worktrees/*` and a re-include of the
   * README and both scripts, when the worktree root is requested.
   */
  @Section(path = '.gitignore', id = 'worktrees')
  List<String> worktreesSection() {
    if (!worktreesCookedWith) {
      return null
    }

    [
      "# ${worktreesCookedWith}",
      "/${WORKTREES}/*",
      "!/${WORKTREES}/README.md",
      "!/${WORKTREES}/setup",
      "!/${WORKTREES}/teardown"
    ]*.toString()
  }

  /**
   * The `worktrees` section of each file `instructions` named, when the worktree root is requested: the recipe and its
   * arguments as an HTML comment, then a list item stating the rule and pointing at `.worktrees/README.md`.
   */
  @Sections(path = '.', id = 'worktrees', comment = '<!--', missing = 'fail')
  Map<String, List<String>> instructionsSections() {
    if (!worktreesCookedWith || !instructions) {
      return null
    }

    def lines = [
      "<!-- ${worktreesCookedWith} -->",
      "- Worktrees: branch `<branch>` is checked out at `${WORKTREES}/<branch>`; `${WORKTREES}/README.md` says how to",
      '  create one ready to run and remove it.',
    ]*.toString()
    instructions.collectEntries { [(it): lines] }
  }

  /**
   * `.worktrees/README.md`, seeded when absent and never owned: the rule, the `git worktree` commands, the setup and
   * teardown scripts and the state every worktree shares, when the worktree root is requested.
   */
  @Seed(path = '.worktrees/README.md')
  String worktreesReadme() {
    worktreesCookedWith ? resource('templates/README.template.md') : null
  }

  /**
   * `.worktrees/setup`, seeded executable when absent and never owned: a `sh` script that does nothing yet, with
   * comments naming what a team adds, when the worktree root is requested.
   */
  @Seed(path = '.worktrees/setup', mode = '0755')
  String worktreesSetup() {
    worktreesCookedWith ? resource('templates/setup.sh') : null
  }

  /**
   * `.worktrees/teardown`, seeded executable when absent and never owned: a `sh` script that does nothing yet, with
   * comments naming what a team adds, when the worktree root is requested.
   */
  @Seed(path = '.worktrees/teardown', mode = '0755')
  String worktreesTeardown() {
    worktreesCookedWith ? resource('templates/teardown.sh') : null
  }
}
