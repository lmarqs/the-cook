package io.github.lmarqs.cookbook.git

/**
 * Models the files Git reads from a repository's working tree: its ignore rules and the root its linked worktrees sit
 * under.
 *
 * ## Writes for
 *
 * Git ignore syntax in `.gitignore`, with patterns relative to this instance's directory, and a Markdown
 * `.worktrees/README.md` for the worktrees `git worktree` makes at `.worktrees/<branch>`.
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
 * git.worktrees('io.github.lmarqs.cookbook:git#worktrees')
 * git.ignore('build/', '*.log')
 * ```
 *
 * ## With other models
 *
 * Git consumes the ignore patterns other models expose, such as `git.ignore(node.ephemeralFiles())`. The recipe
 * passes them to the Git instance for the same directory. Other artifacts name `.worktrees/<branch>` and
 * `.worktrees/setup` only as path conventions.
 */
@Ingredient
class Git {
  /** The directory each linked worktree sits under, at the path of its branch. */
  static final String WORKTREES = '.worktrees'

  private List<String> patterns = []
  private String worktreesCookedWith

  /** Adds Git ignore patterns as individual strings, relative to this instance's directory. */
  void ignore(String... values) { patterns += values.toList() }

  /** Adds a collection of Git ignore patterns, relative to this instance's directory. */
  void ignore(Collection<String> values) { patterns += values }

  /**
   * Requests the worktree root: Git ignores everything under `.worktrees/` except the README, seeded to state the
   * rule `.worktrees/<branch>` and the optional `.worktrees/setup`. `cookedWith` names the recipe and its arguments,
   * which the section's first line records; it must not be blank.
   */
  void worktrees(String cookedWith) {
    if (!cookedWith?.trim()) throw new IllegalArgumentException('worktrees needs the recipe it is cooked with')
    worktreesCookedWith = cookedWith
  }

  /** The `ignore` section of `.gitignore`, with duplicate patterns removed, when patterns are added. */
  @Section(path = '.gitignore', id = 'ignore')
  List<String> ignoreSection() { patterns.unique() ?: null }

  /**
   * The `worktrees` section of `.gitignore`: a comment naming the recipe, then `/.worktrees/*` and
   * `!/.worktrees/README.md`, when the worktree root is requested.
   */
  @Section(path = '.gitignore', id = 'worktrees')
  List<String> worktreesSection() {
    if (!worktreesCookedWith) return null
    ["# ${worktreesCookedWith}".toString(), "/${WORKTREES}/*".toString(), "!/${WORKTREES}/README.md".toString()]
  }

  /**
   * `.worktrees/README.md`, seeded when absent and never owned: the rule, the `git worktree` commands, the setup
   * script and the traps of worktrees inside the repository, when the worktree root is requested.
   */
  @Seed(path = '.worktrees/README.md')
  String worktreesReadme() { worktreesCookedWith ? resource('templates/worktrees-README.template.md') : null }
}
