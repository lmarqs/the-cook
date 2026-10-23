package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook
import static thecook.Kitchens.publish
import static thecook.Kitchens.repositories
import static thecook.Kitchens.request
import static thecook.Kitchens.sourcesSha256

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookbookCook
import thecook.cookbook.CookLock
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.ExistingSeed
import thecook.cookbook.KeyTarget

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

class AgentsLayoutCookTest {
  private static final File TEMPLATES = new File(COOKBOOK, 'agents/skills/agents-layout/templates')

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
    arguments:
      "harness": ["claude"]
    owns:
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:agents#connect/harnesses"
      keys:
        - file: ".agents/adapters/claude/settings.json"
          path: ["hooks","SessionStart"]
        - file: ".agents/adapters/claude/settings.json"
          path: ["permissions"]
      files:
        - ".agents/hooks/session-start"
      links:
        - ".claude/commands"
        - ".claude/settings.json"
        - ".claude/skills"
  - name: "io.github.lmarqs.cookbook:agents#seed"
    source: "io.github.lmarqs.cookbook:agents:0.1.0#seed"
    sha256: "${sha256}"
    arguments: {}
    owns:
      files:
        - ".agents/skills/agents-layout/SKILL.md"
        - ".agents/skills/agents-layout/references/adopt.md"
        - ".agents/skills/agents-layout/references/agents-md.md"
        - ".agents/skills/agents-layout/references/claude.md"
        - ".agents/skills/agents-layout/references/codex.md"
        - ".agents/skills/agents-layout/references/design.md"
        - ".agents/skills/agents-layout/references/investigate.md"
        - ".agents/skills/agents-layout/references/policy.md"
        - ".agents/skills/agents-layout/scripts/check.sh"
        - ".agents/skills/agents-layout/templates/AGENTS.template.md"
        - ".agents/skills/agents-layout/templates/README.template.md"
        - ".agents/skills/agents-layout/templates/policy.template.yaml"
"""
    assert cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).changes == []

    assert new File(kitchen, '.gitignore').readLines()[0..1] == [
      '# cook:begin io.github.lmarqs.cookbook:agents#connect/harnesses',
      '# io.github.lmarqs.cookbook:agents#connect --harness=claude'
    ]

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    assert !new File(kitchen, '.claude').exists()
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:agents#connect --harness=codex'
    assert !new File(kitchen, 'cook.lock').text.contains('.claude/')
  }

  @Test void theAgentsTemplateKeepsItsFixedTextWithinBudgetAndMarksEveryBlankTheSameWay() {
    def lines = new File(TEMPLATES, 'AGENTS.template.md').readLines()
    def fixed = lines.findAll { !it.startsWith('>') }*.replaceAll(/\[\[fill:[^\]]*\]\]/, '')
    def words = fixed.collectMany { it.split(/\s+/).findAll() }
    assert words.size() <= 300
    assert !fixed.any { it.contains('[') || it =~ /\|\s*\|/ }
    // One marker, for a command its writer could not run; approval is the policy's `ask`, never a marker.
    assert lines.any { it.contains('`(unverified)`') }
    def files = []
    new File(COOKBOOK, 'agents').eachFileRecurse(groovy.io.FileType.FILES) { files << it }
    assert files && !files.any { it.text.toLowerCase().contains('ask first') }
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

  @Test void connectKeepsTheTeamsOwnKeys() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    def connect = { cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]) }
    connect().commit()
    def settings = new File(kitchen, '.claude/settings.json')
    def json = new groovy.json.JsonSlurper().parse(settings) + [env: [CI: '1']]
    json.hooks.Stop = [[hooks: [[type: 'command', command: '.agents/hooks/stop.sh']]]]
    settings.text = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)) + '\n'
    def config = new File(kitchen, '.codex/config.toml')
    config.append('\n[features]\nhooks = true\n')
    def team = [settings.text, config.text]

    assert connect().changes == []
    assert [settings.text, config.text] == team
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.names == ['io.github.lmarqs.cookbook:agents#connect', 'io.github.lmarqs.cookbook:agents#seed'] as Set
    def ignore = new File(kitchen, '.gitignore').readLines()
    ['config.toml', 'hooks.json', 'rules'].each {
      assert ignore.indexOf("!/.codex/${it}".toString()) > ignore.indexOf('/.codex/*')
    }
  }

  @Test void connectOwnsTheSessionStartDispatcherAndOneSessionStartKeyPerHarnessAndKeepsTheTeamsHooks() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    def connect = { cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]) }
    connect().commit()

    def dispatcher = new File(kitchen, '.agents/hooks/session-start')
    assert dispatcher.bytes == new File(COOKBOOK, 'agents/hooks/session-start').bytes
    assert Files.getPosixFilePermissions(dispatcher.toPath()) == PosixFilePermissions.fromString('rwxr-xr-x')
    def claudeSettings = new File(kitchen, '.agents/adapters/claude/settings.json')
    def codexHooks = new File(kitchen, '.agents/adapters/codex/hooks.json')
    def slurp = { File file -> new groovy.json.JsonSlurper().parse(file) }
    def claudeCommand = [type: 'command', command: '"$CLAUDE_PROJECT_DIR"/.agents/hooks/session-start claude']
    assert slurp(claudeSettings).hooks == [SessionStart: [[hooks: [claudeCommand]]]]
    // Codex runs a relative hook path from the session's directory, so the command starts from the repository root.
    def codexCommand = [type: 'command', statusMessage: 'Loading session context',
      command: 'sh -c \'exec "$(git rev-parse --show-toplevel)/.agents/hooks/session-start" codex\'']
    assert slurp(codexHooks) == [hooks: [SessionStart: [[matcher: 'startup|resume|clear', hooks: [codexCommand]]]]]
    assert Files.readSymbolicLink(new File(kitchen, '.codex/hooks.json').toPath()).toString() ==
    '../.agents/adapters/codex/hooks.json'
    def owned = CookLock.parse(new File(kitchen, 'cook.lock').text).owned('io.github.lmarqs.cookbook:agents#connect')
    assert new KeyTarget('.agents/adapters/claude/settings.json', ['hooks', 'SessionStart']) in owned
    assert new KeyTarget('.agents/adapters/codex/hooks.json', ['hooks', 'SessionStart']) in owned

    def preToolUse = [[matcher: 'Bash', hooks: [[type: 'command', command: '.agents/hooks/guard.sh']]]]
    [claudeSettings, codexHooks].each { file ->
      def json = slurp(file)
      json.hooks.PreToolUse = preToolUse
      file.text = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)) + '\n'
    }
    def team = [claudeSettings.text, codexHooks.text]
    assert connect().changes == []
    assert [claudeSettings.text, codexHooks.text] == team

    new CookbookDiscard(kitchen).propose(['io.github.lmarqs.cookbook:agents:0.1.0#connect']).commit()
    assert !dispatcher.exists()
    assert slurp(claudeSettings) == [hooks: [PreToolUse: preToolUse]]
    assert slurp(codexHooks) == [hooks: [PreToolUse: preToolUse]]
  }

  @Test void theSessionStartDispatcherPrintsEveryScriptsOutputInNameOrderAsOneJsonEnvelope() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()
    def dispatch = { String harness ->
      def process = new ProcessBuilder('.agents/hooks/session-start', harness).directory(kitchen).start()
      def output = process.inputStream.getText('UTF-8')
      [process.waitFor(), output]
    }
    def scripts = new File(kitchen, '.agents/hooks/session-start.d')
    assert !scripts.exists()
    assert dispatch('claude') == [0, '']
    scripts.mkdirs()
    assert dispatch('claude') == [0, '']

    new File(scripts, '20-text.sh').setText('printf \'quote " backslash \\\\ tab \\t end\\nsecond line\\n\'\n' +
        'printf \'cr \\r ctl \\001 unit \\037 del \\177 utf8 \u00e9 \u2713\\n\'\n', 'UTF-8')
    new File(scripts, '10-harness.sh').text = 'echo "harness $1"\n'
    new File(scripts, '30-skipped.txt').text = 'echo never\n'
    def (status, output) = dispatch('codex')

    assert status == 0
    assert new groovy.json.JsonSlurper().parseText(output) == [hookSpecificOutput: [hookEventName: 'SessionStart',
        additionalContext: 'harness codex\nquote " backslash \\ tab \t end\nsecond line\n' +
        'cr \r ctl \u0001 unit \u001f del \u007f utf8 \u00e9 \u2713']]
  }

  @Test void composesConnectAndTheSeedInOneInvocation() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def proposal = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]),
      request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]),
    ])
    assert proposal.changes == [
      '.agents/README.md',
      '.agents/adapters/claude/settings.json',
      '.agents/adapters/codex/config.toml',
      '.agents/adapters/codex/hooks.json',
      '.agents/commands/.gitkeep',
      '.agents/hooks/.gitkeep',
      '.agents/hooks/session-start',
      '.agents/memory/.gitkeep',
      '.agents/permission/policy.yaml',
      '.agents/skills/.gitkeep',
      '.agents/skills/agents-layout/SKILL.md',
      '.agents/skills/agents-layout/references/adopt.md',
      '.agents/skills/agents-layout/references/agents-md.md',
      '.agents/skills/agents-layout/references/claude.md',
      '.agents/skills/agents-layout/references/codex.md',
      '.agents/skills/agents-layout/references/design.md',
      '.agents/skills/agents-layout/references/investigate.md',
      '.agents/skills/agents-layout/references/policy.md',
      '.agents/skills/agents-layout/scripts/check.sh',
      '.agents/skills/agents-layout/templates/AGENTS.template.md',
      '.agents/skills/agents-layout/templates/README.template.md',
      '.agents/skills/agents-layout/templates/policy.template.yaml',
      '.claude/commands',
      '.claude/settings.json',
      '.claude/skills',
      '.codex/config.toml',
      '.codex/hooks.json',
      '.gitignore',
      'AGENTS.md',
      'cook.lock'
    ]
    proposal.commit()
    assert new File(kitchen, '.agents/permission/policy.yaml').text ==
    new File(TEMPLATES, 'policy.template.yaml').text
    assert Files.readSymbolicLink(new File(kitchen, '.claude/settings.json').toPath()).toString() ==
    '../.agents/adapters/claude/settings.json'
    assert new groovy.json.JsonSlurper().parse(new File(kitchen, '.claude/settings.json')).permissions ==
    [allow: [], ask: [], deny: [], disableBypassPermissionsMode: 'disable']
    assert new File(kitchen, '.codex/config.toml').text.contains('sandbox_mode = "workspace-write"')
    assert new File(kitchen, '.agents/adapters/claude/settings.json').text == '''{
  "permissions": {
    "allow": [],
    "ask": [],
    "deny": [],
    "disableBypassPermissionsMode": "disable"
  },
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "\\"$CLAUDE_PROJECT_DIR\\"/.agents/hooks/session-start claude"
          }
        ]
      }
    ]
  }
}
'''
    def same = new CookbookCook(kitchen, repositories(kitchen)).propose([
      request('io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]),
      request('io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]),
    ])
    assert same.changes == []

    def withoutCodex = cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']])
    assert withoutCodex.changes == [
      '.agents/adapters/codex/config.toml',
      '.agents/adapters/codex/hooks.json',
      '.codex/config.toml',
      '.codex/hooks.json',
      '.gitignore',
      'cook.lock'
    ]
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

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()

    assert new File(kitchen, '.agents/adapters/claude/settings.json').text == fixture('claude-settings.json')
    def roles = new File(kitchen, '.agents/adapters/claude/roles')
    assert roles.list().sort() == ['reviewer.json', 'worker.json']
    assert new File(roles, 'worker.json').text == fixture('claude-worker.json')
    assert new File(roles, 'reviewer.json').text == fixture('claude-reviewer.json')

    policy.text = policy.text.replaceAll(/(?s)\nroles:.*/, '\n')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()
    assert !roles.exists()

    policy.text = policy.text.replaceFirst('version: 2\n', 'version: 2\nbypass: allow\n')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude']]).commit()
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

    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()

    def rules = new File(kitchen, '.agents/adapters/codex/rules/cook.rules').readLines()
    assert rules.first() == '# cook:begin io.github.lmarqs.cookbook:agents#connect/commands'
    assert rules.last() == '# cook:end io.github.lmarqs.cookbook:agents#connect/commands'
    assert rules[1] == '# io.github.lmarqs.cookbook:agents#connect --harness=codex'
    assert rules[2..-2].join('\n') + '\n' == fixture('codex-commands.rules')
    // A Codex run cannot add a rules file of its own, so roles render nothing for Codex.
    assert !new File(kitchen, '.agents/adapters/codex/roles').exists()

    policy.text = policy.text.replaceFirst('version: 2\n', 'version: 2\nbypass: allow\n')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['codex']]).commit()
    def allowed = new File(kitchen, '.agents/adapters/codex/rules/cook.rules').readLines()
    assert allowed[1..-2] == [rules[1]] + rules[3..-2]
  }

  @Test void theLayoutCheckOnlyRecommendsOnAFreshSeedAndOnAFilledAgentsMdNamingWhatIsNotInTheTree() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def run = runner(kitchen)
    assert run(['git', 'init', '-q'])[0] == 0
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]).commit()
    def check = ['sh', '.agents/skills/agents-layout/scripts/check.sh']
    def stage = ['git', 'add', 'AGENTS.md', '.agents', '.claude', '.codex', '.gitignore', 'cook.lock']
    assert run(stage)[0] == 0
    def state = ['git', 'status', '--porcelain', '--ignored']
    def before = run(state)[1]
    def (fresh, freshOutput) = run(check)

    assert fresh == 0
    assert run(state)[1] == before
    def recommendations = freshOutput.readLines().findAll { !it.startsWith('ok ') }
    assert recommendations.size() == 2
    assert recommendations[0].startsWith("WARN AGENTS.md still has the template's note -- ")
    assert recommendations[1].startsWith('WARN AGENTS.md has 27 lines with [[fill: blanks -- ')

    new File(kitchen, 'AGENTS.md').text = fixture('filled-agents.md')
    assert run(stage)[0] == 0
    def (filled, filledOutput) = run(check)

    assert filled == 0
    def warnings = filledOutput.readLines().findAll { it.startsWith('WARN ') }
    assert warnings*.takeWhile { it != '-' } == [
      'WARN AGENTS.md names origin/main, which is not in the tree ',
      'WARN AGENTS.md names .work/services.lock, which is not in the tree ',
    ]
    assert filledOutput.readLines().every { it.startsWith('ok ') || it.startsWith('WARN ') }
    assert filledOutput.contains('ok no CLAUDE.md, .claude/CLAUDE.md or CLAUDE.local.md beside or above an AGENTS.md\n')
    assert filledOutput.contains('ok .claude/settings.json is a tracked link that resolves\n')
    assert filledOutput.contains('ok .codex/config.toml is a tracked link that resolves\n')
  }

  @Test void theLayoutCheckFailsOnlyOnAClaudeFileBesideOrAboveAnAgentsMdAndNeverOnATrackedLink() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def run = runner(kitchen)
    assert run(['git', 'init', '-q'])[0] == 0
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]).commit()
    def check = ['sh', '.agents/skills/agents-layout/scripts/check.sh']
    new File(kitchen, '.gitignore').append('node_modules/\n')
    def dependency = new File(kitchen, 'node_modules/x/CLAUDE.md')
    dependency.parentFile.mkdirs()
    dependency.text = 'a dependency of the project\n'
    new File(kitchen, '.agents/hooks').mkdirs()
    Files.createSymbolicLink(new File(kitchen, '.claude/hooks').toPath(), Paths.get('../.agents/hooks'))
    def stage = ['git', 'add', 'AGENTS.md', '.agents', '.claude', '.codex', '.gitignore', 'cook.lock']
    assert run(stage)[0] == 0
    // The team's own link sits under /.claude/*, so only a forced add tracks it.
    assert run(['git', 'add', '-f', '.claude/hooks'])[0] == 0

    def (installed, installedOutput) = run(check)

    assert installed == 0
    assert !installedOutput.contains('node_modules')
    assert installedOutput.contains('ok .claude/hooks is a tracked link that resolves\n')

    assert run(['git', 'rm', '-q', '--cached', '.claude/hooks'])[0] == 0
    def (untracked, untrackedOutput) = run(check)

    assert untracked == 1
    def hooks = untrackedOutput.readLines().findAll { it.contains('.claude/hooks') }
    assert hooks == [
      'FAIL .claude/hooks is not tracked as a link -- git add .claude/hooks; ' +
      'when git add skips it, git check-ignore -v --no-index .claude/hooks names the rule that hides it'
    ]

    assert run(['git', 'add', '-f', '.claude/hooks'])[0] == 0
    new File(kitchen, 'CLAUDE.md').text = 'a personal file\n'
    def (beside, besideOutput) = run(check)

    assert beside == 1
    assert besideOutput.readLines().findAll { it.startsWith('FAIL ') } ==
    [
      'FAIL CLAUDE.md exists, and Claude skips AGENTS.md while it does -- ' +
      'move what the team needs into AGENTS.md, then delete it'
    ]
  }

  @Test void theLayoutCheckRecommendsMovingATrackedEntryBesideTheSharedDirectories() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def run = runner(kitchen)
    assert run(['git', 'init', '-q'])[0] == 0
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: ['claude', 'codex']]).commit()
    def check = ['sh', '.agents/skills/agents-layout/scripts/check.sh']
    assert run(['git', 'add', 'AGENTS.md', '.agents', '.claude', '.codex', '.gitignore', 'cook.lock'])[0] == 0
    new File(kitchen, '.agents/hook-config.json').text = '{}\n'
    // A file only one machine has is not the team's layout.
    def (local, localOutput) = run(check)

    assert local == 0
    assert localOutput.contains('ok every tracked entry in .agents/ belongs to the layout\n')

    assert run(['git', 'add', '.agents/hook-config.json'])[0] == 0
    def (tracked, trackedOutput) = run(check)

    assert tracked == 0
    assert trackedOutput.readLines().findAll { it.contains('hook-config') } == [
      'WARN .agents/hook-config.json is outside the shared directories -- move it beside what uses it in skills/, ' +
      'commands/, hooks/ or memory/, merge permission rules into permission/policy.yaml, or move it out of .agents/ ' +
      'when no agent reads it; then update the paths that name it'
    ]

    new File(kitchen, '.agents/notas-ção.md').text = 'notes\n'
    assert run(['git', 'add', '.agents/notas-ção.md'])[0] == 0
    def (accented, accentedOutput) = run(check)

    assert accented == 0
    assert accentedOutput.contains('WARN .agents/notas-ção.md is outside the shared directories -- ')
  }

  @Test void theLayoutCheckReadsNoEntryFromAnAgentsLinkItself() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    def run = runner(kitchen)
    assert run(['git', 'init', '-q'])[0] == 0
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    def elsewhere = new File(kitchen, 'shared')
    assert new File(kitchen, '.agents').renameTo(elsewhere)
    Files.createSymbolicLink(new File(kitchen, '.agents').toPath(), Paths.get('shared'))
    assert run(['git', 'add', '.agents'])[0] == 0

    def output = run(['sh', new File(elsewhere, 'skills/agents-layout/scripts/check.sh').absolutePath])[1]

    assert output.contains('ok every tracked entry in .agents/ belongs to the layout\n')
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
# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex
!/.claude/
/.claude/*
!/.claude/settings.json
!/.claude/skills
!/.claude/commands
!/.codex/
/.codex/*
!/.codex/config.toml
!/.codex/hooks.json
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
      '.agents/skills/agents-layout/references/claude.md',
      '.agents/skills/agents-layout/references/codex.md',
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
      '.agents/adapters/codex/hooks.json',
      '.agents/hooks/session-start',
      '.claude/commands',
      '.claude/settings.json',
      '.claude/skills',
      '.codex/config.toml',
      '.codex/hooks.json',
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
    assert again.changes == links + ['.codex/config.toml', '.codex/hooks.json']
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
      '.agents/skills/agents-layout/references/claude.md',
      '.agents/skills/agents-layout/references/codex.md',
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

  private static String fixture(String name) {
    AgentsLayoutCookTest.getResource("/agents/agents-layout/${name}").text
  }

  /** Runs a command in the kitchen with no global or system git configuration, returning its exit code and output. */
  private static Closure<List> runner(File kitchen) {
    def environment = (System.getenv() + [GIT_CONFIG_GLOBAL: '/dev/null', GIT_CONFIG_NOSYSTEM: '1'])
    .collect { name, value -> "${name}=${value}" }
    return { List<String> command ->
      def process = command.execute(environment, kitchen)
      def output = process.text
      [process.waitFor(), output]
    }
  }

  private File kitchen() {
    folder.newFolder('kitchen')
  }

  private static String sha256(File file) {
    sha256(file.bytes)
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
