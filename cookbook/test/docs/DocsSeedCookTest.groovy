package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.RecipeSource

class DocsSeedCookTest {
  private static final String SEED = 'io.github.lmarqs.cookbook:docs:0.1.0#seed'
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

  @Test void seedsTheMapAndOwnsItsKindsSection() {
    def kitchen = kitchen()

    cook(kitchen, SEED, ['skills-root': ['.agents/skills']]).commit()

    def map = new File(kitchen, 'docs/README.md').text
    assert map.startsWith(new File(SKILL, 'templates/map.template.md').text)
    assert map.contains('<!-- cook:begin io.github.lmarqs.cookbook:docs#seed/kinds -->\n' +
    '<!-- io.github.lmarqs.cookbook:docs#seed --skills-root=.agents/skills -->\n')
    KINDS.each { assert map.contains("`${it}/`") || map.contains("`${it}.md`") }
    assert map.contains('The `docs-layout` skill writes pages')
    (KINDS.collectMany { ["docs/${it}", "docs/${it}.md"] } + 'docs/glossary.md').each {
      assert !new File(kitchen, it).exists()
    }
    assert cook(kitchen, SEED, ['skills-root': ['.agents/skills']]).actions == [:]
  }

  @Test void kindsNamesTheKindsTheMapOffersAndKeepsTheTeamsIntroduction() {
    def kitchen = kitchen()
    cook(kitchen, SEED, ['skills-root': ['.agents/skills'], kinds: ['runbooks', 'adr']]).commit()
    def map = new File(kitchen, 'docs/README.md')
    assert map.text.contains('<!-- io.github.lmarqs.cookbook:docs#seed --kinds=adr --kinds=runbooks ' +
    '--skills-root=.agents/skills -->')
    assert map.text.contains('`adr/`') && map.text.contains('`runbooks/`') && !map.text.contains('`standards/`')

    map.text = '# Our docs\n\nStart here.\n' + map.text.substring(map.text.indexOf('<!-- cook:begin'))
    cook(kitchen, SEED, ['skills-root': ['.agents/skills']]).commit()

    assert map.text.startsWith('# Our docs\n\nStart here.\n')
    assert map.text.contains('`standards/`')
  }

  @Test void refusesWithoutSkillsOutsideTheRepositoryAndAnUnknownKind() {
    def kitchen = kitchen()
    assert shouldFail(IllegalArgumentException) { cook(kitchen, SEED, [:]) }.message ==
    "${SEED}: Missing --skills-root: A directory, from the repository root, the docs-layout skill is written under, " +
    'such as .agents/skills'
    def unknown = shouldFail(IllegalArgumentException) { cook(kitchen, SEED, ['skills-root': ['s'], kinds: ['howto']]) }
    assert unknown.message == "${SEED}: --kinds=howto is not one of: ${KINDS.join(', ')}"
    def outside = shouldFail(IllegalArgumentException) { cook(kitchen, SEED, ['skills-root': ['../skills']]) }
    assert outside.message.startsWith("${SEED}: --skills-root=../skills is outside the repository")
  }

  @Test void ownsTheSkillUnderEachNamedRoot() {
    def kitchen = kitchen()

    cook(kitchen, SEED, ['skills-root': ['.claude/skills', '.agents/skills']]).commit()

    def shipped = Kitchens.files(SKILL)
    assert shipped.contains('SKILL.md')
    def lock = new File(kitchen, 'cook.lock').text
    ['.claude/skills', '.agents/skills'].each { root ->
      shipped.each { assert new File(kitchen, "${root}/docs-layout/${it}").bytes == new File(SKILL, it).bytes }
      shipped.each { assert lock.contains("\"${root}/docs-layout/${it}\"") }
    }

    cook(kitchen, SEED, ['skills-root': ['.agents/skills']]).commit()

    assert !new File(kitchen, '.claude/skills/docs-layout/SKILL.md').exists()
    assert new File(kitchen, '.agents/skills/docs-layout/SKILL.md').exists()
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'docs/seed.groovy').text, 'seed.groovy')

    assert texts.params*.names == [['kinds'], ['skills-root']]
    assert texts.ingredients == ['io.github.lmarqs.cookbook.docs.Docs']
    assert texts.help.readLines().findAll { it.startsWith('## ') } ==
    ['## API', '## Produces', '## Connections', '## Re-cooking']
    assert texts.help.contains('Never touches')
    assert texts.yieldText
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
    def frontLines = front.readLines()
    def description = frontLines.find { it.startsWith('description: ') }.substring(13)

    ['`<kind> [title]`', '`term <term>`', '`kind <name>`', '`review`', '`review --fix`', '`adopt`']
    .each {
      assert text.contains("| ${it} |")
    }
    assert front.readLines().contains('name: docs-layout')
    assert description.length() <= 1024
    assert text.readLines().size() < 500
    def pageTemplates = new File(SKILL, 'templates').listFiles()*.name.findAll { it != 'map.template.md' }
    pageTemplates.each { assert text.contains(it) }
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
