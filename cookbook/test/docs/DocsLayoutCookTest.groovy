package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook
import static thecook.Kitchens.publish

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.RecipeSource

class DocsLayoutCookTest {
  private static final String LAYOUT = 'io.github.lmarqs.cookbook:docs:0.1.0#layout'
  private static final File SKILL = new File(COOKBOOK, 'docs/skills/docs-layout')
  private static final List<String> KINDS = new File(SKILL, 'references').list()
  .collect { it - '.md' }
  .findAll { !(it in ['map', 'glossary', 'adopt']) }
  .sort()

  /** The model of a stand-in for the earlier `seed` recipe: an owned section of the map and a skill template. */
  private static final String OLD_MODEL = '''package io.github.lmarqs.cookbook.docs

/** The earlier layout. */
@Ingredient
class OldDocs {
  /** Requested. */
  boolean on

  /** The kinds section. */
  @Section(path = 'docs/README.md', id = 'kinds', comment = '<!--')
  List<String> kinds() {
    on ? ['## Kinds'] : null
  }

  /** The skill and a template. */
  @Files(path = '.agents/skills/docs-layout')
  Map<String, String> skill() {
    on ? ['SKILL.md': 'old\\n', 'templates/adr.template.md': '[[fill: x]]\\n'] : null
  }
}
'''

  /** The stand-in's recipe. */
  private static final String OLD_RECIPE = '''@help(\'\'\'
The earlier recipe.
\'\'\')
import io.github.lmarqs.cookbook.docs.OldDocs

@ingredient(OldDocs) def docs

docs.on = true
'''

  private static final Map<String, Object> OLD_SEED = ['OldDocs.groovy': OLD_MODEL, 'seed.groovy': OLD_RECIPE]

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void writesNothingUnderDocsAndCooksAgainWithoutChange() {
    def kitchen = kitchen()
    def map = new File(kitchen, 'docs/README.md')
    map.parentFile.mkdirs()
    map.text = '# Our docs\n\nStart here.\n'

    cook(kitchen, LAYOUT, ['skills-root': ['.agents/skills']]).commit()

    assert map.text == '# Our docs\n\nStart here.\n'
    assert new File(kitchen, 'docs').list().toList() == ['README.md']
    assert new File(kitchen, '.agents/skills/docs-layout/SKILL.md').exists()
    assert cook(kitchen, LAYOUT, ['skills-root': ['.agents/skills']]).actions == [:]
  }

  @Test void writesNoMapWhereThereIsNone() {
    def kitchen = kitchen()

    cook(kitchen, LAYOUT, ['skills-root': ['.agents/skills']]).commit()

    assert !new File(kitchen, 'docs').exists()
  }

  @Test void anAdopterOfTheSeedRecipeCooksLayoutThenDiscardsSeedAndKeepsItsOwnText() {
    def kitchen = kitchen()
    publish(kitchen, 'io.github.lmarqs.cookbook:docs:0.0.9', OLD_SEED)
    cook(kitchen, 'io.github.lmarqs.cookbook:docs:0.0.9#seed', [:]).commit()
    def map = new File(kitchen, 'docs/README.md')
    map.text = '# Our docs\n\n' + map.text + '\n## Our notes\n'
    assert map.text.contains('<!-- cook:begin io.github.lmarqs.cookbook:docs#seed/kinds -->\n## Kinds\n')
    assert new File(kitchen, '.agents/skills/docs-layout/templates/adr.template.md').exists()

    cook(kitchen, LAYOUT, ['skills-root': ['.agents/skills']]).commit()
    new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:docs#seed']).commit()

    assert map.text == '# Our docs\n\n\n## Our notes\n'
    assert !new File(kitchen, '.agents/skills/docs-layout/templates').exists()
    Kitchens.files(SKILL).each {
      assert new File(kitchen, ".agents/skills/docs-layout/${it}").bytes == new File(SKILL, it).bytes
    }
    assert cook(kitchen, LAYOUT, ['skills-root': ['.agents/skills']]).actions == [:]
  }

  @Test void refusesWithoutSkillsOutsideTheRepositoryAndKinds() {
    def kitchen = kitchen()
    assert shouldFail(IllegalArgumentException) { cook(kitchen, LAYOUT, [:]) }.message ==
    "${LAYOUT}: Missing --skills-root: A directory, from the repository root, the docs-layout skill is written " +
    'under, such as .agents/skills'
    def kinds = shouldFail(IllegalArgumentException) { cook(kitchen, LAYOUT, ['skills-root': ['s'], kinds: ['adr']]) }
    assert kinds.message.startsWith("${LAYOUT}: Unknown argument --kinds")
    def outside = shouldFail(IllegalArgumentException) { cook(kitchen, LAYOUT, ['skills-root': ['../skills']]) }
    assert outside.message.startsWith("${LAYOUT}: --skills-root=../skills is outside the repository")
  }

  @Test void ownsTheSkillUnderEachNamedRoot() {
    def kitchen = kitchen()

    cook(kitchen, LAYOUT, ['skills-root': ['.claude/skills', '.agents/skills']]).commit()

    def shipped = Kitchens.files(SKILL)
    assert shipped.contains('SKILL.md')
    def lock = new File(kitchen, 'cook.lock').text
    ['.claude/skills', '.agents/skills'].each { root ->
      shipped.each { assert new File(kitchen, "${root}/docs-layout/${it}").bytes == new File(SKILL, it).bytes }
      shipped.each { assert lock.contains("\"${root}/docs-layout/${it}\"") }
    }

    cook(kitchen, LAYOUT, ['skills-root': ['.agents/skills']]).commit()

    assert !new File(kitchen, '.claude/skills/docs-layout/SKILL.md').exists()
    assert new File(kitchen, '.agents/skills/docs-layout/SKILL.md').exists()
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'docs/layout.groovy').text, 'layout.groovy')

    assert texts.params*.names == [['skills-root']]
    assert texts.ingredients == ['io.github.lmarqs.cookbook.docs.Docs']
    assert texts.help.readLines().findAll { it.startsWith('## ') } ==
    ['## API', '## Produces', '## Connections', '## Re-cooking']
    assert texts.help.contains('Never touches: `docs/`')
    assert texts.help.contains('./cook discard io.github.lmarqs.cookbook:docs#seed')
    assert texts.yieldText.contains('/docs-layout map')
  }

  @Test void theSkillLinksOnlyFilesItShips() {
    def pages = [new File(SKILL, 'SKILL.md')] + new File(SKILL, 'references').listFiles().toList()
    pages.each { page ->
      boolean fenced = false
      page.readLines().each { line ->
        if (line.trim().startsWith('```')) {
          fenced = !fenced
        }

        if (!fenced) {
          (line.replaceAll(/`[^`]*`/, '') =~ /\]\(([^)#]+)(#[^)]*)?\)/).each { all, target, anchor ->
            assert new File(page.parentFile, target).canonicalPath.startsWith(SKILL.canonicalPath + File.separator)
            assert new File(page.parentFile, target).exists(): "${page.name} links ${target}"
          }
        }
      }
    }
  }

  @Test void thePrinciplesTheSkillIsBuiltFromLiveBesideItAndAreNotShipped() {
    def principles = new File(COOKBOOK, 'docs/principles.md')
    assert principles.exists()
    ['## A fact carries its story', '## One owner per fact', '## The kind is the question a reader brings',
     '## Teach by criteria and one example', '## The team owns its map', '## Shape follows the readers',
     '## A page never holds a placeholder', '## Choices', '| Choice | Reason | Source |']
    .each { assert principles.text.contains(it): "principles.md lacks ${it}" }
    assert !SKILL.listFiles().any { it.name == 'principles.md' }
    assert !new File(SKILL, 'references/design.md').exists()
    assert new File(COOKBOOK, 'docs/README.md').text.contains('(principles.md)')
  }

  @Test void theSkillNamesEveryArgumentAndMeetsTheSkillSpec() {
    def text = new File(SKILL, 'SKILL.md').text
    def front = text.split('---\n')[1]
    def frontLines = front.readLines()
    def description = frontLines.find { it.startsWith('description: ') }.substring(13)

    ['`map`', '`<kind> [title]`', '`term <term>`', '`review`', '`review --fix`', '`adopt`'].each {
      assert text.contains("| ${it} |")
    }
    assert frontLines.contains('name: docs-layout')
    assert description.length() <= 1024
    assert text.readLines().size() < 500
    assert !text.contains('[[fill')
    ['the map wins', 'never holds a placeholder', 'edited for its facts', 'no kind fits', 'changed no fact'].each {
      assert text.contains(it)
    }
    assert !new File(SKILL, 'templates').exists()
    (KINDS + ['map', 'glossary', 'adopt']).each { assert text.contains("references/${it}.md") }
  }

  @Test void theReferencesTeachEachKindWithinTheBudget() {
    def references = new File(SKILL, 'references')
    assert KINDS == ['adr', 'api', 'architecture', 'features', 'observability', 'philosophy', 'runbooks', 'standards']
    KINDS.each { kind ->
      def text = new File(references, "${kind}.md").text
      ['Diátaxis type', 'A fact belongs here when', 'What makes a good', '## Example', 'one good page, not the page']
      .each { assert text.contains(it): "${kind}.md lacks ${it}" }
      assert !text.contains('[[fill')
      assert !(text =~ /(?m)^- (Gives|Names|Lists|Describes|Shows), in `[A-Z]/)
    }
    def map = new File(references, 'map.md').text
    ['one owner', '## The lead', '## Misfilings', '## A new kind', 'what happened'].each { assert map.contains(it) }
    assert !new File(COOKBOOK, 'docs/kinds').exists()
    def words = ([new File(SKILL, 'SKILL.md')] + references.listFiles().toList())
    .sum { it.text.split(/\s+/).size() }
    assert words < 3450: "the skill holds ${words} words"
  }

  private File kitchen() {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'docs')
    kitchen
  }

  private static Throwable shouldFail(Class<? extends Throwable> type, Closure action) {
    try {
      action()
    } catch (Throwable error) {
      assert type.isInstance(error)
      return error
    }

    throw new AssertionError("Expected ${type.simpleName}")
  }
}
