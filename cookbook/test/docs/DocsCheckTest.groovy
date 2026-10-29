package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DocsCheckTest {
  private static final String SEED = 'io.github.lmarqs.cookbook:docs:0.1.0#seed'
  private static final File TEMPLATES = new File(COOKBOOK, 'docs/skills/docs-layout/templates')
  private static final List<String> KINDS = ['adr', 'api', 'features', 'runbooks', 'ux']
  private static final Map<String, String> PAGE_TEMPLATES = [
    adr: 'adr', api: 'api', features: 'feature', runbooks: 'runbook', ux: 'standard',
  ]

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void aFreshSeedPasses() {
    def kitchen = cooked()

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert lines && lines.every { it.startsWith('ok ') }
  }

  @Test void failsOnALinkToAMissingFile() {
    def kitchen = cooked()
    def map = new File(kitchen, 'docs/README.md')
    map.append('\nSee [x](nope.md).\n')
    int line = map.readLines().size()

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == ["FAIL docs/README.md:${line} link nope.md points at a missing file".toString()]
  }

  @Test void failsOnAMissingAnchorAndResolvesHeadingSlugs() {
    def kitchen = cooked()
    page(kitchen, 'docs/runbooks/README.md', guide('runbooks') + '\n- [Disk full](disk-full.md): frees space.\n')
    page(kitchen, 'docs/runbooks/disk-full.md', '''# Disk full

The disk is free again. Owner: ops.

## Before you begin

## Steps

## Steps

## If it fails

See [a](#before-you-begin), [b](#steps-1), [c](#if-it-fails), [d](README.md#runbooks), [e](#missing).
''')

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == ['FAIL docs/runbooks/disk-full.md:13 link #missing points at a missing heading']
  }

  @Test void resolvesRepositoryAbsoluteReferenceStyleAndImageLinks() {
    def kitchen = cooked()
    new File(kitchen, 'README.md').text = '# Project\n\n[Docs](docs/README.md)\n'
    new File(kitchen, 'docs/img.png').bytes = [1] as byte[]
    new File(kitchen, 'docs/README.md').append('\n[a](/README.md), [b][r], ![c](img.png), ' +
        '[d](https://example.com/x)\n\n[r]: ../README.md\n')

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert findings(lines) == []
  }

  @Test void ignoresLinksInFencedCodeInlineCodeAndComments() {
    def kitchen = cooked()
    new File(kitchen, 'docs/README.md').append('\n```md\n[x](nope.md)\n```\n\n`[y](nope.md)`\n\n' +
        '<!-- [z](nope.md) -->\n')

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert findings(lines) == []
  }

  @Test void failsOnAKindDirectoryWithoutAGuideAndAPageMissingFromItsGuide() {
    def kitchen = cooked()
    page(kitchen, 'docs/ux/naming.md', standard('Naming'))
    page(kitchen, 'docs/features/search.md', '# Search\n\nFinds pages.\n')
    page(kitchen, 'docs/features/README.md', guide('features'))

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/features/search.md is missing from docs/features/README.md',
      'FAIL docs/ux has pages and lacks its guide README.md',
    ]
  }

  @Test void failsOnMarkdownOutsideTheKindsTheMapOffers() {
    def kitchen = cooked([skills: ['.agents/skills'], kinds: ['adr']])
    page(kitchen, 'docs/notes.md', '# Notes\n')
    page(kitchen, 'docs/ux/README.md', guide('ux'))

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/notes.md sits outside the map, the glossary and the kind directories the map offers',
      'FAIL docs/ux/README.md sits outside the map, the glossary and the kind directories the map offers',
    ]
  }

  @Test void failsOnADecisionNameADuplicateNumberAndMisorderedSections() {
    def kitchen = cooked()
    def record = '# Use X\n\n## Context\n\nc\n\n## Options\n\no\n\n## Decision\n\nd\n\n## Consequences\n\nq\n'
    page(kitchen, 'docs/adr/12-x.md', record)
    page(kitchen, 'docs/adr/0001-a.md', record)
    page(kitchen, 'docs/adr/0001-b.md', record)
    page(kitchen, 'docs/adr/0002-c.md', '# Use Y\n\n## Context\n\n## Decision\n\n## Options\n')
    page(kitchen, 'docs/adr/README.md', guide('adr') + '\n' +
        ['12-x', '0001-a', '0001-b', '0002-c'].collect { "- [${it}](${it}.md): a decision.\n" }.join(''))

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/adr/0001-b.md reuses the number 0001 of docs/adr/0001-a.md',
      'FAIL docs/adr/0002-c.md decision record sections are [Context, Decision, Options]',
      'FAIL docs/adr/12-x.md is named outside the pattern NNNN-<slug>.md',
    ]
  }

  @Test void failsOnARunbookOrInterfacePageMissingItsSections() {
    def kitchen = cooked()
    page(kitchen, 'docs/runbooks/README.md', guide('runbooks') + '\n- [Deploy](deploy.md): ships a release.\n')
    page(kitchen, 'docs/runbooks/deploy.md', '# Deploy\n\n## Before you begin\n\n## Steps\n')
    page(kitchen, 'docs/api/README.md', guide('api') + '\n- [Search](search.md): the search API.\n')
    page(kitchen, 'docs/api/search.md', '# Search\n\nFor the web app.\n')

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/api/search.md interface page sections are []',
      'FAIL docs/runbooks/deploy.md runbook sections are [Before you begin, Steps]',
    ]
  }

  @Test void warnsOnFlaggedWordsAvoidedTermsAndUnfilledBlanks() {
    def kitchen = cooked()
    page(kitchen, 'docs/glossary.md', '# Glossary\n\n**Container**:\nA box that holds items.\n_Avoid_: bucket, bin\n')
    page(kitchen, 'docs/features/README.md', guide('features') + '\n- [Store](store.md): keeps items.\n')
    page(kitchen, 'docs/features/store.md', '''# Store

The store currently keeps items in a Bucket. TODO: legacy paths.

`TBD` and `bucket` in code are fine, and so are bins and TODOs.

[[fill: the lead]]
''')

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert findings(lines) == [
      'WARN docs/features/store.md:3 says "currently"',
      'WARN docs/features/store.md:3 says "legacy"',
      'WARN docs/features/store.md:3 says "TODO"',
      'WARN docs/features/store.md:3 uses "bucket", a synonym the glossary rejects',
      'WARN docs/features/store.md:7 has an unfilled blank [[fill:',
    ]
  }

  @Test void warnsWhenAGuidesTextDiffersFromTheTemplateAndTheRootReadmeLacksTheMap() {
    def kitchen = cooked()
    new File(kitchen, 'README.md').text = '# Project\n'
    page(kitchen, 'docs/ux/README.md', '# Standards\n\nOur rules.\n\n- [Naming](naming.md): names.\n')
    page(kitchen, 'docs/ux/naming.md', standard('Naming'))

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert findings(lines) == [
      'WARN docs/ux/README.md text above the list differs from the release template',
      'WARN README.md lacks a link to docs/README.md',
    ]
  }

  @Test void everyTemplateFilledInPasses() {
    def kitchen = cooked()
    int blank = 0
    def filled = { String text ->
      text.replaceAll(/\[\[fill: [^\]]*\]\]/) {
        "filled${++blank}"
      }
    }
    new File(kitchen, 'README.md').text = '# Project\n\n[Docs](docs/README.md)\n'
    page(kitchen, 'docs/glossary.md', filled(new File(TEMPLATES, 'glossary.template.md').text))
    KINDS.each { kind ->
      def name = kind == 'adr' ? '0001-first.md' : 'first.md'
      page(kitchen, "docs/${kind}/README.md", guide(kind) + "\n- [First](${name}): the first page.\n")
      page(kitchen, "docs/${kind}/${name}", filled(new File(TEMPLATES, "${PAGE_TEMPLATES[kind]}.template.md").text))
    }

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert findings(lines) == []
  }

  @Test void writesNothing() {
    def kitchen = cooked()
    page(kitchen, 'docs/notes.md', '[x](nope.md)\n')
    def state = snapshot(kitchen)

    check(kitchen)

    assert snapshot(kitchen) == state
  }

  @Test void runsFromAnyDirectoryBelowTheMapAndFailsWhereNoneHoldsIt() {
    def kitchen = cooked()
    def script = new File(kitchen, '.agents/skills/docs-layout/scripts/check.sh').path

    def (exit, output) = run(new File(kitchen, 'docs'), ['sh', script])

    assert exit == 0
    assert findings(output.readLines()) == []

    def (outsideExit, outside) = run(folder.newFolder('elsewhere'), ['sh', script])

    assert outsideExit == 1
    assert findings(outside.readLines()) == ['FAIL docs/README.md, the map, is missing']
  }

  private File cooked(Map<String, List<String>> arguments = [skills: ['.agents/skills']]) {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'docs')
    cook(kitchen, SEED, arguments).commit()
    kitchen
  }

  private static String guide(String kind) {
    new File(TEMPLATES, "guide-${kind}.template.md").text
  }

  private static String standard(String title) {
    "# ${title}\n\nScope: every command.\n\n## Rules\n\n- Names MUST be lowercase. Lowercase is easier to type.\n"
  }

  private static void page(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
  }

  private static List check(File kitchen) {
    def (exit, output) = run(kitchen, ['sh', '.agents/skills/docs-layout/scripts/check.sh'])
    [exit, output.readLines()]
  }

  // The findings without the "-- next step" part, which the tests leave to the script's wording.
  private static List<String> findings(List<String> lines) {
    lines.findAll { !it.startsWith('ok ') }.collect { it.contains(' -- ') ? it.substring(0, it.indexOf(' -- ')) : it }
  }

  // Each file under the kitchen with its content, to compare before and after a run.
  private static Map<String, String> snapshot(File kitchen) {
    Kitchens.files(kitchen).collectEntries { [(it): new File(kitchen, it).text] }
  }

  private static List run(File kitchen, List<String> command) {
    def builder = new ProcessBuilder(command).directory(kitchen).redirectErrorStream(true)
    def process = builder.start()
    def output = process.inputStream.text
    [process.waitFor(), output]
  }
}
