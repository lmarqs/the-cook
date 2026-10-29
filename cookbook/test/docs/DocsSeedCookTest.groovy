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
  private static final List<String> KINDS = ['adr', 'api', 'features', 'runbooks', 'ux']
  private static final File SKILL = new File(COOKBOOK, 'docs/skills/docs-layout')

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void seedsTheMapAndOwnsItsKindsSection() {
    def kitchen = kitchen()

    cook(kitchen, SEED, [skills: ['.agents/skills']]).commit()

    def map = new File(kitchen, 'docs/README.md').text
    assert map.startsWith(new File(SKILL, 'templates/map.template.md').text)
    assert map.contains('<!-- cook:begin io.github.lmarqs.cookbook:docs#seed/kinds -->\n' +
    '<!-- io.github.lmarqs.cookbook:docs#seed --skills=.agents/skills -->\n')
    KINDS.each { assert map.contains("`${it}/`") }
    assert map.contains('`sh .agents/skills/docs-layout/scripts/check.sh`')
    ['docs/adr', 'docs/api', 'docs/features', 'docs/runbooks', 'docs/ux', 'docs/glossary.md'].each {
      assert !new File(kitchen, it).exists()
    }
    assert cook(kitchen, SEED, [skills: ['.agents/skills']]).actions == [:]
  }

  @Test void kindsNamesTheKindsTheMapOffersAndKeepsTheTeamsIntroduction() {
    def kitchen = kitchen()
    cook(kitchen, SEED, [skills: ['.agents/skills'], kinds: ['runbooks', 'adr']]).commit()
    def map = new File(kitchen, 'docs/README.md')
    assert map.text.contains('<!-- io.github.lmarqs.cookbook:docs#seed --kinds=adr --kinds=runbooks ' +
    '--skills=.agents/skills -->')
    assert map.text.contains('`adr/`') && map.text.contains('`runbooks/`') && !map.text.contains('`ux/`')

    map.text = '# Our docs\n\nStart here.\n' + map.text.substring(map.text.indexOf('<!-- cook:begin'))
    cook(kitchen, SEED, [skills: ['.agents/skills']]).commit()

    assert map.text.startsWith('# Our docs\n\nStart here.\n')
    assert map.text.contains('`ux/`')
  }

  @Test void refusesWithoutSkillsAndAnUnknownKind() {
    def kitchen = kitchen()
    assert shouldFail(IllegalArgumentException) { cook(kitchen, SEED, [:]) }.message ==
    "${SEED}: Missing --skills: The directory, from the repository root, the docs-layout skill is written under, " +
    'such as .agents/skills'
    assert shouldFail(IllegalArgumentException) { cook(kitchen, SEED, [skills: ['s'], kinds: ['howto']]) }.message ==
    "${SEED}: --kinds=howto is not one of: adr, api, features, runbooks, ux"
  }

  @Test void ownsTheSkillUnderTheNamedDirectory() {
    def kitchen = kitchen()

    cook(kitchen, SEED, [skills: ['.claude/skills']]).commit()

    def shipped = Kitchens.files(SKILL)
    assert shipped.contains('SKILL.md')
    shipped.each { assert new File(kitchen, ".claude/skills/docs-layout/${it}").bytes == new File(SKILL, it).bytes }
    def lock = new File(kitchen, 'cook.lock').text
    shipped.each { assert lock.contains("\".claude/skills/docs-layout/${it}\"") }
    assert new File(kitchen, 'docs/README.md').text.contains('`sh .claude/skills/docs-layout/scripts/check.sh`')
  }

  @Test void statesTheRuleInEachInstructionsFileAndRemovesItWhenNoLongerNamed() {
    def kitchen = kitchen()
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Agents\n'
    def readme = new File(kitchen, 'README.md')
    readme.text = '# Project\n'
    def both = [skills: ['.agents/skills'], instructions: ['README.md', 'AGENTS.md']]
    def line = 'io.github.lmarqs.cookbook:docs#seed --instructions=AGENTS.md --instructions=README.md ' +
        '--skills=.agents/skills'
    def section = { String opening ->
      '<!-- cook:begin io.github.lmarqs.cookbook:docs#seed/docs -->\n' +
          "<!-- ${opening} -->\n" +
          '- Docs: `docs/` is the source of truth; `docs/README.md` says which page owns a fact. Read the owning ' +
          'page before\n' +
          '  changing an area. A behavior change ships with its page, and a fact learned about the system goes to ' +
          'the page that\n' +
          '  owns it. `sh .agents/skills/docs-layout/scripts/check.sh` must pass; write a page with the ' +
          '`docs-layout` skill.\n' +
          '<!-- cook:end io.github.lmarqs.cookbook:docs#seed/docs -->\n'
    }

    cook(kitchen, SEED, both).commit()

    assert agents.text == '# Agents\n' + section(line)
    assert readme.text == '# Project\n' + section(line)
    def lines = section(line).readLines()
    assert lines.every { it.length() <= 120 }
    assert cook(kitchen, SEED, both).actions == [:]

    cook(kitchen, SEED, [skills: ['.agents/skills'], instructions: ['AGENTS.md']]).commit()

    assert agents.text == '# Agents\n' +
    section('io.github.lmarqs.cookbook:docs#seed --instructions=AGENTS.md --skills=.agents/skills')
    assert readme.text == '# Project\n'
  }

  @Test void refusesAnInstructionsFileOutsideTheRepositoryOrMissing() {
    def kitchen = kitchen()
    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, SEED, [skills: ['.agents/skills'], instructions: ['AGENTS.md']])
    }
    assert error.message.readLines() == [
      "${SEED}: AGENTS.md section io.github.lmarqs.cookbook:docs#seed/docs",
      '  AGENTS.md is missing',
      '    the section goes into a file the team keeps, and the recipe never creates it',
      '    create the file, or cook the recipe that writes it earlier in the same invocation',
    ]*.toString()
    def outside = shouldFail(IllegalArgumentException) {
      cook(kitchen, SEED, [skills: ['.agents/skills'], instructions: ['/tmp/AGENTS.md']])
    }
    assert outside.message.startsWith("${SEED}: --instructions=/tmp/AGENTS.md is outside the repository")
    def skillsOutside = shouldFail(IllegalArgumentException) { cook(kitchen, SEED, [skills: ['../skills']]) }
    assert skillsOutside.message.startsWith("${SEED}: --skills=../skills is outside the repository")
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'docs/seed.groovy').text, 'seed.groovy')

    assert texts.params*.names == [['instructions'], ['kinds'], ['skills']]
    assert texts.ingredients == ['io.github.lmarqs.cookbook.docs.Docs']
    assert texts.help.readLines().findAll { it.startsWith('## ') } ==
    [
      '## API',
      '## Consumes',
      '## Produces',
      '## Connections',
      '## Run-time effects',
      '## Re-cooking',
      '## Traps'
    ]
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
            assert new File(page.parentFile, target).canonicalPath.startsWith(SKILL.canonicalPath + '/')
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

    ['`<kind> <title>`', '`term <term>`', '`review`', '`review --fix`', '`adopt`'].each {
      assert text.contains("| ${it} |")
    }
    assert front.readLines().contains('name: docs-layout')
    assert description.length() <= 1024
    assert text.readLines().size() < 500
    def pageTemplates = new File(SKILL, 'templates').listFiles()*.name.findAll {
      !it.startsWith('guide-') && it != 'map.template.md'
    }
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
