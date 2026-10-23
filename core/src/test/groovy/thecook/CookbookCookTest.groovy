package thecook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Argument
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookCook
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookLock
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository
import thecook.cookbook.ExistingSeed
import thecook.cookbook.KeyTarget
import thecook.cookbook.RecipeRequest
import thecook.cookbook.SectionTarget

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class CookbookCookTest {
  private static final File COOKBOOK = new File(System.getProperty('cook.cookbook'))
  private static final File TEMPLATES = new File(COOKBOOK, 'agents/skills/agents-layout/templates')
  private static final String TEXT_MODEL = '''package acme.text

@Ingredient
class Text {
  String text
  @File(path = 'OUT.md')
  String file() { text }
}
'''

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void refusesEveryExistingSeededFileOfTheCommandAtOnce() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:starters:1.0.0', [
      'Starters.groovy': '''package acme.starters

@Ingredient
class Starters {
  String first
  String second
  @Seed(path = 'FIRST.md', existing = 'fail')
  String firstStarter() { first }
  @Seed(path = 'SECOND.md', existing = 'fail')
  String secondStarter() { second }
}
''',
      'one.groovy': '''@help('Seeds FIRST.md.')
import acme.starters.Starters
@ingredient(Starters) def starters
starters.first = 'first'
''',
      'two.groovy': '''@help('Seeds SECOND.md.')
import acme.starters.Starters
@ingredient(Starters) def starters
starters.second = 'second'
''',
    ])
    new File(kitchen, 'FIRST.md').text = 'ours\n'
    new File(kitchen, 'SECOND.md').text = 'ours\n'
    def error = shouldFail(IllegalArgumentException) {
      new CookbookCook(kitchen, repositories(kitchen))
          .propose([request('acme:starters:1.0.0#one', [:]), request('acme:starters:1.0.0#two', [:])])
    }
    assert error.message == 'These files already exist, and their recipes write them from a template ' +
    '''the first time they are cooked:
  FIRST.md (acme:starters#one)
  SECOND.md (acme:starters#two)
  run with --force to replace them, or --skip to keep them'''
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void refusesToWriteASectionOrKeyThroughASymbolicLink() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def link = { String path, String target ->
      def file = new File(kitchen, path); file.parentFile.mkdirs()
      Files.createSymbolicLink(file.toPath(), java.nio.file.Paths.get(target))
    }
    link('.gitignore', 'OURS')
    def section = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }
    assert section.message == 'io.github.lmarqs.cookbook:agents:0.1.0#connect: .gitignore section ' +
    'io.github.lmarqs.cookbook:agents#connect/harnesses\n  .gitignore is a symbolic link to OURS\n' +
    '    Cook writes sections and keys only into regular files'
    assert Files.isSymbolicLink(new File(kitchen, '.gitignore').toPath())

    new File(kitchen, '.gitignore').delete()
    new File(kitchen, '.agents/adapters/claude').mkdirs()
    new File(kitchen, '.agents/adapters/claude/shared.json').text = '{}\n'
    link('.agents/adapters/claude/settings.json', 'shared.json')
    def key = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }
    assert key.message == 'io.github.lmarqs.cookbook:agents:0.1.0#connect: .agents/adapters/claude/settings.json ' +
    'key permissions\n  .agents/adapters/claude/settings.json is a symbolic link to shared.json\n' +
    '    Cook writes sections and keys only into regular files'
    assert new File(kitchen, '.agents/adapters/claude/shared.json').text == '{}\n'
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void refusesToReplaceAFileOrDirectoryItDoesNotOwnWithALink() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def config = new File(kitchen, '.codex/config.toml'); config.parentFile.mkdirs()
    config.text = '[features]\nhooks = true\n'
    def codex = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']])
    }
    assert codex.message == 'io.github.lmarqs.cookbook:agents:0.1.0#connect: .codex/config.toml link\n' +
    '  .codex/config.toml is a file, and this ' +
    'recipe makes it a link to ../.agents/adapters/codex/config.toml\n    its contents would stop being read\n' +
    '    move its contents into .agents/adapters/codex/config.toml, delete it, and cook again'
    def forced = shouldFail(IllegalArgumentException) {
      new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
          .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']])])
    }
    assert forced.message == codex.message
    assert config.text == '[features]\nhooks = true\n'

    config.delete()
    new File(kitchen, '.claude/skills/ours').mkdirs()
    new File(kitchen, '.claude/skills/ours/SKILL.md').text = '# Ours\n'
    def claude = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }
    assert claude.message == 'io.github.lmarqs.cookbook:agents:0.1.0#connect: .claude/skills link\n' +
    '  .claude/skills is a directory, and ' +
    'this recipe makes it a link to ../.agents/skills\n    its files would stop being read\n' +
    '    move its files into .agents/skills/, delete it, and cook again'
    assert new File(kitchen, '.claude/skills/ours/SKILL.md').text == '# Ours\n'
    assert !new File(kitchen, '.agents').exists()
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void replacesALinkThatPointsElsewhereAndKeepsOneItOwns() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def config = new File(kitchen, '.codex/config.toml'); config.parentFile.mkdirs()
    Files.createSymbolicLink(config.toPath(), java.nio.file.Paths.get('../OURS.toml'))

    def proposal = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']])
    assert '.codex/config.toml' in proposal.changes
    proposal.commit()
    assert Files.readSymbolicLink(config.toPath()).toString() == '../.agents/adapters/codex/config.toml'
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).changes == []
  }

  @Test void aTargetThatFailsToWriteIsNamedAboveItsCause() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def settings = new File(kitchen, '.agents/adapters/claude/settings.json'); settings.parentFile.mkdirs()
    settings.text = '{\n  // ours\n  "model": "opus"\n}\n'
    def json = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }.message.readLines()
    assert json[0] ==
    'io.github.lmarqs.cookbook:agents:0.1.0#connect: .agents/adapters/claude/settings.json key permissions'
    assert json[1].startsWith('  JSON: ')
    assert json[2] == '    settings files are strict JSON: remove comments and trailing commas'
    assert json.size() == 3

    publish(kitchen, 'acme:notes:1.0.0', [
      'Notes.groovy': '''package acme.notes

@Ingredient
class Notes {
  private Map values = [:]
  void set(Map more) { values += more }
  @Keys(path = 'config.toml')
  Map keys() { values ?: null }
}
''',
      'features.groovy': '''@help('Sets features.')
import acme.notes.Notes
@ingredient(Notes) def notes
notes.set(features: [hooks: true])
''',
    ])
    new File(kitchen, 'config.toml').text = '[features]\nhooks = false\n'
    def toml = shouldFail(IllegalArgumentException) { cook(kitchen, 'acme:notes:1.0.0#features', [:]) }
    assert toml.message == 'acme:notes:1.0.0#features: config.toml key features\n' +
    '  key features is written as a [features] table\n' +
    '    Cook owns features whole and writes it as one inline table\n' +
    '    replace it with features = { … } above the first [table], then cook again'

    def config = new File(kitchen, '.agents/adapters/codex/config.toml'); config.parentFile.mkdirs()
    config.text = '[approval_policy]\nx = 1\n'
    def scalar = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']])
    }
    assert scalar.message == 'io.github.lmarqs.cookbook:agents:0.1.0#connect: ' +
    '.agents/adapters/codex/config.toml key approval_policy\n' +
    '  key approval_policy is written as a [approval_policy] table\n' +
    '    Cook owns approval_policy whole and writes it as one line, approval_policy = …\n' +
    '    replace it with approval_policy = … above the first [table], then cook again'
  }

  @Test void aTargetFileThatIsNotARegularFileIsNamedRelativeToTheKitchen() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    new File(kitchen, '.agents/adapters/claude/settings.json').mkdirs()
    def directory = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }
    assert directory.message == 'io.github.lmarqs.cookbook:agents:0.1.0#connect: ' +
    '.agents/adapters/claude/settings.json key permissions\n  Not a regular file: .agents/adapters/claude/settings.json'

    new File(kitchen, '.agents/adapters/claude/settings.json').deleteDir()
    new File(kitchen, '.agents/adapters/claude/settings.json').bytes = [0xC3, 0x28] as byte[]
    def encoding = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }
    assert encoding.message.readLines()[1] == '  Invalid UTF-8 file: .agents/adapters/claude/settings.json'
  }

  @Test void aTargetThatFailsToWithdrawIsNamedAboveItsCause() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]).commit()
    def config = new File(kitchen, '.agents/adapters/codex/config.toml')
    def cooked = config.text
    config.text = cooked.replace('approval_policy = "on-request"\n', '') + '\n[approval_policy]\nx = 1\n'

    def withdrawn = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    }
    assert withdrawn.message.readLines() == [
      'io.github.lmarqs.cookbook:agents:0.1.0#connect: .agents/adapters/codex/config.toml key approval_policy',
      '  key approval_policy is written as a [approval_policy] table',
      '    Cook owns approval_policy whole as one line, approval_policy = …',
      '    replace it with approval_policy = … above the first [table], then cook again'
    ]

    config.text = cooked
    def settings = new File(kitchen, '.agents/adapters/claude/settings.json')
    settings.text = '// ours\n' + settings.text
    def discarded = shouldFail(IllegalArgumentException) {
      new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:0.1.0#connect'])
    }
    assert discarded.message.readLines()[0] == '.agents/adapters/claude/settings.json key hooks.SessionStart'
  }

  @Test void cooksAKitchenWhosePathPassesThroughASymbolicLink() {
    def real = folder.newFolder('real')
    def linked = new File(folder.root, 'linked')
    Files.createSymbolicLink(linked.toPath(), real.toPath())
    def kitchen = new File(linked, 'kitchen'); kitchen.mkdirs()
    publishDirectory(kitchen, 'agents')

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()
    assert new File(real, 'kitchen/.claude/settings.json').text.contains('"permissions"')
  }

  @Test void cooksARecipePathWithTheModelBesideItAndHashesTheModelWithTheRecipe() {
    def kitchen = kitchen()
    def model = recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', """@help('Writes a file.')
import acme.text.Text
@ingredient(Text) def text
text.text = 'first\\n'
""")

    cook(kitchen, 'recipes/setup.groovy', [:]).commit()

    assert new File(kitchen, 'OUT.md').text == 'first\n'
    def recorded = sourcesSha256(kitchen, ['recipes/Text.groovy', 'recipes/setup.groovy'])
    assert new File(kitchen, 'cook.lock').text.contains("""  - name: "recipes/setup"
    source: "recipes/setup.groovy"
    sha256: "${recorded}"
    arguments: {}
""")
    assert cook(kitchen, './recipes/setup.groovy', [:]).changes.empty

    model.text += '// edited\n'
    def again = cook(kitchen, 'recipes/setup.groovy', [:])
    assert again.changes == ['cook.lock']
    assert !again.after('cook.lock').contains(recorded)
  }

  @Test void aModelBesideARecipePathReadsTheFilesOfItsDirectory() {
    def kitchen = kitchen()
    def source = new File(COOKBOOK, 'agents')
    source.eachFileRecurse(groovy.io.FileType.FILES) { file ->
      def copy = new File(kitchen, "agents/${source.toPath().relativize(file.toPath())}")
      copy.parentFile.mkdirs()
      copy.bytes = file.bytes
    }
    recipe(kitchen, 'agents/skill-check.groovy', """@help('Links one skill.')
import io.github.lmarqs.cookbook.agents.Agents
import io.github.lmarqs.cookbook.agents.Claude
@ingredient(Agents) def agents
@ingredient(Claude) def claude
agents.instruct('Run the tests.')
agents.skills('agents-layout')
agents.connect([claude])
""")

    cook(kitchen, 'agents/skill-check.groovy', [:]).commit()

    assert new File(kitchen, 'AGENTS.md').text.startsWith(new File(TEMPLATES, 'AGENTS.template.md').text)
    assert new File(kitchen, '.agents/skills/agents-layout/SKILL.md').text ==
    new File(COOKBOOK, 'agents/skills/agents-layout/SKILL.md').text
  }

  @Test void reportsAChangedSourceUnderARecordedVersionButNotAnEditedPath() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    def path = recipe(kitchen, 'recipes/setup.groovy', """@help('Writes a file.')
import acme.text.Text
@ingredient(Text) def text
text.text = 'first\\n'
""")
    def requests = [request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]), request('recipes/setup.groovy', [:]),]
    def first = new CookbookCook(kitchen, repositories(kitchen)).propose(requests)
    assert first.changedSources.empty
    first.commit()

    publishDirectory(kitchen, 'agents', ['NOTICE.md': 'changed under the same version'])
    path.text += '// edited\n'
    def again = new CookbookCook(kitchen, repositories(kitchen)).propose(requests)

    assert again.changedSources*.address == ['io.github.lmarqs.cookbook:agents:0.1.0#seed']
    def changed = again.changedSources.first()
    assert changed.recorded == sourcesSha256(new File(COOKBOOK, 'agents'))
    assert changed.current != changed.recorded
    assert again.after('cook.lock').contains(changed.current)
  }

  @Test void aRecipePathReadsAResourceBesideItAndHashesItWithTheRecipe() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', """import acme.text.Text
@resource('setup.md') String template
@ingredient(Text) def text
text.text = template
""")
    def template = recipe(kitchen, 'recipes/setup.md', 'first\n')

    cook(kitchen, 'recipes/setup.groovy', [:]).commit()

    assert new File(kitchen, 'OUT.md').text == 'first\n'
    def recorded = sourcesSha256(kitchen, ['recipes/Text.groovy', 'recipes/setup.groovy', 'recipes/setup.md'])
    assert new File(kitchen, 'cook.lock').text.contains("sha256: \"${recorded}\"")

    template.text = 'second\n'
    def again = cook(kitchen, 'recipes/setup.groovy', [:])
    assert again.changes == ['OUT.md', 'cook.lock']
    assert again.after('OUT.md') == 'second\n'
    assert !again.after('cook.lock').contains(recorded)
    assert again.changedSources.empty
  }

  @Test void refusesAResourceOutsideTheKitchenMissingOrMistyped() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    def outside = 'the resource is outside the kitchen\n  move it into the kitchen'
    new File(kitchen.parentFile, 'outside.md').text = 'outside\n'
    recipe(kitchen, 'recipes/outside.md', 'outside\n')
    Files.createSymbolicLink(new File(kitchen, 'recipes/linked.md').toPath(),
        new File(kitchen.parentFile, 'outside.md').toPath())
    [
      ["@resource('../../outside.md') String template", outside],
      ["@resource('linked.md') String template", outside],
      ["@resource('missing.md') String template", 'Resource file not found: recipes/missing.md'],
      ["@resource('outside' + '.md') String template", '@resource takes a constant path'],
      ["@resource('outside.md') List template", 'template has the type List, which a resource cannot have'],
    ].each { declaration, message ->
      recipe(kitchen, 'recipes/setup.groovy', """import acme.text.Text
${declaration}
@ingredient(Text) def text
text.text = template.toString()
""")

      def error = shouldFail(IllegalArgumentException) { cook(kitchen, 'recipes/setup.groovy', [:]) }
      assert error.message == "recipes/setup.groovy: ${message}"
      assert !new File(kitchen, 'OUT.md').exists()
    }
  }

  @Test void aRecipeByCoordinatesReadsAResourceFromItsArtifact() {
    def kitchen = kitchen()
    publishText(kitchen, [
      'setup.groovy': """import acme.text.Text
@resource('setup.md') String template
@ingredient(Text) def text
text.text = template
""",
      'setup.md': 'packaged\n',
      'missing.groovy': """import acme.text.Text
@resource('missing.md') String template
@ingredient(Text) def text
text.text = template
""",
    ])

    cook(kitchen, 'acme:text:1.0.0#setup', [:]).commit()
    assert new File(kitchen, 'OUT.md').text == 'packaged\n'

    new File(kitchen, 'OUT.md').delete()
    def error = shouldFail(IllegalArgumentException) { cook(kitchen, 'acme:text:1.0.0#missing', [:]) }
    assert error.message == 'acme:text:1.0.0#missing: No file missing.md in acme:text:1.0.0'
    assert !new File(kitchen, 'OUT.md').exists()
  }

  @Test void aRecipeReadsTheNameItsMarkersCarryByPathAndByCoordinates() {
    def kitchen = kitchen()
    def source = """import acme.text.Text
@recipe String self
@ingredient(Text) def text
text.text = self + '\\n'
"""
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', source)
    publishText(kitchen, ['setup.groovy': source])

    assert cook(kitchen, 'recipes/setup.groovy', [:]).after('OUT.md') == 'recipes/setup\n'
    assert cook(kitchen, 'acme:text:1.0.0#setup', [:]).after('OUT.md') == 'acme:text#setup\n'
  }

  @Test void aRecipeReadsItsArgumentLineWithGivenParamsInDeclarationOrderAndRepeatsDeduplicated() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', """import acme.text.Text
@param(value = ['agent'], many = true, doc = 'Agents, the first by default') String[] agent
@param(value = ['mode', 'm'], default = ['fast'], doc = 'The mode') String mode
@param(value = ['note'], default = [], doc = 'A note') String note
@param(value = ['file'], many = true, sorted = true, path = true, default = [], doc = 'Files') List<String> file
@recipe(arguments = true) String cookedWith
@ingredient(Text) def text
text.text = [cookedWith, agent.join(','), file.join(',')].join('\\n') + '\\n'
""")

    def cooked = cook(kitchen, 'recipes/setup.groovy',
        [agent: ['codex', 'claude', 'codex'], m: ['slow'], file: ['./b.md', 'a.md', 'b.md']])

    assert cooked.after('OUT.md') == '''\
recipes/setup --agent=codex --agent=claude --mode=slow --file=a.md --file=b.md
codex,claude
a.md,b.md
'''
    def defaulted = cook(kitchen, 'recipes/setup.groovy', [agent: ['claude']]).after('OUT.md')
    assert defaulted.readLines()[0] == 'recipes/setup --agent=claude'
  }

  @Test void recordsNormalizedArgumentsInTheLockWithoutRequiringARecipeNameField() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', """import acme.text.Text
@param(value = ['agent'], many = true, doc = 'Agents') String[] agent
@param(value = ['mode', 'm'], default = ['fast'], doc = 'The mode') String mode
@param(value = ['note'], default = [], doc = 'A note') String note
@param(value = ['file'], many = true, sorted = true, path = true, default = [], doc = 'Files') List<String> file
@ingredient(Text) def text
text.text = agent.join(',')
""")

    cook(kitchen, 'recipes/setup.groovy',
        [agent: ['codex', 'claude', 'codex'], m: ['slow'], file: ['./b.md', 'a.md', 'b.md']]).commit()

    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.arguments('recipes/setup') == [agent: ['codex', 'claude'], mode: ['slow'], file: ['a.md', 'b.md']]
    assert new File(kitchen, 'cook.lock').text.contains('''\
    arguments:
      "agent": ["codex","claude"]
      "mode": ["slow"]
      "file": ["a.md","b.md"]
''')
  }

  @Test void recordsAnEmptyArgumentMapWhenNoneAreGiven() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', """import acme.text.Text
@param(value = ['mode'], default = ['fast'], doc = 'The mode') String mode
@ingredient(Text) def text
text.text = mode
""")

    cook(kitchen, 'recipes/setup.groovy', [:]).commit()

    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.arguments('recipes/setup') == [:]
    assert new File(kitchen, 'cook.lock').text.contains('    arguments: {}\n')
  }

  @Test void quotesArgumentNamesAndValuesAsJsonStringsSoNoneIsReadAsAnotherType() {
    def lock = new CookLock()
    def arguments = [note: ['with spaces', '"quoted"\\path\nnext'], yes: ['true'], count: ['1']]
    lock.record('recipes/setup', 'recipes/setup.groovy', 'abc', arguments, [])

    def rendered = lock.render()

    assert rendered.readLines()[6..9] == [
      '    arguments:',
      '      "note": ["with spaces","\\"quoted\\"\\\\path\\nnext"]',
      '      "yes": ["true"]',
      '      "count": ["1"]',
    ]
    assert CookLock.parse(rendered).arguments('recipes/setup') == arguments
  }

  @Test void keepsArgumentsThatAreNotNamesWithListsOfValuesUntilTheRecipeIsCookedAgain() {
    ['["--mode=fast"]', '{"mode":"fast"}', '{"mode":[1]}'].each { arguments ->
      def lock = CookLock.parse("""version: 1
recipes:
  - name: "recipes/setup"
    source: "recipes/setup.groovy"
    sha256: "abc"
    arguments: ${arguments}
    owns: {}
""")

      def error = shouldFail(IllegalArgumentException) { lock.arguments('recipes/setup') }

      assert error.message == 'cook.lock records the arguments of recipes/setup in a form Cook cannot read\n' +
      '  each argument is a name with a list of values, such as "mode": ["fast"]\n' +
      '  cook recipes/setup again with its arguments, or discard it'
      assert lock.render().contains("    arguments: ${arguments}\n")
      lock.record('recipes/setup', 'recipes/setup.groovy', 'abc', [mode: ['fast']], [])
      assert lock.arguments('recipes/setup') == [mode: ['fast']]
    }
  }

  @Test void readsAnOldLockWithoutArgumentsAsNotRecordedAndPreservesItsOwnership() {
    def text = '''# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "recipes/setup"
    source: "recipes/setup.groovy"
    sha256: "abc"
    owns:
      sections:
        - file: "NOTES.md"
          id: "recipes/setup/notes"
'''
    def lock = CookLock.parse(text)

    assert lock.arguments('recipes/setup') == null
    assert lock.arguments('missing') == null
    assert lock.sha256('recipes/setup', 'recipes/setup.groovy') == 'abc'
    assert lock.owned('recipes/setup') == [new SectionTarget('NOTES.md', 'recipes/setup/notes')]
    assert lock.render() == text
  }

  @Test void refusesARecipeNameFieldThatIsNotAString() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Text.groovy', TEXT_MODEL)
    recipe(kitchen, 'recipes/setup.groovy', """import acme.text.Text
@recipe List self
@ingredient(Text) def text
text.text = self.toString()
""")

    def error = shouldFail(IllegalArgumentException) { cook(kitchen, 'recipes/setup.groovy', [:]) }
    assert error.message == 'recipes/setup.groovy: self has the type List, which the recipe name cannot have'
  }

  @Test void composesARecipePathWithCoordinatesUnderItsShortName() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    // A model and a recipe whose names differ only in case would be one file on macOS.
    recipe(kitchen, 'NotesModel.groovy', '''package acme.notes

@Ingredient
class Notes {
  private List<String> lines = []
  void add(String line) { lines += line }
  @Section(path = 'NOTES.md', id = 'notes')
  List<String> section() { lines ?: null }
}
''')
    recipe(kitchen, 'notes.groovy', """import acme.notes.Notes
@param(value = ['line'], doc = 'The line') String line
@ingredient(Notes) def notes
notes.add(line)
""")

    def proposal = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]),
      request('notes.groovy', [line: ['first']]),
    ])

    assert proposal.recipes*.address == ['io.github.lmarqs.cookbook:agents:0.1.0#seed', 'notes.groovy']
    proposal.commit()
    assert new File(kitchen, 'NOTES.md').text ==
    '<!-- cook:begin notes/notes -->\nfirst\n<!-- cook:end notes/notes -->\n'
    def lock = new File(kitchen, 'cook.lock').text
    assert lock.contains('- name: "io.github.lmarqs.cookbook:agents#seed"')
    assert lock.contains('- name: "notes"\n    source: "notes.groovy"')
  }

  @Test void refusesARecipePathOrAModelOutsideTheKitchen() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def outside = new File(kitchen.parentFile, 'outside.groovy')
    outside.text = "@help('Outside.')\nimport java.util.List\n"
    Files.createSymbolicLink(new File(kitchen, 'linked.groovy').toPath(), outside.toPath())
    recipe(kitchen, 'models/inside.groovy', "@help('Inside.')\nimport java.util.List\n")
    Files.createSymbolicLink(new File(kitchen, 'models/Linked.groovy').toPath(), outside.toPath())
    [
      '../outside.groovy': '../outside.groovy: the recipe is outside the kitchen\n  move it into the kitchen',
      'linked.groovy': 'linked.groovy: the recipe is outside the kitchen\n  move it into the kitchen',
      'missing.groovy': 'missing.groovy: Recipe file not found: missing.groovy',
      'https://example.com/notes.groovy': 'https://example.com/notes.groovy: a URL is not a recipe\n  list its ' +
      'repository in cook.toml and cook the recipe by its coordinates',
      'models/inside.groovy': 'models/inside.groovy: the model is outside the kitchen\n  move it into the kitchen',
    ].each { operand, message ->
      def error = shouldFail(IllegalArgumentException) { cook(kitchen, operand, [:]) }
      assert error.message == message
    }
  }

  @Test void theLastRecipeToWriteAKeyOwnsIt() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:notes:1.0.0', [
      'Notes.groovy': '''package acme.notes

@Ingredient
class Notes {
  private Map values = [:]
  void set(Map more) { values += more }
  @Keys(path = 'settings.json')
  Map keys() { values ?: null }
}
''',
      'first.groovy': '''@help('Sets test.')
import acme.notes.Notes
@param(value = ['value'], doc = 'The value') String value
@ingredient(Notes) def notes
notes.set(test: value, other: 'first')
''',
      'second.groovy': '''@help('Sets test.')
import acme.notes.Notes
@ingredient(Notes) def notes
notes.set(test: 'second')
''',
    ])
    new File(kitchen, 'settings.json').text = '{"test": "old", "kept": true}\n'

    cook(kitchen, 'acme:notes:1.0.0#first', [value: ['A']]).commit()
    assert new File(kitchen, 'settings.json').text == '{"test": "A", "kept": true, "other":"first"}\n'
    cook(kitchen, 'acme:notes:1.0.0#second', [:]).commit()
    assert new File(kitchen, 'settings.json').text == '{"test": "second", "kept": true, "other":"first"}\n'
    def lock = new File(kitchen, 'cook.lock').text
    assert lock.contains('''  - name: "acme:notes#first"
    source: "acme:notes:1.0.0#first"''')
    assert lock.contains('''        - file: "settings.json"
          path: ["other"]''')
    assert lock.contains('''  - name: "acme:notes#second"''')
    assert lock.indexOf('path: ["test"]') > lock.indexOf('acme:notes#second')

    def failures = [
      [['acme:notes:1.0.0#first', [:]]]: 'acme:notes:1.0.0#first: Missing --value: The value',
      [['acme:notes:1.0.0#first', [value: ['A', 'B']]]]:
      'acme:notes:1.0.0#first: --value takes one value\n  it was given 2',
      [['acme:notes:1.0.0#first', [value: ['A'], other: ['x']]]]:
      'acme:notes:1.0.0#first: Unknown argument --other\n  its arguments are --value',
      [['acme:notes:1.0.0#second', [:]], ['acme:notes:1.0.0#second', [:]]]:
      'recipe 2 (acme:notes:1.0.0#second): acme:notes#second is named twice\n  name it once per invocation',
      [['acme:notes:1.0.0', [:]]]:
      'acme:notes:1.0.0: acme:notes:1.0.0 names an artifact, not a recipe\n  name one: ' +
      'acme:notes:1.0.0#first, acme:notes:1.0.0#second',
    ]
    failures.each { requests, message ->
      def error = shouldFail(IllegalArgumentException) {
        new CookbookCook(kitchen, repositories(kitchen)).propose(requests.collect { request(it[0], it[1]) })
      }
      assert error.message == message
    }
  }

  @Test void withdrawsASectionInTheCommentSyntaxItWasWrittenIn() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:script:1.0.0', [
      'Script.groovy': '''package acme.script

@Ingredient
class Script {
  private List<String> lines = []
  void add(String line) { lines += line }
  @Section(path = 'build.js', id = 'steps', comment = '//')
  List<String> steps() { lines ?: null }
}
''',
      'script.groovy': '''@help('Adds build steps.')
import acme.script.Script
@param(value = ['step'], doc = 'A step', default = [], many = true) List<String> step
@ingredient(Script) def script
step.each { script.add(it) }
''',
    ])
    new File(kitchen, 'build.js').text = 'run();\n'
    cook(kitchen, 'acme:script:1.0.0#script', [step: ['lint();']]).commit()
    assert new File(kitchen, 'build.js').text == '''run();
// cook:begin acme:script#script/steps
lint();
// cook:end acme:script#script/steps
'''
    cook(kitchen, 'acme:script:1.0.0#script', [:]).commit()
    assert new File(kitchen, 'build.js').text == 'run();\n'
  }

  @Test void deletesAFileTheRecipeOwnedAndNoLongerDeclares() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:config:1.0.0', [
      'Config.groovy': '''package acme.config

@Ingredient
class Config {
  Boolean migrated
  @File(path = 'old.cfg')
  String old() { migrated == false ? 'old' : null }
  @Delete(path = 'old.cfg')
  Boolean gone() { migrated ?: null }
}
''',
      'config.groovy': '''@help('Writes or retires old.cfg.')
import acme.config.Config
@param(value = ['migrated'], doc = 'Whether old.cfg is retired', default = ['false']) boolean migrated
@ingredient(Config) def config
config.migrated = migrated
''',
    ])
    cook(kitchen, 'acme:config:1.0.0#config', [:]).commit()
    assert new File(kitchen, 'old.cfg').text == 'old'
    cook(kitchen, 'acme:config:1.0.0#config', [migrated: ['true']]).commit()
    assert !new File(kitchen, 'old.cfg').exists()
  }

  @Test void discardRemovesTheSectionsARecipeOwnsAndLeavesTheRestOfTheFile() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()
    def ignore = new File(kitchen, '.gitignore')
    ignore.text = 'mine\n' + ignore.text
    def policy = new File(kitchen, '.agents/permission/policy.yaml').text

    def discard = new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:9.9.9#connect'])

    assert discard.actions['.gitignore'] == 'update'
    assert discard.actions['cook.lock'] == 'delete'
    discard.commit()
    assert ignore.text == 'mine\n'
    assert new File(kitchen, '.agents/permission/policy.yaml').text == policy
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void discardRemovesOwnedKeysWithoutRestoringEarlierValues() {
    def kitchen = kitchen()
    def model = '''package acme.notes

@Ingredient
class Notes {
  private Map values = [:]
  void set(Map more) { values += more }
  @Keys(path = 'settings.json')
  Map keys() { values ?: null }
}
'''
    publish(kitchen, 'acme:notes:1.0.0', [
      'Notes.groovy': model,
      'notes.groovy': '''@help('Sets test.')
import acme.notes.Notes
@ingredient(Notes) def notes
notes.set(test: 'new', other: 'mine')
''',
    ])
    def settings = new File(kitchen, 'settings.json')
    settings.text = '{"test": "old", "kept": true}\n'
    cook(kitchen, 'acme:notes:1.0.0#notes', [:]).commit()
    recipe(kitchen, 'Notes.groovy', model)
    recipe(kitchen, 'local.groovy', '''import acme.notes.Notes
@ingredient(Notes) def notes
notes.set(test: 'new', other: 'mine')
''')
    cook(kitchen, 'local.groovy', [:]).commit()
    new File(kitchen, 'local.groovy').delete()

    new CookbookDiscard(kitchen).propose(['local.groovy']).commit()

    assert new groovy.json.JsonSlurper().parse(settings) == [kept: true]
    assert new File(kitchen, 'cook.lock').text.contains('name: "acme:notes#notes"')
    assert !new File(kitchen, 'cook.lock').text.contains('local')
  }

  @Test void sectionsWritesOneSectionPerNamedFileAndRemovesItFromAFileNoLongerNamed() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/NotesModel.groovy', '''package acme.notes

@Ingredient
class Notes {
  private List<String> files = []
  void into(List<String> paths) { files = paths }
  @Sections(path = '.', id = 'notes')
  Map<String, List<String>> sections() { files.collectEntries { [(it): ["into ${it}".toString()]] } ?: null }
}
''')
    recipe(kitchen, 'recipes/notes.groovy', '''import acme.notes.Notes
@param(value = ['into'], many = true, doc = 'The files') String[] into
@ingredient(Notes) def notes
notes.into(into as List)
''')
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Agents\n'
    def readme = new File(kitchen, 'docs/README.md')
    readme.parentFile.mkdirs()
    readme.text = '# Docs\n'

    cook(kitchen, 'recipes/notes.groovy', [into: ['AGENTS.md', 'docs/README.md']]).commit()

    assert agents.text == '# Agents\n<!-- cook:begin recipes/notes/notes -->\ninto AGENTS.md\n' +
    '<!-- cook:end recipes/notes/notes -->\n'
    assert readme.text == '# Docs\n<!-- cook:begin recipes/notes/notes -->\ninto docs/README.md\n' +
    '<!-- cook:end recipes/notes/notes -->\n'
    assert CookLock.parse(new File(kitchen, 'cook.lock').text).owned('recipes/notes')*.file as Set ==
    ['AGENTS.md', 'docs/README.md'] as Set

    cook(kitchen, 'recipes/notes.groovy', [into: ['docs/README.md']]).commit()

    assert agents.text == '# Agents\n'
    assert readme.text.contains('into docs/README.md')
  }

  @Test void sectionsWithMissingFailRefusesAFileTheDraftLacksAndAPathParamRefusesAPathOutsideTheRepository() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/NotesModel.groovy', '''package acme.notes

@Ingredient
class Notes {
  private List<String> files = []
  void into(List<String> paths) { files = paths }
  @Sections(path = '.', id = 'notes', missing = 'fail')
  Map<String, List<String>> sections() { files.collectEntries { [(it): ['note']] } ?: null }
}
''')
    recipe(kitchen, 'recipes/notes.groovy', '''import acme.notes.Notes
@param(value = ['into'], many = true, path = true, doc = 'The files') String[] into
@ingredient(Notes) def notes
notes.into(into as List)
''')
    def outside = { String path ->
      "recipes/notes.groovy: --into=${path} is outside the repository\n" +
          '  a recipe reads and writes only inside the repository\n' +
          '  name the file by its path from the repository root'
    }
    [
      'NOTES.md': 'recipes/notes.groovy: NOTES.md section recipes/notes/notes\n  NOTES.md is missing\n' +
      '    the section goes into a file the team keeps, and the recipe never creates it\n' +
      '    create the file, or cook the recipe that writes it earlier in the same invocation',
      '/NOTES.md': outside('/NOTES.md'),
      'a/../../NOTES.md': outside('a/../../NOTES.md'),
      '': 'recipes/notes.groovy: --into needs a path\n  name the file by its path from the repository root',
    ].each { path, message ->
      def error = shouldFail(IllegalArgumentException) { cook(kitchen, 'recipes/notes.groovy', [into: [path]]) }
      assert error.message == message
    }
    assert !new File(kitchen, 'NOTES.md').exists()

    new File(kitchen, 'NOTES.md').text = '# Notes\n'
    cook(kitchen, 'recipes/notes.groovy', [into: ['./NOTES.md']]).commit()

    assert new File(kitchen, 'NOTES.md').text.startsWith('# Notes\n<!-- cook:begin recipes/notes/notes -->\nnote\n')
  }

  @Test void movingARecipeRenamesItAndDiscardingTheOldNameRemovesOnlyWhatTheLockStillListsForIt() {
    def kitchen = kitchen()
    recipe(kitchen, 'recipes/Notes.groovy', '''package acme.notes

@Ingredient
class Notes {
  private String line
  void set(String value) { line = value }
  @Section(path = 'NOTES.md', id = 'notes')
  List<String> section() { line ? [line] : null }
  @Keys(path = 'settings.json')
  Map keys() { line ? [test: line] : null }
}
''')
    def source = """import acme.notes.Notes
@ingredient(Notes) def notes
notes.set('kept')
"""
    def a = recipe(kitchen, 'recipes/a.groovy', source)
    cook(kitchen, 'recipes/a.groovy', [:]).commit()
    a.delete()
    recipe(kitchen, 'recipes/b.groovy', source)
    cook(kitchen, 'recipes/b.groovy', [:]).commit()

    def notes = new File(kitchen, 'NOTES.md')
    assert notes.text.contains('cook:begin recipes/a/notes') && notes.text.contains('cook:begin recipes/b/notes')
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.names == ['recipes/a', 'recipes/b'] as Set
    assert lock.owned('recipes/a').every { it instanceof SectionTarget }
    assert lock.owned('recipes/b').any { it instanceof KeyTarget }

    new CookbookDiscard(kitchen).propose(['recipes/a.groovy']).commit()

    assert notes.text == '<!-- cook:begin recipes/b/notes -->\nkept\n<!-- cook:end recipes/b/notes -->\n'
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, 'settings.json')) == [test: 'kept']
    assert CookLock.parse(new File(kitchen, 'cook.lock').text).names == ['recipes/b'] as Set
  }

  @Test void discardByLockNameRemovesTheSameTargetsAsVersionedCoordinates() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()
    def ignore = new File(kitchen, '.gitignore')
    ignore.text = 'mine\n' + ignore.text
    def policy = new File(kitchen, '.agents/permission/policy.yaml').text
    def discard = new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents#connect'])

    ['0.0.0', '9.9.9'].each { version ->
      def versioned = new CookbookDiscard(kitchen).propose(["io.github.lmarqs.cookbook:agents:${version}#connect"])
      assert discard.actions == versioned.actions
      discard.changes.each { path -> assert discard.after(path) == versioned.after(path) }
    }
    discard.commit()

    assert ignore.text == 'mine\n'
    assert new File(kitchen, '.agents/permission/policy.yaml').text == policy
    assert !new File(kitchen, '.claude/settings.json').exists()
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void discardRefusesARecipeTheLockDoesNotListOrThatIsGivenTwice() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    [
      [
        ['io.github.lmarqs.cookbook:agents:0.1.0#connect'],
        'cook.lock lists no recipe io.github.lmarqs.cookbook:agents#connect; nothing was written\n' +
        '  discard removes only what cook.lock records for a recipe\n' +
        '  check the recipe\'s name; cook.lock lists io.github.lmarqs.cookbook:agents#seed'
      ],
      [
        ['io.github.lmarqs.cookbook:agents:0.1.0#seed', 'io.github.lmarqs.cookbook:agents:0.2.0#seed'],
        'io.github.lmarqs.cookbook:agents#seed is discarded twice\n  discard it once per invocation'
      ],
      [
        ['io.github.lmarqs.cookbook:agents#seed', 'io.github.lmarqs.cookbook:agents:0.1.0#seed'],
        'io.github.lmarqs.cookbook:agents#seed is discarded twice\n  discard it once per invocation'
      ],
      [['../outside.groovy'], 'the recipe is outside the kitchen\n  move it into the kitchen'],
      [
        ['https://example.com/notes.groovy'],
        'a URL is not a recipe\n  list its repository in cook.toml and cook the recipe by its coordinates'
      ],
    ].each { operands, message ->
      assert shouldFail(IllegalArgumentException) { new CookbookDiscard(kitchen).propose(operands) }.message == message
    }
    new File(kitchen, 'cook.lock').delete()
    assert shouldFail(IllegalArgumentException) { new CookbookDiscard(kitchen).propose(['notes.groovy']) }.message ==
    'cook.lock lists no recipe notes; nothing was written\n' +
    '  discard removes only what cook.lock records for a recipe\n' +
    '  check the recipe\'s name; cook.lock lists none'
  }

  @Test void aFailedWriteLeavesTheFilesAsTheyWere() {
    def kitchen = kitchen()
    publishConfig(kitchen, 'sub/old.cfg')
    def proposal = cook(kitchen, 'acme:config:1.0.0#config', [:])
    def blocked = new File(kitchen, 'sub')
    blocked.mkdir()
    blocked.setWritable(false)

    try {
      shouldFail(Exception) { proposal.commit() }
    } finally {
      blocked.setWritable(true)
    }

    assert !new File(kitchen, 'cook.lock').exists()
    assert !new File(kitchen, 'sub/old.cfg').exists()
  }

  @Test void namesTheRecipeWhoseModelFailsToConstruct() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:broken:1.0.0', [
      'Broken.groovy': '''package acme.broken

@Ingredient
class Broken {
  Broken() { throw new IllegalStateException('cannot start') }
}
''',
      'broken.groovy': '''@help('Fails.')
import acme.broken.Broken
@ingredient(Broken) def broken
''',
    ])
    def error = shouldFail(IllegalArgumentException) { cook(kitchen, 'acme:broken:1.0.0#broken', [:]) }
    assert error.message == 'acme:broken:1.0.0#broken: cannot start'
  }

  @Test void seedsAFileWithItsModeAndLeavesAnExistingFileMode() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:script:1.0.0', [
      'Script.groovy': '''package acme.script

@Ingredient
class Script {
  private boolean wanted
  void want() { wanted = true }
  @Seed(path = 'run', mode = '0755')
  String run() { wanted ? '#!/bin/sh\\n' : null }
}
''',
      'script.groovy': '''@help('Seeds a script.')
import acme.script.Script
@ingredient(Script) def script
script.want()
''',
    ])

    cook(kitchen, 'acme:script:1.0.0#script', [:]).commit()
    def run = new File(kitchen, 'run')
    assert run.text == '#!/bin/sh\n'
    assert Files.getPosixFilePermissions(run.toPath()) == PosixFilePermissions.fromString('rwxr-xr-x')

    run.text = 'mine\n'
    Files.setPosixFilePermissions(run.toPath(), PosixFilePermissions.fromString('rw-r--r--'))
    assert cook(kitchen, 'acme:script:1.0.0#script', [:]).changes == []
    assert Files.getPosixFilePermissions(run.toPath()) == PosixFilePermissions.fromString('rw-r--r--')
  }

  @Test void refusesAModelThatDeclaresOutputOnANewInstance() {
    def kitchen = kitchen()
    publish(kitchen, 'acme:noisy:1.0.0', [
      'Noisy.groovy': '''package acme.noisy

@Ingredient
class Noisy {
  @Seed(path = 'NOTES.md')
  String notes() { 'always' }
}
''',
      'noisy.groovy': '''@help('Seeds notes.')
import acme.noisy.Noisy
@ingredient(Noisy) def noisy
''',
    ])
    def error = shouldFail(IllegalArgumentException) { cook(kitchen, 'acme:noisy:1.0.0#noisy', [:]) }
    assert error.message == 'acme:noisy:1.0.0#noisy: Noisy.notes() declares a seed of NOTES.md on a new instance\n' +
    '  a model declares only what a recipe asks for'
  }

  // The policy and the output the layout's mock-up was written with, copied from it.

  private File kitchen() {
    folder.newFolder('kitchen')
  }

  // Coordinates resolve from the jars that publish writes under the kitchen's repository directory.
  private static CookbookRepositories repositories(File kitchen) {
    def directory = new File(kitchen, 'repository')
    def jars = { Coordinates coordinates ->
      def jar = new File(directory, coordinates.jarPath)
      jar.isFile() ? CookbookArtifact.fromZip(coordinates, jar.bytes, coordinates.packagePath) : null
    } as CookbookRepository
    new CookbookRepositories([jars], directory.path)
  }

  private static File recipe(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
    file
  }

  private static void publishConfig(File kitchen, String file) {
    publish(kitchen, 'acme:config:1.0.0', [
      'Config.groovy': """package acme.config

@Ingredient
class Config {
  Boolean migrated
  @File(path = '${file}')
  String old() { migrated == false ? 'old' : null }
  @Delete(path = '${file}')
  Boolean gone() { migrated ?: null }
}
""",
      'config.groovy': '''@help('Writes or retires a file.')
import acme.config.Config
@param(value = ['migrated'], doc = 'Whether the file is retired', default = ['false']) boolean migrated
@ingredient(Config) def config
config.migrated = migrated
''',
    ])
  }

  private static def cook(File kitchen, String operand, Map<String, List<String>> arguments) {
    new CookbookCook(kitchen, repositories(kitchen)).propose([request(operand, arguments)])
  }

  private static RecipeRequest request(String operand, Map<String, List<String>> arguments) {
    new RecipeRequest(operand, arguments.collectMany { name, values ->
      values.collect {
        Argument.inline(name, it)
      }
    })
  }

  private static File publishDirectory(File kitchen, String name, Map<String, Object> extra = [:]) {
    def directory = new File(COOKBOOK, name)
    Map<String, Object> files = [:] + extra
    directory.eachFileRecurse { file ->
      if (file.isFile()) {
        files[directory.toPath()
              .relativize(file.toPath())
              .toString()] = file.bytes
      }
    }
    publish(kitchen, "io.github.lmarqs.cookbook:${name}:0.1.0", files)
  }

  private static File publishText(File kitchen, Map<String, Object> extra = [:]) {
    publish(kitchen, 'acme:text:1.0.0', ['Text.groovy': TEXT_MODEL] + extra)
  }

  private static File publish(File kitchen, String address, Map<String, Object> files) {
    def coordinates = Coordinates.parse(address)
    def jar = new File(kitchen, "repository/${coordinates.jarPath}")
    jar.parentFile.mkdirs()
    new JarOutputStream(new FileOutputStream(jar)).withCloseable { out ->
      files.each { path, content ->
        out.putNextEntry(new JarEntry("${coordinates.packagePath}${path}"))
        out.write(content instanceof byte[] ? content : content.toString().getBytes('UTF-8'))
        out.closeEntry()
      }
    }
    jar
  }

  private static String sha256(File file) {
    sha256(file.bytes)
  }

  private static String sha256(byte[] bytes) {
    MessageDigest.getInstance('SHA-256')
        .digest(bytes)
        .encodeHex()
        .toString()
  }

  // The definition in docs/architecture.md: one line `<sha256 of the file>  <path>` per file, in path order.
  private static String sourcesSha256(File directory) {
    List<String> paths = []
    directory.eachFileRecurse(groovy.io.FileType.FILES) {
      paths << directory.toPath()
          .relativize(it.toPath())
          .toString()
    }
    sourcesSha256(directory, paths.findAll { !it.startsWith('test/') })
  }

  private static String sourcesSha256(File directory, List<String> paths) {
    sha256(paths.sort()
        .collect {
          "${sha256(new File(directory, it))}  ${it}\n"
        }
        .join()
        .getBytes('UTF-8'))
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
