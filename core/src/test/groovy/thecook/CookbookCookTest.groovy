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

  @Test void cooksTheAgentsLayoutAndRecordsOwnershipInTheLock() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')

    def seed = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])
    assert seed.recipes*.address == ['io.github.lmarqs.cookbook:agents:0.1.0#seed']
    assert seed.recipes.first().yieldText.startsWith('Have an agent fill in `AGENTS.md`')
    assert !new File(kitchen, 'AGENTS.md').exists()
    seed.commit()
    [
      'AGENTS.md': 'AGENTS.template.md',
      '.agents/README.md': 'README.template.md',
      '.agents/permission/policy.yaml': 'policy.template.yaml',
    ].each { seeded, template ->
      assert new File(kitchen, seeded).bytes == new File(TEMPLATES, template).bytes
      assert new File(kitchen, ".agents/skills/agents-layout/templates/${template}").bytes ==
      new File(TEMPLATES, template).bytes
    }

    def proposal = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    proposal.commit()
    def sha256 = sourcesSha256(new File(COOKBOOK, 'agents'))
    assert new File(kitchen, 'cook.lock').text == """# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "io.github.lmarqs.cookbook:agents#connect"
    source: "io.github.lmarqs.cookbook:agents:0.1.0#connect"
    sha256: "${sha256}"
    owns:
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:agents#connect/harnesses"
      keys:
        - file: ".agents/adapters/claude/settings.json"
          path: ["permissions"]
      links:
        - ".claude/commands"
        - ".claude/settings.json"
        - ".claude/skills"
  - name: "io.github.lmarqs.cookbook:agents#seed"
    source: "io.github.lmarqs.cookbook:agents:0.1.0#seed"
    sha256: "${sha256}"
    owns:
      files:
        - ".agents/skills/agents-layout/SKILL.md"
        - ".agents/skills/agents-layout/references/adopt.md"
        - ".agents/skills/agents-layout/references/agents-md.md"
        - ".agents/skills/agents-layout/references/design.md"
        - ".agents/skills/agents-layout/references/investigate.md"
        - ".agents/skills/agents-layout/references/policy.md"
        - ".agents/skills/agents-layout/scripts/check.sh"
        - ".agents/skills/agents-layout/templates/AGENTS.template.md"
        - ".agents/skills/agents-layout/templates/README.template.md"
        - ".agents/skills/agents-layout/templates/policy.template.yaml"
"""
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).changes == []

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    assert !new File(kitchen, '.claude').exists()
    assert !new File(kitchen, 'cook.lock').text.contains('.claude/')
  }

  @Test void theAgentsTemplateKeepsItsFixedTextWithinBudgetAndMarksEveryBlankTheSameWay() {
    def lines = new File(TEMPLATES, 'AGENTS.template.md').readLines()
    def fixed = lines.findAll { !it.startsWith('>') }*.replaceAll(/\[\[fill:[^\]]*\]\]/, '')
    def words = fixed.collectMany { it.split(/\s+/).findAll() }
    assert words.size() <= 300
    assert !fixed.any { it.contains('[') || it =~ /\|\s*\|/ }
  }

  @Test void refusesToSeedOverAnExistingAgentsFile() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    new File(kitchen, 'AGENTS.md').text = '# Ours\n'
    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])
    }
    assert error.message == 'AGENTS.md already exists\n  io.github.lmarqs.cookbook:agents#seed writes it from a ' +
    'template the first time it is cooked\n  run with --force to replace it, or --skip to keep it'
    assert !new File(kitchen, 'cook.lock').exists()

    new File(kitchen, 'AGENTS.md').delete()
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    new File(kitchen, 'AGENTS.md').text = '# Filled in\n'
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).changes == []
  }

  @Test void forceReplacesAndSkipKeepsAnExistingAgentsFileOnTheFirstCook() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def template = new File(TEMPLATES, 'AGENTS.template.md').text
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Ours\n'

    def kept = new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.KEEP)
        .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])])
    assert kept.actions['AGENTS.md'] == 'keep'
    assert kept.actions['.agents/README.md'] == 'create'
    kept.commit()
    assert agents.text == '# Ours\n'
    assert new File(kitchen, 'cook.lock').text.contains('"io.github.lmarqs.cookbook:agents#seed"')
    assert new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
    .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])])
    .actions == [:]

    new File(kitchen, 'cook.lock').delete()
    new File(kitchen, '.agents').deleteDir()
    def replaced = new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
        .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])])
    assert replaced.actions['AGENTS.md'] == 'replace'
    replaced.commit()
    assert agents.text == template

    new File(kitchen, 'cook.lock').delete()
    agents.delete()
    def ours = new File(kitchen, 'OURS.md'); ours.text = '# Ours\n'
    Files.createSymbolicLink(agents.toPath(), java.nio.file.Paths.get('OURS.md'))
    new CookbookCook(kitchen, repositories(kitchen), ExistingSeed.REPLACE)
        .propose([request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:])]).commit()
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
    'io.github.lmarqs.cookbook:agents#connect/harnesses\n.gitignore is a symbolic link to OURS\n' +
    '  Cook writes sections and keys only into regular files'

    new File(kitchen, '.agents/adapters/claude').mkdirs()
    new File(kitchen, '.agents/adapters/claude/shared.json').text = '{}\n'
    link('.agents/adapters/claude/settings.json', 'shared.json')
    def key = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']])
    }
    assert key.message == 'io.github.lmarqs.cookbook:agents:0.1.0#permissions: .agents/adapters/claude/settings.json ' +
    'key permissions\n.agents/adapters/claude/settings.json is a symbolic link to shared.json\n' +
    '  Cook writes sections and keys only into regular files'
    assert new File(kitchen, '.agents/adapters/claude/shared.json').text == '{}\n'
    assert !new File(kitchen, 'cook.lock').exists()
    assert Files.isSymbolicLink(new File(kitchen, '.gitignore').toPath())
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
    '.codex/config.toml is a file, and this ' +
    'recipe makes it a link to ../.agents/adapters/codex/config.toml\n  its contents would stop being read\n' +
    '  move its contents into the inputs of #codex-settings, delete it, and cook again'
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
    '.claude/skills is a directory, and ' +
    'this recipe makes it a link to ../.agents/skills\n  its files would stop being read\n' +
    '  move its files into .agents/skills/, delete it, and cook again'
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

  @Test void codexOwnsOnlyNetworkAccessInTheSandboxTable() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def config = new File(kitchen, '.agents/adapters/codex/config.toml'); config.parentFile.mkdirs()
    config.text = '[sandbox_workspace_write]\nnetwork_access = true\nwritable_roots = ["/tmp"]\n'

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    assert config.text == 'approval_policy = "on-request"\nsandbox_mode = "workspace-write"\n' +
    '[sandbox_workspace_write]\nnetwork_access = false\nwritable_roots = ["/tmp"]\n'
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert new KeyTarget('.agents/adapters/codex/config.toml', ['sandbox_workspace_write', 'network_access']) in
    lock.owned('io.github.lmarqs.cookbook:agents#connect')

    def discarded = new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:0.1.0#connect'])
    discarded.commit()
    assert config.text == '[sandbox_workspace_write]\nwritable_roots = ["/tmp"]\n'
  }

  @Test void codexAddsTheSandboxTableToANewKitchenAndRemovesItWhenDiscarded() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    def config = new File(kitchen, '.agents/adapters/codex/config.toml')
    assert config.text == 'approval_policy = "on-request"\nsandbox_mode = "workspace-write"\n\n' +
    '[sandbox_workspace_write]\nnetwork_access = false\n'

    new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:0.1.0#connect']).commit()
    assert !config.exists()
  }

  @Test void codexUpgradesAKitchenThatOwnedTheWholeSandboxKey() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    def config = new File(kitchen, '.agents/adapters/codex/config.toml')
    config.text = 'sandbox_workspace_write = { network_access = false }\napproval_policy = "on-request"\n' +
        'sandbox_mode = "workspace-write"\n'
    def lock = new File(kitchen, 'cook.lock')
    lock.text = lock.text.replace('["sandbox_workspace_write","network_access"]', '["sandbox_workspace_write"]')
    assert lock.text.contains('["sandbox_workspace_write"]')

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    assert config.text == 'approval_policy = "on-request"\nsandbox_mode = "workspace-write"\n\n' +
    '[sandbox_workspace_write]\nnetwork_access = false\n'
    assert lock.text.contains('["sandbox_workspace_write","network_access"]')
    assert !lock.text.contains('["sandbox_workspace_write"]')
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
    assert json[1].startsWith('JSON: ')
    assert json[2] == '  settings files are strict JSON: remove comments and trailing commas'
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
    'key features is written as a [features] table\n' +
    '  Cook owns features whole and writes it as one inline table\n' +
    '  replace it with features = { … } above the first [table], then cook again'
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
    assert withdrawn.message.readLines()[0..1] == [
      'io.github.lmarqs.cookbook:agents:0.1.0#connect: .agents/adapters/codex/config.toml key approval_policy',
      'key approval_policy is written as a [approval_policy] table'
    ]

    config.text = cooked
    def settings = new File(kitchen, '.agents/adapters/claude/settings.json')
    settings.text = '// ours\n' + settings.text
    def discarded = shouldFail(IllegalArgumentException) {
      new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:0.1.0#connect'])
    }
    assert discarded.message.readLines()[0] == '.agents/adapters/claude/settings.json key permissions'
  }

  @Test void claudeSettingsCooksWithAndWithoutItsSettingsFile() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#claude-settings', [:]).changes == ['cook.lock']
    def missing = shouldFail(IllegalArgumentException) {
      cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#claude-settings', [settings: ['setup/claude.json']])
    }
    assert missing.message == 'io.github.lmarqs.cookbook:agents:0.1.0#claude-settings: setup/claude.json must exist'

    new File(kitchen, 'setup').mkdirs()
    new File(kitchen, 'setup/claude.json').text = '{"model": "opus"}\n'
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#claude-settings', [settings: ['setup/claude.json']]).commit()
    assert new File(kitchen, '.claude/settings.json').text == '{\n  "model": "opus"\n}\n'
  }

  @Test void codexSettingsCooksWithAndWithoutItsInputs() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#codex-settings', [:]).changes == ['cook.lock']

    new File(kitchen, 'setup').mkdirs()
    new File(kitchen, 'setup/codex.toml').text = 'model = "o3"\n'
    new File(kitchen, 'setup/codex-hooks.json').text = '{"hooks": {}}\n'
    new File(kitchen, 'setup/git.rules').text = 'prefix_rule(pattern = ["git"])\n'
    new File(kitchen, 'setup/npm.rules').text = 'prefix_rule(pattern = ["npm"])\n'
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#codex-settings', [
      config: ['setup/codex.toml'], hooks: ['setup/codex-hooks.json'], rule: ['setup/git.rules', 'setup/npm.rules'],
    ]).commit()
    assert new File(kitchen, '.codex/config.toml').text == 'model = "o3"\n'
    assert new File(kitchen, '.codex/hooks.json').text == '{\n  "hooks": {}\n}\n'
    assert new File(kitchen, '.codex/rules/git.rules').text == 'prefix_rule(pattern = ["git"])\n'
    assert new File(kitchen, '.codex/rules/npm.rules').text == 'prefix_rule(pattern = ["npm"])\n'
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

  @Test void composesPermissionsAndTheSeedInOneInvocation() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def proposal = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude', 'codex']]),
      request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]),
    ])
    assert proposal.changes == [
      '.agents/README.md',
      '.agents/adapters/claude/settings.json',
      '.agents/adapters/codex/config.toml',
      '.agents/commands/.gitkeep',
      '.agents/hooks/.gitkeep',
      '.agents/memory/.gitkeep',
      '.agents/permission/policy.yaml',
      '.agents/skills/.gitkeep',
      '.agents/skills/agents-layout/SKILL.md',
      '.agents/skills/agents-layout/references/adopt.md',
      '.agents/skills/agents-layout/references/agents-md.md',
      '.agents/skills/agents-layout/references/design.md',
      '.agents/skills/agents-layout/references/investigate.md',
      '.agents/skills/agents-layout/references/policy.md',
      '.agents/skills/agents-layout/scripts/check.sh',
      '.agents/skills/agents-layout/templates/AGENTS.template.md',
      '.agents/skills/agents-layout/templates/README.template.md',
      '.agents/skills/agents-layout/templates/policy.template.yaml',
      '.claude/settings.json',
      '.codex/config.toml',
      'AGENTS.md',
      'cook.lock'
    ]
    proposal.commit()
    assert new File(kitchen, '.agents/permission/policy.yaml').text ==
    new File(TEMPLATES, 'policy.template.yaml').text
    assert Files.readSymbolicLink(new File(kitchen, '.claude/settings.json').toPath()).toString() ==
    '../.agents/adapters/claude/settings.json'
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, '.claude/settings.json')) ==
    [permissions: [allow: [], ask: [], deny: [], disableBypassPermissionsMode: 'disable']]
    assert new File(kitchen, '.codex/config.toml').text.contains('sandbox_mode = "workspace-write"')
    assert new File(kitchen, '.agents/adapters/claude/settings.json').text == '''{
  "permissions": {
    "allow": [],
    "ask": [],
    "deny": [],
    "disableBypassPermissionsMode": "disable"
  }
}
'''
    def same = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude', 'codex']]),
      request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]),
    ])
    assert same.changes == []

    def withoutCodex = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']])
    assert withoutCodex.changes == ['.agents/adapters/codex/config.toml', '.codex/config.toml', 'cook.lock']
    withoutCodex.commit()
    assert !new File(kitchen, '.codex').exists()
    assert !new File(kitchen, '.agents/adapters/codex').exists()
  }

  @Test void readsEveryFormOfThePolicyAndNamesTheFieldOfEachRefusal() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents', [
      'PolicyCheck.groovy': '''package io.github.lmarqs.cookbook.agents

@Ingredient
class PolicyCheck {
  Policy policy
  @File(path = 'POLICY.txt')
  String summary() {
    if (!policy) return null
    def entries = { Map<String, List<Command>> byName ->
      byName.collect { name, list -> "${name} ${list.collect { it.claude ? "claude ${it.claude}" : it.prefix }}" }
    }
    ([policy.commands, policy.tools, policy.roles].collect { entries(it).join('; ') } + "bypass ${policy.bypass}")
        .join('\\n') + '\\n'
  }
}
''',
      'policy-check.groovy': '''@help('Reads the policy.')
import io.github.lmarqs.cookbook.agents.Agents
import io.github.lmarqs.cookbook.agents.PolicyCheck
@ingredient(Agents) def agents
@ingredient(PolicyCheck) def check
check.policy = agents.policy()
''',
    ])
    def policy = new File(kitchen, '.agents/permission/policy.yaml')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#policy-check', [:]).commit()
    assert policy.text == new File(TEMPLATES, 'policy.template.yaml').text
    assert new File(kitchen, 'POLICY.txt').text ==
    'allow []; ask []; deny []\nallow []; ask []; deny []\n\nbypass refuse\n'

    // The starter's commented example, uncommented, is a valid policy.
    def commented = policy.readLines().findAll { it.startsWith('#   ') }
    def example = commented*.substring(4)
    assert example
    policy.text = 'version: 2\n' + example.join('\n') + '\n'
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#policy-check', [:]).commit()
    assert new File(kitchen, 'POLICY.txt').text.contains('worker [[git, [commit, push]], [gh]]')

    policy.text = '''version: 2
bypass: allow
commands:
  allow:
    - [git, status]
    - [gh, pr, [view, list]]
  ask:
    - [git, push]
  deny:
    - claude: "git push * --force*"
tools:
  allow:
    - claude: "WebFetch(domain:docs.gradle.org)"
roles:
  worker:
    deny:
      - [git, [commit, push]]
'''
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#policy-check', [:]).commit()
    assert new File(kitchen, 'POLICY.txt').text == '''\
allow [[git, status], [gh, pr, [view, list]]]; ask [[git, push]]; deny [claude git push * --force*]
allow [claude WebFetch(domain:docs.gradle.org)]; ask []; deny []
worker [[git, [commit, push]]]
bypass allow
'''

    def entry = 'is not an entry; write the literal words a command starts with, a nested list for alternatives of ' +
        'one word, or {claude: "<pattern>"}'
    [
      'version: 1\n': 'version must be 2',
      'version: 2\ninstructions: []\n': 'unknown field [instructions]',
      'version: 2\nbypass: maybe\n': 'bypass must be refuse or allow',
      'version: 2\ncommands:\n  maybe: []\n': 'commands has unknown decision [maybe]; use allow, ask or deny',
      'version: 2\ncommands:\n  allow:\n    - [git, "a b"]\n': "commands.allow: [git, a b] ${entry}",
      'version: 2\ncommands:\n  deny:\n    - [git, []]\n': "commands.deny: [git, []] ${entry}",
      'version: 2\ncommands:\n  ask:\n    - codex: x\n': "commands.ask: [codex:x] ${entry}",
      'version: 2\ntools:\n  allow:\n    - [git]\n':
      'tools.allow: [git] is not a tool entry; write {claude: "<pattern>"}',
      'version: 2\nroles:\n  Worker:\n    deny: []\n':
      'roles: Worker is not a role name; use lower-case letters, digits and hyphens, starting with a letter',
      'version: 2\nroles:\n  worker:\n    allow:\n      - [git]\n':
      'roles.worker: a role only adds denies; list them under deny, and keep allow and ask under commands',
    ].each { text, message ->
      policy.text = text
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#policy-check', [:])
      }
      assert error.message == 'io.github.lmarqs.cookbook:agents:0.1.0#policy-check: ' +
      ".agents/permission/policy.yaml: ${message}"
    }
  }

  @Test void rendersThePolicyIntoClaudeSettingsAndOneDenyFilePerRole() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def policy = new File(kitchen, '.agents/permission/policy.yaml')
    policy.parentFile.mkdirs()
    policy.text = fixture('policy.yaml')

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']]).commit()

    assert new File(kitchen, '.agents/adapters/claude/settings.json').text == fixture('claude-settings.json')
    def roles = new File(kitchen, '.agents/adapters/claude/roles')
    assert roles.list().sort() == ['reviewer.json', 'worker.json']
    assert new File(roles, 'worker.json').text == fixture('claude-worker.json')
    assert new File(roles, 'reviewer.json').text == fixture('claude-reviewer.json')

    policy.text = policy.text.replaceAll(/(?s)\nroles:.*/, '\n')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']]).commit()
    assert !roles.exists()

    policy.text = policy.text.replaceFirst('version: 2\n', 'version: 2\nbypass: allow\n')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['claude']]).commit()
    // A later cook lays the changed value out as the first cook did.
    assert new File(kitchen, '.agents/adapters/claude/settings.json').text ==
    fixture('claude-settings.json').replace('],\n    "disableBypassPermissionsMode": "disable"', ']')
  }

  @Test void rendersThePolicyIntoCodexRulesAndNamesWhatCodexCannotEnforce() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def policy = new File(kitchen, '.agents/permission/policy.yaml')
    policy.parentFile.mkdirs()
    policy.text = fixture('policy.yaml')

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['codex']]).commit()

    def rules = new File(kitchen, '.agents/adapters/codex/rules/cook.rules').readLines()
    assert rules.first() == '# cook:begin io.github.lmarqs.cookbook:agents#permissions/commands'
    assert rules.last() == '# cook:end io.github.lmarqs.cookbook:agents#permissions/commands'
    assert rules[1..-2].join('\n') + '\n' == fixture('codex-commands.rules')
    // A Codex run cannot add a rules file of its own, so roles render nothing for Codex.
    assert !new File(kitchen, '.agents/adapters/codex/roles').exists()

    policy.text = policy.text.replaceFirst('version: 2\n', 'version: 2\nbypass: allow\n')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#permissions', [harness: ['codex']]).commit()
    def allowed = new File(kitchen, '.agents/adapters/codex/rules/cook.rules').readLines()
    assert allowed[1..-2] == rules[2..-2]
  }

  @Test void theLayoutCheckFailsOnAFreshSeedAndPassesOnceAgentsMdIsFilledIn() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def environment = System.getenv() + [GIT_CONFIG_GLOBAL: '/dev/null', GIT_CONFIG_NOSYSTEM: '1']
    def run = { List<String> command ->
      def process = command.execute(environment.collect { name, value -> "${name}=${value}" }, kitchen)
      def output = process.text
      [process.waitFor(), output]
    }
    assert run(['git', 'init', '-q'])[0] == 0
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]).commit()
    def check = ['sh', '.agents/skills/agents-layout/scripts/check.sh']
    def stage = ['git', 'add', 'AGENTS.md', '.agents', '.claude', '.codex', '.gitignore', 'cook.lock']
    assert run(stage)[0] == 0
    def state = ['git', 'status', '--porcelain', '--ignored']
    def before = run(state)[1]
    def (fresh, freshOutput) = run(check)

    assert fresh == 1
    assert run(state)[1] == before
    def failures = freshOutput.readLines().findAll { it.startsWith('FAIL ') }
    assert failures.size() == 2
    assert failures[0].startsWith("FAIL AGENTS.md still has the template's note -- ")
    assert failures[1].startsWith('FAIL AGENTS.md has 27 lines with [[fill: blanks -- ')

    new File(kitchen, 'AGENTS.md').text = fixture('filled-agents.md')
    assert run(stage)[0] == 0
    def (filled, filledOutput) = run(check)

    assert filled == 0
    assert filledOutput.readLines().every { it.startsWith('ok ') }
    assert filledOutput.contains('ok .claude/settings.json is a tracked link that resolves and is not ignored\n')
    assert filledOutput.contains('ok .codex/config.toml is a tracked link that resolves and is not ignored\n')
  }

  @Test void commitsTheHarnessLinksAndKeepsEverythingElseInTheirDirectoriesOutOfGit() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    assert ['git', 'init', '-q', kitchen.path].execute().waitFor() == 0
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    // Every repository with the layout carries the skill that explains it, from the moment the layout exists.
    assert new File(kitchen, '.agents/skills/agents-layout/SKILL.md').text ==
    new File(COOKBOOK, 'agents/skills/agents-layout/SKILL.md').text
    new File(kitchen, '.agents/permission/policy.yaml').text = fixture('policy.yaml')
    // A team's own rule ignoring a whole harness directory would hide the links, unless the section re-includes it.
    new File(kitchen, '.gitignore').text = '.claude/\n.codex/\n'

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]).commit()

    assert new File(kitchen, '.claude/skills/agents-layout/SKILL.md').isFile()
    assert new File(kitchen, '.gitignore').text == '''\
.claude/
.codex/
# cook:begin io.github.lmarqs.cookbook:agents#connect/harnesses
!/.claude/
/.claude/*
!/.claude/settings.json
!/.claude/skills
!/.claude/commands
!/.codex/
/.codex/*
!/.codex/config.toml
!/.codex/rules
# Claude Code skips AGENTS.md while any of these exists, so they stay out of commits.
CLAUDE.md
CLAUDE.local.md
**/.claude/CLAUDE.md
# cook:end io.github.lmarqs.cookbook:agents#connect/harnesses
'''
    def ignored = { String path ->
      ['git', '-C', kitchen.path, 'check-ignore', '-q', '--no-index', path].execute().waitFor() == 0
    }
    ['.claude/settings.json', '.claude/skills', '.claude/commands', '.codex/config.toml', '.codex/rules'].each {
      assert Files.isSymbolicLink(new File(kitchen, it).toPath())
      assert !ignored(it)
    }
    ['.claude/hooks', '.claude/memory', '.codex/hooks'].each { assert !new File(kitchen, it).exists() }
    ['.claude/settings.local.json', '.claude/worktrees/x', 'CLAUDE.md', 'CLAUDE.local.md', 'sub/.claude/CLAUDE.md']
    .each { assert ignored(it) }
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
      '.agents/permission/policy.yaml',
      '.agents/skills/.gitkeep',
      '.agents/skills/agents-layout/SKILL.md',
      '.agents/skills/agents-layout/references/adopt.md',
      '.agents/skills/agents-layout/references/agents-md.md',
      '.agents/skills/agents-layout/references/design.md',
      '.agents/skills/agents-layout/references/investigate.md',
      '.agents/skills/agents-layout/references/policy.md',
      '.agents/skills/agents-layout/scripts/check.sh',
      '.agents/skills/agents-layout/templates/AGENTS.template.md',
      '.agents/skills/agents-layout/templates/README.template.md',
      '.agents/skills/agents-layout/templates/policy.template.yaml',
      'AGENTS.md',
      'cook.lock'
    ]
    seed.commit()

    def connect = { -> cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]) }
    def connected = connect()
    assert connected.changes == [
      '.agents/adapters/claude/settings.json',
      '.agents/adapters/codex/config.toml',
      '.claude/commands',
      '.claude/settings.json',
      '.claude/skills',
      '.codex/config.toml',
      '.gitignore',
      'cook.lock'
    ]
    connected.commit()
    assert new File(kitchen, '.gitignore').text.contains('/.claude/*\n')
    // Both harnesses read `AGENTS.md` natively, so the layout writes no instruction file of its own.
    def files = []
    kitchen.eachFileRecurse(groovy.io.FileType.FILES) { files << it.name }
    assert !('CLAUDE.md' in files)
    assert !new File(kitchen, 'AGENTS.md').text.contains('#codex')
    assert Files.readSymbolicLink(new File(kitchen, '.codex/config.toml').toPath()).toString() ==
    '../.agents/adapters/codex/config.toml'
    assert Files.readSymbolicLink(new File(kitchen, '.claude/commands').toPath()).toString() == '../.agents/commands'
    assert connect().changes == []

    // `File.deleteDir` would follow the links into `.agents/`; `rm -rf` does not, and neither does this.
    ['.claude', '.codex'].each { directory ->
      Files.walk(new File(kitchen, directory).toPath())
          .sorted(Comparator.reverseOrder())
          .each { Files.delete(it) }
    }
    def again = connect()
    def links = ['.claude/commands', '.claude/settings.json', '.claude/skills']
    assert again.changes == links + ['.codex/config.toml']
    again.commit()
    // How a session asks is each person's choice, so the shared settings leave out `defaultMode`.
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, '.claude/settings.json')).permissions ==
    [allow: [], ask: [], deny: [], disableBypassPermissionsMode: 'disable']
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
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#skill-check', [skill: ['agents-layout']]).commit()
    assert new File(kitchen, '.agents/skills/agents-layout/SKILL.md').text ==
    new File(COOKBOOK, 'agents/skills/agents-layout/SKILL.md').text
    def link = new File(kitchen, '.claude/skills').toPath()
    assert Files.readSymbolicLink(link).toString() == '../.agents/skills'
    assert new File(kitchen, '.claude/skills/agents-layout/SKILL.md').isFile()

    def fewer = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#skill-check', [:])
    assert fewer.changes == [
      '.agents/skills/agents-layout/SKILL.md',
      '.agents/skills/agents-layout/references/adopt.md',
      '.agents/skills/agents-layout/references/agents-md.md',
      '.agents/skills/agents-layout/references/design.md',
      '.agents/skills/agents-layout/references/investigate.md',
      '.agents/skills/agents-layout/references/policy.md',
      '.agents/skills/agents-layout/scripts/check.sh',
      '.agents/skills/agents-layout/templates/AGENTS.template.md',
      '.agents/skills/agents-layout/templates/README.template.md',
      '.agents/skills/agents-layout/templates/policy.template.yaml',
      '.claude/skills',
      'cook.lock',
    ]
    fewer.commit()
    assert !new File(kitchen, '.agents/skills').exists()
    assert !new File(kitchen, '.claude/skills').exists()
    assert !new File(kitchen, '.claude/CLAUDE.md').exists()
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

  @Test void discardRefusesARecipeTheLockDoesNotListOrThatIsGivenTwice() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    [
      [
        ['io.github.lmarqs.cookbook:agents:0.1.0#permissions'],
        'cook.lock lists no recipe io.github.lmarqs.cookbook:agents#permissions; nothing was written\n' +
        '  discard removes only what cook.lock records for a recipe\n' +
        '  check the recipe\'s name; cook.lock lists io.github.lmarqs.cookbook:agents#seed'
      ],
      [
        ['io.github.lmarqs.cookbook:agents:0.1.0#seed', 'io.github.lmarqs.cookbook:agents:0.2.0#seed'],
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
  private static String fixture(String name) {
    CookbookCookTest.getResource("/agents-layout/${name}").text
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
