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
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class CookbookCookTest {
  private static final File COOKBOOK = new File(System.getProperty('cook.cookbook'))
  private static final String TEXT_MODEL = '''package acme.text

@Ingredient
class Text {
  String text
  @File(path = 'OUT.md')
  String file() { text }
}
'''

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void cooksTheAgentsInstructionsAndRecordsOwnershipInTheLock() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')

    def proposal = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']])
    assert proposal.changes == ['.agents/adapters/claude/CLAUDE.md', '.claude/CLAUDE.md', 'AGENTS.md', 'cook.lock']
    assert proposal.recipes*.address == ['io.github.lmarqs.cookbook:agents:0.1.0#instructions']
    assert proposal.recipes.first().yieldText.startsWith('Have an agent fill in `AGENTS.md`')
    assert !new File(kitchen, 'AGENTS.md').exists()
    proposal.commit()

    assert new File(kitchen, 'AGENTS.md').text == new File(COOKBOOK, 'agents/AGENTS.template.md').text
    def claude = new File(kitchen, '.claude/CLAUDE.md').text
    assert claude == '''<!-- cook:begin io.github.lmarqs.cookbook:agents#instructions#instructions -->
@../../../AGENTS.md
<!-- cook:end io.github.lmarqs.cookbook:agents#instructions#instructions -->
'''
    assert new File(kitchen, 'cook.lock').text == """# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "io.github.lmarqs.cookbook:agents#instructions"
    source: "io.github.lmarqs.cookbook:agents:0.1.0#instructions"
    sha256: "${sourcesSha256(new File(COOKBOOK, 'agents'))}"
    owns:
      sections:
        - file: ".agents/adapters/claude/CLAUDE.md"
          id: "io.github.lmarqs.cookbook:agents#instructions#instructions"
      links:
        - ".claude/CLAUDE.md"
"""
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]).changes == []

    def fewer = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:])
    assert fewer.changes == ['.agents/adapters/claude/CLAUDE.md', '.claude/CLAUDE.md', 'cook.lock']
    fewer.commit()
    assert !new File(kitchen, '.agents/adapters/claude/CLAUDE.md').exists()
    assert !new File(kitchen, '.claude/CLAUDE.md').exists()
    assert new File(kitchen, 'cook.lock').text.endsWith('    owns: {}\n')
  }

  @Test void refusesToStartInstructionsOverAnExistingAgentsFile() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    new File(kitchen, 'AGENTS.md').text = '# Ours\n'
    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:])
    }
    assert error.message == 'AGENTS.md already exists; io.github.lmarqs.cookbook:agents#instructions ' +
    'writes it from a template the first time it is cooked. Run with --force to replace it, or --skip to keep it.'
    assert !new File(kitchen, 'cook.lock').exists()

    new File(kitchen, 'AGENTS.md').delete()
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:]).commit()
    new File(kitchen, 'AGENTS.md').text = '# Filled in\n'
    def again = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']])
    assert again.changes == ['.agents/adapters/claude/CLAUDE.md', '.claude/CLAUDE.md', 'cook.lock']
  }

  @Test void forceReplacesAndSkipKeepsAnExistingAgentsFileOnTheFirstCook() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def template = new File(COOKBOOK, 'agents/AGENTS.template.md').text
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Ours\n'

    def kept = new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.KEEP)
        .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']])])
    assert kept.actions == [
      '.agents/adapters/claude/CLAUDE.md': 'create',
      '.claude/CLAUDE.md': 'create',
      'AGENTS.md': 'keep',
      'cook.lock': 'create'
    ]
    kept.commit()
    assert agents.text == '# Ours\n'
    assert new File(kitchen, 'cook.lock').text.contains('"io.github.lmarqs.cookbook:agents#instructions"')
    assert new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
    .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']])])
    .actions == [:]

    new File(kitchen, 'cook.lock').delete()
    new File(kitchen, '.agents').deleteDir()
    new File(kitchen, '.claude/CLAUDE.md').delete()
    def replaced = new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
        .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:])])
    assert replaced.actions == ['AGENTS.md': 'replace', 'cook.lock': 'create']
    replaced.commit()
    assert agents.text == template

    new File(kitchen, 'cook.lock').delete()
    agents.delete()
    def ours = new File(kitchen, 'OURS.md'); ours.text = '# Ours\n'
    Files.createSymbolicLink(agents.toPath(), java.nio.file.Paths.get('OURS.md'))
    new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
        .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:])]).commit()
    assert !Files.isSymbolicLink(agents.toPath())
    assert agents.text == template
    assert ours.text == '# Ours\n'
  }

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
Run with --force to replace them, or --skip to keep them.'''
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void refusesToWriteASectionOrKeyThroughASymbolicLink() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def link = { String path, String target ->
      def file = new File(kitchen, path); file.parentFile.mkdirs()
      Files.createSymbolicLink(file.toPath(), java.nio.file.Paths.get(target))
    }
    link('.agents/adapters/claude/CLAUDE.md', 'OURS.md')
    def section = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']])
    }
    assert section.message == 'io.github.lmarqs.cookbook:agents:0.1.0#instructions: ' +
    '.agents/adapters/claude/CLAUDE.md is a symbolic link to OURS.md; Cook writes sections and keys only into ' +
    'regular files'

    new File(kitchen, '.agents/adapters/claude/shared.json').text = '{}\n'
    link('.agents/adapters/claude/settings.json', 'shared.json')
    def key = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']])
    }
    assert key.message == 'io.github.lmarqs.cookbook:agents:0.1.0#permissions: .agents/adapters/claude/settings.json ' +
    'is a symbolic link to shared.json; Cook writes sections and keys only into regular files'
    assert new File(kitchen, '.agents/adapters/claude/shared.json').text == '{}\n'
    assert !new File(kitchen, 'cook.lock').exists()
    assert Files.isSymbolicLink(new File(kitchen, '.agents/adapters/claude/CLAUDE.md').toPath())
  }

  @Test void cooksAKitchenWhosePathPassesThroughASymbolicLink() {
    def real = folder.newFolder('real')
    def linked = new File(folder.root, 'linked')
    Files.createSymbolicLink(linked.toPath(), real.toPath())
    def kitchen = new File(linked, 'kitchen'); kitchen.mkdirs()
    publishDirectory(kitchen, 'agents')

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]).commit()
    assert new File(real, 'kitchen/.claude/CLAUDE.md').text.contains('@../../../AGENTS.md')
  }

  @Test void refusesASecondAgentsImportInClaudeInstructions() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def claude = new File(kitchen, 'CLAUDE.md')
    [
      '# Notes\n@AGENTS.md\n': 2,
      '# cook:begin claude-context/instructions\n@AGENTS.md\n# cook:end claude-context/instructions\n': 2,
    ].each { text, line ->
      claude.text = text
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']])
      }
      assert error.message == 'io.github.lmarqs.cookbook:agents:0.1.0#instructions: CLAUDE.md already imports ' +
      "@AGENTS.md on line ${line}; remove that line, since .claude/CLAUDE.md imports it"
      assert claude.text == text
      assert !new File(kitchen, 'cook.lock').exists()
    }

    claude.text = '# Notes\n'
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]).commit()
    assert claude.text == '# Notes\n'
    assert new File(kitchen, '.claude/CLAUDE.md').text.contains('@../../../AGENTS.md')
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]).changes == []
  }

  @Test void composesPermissionsAndInstructionsInOneInvocation() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def proposal = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude', 'codex']]),
      request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]),
    ])
    assert proposal.changes == [
      '.agents/adapters/claude/CLAUDE.md',
      '.agents/adapters/claude/settings.json',
      '.agents/adapters/codex/config.toml',
      '.agents/policy.yaml',
      '.claude/CLAUDE.md',
      '.claude/settings.json',
      '.codex/config.toml',
      'AGENTS.md',
      'cook.lock'
    ]
    proposal.commit()
    assert new File(kitchen, '.agents/policy.yaml').text == new File(COOKBOOK, 'agents/policy.template.yaml').text
    assert Files.readSymbolicLink(new File(kitchen, '.claude/settings.json').toPath()).toString() ==
    '../.agents/adapters/claude/settings.json'
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, '.claude/settings.json')) ==
    [permissions: [defaultMode: 'acceptEdits', allow: [], ask: [], deny: []]]
    assert new File(kitchen, '.codex/config.toml').text.contains('sandbox_mode = "workspace-write"')
    assert new File(kitchen, '.agents/adapters/claude/settings.json').text == '''{
  "permissions": {
    "defaultMode": "acceptEdits",
    "allow": [],
    "ask": [],
    "deny": []
  }
}
'''
    def same = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude', 'codex']]),
      request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]),
    ])
    assert same.changes == []

    def withoutCodex = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']])
    assert withoutCodex.changes == ['.agents/adapters/codex/config.toml', '.codex/config.toml', 'cook.lock']
    withoutCodex.commit()
    assert !new File(kitchen, '.codex').exists()
    assert !new File(kitchen, '.agents/adapters/codex').exists()
  }

  @Test void seedsTheSharedLayoutAndConnectsHarnessesThroughDirectoriesThatCanBeDeleted() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def seed = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])
    assert seed.changes == [
      '.agents/README.md',
      '.agents/commands/.gitkeep',
      '.agents/hooks/.gitkeep',
      '.agents/memory/.gitkeep',
      '.agents/policy.yaml',
      '.agents/skills/.gitkeep',
      'AGENTS.md',
      'cook.lock'
    ]
    seed.commit()

    def connect = { -> cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]) }
    def connected = connect()
    assert connected.changes == [
      '.agents/adapters/claude/CLAUDE.md',
      '.agents/adapters/claude/settings.json',
      '.agents/adapters/codex/config.toml',
      '.claude/CLAUDE.md',
      '.claude/commands',
      '.claude/hooks',
      '.claude/memory',
      '.claude/settings.json',
      '.claude/skills',
      '.codex/config.toml',
      '.codex/hooks',
      '.gitignore',
      'cook.lock'
    ]
    connected.commit()
    assert new File(kitchen, '.gitignore').text.contains('/.claude/\n/.codex/\n')
    assert Files.readSymbolicLink(new File(kitchen, '.codex/config.toml').toPath()).toString() ==
    '../.agents/adapters/codex/config.toml'
    assert Files.readSymbolicLink(new File(kitchen, '.claude/memory').toPath()).toString() == '../.agents/memory'
    assert new File(kitchen, '.codex/hooks/.gitkeep').isFile()
    assert connect().changes == []

    // `File.deleteDir` would follow the links into `.agents/`; `rm -rf` does not, and neither does this.
    ['.claude', '.codex'].each { directory ->
      Files.walk(new File(kitchen, directory).toPath())
          .sorted(Comparator.reverseOrder())
          .each { Files.delete(it) }
    }
    def again = connect()
    def links = [
      '.claude/CLAUDE.md',
      '.claude/commands',
      '.claude/hooks',
      '.claude/memory',
      '.claude/settings.json',
      '.claude/skills'
    ]
    assert again.changes == links + ['.codex/config.toml', '.codex/hooks']
    again.commit()
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, '.claude/settings.json')).permissions.defaultMode ==
    'acceptEdits'
  }

  @Test void linksTheSharedSkillsAndRemovesWhatALaterVersionNoLongerDeclares() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents', ['skill-check.groovy': """@help('Links one skill.')
import io.github.lmarqs.cookbook.agents.Agents
import io.github.lmarqs.cookbook.agents.Claude
@param(value = ['skill'], doc = 'The skill', default = []) String[] skill
@ingredient(Agents) def agents
@ingredient(Claude) def claude
agents.skills(skill)
agents.connect([claude])
"""])
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#skill-check', [skill: ['solid']]).commit()
    assert new File(kitchen, '.agents/skills/solid/SKILL.md').text ==
    new File(COOKBOOK, 'agents/skills/solid/SKILL.md').text
    def link = new File(kitchen, '.claude/skills').toPath()
    assert Files.readSymbolicLink(link).toString() == '../.agents/skills'
    assert new File(kitchen, '.claude/skills/solid/SKILL.md').isFile()

    def fewer = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#skill-check', [:])
    assert fewer.changes == ['.agents/skills/solid/SKILL.md', '.claude/skills', 'cook.lock']
    fewer.commit()
    assert !new File(kitchen, '.agents/skills').exists()
    assert !new File(kitchen, '.claude/skills').exists()
    assert new File(kitchen, '.claude/CLAUDE.md').isFile()
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
agents.skills('solid')
agents.connect([claude])
""")

    cook(kitchen, 'agents/skill-check.groovy', [:]).commit()

    assert new File(kitchen, 'AGENTS.md').text.startsWith(new File(COOKBOOK, 'agents/AGENTS.template.md').text)
    assert new File(kitchen, '.agents/skills/solid/SKILL.md').text ==
    new File(COOKBOOK, 'agents/skills/solid/SKILL.md').text
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
    def requests = [
      request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:]),
      request('recipes/setup.groovy', [:]),
    ]
    def first = new CookbookCook(kitchen, repositories(kitchen)).propose(requests)
    assert first.changedSources.empty
    first.commit()

    publishDirectory(kitchen, 'agents', ['NOTICE.md': 'changed under the same version'])
    path.text += '// edited\n'
    def again = new CookbookCook(kitchen, repositories(kitchen)).propose(requests)

    assert again.changedSources*.address == ['io.github.lmarqs.cookbook:agents:0.1.0#instructions']
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
    def outside = 'the resource is outside the kitchen; move it into the kitchen'
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
      request('io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]),
      request('notes.groovy', [line: ['first']]),
    ])

    assert proposal.recipes*.address == ['io.github.lmarqs.cookbook:agents:0.1.0#instructions', 'notes.groovy']
    proposal.commit()
    assert new File(kitchen, 'NOTES.md').text ==
    '<!-- cook:begin notes#notes -->\nfirst\n<!-- cook:end notes#notes -->\n'
    def lock = new File(kitchen, 'cook.lock').text
    assert lock.contains('- name: "io.github.lmarqs.cookbook:agents#instructions"')
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
      '../outside.groovy': '../outside.groovy: the recipe is outside the kitchen; move it into the kitchen',
      'linked.groovy': 'linked.groovy: the recipe is outside the kitchen; move it into the kitchen',
      'missing.groovy': 'missing.groovy: Recipe file not found: missing.groovy',
      'https://example.com/notes.groovy': 'https://example.com/notes.groovy: a URL is not a recipe; list its ' +
      'repository in cook.toml and cook the recipe by its coordinates',
      'models/inside.groovy': 'models/inside.groovy: the model is outside the kitchen; move it into the kitchen',
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
      [['acme:notes:1.0.0#first', [value: ['A'], other: ['x']]]]:
      'acme:notes:1.0.0#first: Unknown argument --other; its arguments are --value',
      [['acme:notes:1.0.0#second', [:]], ['acme:notes:1.0.0#second', [:]]]:
      'recipe 2 (acme:notes:1.0.0#second): acme:notes#second is named twice; name it once per invocation',
      [['acme:notes:1.0.0', [:]]]:
      'acme:notes:1.0.0: acme:notes:1.0.0 names an artifact, not a recipe; name one: ' +
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
// cook:begin acme:script#script#steps
lint();
// cook:end acme:script#script#steps
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
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [harness: ['claude']]).commit()
    def claude = new File(kitchen, '.agents/adapters/claude/CLAUDE.md')
    claude.text = 'mine\n' + claude.text
    def agents = new File(kitchen, 'AGENTS.md').text

    def discard = new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:9.9.9#instructions'])

    assert discard.actions == [
      '.agents/adapters/claude/CLAUDE.md': 'update',
      '.claude/CLAUDE.md': 'delete',
      'cook.lock': 'delete'
    ]
    discard.commit()
    assert claude.text == 'mine\n'
    assert new File(kitchen, 'AGENTS.md').text == agents
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
    assert notes.text.contains('cook:begin recipes/a#notes') && notes.text.contains('cook:begin recipes/b#notes')
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.names == ['recipes/a', 'recipes/b'] as Set
    assert lock.owned('recipes/a').every { it instanceof SectionTarget }
    assert lock.owned('recipes/b').any { it instanceof KeyTarget }

    new CookbookDiscard(kitchen).propose(['recipes/a.groovy']).commit()

    assert notes.text == '<!-- cook:begin recipes/b#notes -->\nkept\n<!-- cook:end recipes/b#notes -->\n'
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, 'settings.json')) == [test: 'kept']
    assert CookLock.parse(new File(kitchen, 'cook.lock').text).names == ['recipes/b'] as Set
  }

  @Test void discardRefusesARecipeTheLockDoesNotListOrThatIsGivenTwice() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#instructions', [:]).commit()
    [
      [
        ['io.github.lmarqs.cookbook:agents:0.1.0#permissions'],
        'io.github.lmarqs.cookbook:agents#permissions is not in cook.lock, which lists ' +
        'io.github.lmarqs.cookbook:agents#instructions'
      ],
      [
        ['io.github.lmarqs.cookbook:agents:0.1.0#instructions', 'io.github.lmarqs.cookbook:agents:0.2.0#instructions'],
        'io.github.lmarqs.cookbook:agents#instructions is discarded twice; discard it once per invocation'
      ],
      [['../outside.groovy'], 'the recipe is outside the kitchen; move it into the kitchen'],
      [
        ['https://example.com/notes.groovy'],
        'a URL is not a recipe; list its repository in cook.toml and cook the recipe by its coordinates'
      ],
    ].each { operands, message ->
      assert shouldFail(IllegalArgumentException) { new CookbookDiscard(kitchen).propose(operands) }.message == message
    }
    new File(kitchen, 'cook.lock').delete()
    assert shouldFail(IllegalArgumentException) { new CookbookDiscard(kitchen).propose(['notes.groovy']) }.message ==
    'notes is not in cook.lock, which lists no recipe'
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
    assert error.message == 'acme:noisy:1.0.0#noisy: Noisy.notes() declares a seed of NOTES.md on a new instance; ' +
    'a model declares only what a recipe asks for'
  }

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
    sourcesSha256(directory, paths)
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
