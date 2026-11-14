package io.github.lmarqs.cookbook.beads

/**
 * Models local Beads Viewer export ignores and guidance for explicitly selected instruction files.
 *
 * ## Writes for
 *
 * bd 1.3.1 and bv 0.25.2: a root `.gitignore` section for the compatibility snapshot and Markdown Viewer
 * sections. The native exporter owns snapshot contents; native Beads owns its metadata and task store.
 *
 * ## Instances
 *
 * One instance covers the cooking repository. Runtime commands select one native directory for export and Viewer,
 * including an external store or the main checkout's store from a linked worktree.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.beads.BeadsViewer
 *
 * @ingredient(BeadsViewer) def viewer
 *
 * viewer.viewer(['AGENTS.md'], 'io.github.lmarqs.cookbook:beads#viewer --instructions=AGENTS.md')
 * ```
 *
 * ## With other models
 *
 * Tracker configuration and guidance keep their own ownership. Instruction paths are the only recipe connection;
 * this model neither detects nor cooks tracker, agent or runner setup.
 */
@Ingredient
class BeadsViewer {
  private String cookedWith
  private List<String> instructions = []

  /** Requests Viewer ignores and owned guidance in existing Markdown paths, recording the normalized invocation. */
  void viewer(List<String> instructions, String cookedWith) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('viewer needs the recipe it is cooked with')
    }

    this.instructions = instructions.unique(false)
    this.cookedWith = cookedWith
  }

  /** Native Beads does not ignore its root compatibility export; this section has independent Viewer ownership. */
  @Section(path = '.gitignore', id = 'viewer')
  List<String> ignoreSection() {
    cookedWith ? ["# ${cookedWith}".toString(), '/.beads/issues.jsonl'] : null
  }

  /** Explicit paths keep runtime guidance independent of tracker and agent layout discovery. */
  @Sections(path = '.', id = 'viewer', comment = '<!--', missing = 'fail')
  Map<String, List<String>> instructionsSections() {
    if (!cookedWith || !instructions) {
      return null
    }

    def lines = ["<!-- ${cookedWith} -->".toString()] + resource('instructions/viewer.md').readLines()
    instructions.collectEntries { [(it): lines] }
  }
}
