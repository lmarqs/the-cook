package io.github.lmarqs.cookbook.docs

/**
 * Models a repository's `docs/` tree as the single source of truth for people and coding agents: a map that says
 * which page owns each fact, the kinds of page and their criteria, and the `docs-layout` skill that writes, reviews
 * and checks the pages.
 *
 * ## Writes for
 *
 * GitHub-flavored Markdown in `docs/README.md` and in each instructions file named, such as `AGENTS.md`; an Agent
 * Skill, `SKILL.md` with its references, templates and a POSIX `sh` check script, for Claude Code 2.1.312 and Codex
 * 0.160.1, under the skills directory named.
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
 * docs.instructions(['AGENTS.md'])
 * docs.layout('io.github.lmarqs.cookbook:docs#seed --instructions=AGENTS.md --skills=.agents/skills',
 *     '.agents/skills', Docs.KINDS)
 * ```
 *
 * ## With other models
 *
 * Docs consumes no other model. The skills directory is a path the person names, so the skill lands where the
 * harnesses read skills, such as `.agents/skills` from the `agents` artifact's layout.
 */
@Ingredient
class Docs {
  /** The kinds of page the map can offer, each a directory under `docs/`. */
  static final List<String> KINDS = ['adr', 'api', 'features', 'runbooks', 'ux']

  /** The name of the skill, the directory it is written to under the skills directory. */
  static final String SKILL = 'docs-layout'

  private String cookedWith
  private String skills
  private List<String> kinds = []
  private List<String> instructions = []

  /**
   * Requests the layout: the map at `docs/README.md` offering `kinds`, and the `docs-layout` skill under `skills`, a
   * directory by its path from the repository root. `cookedWith` names the recipe and its arguments, which the map's
   * section and each instructions section open with. Rejects a blank `cookedWith` or `skills`, an empty `kinds` and a
   * kind outside `KINDS`.
   */
  void layout(String cookedWith, String skills, List<String> kinds) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('layout needs the recipe it is cooked with')
    }

    if (!skills?.trim()) {
      throw new IllegalArgumentException('layout needs the directory the skill is written under')
    }

    if (!kinds) {
      throw new IllegalArgumentException("layout needs at least one kind of ${KINDS.join(', ')}")
    }

    def unknown = kinds - KINDS
    if (unknown) {
      throw new IllegalArgumentException("${unknown.join(', ')} is not a kind of page: ${KINDS.join(', ')}")
    }

    this.cookedWith = cookedWith
    this.skills = skills
    this.kinds = KINDS.findAll { it in kinds }
  }

  /**
   * Has the docs rule stated in each Markdown file in `paths`, by its path from the repository root, in a `docs`
   * section. A file the draft lacks is refused when the sections are written: the rule goes into a file the team
   * keeps.
   */
  void instructions(List<String> paths) {
    instructions = paths.unique(false)
  }

  /** `docs/README.md`, seeded when absent and never owned: the map's title and introduction, which the team edits. */
  @Seed(path = 'docs/README.md')
  String mapSeed() {
    cookedWith ? resource("skills/${SKILL}/templates/map.template.md") : null
  }

  /**
   * The `kinds` section of `docs/README.md`: the recipe and its arguments as an HTML comment, then each kind offered
   * with what it holds, its Diátaxis type and its criteria, then where a fact goes, what a page holds, terms, guides
   * and the check command.
   */
  @Section(path = 'docs/README.md', id = 'kinds', comment = '<!--')
  List<String> kindsSection() {
    if (!cookedWith) {
      return null
    }

    def text = (kinds.collect { resource("kinds/${it}.md") } + resource('kinds/rules.md')).join('\n')
    ["<!-- ${cookedWith} -->".toString()] + text.replace('{{skills}}', skills).readLines()
  }

  /** Whole files under the skills directory, `docs-layout/` and everything in it, when the layout is requested. */
  @Files(path = '${skills}')
  Map<String, String> skillFiles() {
    if (!cookedWith) {
      return null
    }

    resources("skills/${SKILL}").collectEntries { path, body -> ["${SKILL}/${path}".toString(), body] }
  }

  /**
   * The `docs` section of each file `instructions` named, when the layout is requested: the recipe and its arguments
   * as an HTML comment, then a list item saying `docs/` is the source of truth, where a fact goes, and the check and
   * the skill to use.
   */
  @Sections(path = '.', id = 'docs', comment = '<!--', missing = 'fail')
  Map<String, List<String>> instructionsSections() {
    if (!cookedWith || !instructions) {
      return null
    }

    def lines = [
      "<!-- ${cookedWith} -->",
      '- Docs: `docs/` is the source of truth; `docs/README.md` says which page owns a fact. Read the owning page ' +
          'before',
      '  changing an area. A behavior change ships with its page, and a fact learned about the system goes to the ' +
          'page that',
      "  owns it. `sh ${skills}/${SKILL}/scripts/check.sh` must pass; write a page with the `${SKILL}` skill.",
    ]*.toString()
    instructions.collectEntries { [(it): lines] }
  }
}
