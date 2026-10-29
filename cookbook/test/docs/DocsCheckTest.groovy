package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook

import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DocsCheckTest {
  private static final String SEED = 'io.github.lmarqs.cookbook:docs:0.1.0#seed'
  private static final File TEMPLATES = new File(COOKBOOK, 'docs/skills/docs-layout/templates')
  private static final List<String> KINDS = ['adrs', 'api', 'architecture', 'features', 'philosophy', 'runbooks', 'ux']
  private static final Map<String, String> PAGE_TEMPLATES = [
    adrs: 'adr', api: 'api', architecture: 'architecture', features: 'feature', philosophy: 'philosophy',
    runbooks: 'runbook', ux: 'standard',
  ]
  private static final List<String> SINGLE_PAGE_KINDS = ['architecture', 'philosophy']
  private static final String README = '# Project\n\n[Docs](docs/README.md)\n\n## Objective\n\nShip it.\n'

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
    new File(kitchen, 'README.md').text = README
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

  @Test void resolvesRootLinksFootnotesAndEveryHeadingForm() {
    def kitchen = cooked()
    new File(kitchen, 'docs/README.md').append('''
See [the repository](..), [its root](/) and a note.[^1]

[^1]: A note on the past.

Setext Heading
--------------

##\tTabbed

## See [the map](README.md)

   ## Indented

````md
```
[x](nope.md)
```
````

[legacy]: README.md

[a](#setext-heading), [b](#tabbed), [c](#see-the-map), [d](#indented)
''')

    def (exit, lines) = check(kitchen)

    assert exit == 0
    assert findings(lines) == []
  }

  @Test void readsPagesWithWindowsLineEndings() {
    def kitchen = cooked([skills: ['.agents/skills'], kinds: ['adrs']])
    def crlf = { String text -> text.replace('\n', '\r\n') }
    def map = new File(kitchen, 'docs/README.md')
    map.text = crlf(map.text)
    page(kitchen, 'docs/adrs/README.md', crlf(guide('adrs') + '\n- [Use X](0001-use-x.md): a decision.\n'))
    page(kitchen, 'docs/adrs/0001-use-x.md',
        crlf('# Use X\n\n## Context\n\nc\n\n## Options\n\no\n\n## Decision\n\nd\n\n## Consequences\n\nq\n'))
    page(kitchen, 'docs/ux/README.md', crlf(guide('ux')))

    def (exit, lines) = check(kitchen)

    assert exit == 1
    def outside = 'sits outside the map, the glossary and the kinds the map declares'
    assert findings(lines) == ["FAIL docs/ux/README.md ${outside}".toString()]
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
    def kitchen = cooked([skills: ['.agents/skills'], kinds: ['adrs']])
    page(kitchen, 'docs/notes.md', '# Notes\n')
    page(kitchen, 'docs/ux/README.md', guide('ux'))

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/notes.md sits outside the map, the glossary and the kinds the map declares',
      'FAIL docs/ux/README.md sits outside the map, the glossary and the kinds the map declares',
    ]
  }

  @Test void failsOnADecisionNameADuplicateNumberAndMisorderedSections() {
    def kitchen = cooked()
    def record = '# Use X\n\n## Context\n\nc\n\n## Options\n\no\n\n## Decision\n\nd\n\n## Consequences\n\nq\n'
    page(kitchen, 'docs/adrs/12-x.md', record)
    page(kitchen, 'docs/adrs/0001-a.md', record)
    page(kitchen, 'docs/adrs/0001-b.md', record)
    page(kitchen, 'docs/adrs/0002-c.md', '# Use Y\n\n## Context\n\n## Decision\n\n## Options\n')
    page(kitchen, 'docs/adrs/README.md', guide('adrs') + '\n' +
        ['12-x', '0001-a', '0001-b', '0002-c'].collect { "- [${it}](${it}.md): a decision.\n" }.join(''))

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/adrs/0001-b.md reuses the number 0001 of docs/adrs/0001-a.md',
      'FAIL docs/adrs/0002-c.md decision record sections are [Context, Decision, Options]',
      'FAIL docs/adrs/12-x.md is named outside the pattern NNNN-<slug>.md',
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

  @Test void failsOnAnArchitectureOrPhilosophyPageMissingItsSection() {
    def kitchen = cooked()
    page(kitchen, 'docs/architecture.md', '# Architecture\n\nA shop.\n\n## How they connect\n\nOver HTTP.\n')
    page(kitchen, 'docs/philosophy.md', '# Philosophy\n\nOur tenets.\n')

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines) == [
      'FAIL docs/architecture.md architecture page sections are []',
      'FAIL docs/philosophy.md philosophy page sections are []',
    ]
  }

  @Test void acceptsTheKindsTheTeamDeclaresInTheMap() {
    def kitchen = cooked([skills: ['.agents/skills'], kinds: ['adrs']])
    new File(kitchen, 'docs/README.md').append('''
## Threat models, `threats/`

One page per asset an attacker wants.

## Data model, `data-model.md`

The entities and how they relate.

```md
## Notes, `notes/`
```
''')
    page(kitchen, 'docs/threats/README.md', '# Threat models\n\n- [Login](login.md): the login form.\n')
    page(kitchen, 'docs/threats/login.md', '# Login\n\nStolen passwords.\n')
    page(kitchen, 'docs/threats/signup.md', '# Signup\n\nFake accounts.\n')
    page(kitchen, 'docs/data-model.md', '# Data model\n\nOrders have lines.\n')
    page(kitchen, 'docs/notes/a.md', '# A\n')
    page(kitchen, 'docs/architecture.md', '# Architecture\n')

    def (exit, lines) = check(kitchen)

    assert exit == 1
    def outside = 'sits outside the map, the glossary and the kinds the map declares'
    assert findings(lines) == [
      "FAIL docs/architecture.md ${outside}".toString(),
      "FAIL docs/notes/a.md ${outside}".toString(),
      'FAIL docs/threats/signup.md is missing from docs/threats/README.md',
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

  @Test void acceptsTheTeamsOwnGuideTextAndWarnsWhenTheRootReadmeLacksTheMapAndTheObjective() {
    def kitchen = cooked()
    new File(kitchen, 'README.md').text = '# Project\n'
    page(kitchen, 'docs/ux/README.md', '# Standards\n\nOur rules.\n\n- [Naming](naming.md): names.\n')
    page(kitchen, 'docs/ux/naming.md', standard('Naming'))

    def (exit, lines) = check(kitchen)

    assert exit == 0
    def lacks = 'WARN README.md lacks'
    assert findings(lines) == ["${lacks} a link to docs/README.md", "${lacks} an Objective section"]*.toString()
  }

  @Test void everyTemplateFilledInPasses() {
    def kitchen = cooked()
    int blank = 0
    def filled = { String text ->
      text.replaceAll(/\[\[fill: [^\]]*\]\]/) {
        "filled${++blank}"
      }
    }
    new File(kitchen, 'README.md').text = '# Project\n\n[Docs](docs/README.md)\n\n' +
        filled(new File(TEMPLATES, 'objective.template.md').text)
    page(kitchen, 'docs/glossary.md', filled(new File(TEMPLATES, 'glossary.template.md').text))
    SINGLE_PAGE_KINDS.each { kind ->
      page(kitchen, "docs/${kind}.md", filled(new File(TEMPLATES, "${kind}.template.md").text))
    }
    (KINDS - SINGLE_PAGE_KINDS).each { kind ->
      def name = kind == 'adrs' ? '0001-first.md' : 'first.md'
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

  @Test void readsTheTreeOfTheDirectoryItRunsIn() {
    def kitchen = cooked()
    def script = new File(kitchen, '.agents/skills/docs-layout/scripts/check.sh').path
    def nested = new File(kitchen, '.worktrees/feature')
    nested.mkdirs()

    [new File(kitchen, 'docs'), nested, folder.newFolder('elsewhere')].each { directory ->
      def (exit, output) = run(directory, ['sh', script])

      assert exit == 1
      assert findings(output.readLines()) == ['FAIL docs/README.md, the map, is missing']
    }
  }

  @Test void failsOnAPageItCannotRead() {
    def kitchen = cooked()
    page(kitchen, 'docs/features/README.md', guide('features') + '\n- [A](a.md): a page.\n')
    page(kitchen, 'docs/features/a.md', '# A\n\n[x](nope.md)\n')
    def unreadable = new File(kitchen, 'docs/features/a.md')
    unreadable.setReadable(false, false)
    Assume.assumeFalse('this user reads every file', unreadable.canRead())

    def (exit, lines) = check(kitchen)

    assert exit == 1
    assert findings(lines).findAll { it ==~ /(FAIL|WARN) .*/ } == ['FAIL docs/ holds a page the check could not read']
  }

  private File cooked(Map<String, List<String>> arguments = [skills: ['.agents/skills']]) {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'docs')
    cook(kitchen, SEED, arguments).commit()
    kitchen
  }

  private static String guide(String kind) {
    "# ${kind}\n"
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
