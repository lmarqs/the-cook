package io.github.lmarqs.cookbook.beads

/**
 * Models Beads tracker policy and the repository guidance for its task store.
 *
 * ## Writes for
 *
 * bd 1.3.1 embedded Dolt: five nested YAML keys in `.beads/config.yaml`, Git ignore patterns in the root
 * `.gitignore`, and Markdown tracker sections in explicitly named instruction files. Native bootstrap owns the
 * database, metadata and `sync.remote`; this model neither initializes nor synchronizes them.
 *
 * ## Instances
 *
 * One instance covers a repository's main checkout. Linked worktrees resolve that checkout's store natively;
 * they need neither another instance nor a `BEADS_DIR` override.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.beads.BeadsTracker
 *
 * @ingredient(BeadsTracker) def beads
 *
 * beads.tracker('clone', ['AGENTS.md'],
 *     'io.github.lmarqs.cookbook:beads#tracker --share=clone --instructions=AGENTS.md')
 * ```
 *
 * ## With other models
 *
 * Viewer and loop runners consume the native task store independently. The recipe connects guidance to the team's
 * files only through its instruction paths; no hook, agent configuration or runner configuration is detected.
 */
@Ingredient
class BeadsTracker {
  private String share
  private String cookedWith
  private List<String> instructions = []

  /**
   * Requests tracker policy for `team` or `clone`, and owned guidance in each existing Markdown path. `cookedWith`
   * names the recipe and normalized arguments at the start of each section, making recooking explicit.
   */
  void tracker(String share, List<String> instructions, String cookedWith) {
    if (!(share in ['team', 'clone'])) {
      throw new IllegalArgumentException("tracker is shared by team or clone, not ${share}")
    }

    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('tracker needs the recipe it is cooked with')
    }

    this.share = share
    this.instructions = instructions.unique(false)
    this.cookedWith = cookedWith
  }

  /** The five policy keys alone; native remotes and unrelated YAML siblings retain their ownership. */
  @Keys(path = '.beads/config.yaml')
  Map configKeys() {
    cookedWith ? [
      (['dolt', 'local-only']): share == 'clone',
      (['dolt', 'auto-push']): false,
      'no-git-ops': true,
      (['export', 'auto']): false,
      (['export', 'git-add']): false,
    ] : null
  }

  /** Required ignores live at the root; native bootstrap's `.beads/.gitignore` is redundant local output. */
  @Section(path = '.gitignore', id = 'tracker')
  List<String> ignoreSection() {
    if (!cookedWith) {
      return null
    }

    def storeRules = share == 'team' ? ['/.beads/*', '!/.beads/config.yaml'] : ['/.beads/']
    def runtimeRules = ['.dolt/', '*.db', '.beads-credential-key', '.beads/proxieddb/', '*.gate.lock*']
    ["# ${cookedWith}".toString()] + storeRules + runtimeRules
  }

  /** Mode-specific guidance is owned so switching mode cannot leave stale seed instructions behind. */
  @Sections(path = '.', id = 'tracker', comment = '<!--', missing = 'fail')
  Map<String, List<String>> instructionsSections() {
    if (!cookedWith || !instructions) {
      return null
    }

    def lines = ["<!-- ${cookedWith} -->".toString()] + resource("instructions/${share}.md").readLines()
    instructions.collectEntries { [(it): lines] }
  }
}
