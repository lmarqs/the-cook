package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.RecipeSource

class DocsLayoutCookTest {
  private static final String LAYOUT = 'io.github.lmarqs.cookbook:docs:0.1.0#layout'
  private static final List<String> KINDS = [
    'adr',
    'api',
    'architecture',
    'features',
    'observability',
    'philosophy',
    'runbooks',
    'standards'
  ]
  private static final File SKILL = new File(COOKBOOK, 'docs/skills/docs-layout')

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

  @Test void refusesWithoutSkillsOutsideTheRepositoryAndKinds() {
    def kitchen = kitchen()
    assert shouldFail(IllegalArgumentException) { cook(kitchen, LAYOUT, [:]) }.message ==
    "${LAYOUT}: Missing --skills-root: A directory, from the repository root, the docs-layout skill is written under, " +
    'such as .agents/skills'
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

  @Test void theSkillNamesEveryArgumentAndMeetsTheSkillSpec() {
    def text = new File(SKILL, 'SKILL.md').text
    def front = text.split('---\n')[1]
    def description = front.readLines().find { it.startsWith('description: ') }.substring(13)

    ['`map`', '`<kind> [title]`', '`term <term>`', '`review`', '`review --fix`', '`adopt`'].each {
      assert text.contains("| ${it} |")
    }
    assert front.readLines().contains('name: docs-layout')
    assert description.length() <= 1024
    assert text.readLines().size() < 500
    assert !text.contains('[[fill')
    ['the map wins', 'never holds a placeholder', 'edited for its facts', 'no kind fits', 'changed no fact'].each {
      assert text.contains(it)
    }
    assert !new File(SKILL, 'templates').exists()
    (KINDS + ['map', 'glossary', 'adopt', 'design']).each { assert text.contains("references/${it}.md") }
  }

  @Test void theReferencesTeachEachKindWithinTheBudget() {
    def references = new File(SKILL, 'references')
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
    def words = ([new File(SKILL, 'SKILL.md')] + references.listFiles().findAll { it.name != 'design.md' })
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
