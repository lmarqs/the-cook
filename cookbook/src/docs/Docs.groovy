package io.github.lmarqs.cookbook.docs

/**
 * Models a repository's `docs/` tree as the single source of truth for people and coding agents: a map that says
 * which page owns each fact, the kinds of page and their criteria, and the `docs-layout` skill that writes and
 * reviews the pages.
 *
 * ## Writes for
 *
 * Markdown in `docs/README.md`; an Agent Skill, `SKILL.md` with its references and templates, under the skills
 * directory named.
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
 * docs.layout('io.github.lmarqs.cookbook:docs#seed --skills-root=.agents/skills', ['.agents/skills'], Docs.KINDS)
 * ```
 *
 * ## With other models
 *
 * Docs consumes no other model. The skills directory is a path the person names, so the skill lands where the
 * harnesses read skills, such as `.agents/skills` from the `agents` artifact's layout.
 */
@Ingredient
class Docs {
  /**
   * The kinds of page the map can offer: each a directory under `docs/`, except `architecture` and `philosophy`, one
   * page each.
   */
  static final List<String> KINDS = [
    'adr',
    'api',
    'architecture',
    'features',
    'observability',
    'philosophy',
    'runbooks',
    'standards'
  ]

  /** The name of the skill, the directory it is written to under each skills root. */
  static final String SKILL = 'docs-layout'

  private String cookedWith
  private List<String> skillsRoots = []
  private List<String> kinds = []

  /**
   * Requests the layout: the map at `docs/README.md` offering `kinds`, and the `docs-layout` skill under each of
   * `skillsRoots`, directories by their paths from the repository root. `cookedWith` names the recipe and its
   * arguments, which the map's section opens with. Rejects a blank `cookedWith`, no skills root or a blank one, an
   * empty `kinds` and a kind outside `KINDS`.
   */
  void layout(String cookedWith, List<String> skillsRoots, List<String> kinds) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('layout needs the recipe it is cooked with')
    }

    if (!skillsRoots || skillsRoots.any { !it?.trim() }) {
      throw new IllegalArgumentException('layout needs the directories the skill is written under')
    }

    if (!kinds) {
      throw new IllegalArgumentException("layout needs at least one kind of ${KINDS.join(', ')}")
    }

    def unknown = kinds - KINDS

    if (unknown) {
      throw new IllegalArgumentException("${unknown.join(', ')} is not a kind of page: ${KINDS.join(', ')}")
    }

    this.cookedWith = cookedWith
    this.skillsRoots = skillsRoots.unique(false)
    this.kinds = KINDS.findAll { it in kinds }
  }

  /** `docs/README.md`, seeded when absent and never owned: the map's title and introduction, which the team edits. */
  @Seed(path = 'docs/README.md')
  String mapSeed() {
    cookedWith ? resource("skills/${SKILL}/templates/map.template.md") : null
  }

  /**
   * The `kinds` section of `docs/README.md`: the recipe and its arguments as an HTML comment, then each kind offered
   * with what it holds, its Diátaxis type and its criteria, then where a fact goes, what a page holds, terms, guides
   * and the skill.
   */
  @Section(path = 'docs/README.md', id = 'kinds', comment = '<!--')
  List<String> kindsSection() {
    if (!cookedWith) {
      return null
    }

    def text = (kinds.collect { resource("kinds/${it}.md") } + resource('kinds/common.md')).join('\n')
    ["<!-- ${cookedWith} -->".toString()] + text.readLines()
  }

  /** Whole files under each skills root, `docs-layout/` and everything in it, when the layout is requested. */
  @Files(path = '.')
  Map<String, String> skillFiles() {
    if (!cookedWith) {
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
