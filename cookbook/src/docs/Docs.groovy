package io.github.lmarqs.cookbook.docs

/**
 * Gives a repository the `docs-layout` skill, which teaches where a fact goes in `docs/` and why, writes the map at
 * `docs/README.md` with the team, and writes and reviews pages by it. The model writes nothing under `docs/`: the map
 * and the pages are the team's.
 *
 * ## Writes for
 *
 * An Agent Skill, `SKILL.md` with its references, under the skills directory named.
 *
 * ## Instances
 *
 * One instance covers the repository: `docs/` sits at its root.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.docs.Docs
 *
 * @ingredient(Docs) def docs
 *
 * docs.layout(['.agents/skills'])
 * ```
 *
 * ## With other models
 *
 * Docs consumes no other model. The skills directory is a path the person names, so the skill lands where the
 * harnesses read skills, such as `.agents/skills` from the `agents` artifact's layout.
 */
@Ingredient
class Docs {
  /** The name of the skill, the directory it is written to under each skills root. */
  static final String SKILL = 'docs-layout'

  private List<String> skillsRoots = []

  /**
   * Requests the `docs-layout` skill under each of `skillsRoots`, directories by their paths from the repository root.
   * Rejects no skills root or a blank one.
   */
  void layout(List<String> skillsRoots) {
    if (!skillsRoots || skillsRoots.any { !it?.trim() }) {
      throw new IllegalArgumentException('layout needs the directories the skill is written under')
    }

    this.skillsRoots = skillsRoots.unique(false)
  }

  /** Whole files under each skills root, `docs-layout/` and everything in it, when the layout is requested. */
  @Files(path = '.')
  Map<String, String> skillFiles() {
    if (!skillsRoots) {
      return null
    }

    def files = resources("skills/${SKILL}")
    skillsRoots.collectEntries { root ->
      files.collectEntries { path, body ->
        ["${root}/${SKILL}/${path}".toString(), body]
      }
    }
  }
}
