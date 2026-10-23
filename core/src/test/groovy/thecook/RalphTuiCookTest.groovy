package thecook

import static thecook.Kitchens.COOKBOOK
import static thecook.Kitchens.cook
import static thecook.Kitchens.ignored
import static thecook.Kitchens.publishDirectory
import static thecook.Kitchens.sourcesSha256

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.RecipeSource

import java.nio.file.Files
import java.util.concurrent.TimeUnit

class RalphTuiCookTest {
  private static final String RALPH = 'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui'
  private static final String WORKTREES = 'io.github.lmarqs.cookbook:git:0.1.0#worktrees'
  private static final File SOURCES = new File(COOKBOOK, 'ralph')
  private static final String CONFIG = '''\
configVersion = "2.1"
tracker = "beads"
# ralph-tui stops a run after this many iterations, each one agent run on one task; 0 means no limit.
maxIterations = 10
autoCommit = true

[errorHandling]
strategy = "skip"

[parallel]
directMerge = true

[[agents]]
name = "claude"
plugin = "claude"
defaultFlags = ["--permission-prompts", "none", "--permission-mode", "acceptEdits"]

[agents.options]
skipPermissions = false

[[agents]]
name = "codex"
plugin = "codex"
defaultFlags = ["--dangerously-bypass-hook-trust", "-c", "approval_policy=never", "-c", \
"sandbox_workspace_write.network_access=false"]

[agents.options]
fullAuto = false
sandbox = "workspace-write"
'''
  private static final String CLAUDE_FLAGS =
  'defaultFlags = ["--permission-prompts", "none", "--permission-mode", "acceptEdits"]'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void seedsTheConfigurationAndPromptsAndOwnsTheLauncherOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['claude', 'codex'], tracker: ['beads', 'json']]).commit()

    assert files(kitchen) == [
      '.gitignore',
      '.ralph-tui/bin/clean',
      '.ralph-tui/bin/run',
      '.ralph-tui/bin/worker',
      '.ralph-tui/config.toml',
      '.ralph-tui/templates/beads.hbs',
      '.ralph-tui/templates/json.hbs',
      'cook.lock',
    ]
    assert new File(kitchen, '.ralph-tui/config.toml').text == CONFIG
    ['beads.hbs', 'json.hbs'].each {
      assert new File(kitchen, ".ralph-tui/templates/${it}").bytes == new File(SOURCES, "templates/${it}").bytes
    }
    ['run', 'clean'].each {
      def script = new File(kitchen, ".ralph-tui/bin/${it}")
      assert script.bytes == new File(SOURCES, "bin/${it}.sh").bytes
      assert Files.isExecutable(script.toPath())
    }
    assert new File(kitchen, '.gitignore').text == '''\
# cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui
# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --tracker=beads --tracker=json
/.ralph-tui/*
!/.ralph-tui/config.toml
!/.ralph-tui/templates/
!/.ralph-tui/bin/
/.worktrees/ralph/
# cook:end io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui
'''
    assert new File(kitchen, 'cook.lock').text == """# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "io.github.lmarqs.cookbook:ralph#ralph-tui"
    source: "${RALPH}"
    sha256: "${sourcesSha256('ralph')}"
    owns:
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui"
      files:
        - ".ralph-tui/bin/clean"
        - ".ralph-tui/bin/run"
        - ".ralph-tui/bin/worker"
"""
    // No actions is what `./cook` reports as `Already cooked.`.
    assert cook(kitchen, RALPH, [harness: ['claude', 'codex'], tracker: ['beads', 'json']]).actions == [:]
  }

  @Test void eachHarnessGetsOneEntryAndNoDefaultAgent() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['codex', 'codex']]).commit()

    def config = new File(kitchen, '.ralph-tui/config.toml').text
    assert !config.contains('defaultAgent')
    assert config.count('[[agents]]') == 1
    assert !config.contains('claude')
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=codex'
  }

  @Test void keepsAHandEditedConfigurationAndPrompt() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    def config = new File(kitchen, '.ralph-tui/config.toml')
    config.text = config.text.replace('maxIterations = 10', 'maxIterations = 3')
    def edited = config.text
    def prompt = new File(kitchen, '.ralph-tui/templates/beads.hbs')
    prompt.text = 'Our prompt for {{taskId}}\n'

    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-root': ['wt']]).commit()
    assert config.text.contains('maxIterations = 3')
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worktree-root=wt'
    assert config.text == edited
    assert prompt.text == 'Our prompt for {{taskId}}\n'
  }

  @Test void aRoleAddsItsRenderedSettingsToTheClaudeEntryOnly() {
    def kitchen = kitchen()
    write(kitchen, '.agents/adapters/claude/roles/worker.json', '{"permissions":{"deny":["Bash(git push *)"]}}\n')

    cook(kitchen, RALPH,
        [harness: ['claude', 'codex'], 'worker-role': ['claude:worker'], tracker: ['beads', 'json']]).commit()

    def config = new File(kitchen, '.ralph-tui/config.toml').text
    assert config == CONFIG.replace(CLAUDE_FLAGS, CLAUDE_FLAGS.replace(']',
    ', "--settings", ".agents/adapters/claude/roles/worker.json"]'))
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worker-role=claude:worker ' +
    '--tracker=beads --tracker=json'
    assert new File(kitchen, '.agents/adapters/claude/roles/worker.json').text ==
    '{"permissions":{"deny":["Bash(git push *)"]}}\n'
  }

  @Test void refusesARoleWhoseRenderedSettingsAreNotInTheDraft() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-role': ['claude:worker']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=claude:worker needs ' +
      '.agents/adapters/claude/roles/worker.json',
      '  the role file is not in the draft',
      '  define roles.worker in .agents/permission/policy.yaml, then cook ' +
      'io.github.lmarqs.cookbook:agents#connect --harness=claude',
    ]
    assert files(kitchen) == []
  }

  @Test void takesTheRoleFileAgentsConnectRendersEarlierInTheInvocation() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')
    write(kitchen, '.agents/permission/policy.yaml', new File(COOKBOOK,
        'agents/skills/agents-layout/templates/policy.template.yaml').text + '''\
roles:
  worker:
    deny:
      - [git, push]
''')

    cook(kitchen, [
      'io.github.lmarqs.cookbook:agents:0.1.0#connect': [harness: ['claude']],
      (RALPH): [harness: ['claude'], 'worker-role': ['claude:worker']],
    ]).commit()

    assert new File(kitchen, '.agents/adapters/claude/roles/worker.json').text.contains('Bash(git push *)')
    assert new File(kitchen, '.ralph-tui/config.toml').text.contains(
    '"--settings", ".agents/adapters/claude/roles/worker.json"]')
  }

  @Test void aSettingsFileIsPassedAsGivenAndNeverWritten() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['claude:x.json']]).commit()

    assert new File(kitchen, '.ralph-tui/config.toml').text.contains(
    CLAUDE_FLAGS.replace(']', ', "--settings", "x.json"]\n'))
    assert !new File(kitchen, 'x.json').exists()
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worker-settings=claude:x.json'
  }

  @Test void refusesARoleAndASettingsFileTogether() {
    def kitchen = kitchen()
    write(kitchen, '.agents/adapters/claude/roles/worker.json', '{}\n')

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH,
          [harness: ['claude'], 'worker-role': ['claude:worker'], 'worker-settings': ['claude:x.json']])
    }

    assert error.message.readLines()[0] ==
    'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=claude:worker and ' +
    "--worker-settings=claude:x.json both set claude's settings"
  }

  @Test void refusesAWorkerFileForAHarnessNotGiven() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['codex:x.toml']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=codex:x.toml names codex, which no ' +
      '--harness gives',
      '  add --harness=codex, or leave it out',
    ]
  }

  @Test void aCodexSettingsFileBecomesConfigFlagsOfTheCodexEntry() {
    def kitchen = kitchen()
    write(kitchen, 'x.toml', 'model_reasoning_effort = "high"\n\n[sandbox_workspace_write]\n' +
        'writable_roots = ["/var/cache"]\n')

    cook(kitchen, RALPH,
        [harness: ['claude', 'codex'], 'worker-settings': ['codex:x.toml'], tracker: ['beads', 'json']]).commit()

    def config = new File(kitchen, '.ralph-tui/config.toml').text
    assert config == CONFIG.replace('"-c", "sandbox_workspace_write.network_access=false"]',
    '"-c", "sandbox_workspace_write.network_access=false", "-c", "model_reasoning_effort=\\"high\\"", ' +
    '"-c", "sandbox_workspace_write.writable_roots=[\\"/var/cache\\"]"]')
  }

  @Test void refusesACodexRoleEvenWithARoleFileInTheDraft() {
    def kitchen = kitchen()
    write(kitchen, '.agents/adapters/codex/roles/worker.toml', 'approval_policy = "untrusted"\n')

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['codex'], 'worker-role': ['codex:worker']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=codex:worker binds a role to codex, which has ' +
      'no roles',
      '  Codex loads no rules for one run, so a role\'s denies reach Claude only',
      '  give the Codex entry settings with --worker-settings=codex:<path>',
    ]
    assert files(kitchen) == ['.agents/adapters/codex/roles/worker.toml']
  }

  @Test void refusesAWorkerValueWithoutAHarness() {
    def kitchen = kitchen()

    ['worker', 'claude:', ':worker', ''].each { value ->
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, RALPH, [harness: ['claude'], 'worker-role': [value]])
      }

      assert error.message.readLines() == [
        "io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=${value} is not <harness>:<role>".toString(),
        '  name the harness the role binds, as --worker-role=claude:worker',
      ]
    }

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['x.json']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=x.json is not <harness>:<path>',
      '  name the harness the file binds, as --worker-settings=claude:ralph-claude.json',
    ]
  }

  @Test void refusesABlankWorkerSettingsPath() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['claude: ']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=claude:  is not <harness>:<path>',
      '  name the harness the file binds, as --worker-settings=claude:ralph-claude.json',
    ]
    assert files(kitchen) == []
  }

  @Test void refusesAWorkerValueNamingAHarnessThatIsNotOne() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-role': ['claud:worker']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=claud:worker names claud, which is not one of: ' +
      'claude, codex',
    ]
  }

  @Test void refusesAHarnessGivenTwiceInOneWorkerParam() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['claude:x.json', 'claude:y.json']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings names claude twice',
      '  give one file per harness',
    ]
  }

  @Test void refusesAWorkerSettingsPathOutsideTheRepository() {
    def kitchen = kitchen()

    ['/x.json', '../x.json', 'a/../x.json'].each { path ->
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ["claude:${path}".toString()]])
      }

      assert error.message.readLines() == [
        "io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=claude:${path} is not a path from the " +
        'repository root',
        '  name a file inside the repository, without .. segments',
      ]
    }
  }

  @Test void refusesACodexSettingsFileThatIsMissingOrNotToml() {
    def kitchen = kitchen()

    def missing = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['codex'], 'worker-settings': ['codex:x.toml']])
    }

    assert missing.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=codex:x.toml needs x.toml in the draft',
      '  Codex settings are read when cooking',
      '  write the file, then cook again',
    ]

    write(kitchen, 'x.toml', 'key = \n')
    def invalid = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['codex'], 'worker-settings': ['codex:x.toml']])
    }

    assert invalid.message.readLines().take(1) ==
    ['io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: x.toml is not valid TOML']
    assert invalid.message.readLines()[1].startsWith('  ') && invalid.message.readLines()[1].trim()

    write(kitchen, 'x.toml', '[[tables]]\nname = "a"\n')
    def unrenderable = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['codex'], 'worker-settings': ['codex:x.toml']])
    }

    assert unrenderable.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: x.toml: tables cannot be passed as -c tables=<value>',
      '  use strings, numbers, booleans or lists of them',
    ]
  }

  @Test void refusesACodexSettingsKeyThatMinusCCannotName() {
    def kitchen = kitchen()

    [
      '[mcp_servers."my.server"]\ncommand = "x"\n': 'mcp_servers."my.server"',
      '"a=b" = 1\n': '"a=b"',
    ].each { text, key ->
      write(kitchen, 'x.toml', text)
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, RALPH, [harness: ['codex'], 'worker-settings': ['codex:x.toml']])
      }

      assert error.message.readLines() == [
        "io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: x.toml: ${key} cannot be passed as " +
        '-c <key>=<value>',
        '  -c reads each . in a key as a nested table and an = as the end of the key; use keys without them',
      ]*.toString()
    }
  }

  @Test void requiresAHarness() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) { cook(kitchen, RALPH) }

    assert error.message.startsWith("${RALPH}: Missing --harness")
  }

  @Test void writesNothingForMiseBesideAMiseFile() {
    def kitchen = kitchen()
    write(kitchen, 'mise.toml', '[tools]\njava = "17"\n')

    cook(kitchen, RALPH, [harness: ['claude']]).commit()

    assert new File(kitchen, 'mise.toml').text == '[tools]\njava = "17"\n'
    assert !new File(kitchen, '.mise').exists()
    assert !new File(kitchen, 'cook.lock').text.contains('mise')
  }

  @Test void theLauncherScriptsAreExecutableAndPassShellSyntaxChecks() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude'], 'worker-check': ['scripts/check']]).commit()

    ['run', 'clean', 'worker'].each {
      assert Files.isExecutable(new File(kitchen, ".ralph-tui/bin/${it}").toPath())
      assert launch(kitchen, 'sh', '-n', ".ralph-tui/bin/${it}").exit == 0
    }
  }

  @Test void cooksWithGitWorktreesInEitherOrder() {
    [[RALPH, WORKTREES], [WORKTREES, RALPH]].eachWithIndex { order, index ->
      def kitchen = folder.newFolder("kitchen${index}")
      publishDirectory(kitchen, 'ralph')
      publishDirectory(kitchen, 'git')
      assert ['git', 'init', '-q', kitchen.path].execute().waitFor() == 0

      cook(kitchen, order.collectEntries { [(it): it == RALPH ? [harness: ['claude']] : [:]] }).commit()

      def gitignore = new File(kitchen, '.gitignore').text
      assert gitignore.contains('# cook:begin io.github.lmarqs.cookbook:git#worktrees/worktrees\n')
      assert gitignore.contains('# cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui\n')
      assert new File(kitchen, '.worktrees/README.md').exists()
      ['.worktrees/ralph/run/a', '.ralph-tui/progress.md', '.ralph-tui/iterations/1.log'].each {
        assert ignored(kitchen, it)
      }
      ['.worktrees/README.md', '.ralph-tui/config.toml', '.ralph-tui/templates/beads.hbs', '.ralph-tui/bin/run'].each {
        assert !ignored(kitchen, it)
      }
    }
  }

  @Test void cooksWithBeadsTrackerInOneInvocation() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'beads')

    cook(kitchen, [
      'io.github.lmarqs.cookbook:beads:0.1.0#tracker': [share: ['clone']],
      (RALPH): [harness: ['claude', 'codex'], tracker: ['beads', 'json']],
    ]).commit()

    assert files(kitchen).findAll { it.startsWith('.ralph-tui/') || it.startsWith('.beads/') } == [
      '.beads/PRIME.md',
      '.ralph-tui/bin/clean',
      '.ralph-tui/bin/run',
      '.ralph-tui/bin/worker',
      '.ralph-tui/config.toml',
      '.ralph-tui/templates/beads.hbs',
      '.ralph-tui/templates/json.hbs',
    ]
    assert new File(kitchen, '.ralph-tui/config.toml').text == CONFIG
    def gitignore = new File(kitchen, '.gitignore').text
    assert gitignore.contains('# cook:begin io.github.lmarqs.cookbook:beads#tracker/beads\n')
    assert gitignore.contains('# cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui\n')
    def lock = new File(kitchen, 'cook.lock').text
    assert lock.contains('  - name: "io.github.lmarqs.cookbook:beads#tracker"\n')
    assert lock.contains('  - name: "io.github.lmarqs.cookbook:ralph#ralph-tui"\n')
  }

  @Test void givesTheLauncherCommandsInEachInstructionsFile() {
    def kitchen = kitchen()
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Agents\n'
    def arguments = [harness: ['claude'], tracker: ['beads', 'json'], instructions: ['AGENTS.md']]

    cook(kitchen, RALPH, arguments).commit()

    assert agents.text == '''\
# Agents
<!-- cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui -->
<!-- io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --tracker=beads --tracker=json \
--instructions=AGENTS.md -->
- Loop runs: `.ralph-tui/bin/run <name> --agent <entry> --epic <id>`, or `--prd <file>`, runs ralph-tui in
  `.worktrees/ralph/<name>` on branch `ralph/<name>`; `.ralph-tui/bin/clean <name>` removes both once the
  branch is delivered.
<!-- cook:end io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui -->
'''
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --tracker=beads --tracker=json ' +
    '--instructions=AGENTS.md'
    assert cook(kitchen, RALPH, arguments).actions == [:]
  }

  @Test void aJsonOnlyCookSeedsOnlyTheJsonPromptAndOffersOnlyPrd() {
    def kitchen = repository()
    write(kitchen, 'AGENTS.md', '# Agents\n')
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['json'], instructions: ['AGENTS.md']]).commit()
    commitAll(kitchen)

    assert !new File(kitchen, '.ralph-tui/templates/beads.hbs').exists()
    assert new File(kitchen, '.ralph-tui/templates/json.hbs').exists()
    assert !new File(kitchen, '.ralph-tui/config.toml').text.contains('tracker')
    assert new File(kitchen, 'AGENTS.md').text.contains(
    '`.ralph-tui/bin/run <name> --agent <entry> --prd <file>` runs ralph-tui in')
    def usage = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--headless')
    assert usage.output.startsWith('usage: .ralph-tui/bin/run <name> --agent <entry> --prd <file> [--base <ref>]')
    write(kitchen, 'tasks/prd.json', '{}\n')
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--prd', 'tasks/prd.json').exit == 0
  }

  @Test void theTrackerIsJsonByDefault() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['claude']]).commit()

    assert files(kitchen).findAll { it.startsWith('.ralph-tui/templates/') } == ['.ralph-tui/templates/json.hbs']
    assert !new File(kitchen, '.ralph-tui/config.toml').text.contains('tracker')
    assert new File(kitchen, '.ralph-tui/bin/run').readLines().contains("flags='--prd <file>'")
  }

  @Test void aBeadsBvCookSeedsItsTrackerAndPromptAndOffersOnlyEpic() {
    def kitchen = repository()
    write(kitchen, 'AGENTS.md', '# Agents\n')
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads-bv'], instructions: ['AGENTS.md']]).commit()
    commitAll(kitchen)

    assert files(kitchen).findAll { it.startsWith('.ralph-tui/templates/') } == ['.ralph-tui/templates/beads-bv.hbs']
    assert new File(kitchen, '.ralph-tui/templates/beads-bv.hbs').bytes ==
    new File(SOURCES, 'templates/beads-bv.hbs').bytes
    assert new File(kitchen, '.ralph-tui/config.toml').text.contains('\ntracker = "beads-bv"\n')
    assert new File(kitchen, 'AGENTS.md').text.contains(
    '`.ralph-tui/bin/run <name> --agent <entry> --epic <id>` runs ralph-tui in')
    def usage = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--headless')
    assert usage.output.startsWith('usage: .ralph-tui/bin/run <name> --agent <entry> --epic <id> [--base <ref>]')
  }

  @Test void refusesTheBeadsAndBeadsBvTrackersTogether() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads', 'beads-bv']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --tracker=beads and --tracker=beads-bv are both given',
      '  both run with --epic, and the configuration names one tracker',
      '  give one of them',
    ]
    assert files(kitchen) == []
  }

  @Test void aBeadsBvRunPointsBvAtTheMainCheckoutsBeadsAndPassesItsPrompt() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads-bv', 'json']]).commit()
    new File(kitchen, '.git/info/exclude') << '/.ralph-tui/\n'
    write(kitchen, 'tasks/prd.json', '{"userStories":[]}\n')
    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo').canonicalPath

    def epic = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert epic.exit == 0
    assert epic.output.readLines().drop(4) == [
      "ralph-tui run --epic ep-1 --agent claude --prompt ${worktree}/.ralph-tui/templates/beads-bv.hbs",
      "cwd ${worktree}",
      'effort high',
      "beads ${kitchen.canonicalPath}/.beads",
    ]*.toString()

    def own = launch([BEADS_DIR: '/elsewhere/.beads'], kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude',
    '--epic', 'ep-1')
    assert own.output.readLines().last() == 'beads /elsewhere/.beads'

    def prd = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--prd', 'tasks/prd.json')
    assert prd.exit == 0
    assert prd.output.readLines().last() == 'effort high'
  }

  @Test void aBeadsRunLeavesBeadsDirUnset() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads']]).commit()
    commitAll(kitchen)

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines().last() == 'effort high'
  }

  @Test void aWorktreeRootMovesTheRunWorktreesTheirParallelWorkersAndTheIgnoreLine() {
    def kitchen = repository()
    write(kitchen, 'AGENTS.md', '# Agents\n')
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-root': ['wt'], instructions: ['AGENTS.md']]).commit()
    commitAll(kitchen)

    assert new File(kitchen, '.gitignore').readLines().contains('/wt/ralph/')
    assert new File(kitchen, 'AGENTS.md').text.contains('`wt/ralph/<name>` on branch `ralph/<name>`')
    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')
    assert run.exit == 0
    assert run.output.readLines()[1] == "worktree: ${kitchen.canonicalPath}/wt/ralph/demo".toString()
    def workers = new File(kitchen, 'wt/ralph/.ralph-worktrees/demo')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t1', new File(workers, 'worker-1').path)
    assert git(kitchen, 'status', '--porcelain') == ''

    def clean = launch(kitchen, '.ralph-tui/bin/clean', 'demo')

    assert clean.exit == 0
    assert clean.output.readLines().contains(
    "Removed the worktree ${workers.canonicalPath}/worker-1 and the branch ralph-parallel/s1/t1.".toString())
    assert !new File(kitchen, 'wt/ralph').exists()
    assert !new File(kitchen, '.worktrees').exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  @Test void aCloneShareIgnoresTheWholeDirectoryAndRunCopiesItIntoTheWorktree() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], share: ['clone']]).commit()

    assert new File(kitchen, '.gitignore').text == '''\
# cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui
# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --share=clone
/.ralph-tui/
/.worktrees/ralph/
# cook:end io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui
'''
    ['config.toml', 'templates/json.hbs', 'bin/run', 'bin/clean', 'bin/worker'].each {
      assert ignored(kitchen, ".ralph-tui/${it}")
    }
    commitAll(kitchen)
    assert git(kitchen, 'ls-files', '.ralph-tui') == ''

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines()[1].startsWith('Copied .ralph-tui/config.toml, templates/ and bin/worker from ')
    assert git(new File(kitchen, '.worktrees/ralph/demo'), 'status', '--porcelain') == ''
  }

  @Test void aShareChangeRewritesOnlyTheIgnoreSection() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    def team = new File(kitchen, '.gitignore').readLines().drop(2)

    def clone = cook(kitchen, RALPH, [harness: ['claude'], share: ['clone']])
    assert clone.actions == ['.gitignore': 'update']
    clone.commit()
    def back = cook(kitchen, RALPH, [harness: ['claude'], share: ['team']])
    assert back.actions == ['.gitignore': 'update']
    back.commit()

    assert new File(kitchen, '.gitignore').readLines().drop(2) == team
    def fork = shouldFail(IllegalArgumentException) { cook(kitchen, RALPH, [harness: ['claude'], share: ['fork']]) }
    assert fork.message == "${RALPH}: --share=fork is not one of: team, clone"
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(SOURCES, 'ralph-tui.groovy').text, 'ralph-tui.groovy')

    assert texts.params*.names ==
    [
      ['harness'],
      ['worker-role'],
      ['worker-settings'],
      ['worktree-root'],
      ['worktree-setup'],
      ['worktree-teardown'],
      ['worker-check'],
      ['tracker'],
      ['share'],
      ['instructions']
    ]
    assert texts.ingredients == ['io.github.lmarqs.cookbook.ralph.RalphTui']
    assert texts.help.readLines().findAll { it.startsWith('## ') } == [
      '## API',
      '## Consumes',
      '## Produces',
      '## Connections',
      '## Run-time effects',
      '## Re-cooking',
      '## Traps',
    ]
    assert texts.help.contains('Never touches')
    assert texts.yieldText.contains('mise use -g node@22 bun@1.3 npm:ralph-tui@0.12.0')
  }

  @Test void theModelDocumentsTheEntriesToAdd() {
    def text = new File(SOURCES, 'RalphTui.groovy').text
    [
      'skipPermissions = true',
      '"--settings", ".agents/adapters/claude/roles/worker.json"',
      'ralph-tui run --model',
      'CLAUDE_CODE_EFFORT_LEVEL',
      'model_reasoning_effort',
      '`sonnet`, `opus` and `haiku`',
    ].each { assert text.contains(it) }
  }

  @Test void theHelpStatesTheKnownLimits() {
    def help = RecipeSource.texts(new File(SOURCES, 'ralph-tui.groovy').text, 'ralph-tui.groovy').help
    def flat = help.replace('\\\n', '').replaceAll(/\s*\n\s*/, ' ')
    [
      'fewer than three tasks',
      'neither read nor write the Beads database',
      'permissions.allow',
      'prefix rules',
      'leaves `beads.hbs` unread',
      '`bv` orders serial runs only',
    ].each { assert flat.contains(it): "the help lacks '${it}'" }
  }

  @Test void everyVariableTheModelSetsIsInItsScript() {
    def scripts = [
      run: ['worktrees', 'setup', 'flags', 'epic_tracker'],
      clean: ['worktrees', 'teardown'],
      worker: ['check', 'agents'],
    ]

    scripts.each { script, names ->
      def lines = new File(SOURCES, "bin/${script}.sh").readLines()
      names.each { name -> assert lines.any { it.startsWith("${name}=") }: "${script}.sh lacks ${name}=" }
    }
  }

  @Test void thePromptsLeaveCommittingClosingAndPushingToOthers() {
    def common = [
      'Stay in this worktree',
      'ralph-tui commits your changes',
      'Never push',
      '<promise>COMPLETE</promise>',
      'BLOCKED: ',
    ]
    def beads = new File(SOURCES, 'templates/beads.hbs').text
    def json = new File(SOURCES, 'templates/json.hbs').text
    (common + ['Never close or update the bead', 'do not run `bd`']).each { assert beads.contains(it) }
    (common + ['Never edit the PRD file']).each { assert json.contains(it) }
    assert !json.contains('bd ')
  }

  @Test void runRefusesWhileTheCookIsUncommittedThenCreatesTheWorktreeAndStartsRalphTuiThere() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()

    def refused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')
    assert refused.exit == 1
    assert refused.output.startsWith('run: .ralph-tui has uncommitted changes\n')
    assert !new File(kitchen, '.worktrees').exists()

    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo').canonicalPath
    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1', '--headless',
        '--model', 'opus')
    assert run.exit == 0
    assert run.output.readLines() == [
      "Created the worktree ${kitchen.canonicalPath}/.worktrees/ralph/demo on ralph/demo.",
      "worktree: ${kitchen.canonicalPath}/.worktrees/ralph/demo",
      'branch: ralph/demo',
      'ralph-tui run --epic ep-1 --agent claude --headless --model opus',
      "cwd ${worktree}",
      'effort high',
    ]*.toString()
    assert git(kitchen, 'rev-parse', 'ralph/demo') == git(kitchen, 'rev-parse', 'HEAD')

    def reused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic=ep-1')
    assert reused.exit == 0
    assert reused.output.readLines()[0] == "Reusing the worktree ${worktree} on ralph/demo."
    assert reused.output.readLines()[3] == 'ralph-tui run --epic ep-1 --agent claude'
  }

  @Test void runCopiesAnIgnoredUncommittedConfigurationAndPromptsIntoTheWorktreeOnEachRunAndPassesThePrompt() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads', 'json']]).commit()
    new File(kitchen, '.git/info/exclude') << '/.ralph-tui/\n'
    write(kitchen, 'tasks/prd.json', '{"userStories":[]}\n')
    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo').canonicalPath
    def copied = "Copied .ralph-tui/config.toml, templates/ and bin/worker from ${kitchen.canonicalPath}: " +
        'they are not committed on ralph/demo.'

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1', '--headless')

    assert run.exit == 0
    assert run.output.readLines()[1] == copied.toString()
    assert run.output.readLines()[4] ==
    "ralph-tui run --epic ep-1 --agent claude --prompt ${worktree}/.ralph-tui/templates/beads.hbs --headless".toString()
    ['config.toml', 'templates/beads.hbs', 'templates/json.hbs'].each {
      assert new File(worktree, ".ralph-tui/${it}").text == new File(kitchen, ".ralph-tui/${it}").text
    }
    assert git(new File(worktree), 'status', '--porcelain') == ''

    def edited = 'maxIterations = 3\n\n[[agents]]\nname = "claude"\nplugin = "claude"\n'
    write(kitchen, '.ralph-tui/config.toml', edited)
    def reused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--prd', 'tasks/prd.json')

    assert reused.exit == 0
    assert reused.output.readLines()[1] == copied.toString()
    assert reused.output.readLines()[4] == ("ralph-tui run --prd ${kitchen.canonicalPath}/tasks/prd.json " +
    "--agent claude --prompt ${worktree}/.ralph-tui/templates/json.hbs").toString()
    assert new File(worktree, '.ralph-tui/config.toml').text == edited

    def own = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1', '--prompt',
        'mine.hbs')

    assert own.exit == 0
    assert own.output.readLines()[4] == 'ralph-tui run --epic ep-1 --agent claude --prompt mine.hbs'
  }

  @Test void runRefusesToCopyTheConfigurationIntoAWorktreeWhereGitWouldCommitIt() {
    def kitchen = repository()
    git(kitchen, 'branch', 'old')
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo')

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--base', 'old', '--epic', 'ep-1')

    assert run.exit == 1
    assert run.output.readLines()[1] ==
    'run: .ralph-tui/config.toml is not committed on ralph/demo, and Git does not ignore ' +
    '.ralph-tui/config.toml there'
    assert run.output.contains("echo /.ralph-tui/ >> ${kitchen.canonicalPath}/.git/info/exclude")
    assert !new File(worktree, '.ralph-tui').exists()
  }

  @Test void runPassesThePrdByItsAbsolutePathAndStartsFromTheBase() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    write(kitchen, 'tasks/prd.json', '{"userStories":[]}\n')
    commitAll(kitchen)
    git(kitchen, 'branch', 'base')
    write(kitchen, 'later.txt', 'after the base\n')
    commitAll(kitchen)

    def tasks = new File(kitchen, 'tasks')
    def run = launch(tasks, '../.ralph-tui/bin/run', 'story', '--agent', 'claude', '--base', 'base', '--prd',
        'prd.json')

    assert run.exit == 0
    assert run.output.readLines()[3] ==
    "ralph-tui run --prd ${kitchen.canonicalPath}/tasks/prd.json --agent claude".toString()
    assert git(kitchen, 'rev-parse', 'ralph/story') == git(kitchen, 'rev-parse', 'base')
  }

  @Test void runRunsNoSetupWithoutSetupEvenWhenWorktreesSetupIsExecutable() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    executable(kitchen, '.worktrees/setup', '#!/bin/sh\ntouch setup-ran\n')
    commitAll(kitchen)

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines()[3] == 'ralph-tui run --epic ep-1 --agent claude'
    assert !new File(kitchen, '.worktrees/ralph/demo/setup-ran').exists()
    assert !new File(kitchen, 'setup-ran').exists()
  }

  @Test void runRunsTheSetupFileOnceFromTheRootOfAWorktreeItCreatesAndNotOnReuse() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': ['.worktrees/setup']]).commit()
    executable(kitchen, '.worktrees/setup', '#!/bin/sh\npwd -P >> setup-ran\n')
    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo')

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines()[3] == 'ralph-tui run --epic ep-1 --agent claude'
    assert new File(worktree, 'setup-ran').readLines() == [worktree.canonicalPath]
    assert !new File(kitchen, 'setup-ran').exists()

    def reused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')
    assert reused.exit == 0
    assert new File(worktree, 'setup-ran').readLines() == [worktree.canonicalPath]
  }

  @Test void aSetupPathIsShellQuotedIntoTheLauncherAndRecordedOnTheArgumentLine() {
    def kitchen = repository()
    def path = "tools/it's \$HOME `set` up"
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': [path]]).commit()
    executable(kitchen, path, '#!/bin/sh\ntouch setup-ran\n')
    commitAll(kitchen)

    assert new File(kitchen, '.ralph-tui/bin/run').readLines().contains("setup='tools/it'\\''s \$HOME `set` up'")
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    "# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worktree-setup=${path}".toString()
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    assert new File(kitchen, '.worktrees/ralph/demo/setup-ran').exists()
  }

  @Test void runFailsWhenTheSetupFileIsMissingNotExecutableOrFails() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': ['scripts/setup']]).commit()
    commitAll(kitchen)

    def missing = launch(kitchen, '.ralph-tui/bin/run', 'missing', '--agent', 'claude', '--epic', 'ep-1')
    assert missing.exit == 1
    def worktree = new File(kitchen, '.worktrees/ralph/missing').canonicalPath
    assert missing.output.readLines()[1..-1] == [
      "run: scripts/setup is not in ${worktree}",
      '  a worktree holds only what is committed',
      '  fix it, then run .ralph-tui/bin/clean missing and run again',
    ]*.toString()

    write(kitchen, 'scripts/setup', '#!/bin/sh\nexit 0\n')
    commitAll(kitchen)
    def plain = launch(kitchen, '.ralph-tui/bin/run', 'plain', '--agent', 'claude', '--epic', 'ep-1')
    assert plain.exit == 1
    assert plain.output.readLines()[1] == 'run: scripts/setup is not executable'

    executable(kitchen, 'scripts/setup', '#!/bin/sh\nexit 3\n')
    commitAll(kitchen)
    def failing = launch(kitchen, '.ralph-tui/bin/run', 'failing', '--agent', 'claude', '--epic', 'ep-1')
    assert failing.exit == 1
    assert failing.output.readLines()[1..-1] == [
      'run: scripts/setup failed',
      "  in ${new File(kitchen, '.worktrees/ralph/failing').canonicalPath}",
      '  fix it, then run .ralph-tui/bin/clean failing and run again',
    ]*.toString()
    assert !failing.output.contains('ralph-tui run')
  }

  @Test void refusesASetupPathOutsideTheRepositoryOrUnderRalphTui() {
    def kitchen = kitchen()

    ['/etc/setup', '../setup', 'a/../../setup'].each { path ->
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': [path]])
      }
      assert error.message.readLines() == [
        "${RALPH}: --worktree-setup=${path} is outside the repository",
        '  a recipe reads and writes only inside the repository',
        '  name the file by its path from the repository root',
      ]*.toString()
    }
    ['.ralph-tui/setup': '.ralph-tui/setup', './.ralph-tui/bin/setup': '.ralph-tui/bin/setup'].each { path, given ->
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': [path]])
      }
      assert error.message.readLines() == [
        "${RALPH}: --worktree-setup=${given} is under .ralph-tui/",
        '  a run worktree has there only the configuration, prompts and launcher this recipe writes',
        '  name a committed file outside .ralph-tui/, as --worktree-setup=.worktrees/setup',
      ]*.toString()
    }
    assert files(kitchen) == []
  }

  @Test void refusesATeardownPathUnderRalphTui() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worktree-teardown': ['.ralph-tui/teardown']])
    }

    assert error.message.readLines() == [
      "${RALPH}: --worktree-teardown=.ralph-tui/teardown is under .ralph-tui/",
      '  a run worktree has there only the configuration, prompts and launcher this recipe writes',
      '  name a committed file outside .ralph-tui/, as --worktree-teardown=.worktrees/teardown',
    ]*.toString()
    assert files(kitchen) == []
  }

  @Test void aWorkerCheckWritesTheWorkerAndPointsEachSeededEntryAtItWithRetries() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['claude', 'codex'], 'worker-check': ['scripts/check']]).commit()

    def config = new File(kitchen, '.ralph-tui/config.toml').text
    assert config.contains('[errorHandling]\nstrategy = "retry"\nmaxRetries = 2\ncontinueOnNonZeroExit = false\n\n')
    assert config.contains('name = "claude"\nplugin = "claude"\ncommand = ".ralph-tui/bin/worker"\n' +
    'defaultFlags = ["claude", "--permission-prompts", "none", "--permission-mode", "acceptEdits"]\n')
    assert config.contains('name = "codex"\nplugin = "codex"\ncommand = ".ralph-tui/bin/worker"\n' +
    'defaultFlags = ["codex", "--dangerously-bypass-hook-trust", "-c", "approval_policy=never", "-c", ' +
    '"sandbox_workspace_write.network_access=false"]\n')
    def worker = new File(kitchen, '.ralph-tui/bin/worker')
    assert Files.isExecutable(worker.toPath())
    assert worker.text == new File(SOURCES, 'bin/worker.sh').text
    .replace("\ncheck=''\n", "\ncheck='scripts/check'\n").replace("\nagents=''\n", "\nagents='claude codex'\n")
    assert new File(kitchen, 'cook.lock').text.contains('        - ".ralph-tui/bin/worker"\n')
    assert new File(kitchen, '.gitignore').readLines()[1].endsWith(' --worker-check=scripts/check')
  }

  @Test void refusesAWorkerCheckOverAConfigurationSeededWithoutIt() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude', 'codex']]).commit()

    def error = refusedRecook(kitchen, [harness: ['claude', 'codex'], 'worker-check': ['scripts/check']])

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-check=scripts/check is not applied in ' +
      '.ralph-tui/config.toml',
      '  no [[agents]] entry there with plugin = "claude" has command = ".ralph-tui/bin/worker" and "claude" first ' +
      'in its defaultFlags',
      '  the configuration is seeded once and never rewritten, so a later cook cannot add it',
      '  add it there, or delete .ralph-tui/config.toml and cook again',
    ]
  }

  @Test void refusesARecookWhoseHarnessTheConfigurationLacks() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()

    def error = refusedRecook(kitchen, [harness: ['claude', 'codex']])

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --harness=codex is not applied in .ralph-tui/config.toml',
      '  no [[agents]] entry there has plugin = "codex"',
      '  the configuration is seeded once and never rewritten, so a later cook cannot add it',
      '  add it there, or delete .ralph-tui/config.toml and cook again',
    ]
  }

  @Test void refusesARecookWhoseWorkerFilesTheConfigurationLacks() {
    def kitchen = kitchen()
    write(kitchen, '.agents/adapters/claude/roles/worker.json', '{}\n')
    write(kitchen, 'x.json', '{}\n')
    write(kitchen, 'x.toml', 'retries = 3\nname = "a\\"b"\n')
    cook(kitchen, RALPH, [harness: ['claude', 'codex']]).commit()

    def role = refusedRecook(kitchen, [harness: ['claude', 'codex'], 'worker-role': ['claude:worker']])
    def claude = refusedRecook(kitchen, [harness: ['claude', 'codex'], 'worker-settings': ['claude:x.json']])
    def codex = refusedRecook(kitchen, [harness: ['claude', 'codex'], 'worker-settings': ['codex:x.toml']])

    assert role.message.readLines().take(2) == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=claude:worker is not applied in ' +
      '.ralph-tui/config.toml',
      '  no [[agents]] entry there with plugin = "claude" has "--settings", ' +
      '".agents/adapters/claude/roles/worker.json" in its defaultFlags',
    ]
    assert claude.message.readLines().take(2) == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=claude:x.json is not applied in ' +
      '.ralph-tui/config.toml',
      '  no [[agents]] entry there with plugin = "claude" has "--settings", "x.json" in its defaultFlags',
    ]
    assert codex.message.readLines().take(2) == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=codex:x.toml is not applied in ' +
      '.ralph-tui/config.toml',
      '  no [[agents]] entry there with plugin = "codex" has "-c", "retries=3", "-c", "name=\\"a\\\\\\"b\\"" in ' +
      'its defaultFlags',
    ]
  }

  @Test void refusesARecookWhoseBeadsTrackerTheConfigurationLacks() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['json']]).commit()

    def lacking = refusedRecook(kitchen, [harness: ['claude'], tracker: ['beads']])

    assert lacking.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --tracker=beads is not applied in .ralph-tui/config.toml',
      '  it sets no tracker, and a run with --epic needs tracker = "beads"',
      '  the configuration is seeded once and never rewritten, so a later cook cannot add it',
      '  add it there, or delete .ralph-tui/config.toml and cook again',
    ]

    def config = new File(kitchen, '.ralph-tui/config.toml')
    config.text = 'tracker = "json"\n' + config.text
    def other = refusedRecook(kitchen, [harness: ['claude'], tracker: ['beads']])

    assert other.message.readLines()[1] == '  its tracker is "json", and a run with --epic needs tracker = "beads"'
  }

  @Test void aBeadsBvRecookNeedsTheBeadsBvTrackerAndABeadsRecookAcceptsIt() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads']]).commit()

    def lacking = refusedRecook(kitchen, [harness: ['claude'], tracker: ['beads-bv']])

    assert lacking.message.readLines().take(2) == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --tracker=beads-bv is not applied in .ralph-tui/config.toml',
      '  its tracker is "beads", and --tracker=beads-bv needs tracker = "beads-bv"',
    ]

    def config = new File(kitchen, '.ralph-tui/config.toml')
    config.text = config.text.replace('tracker = "beads"', 'tracker = "beads-bv"')
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads-bv']]).commit()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads']]).commit()
    assert config.text.contains('tracker = "beads-bv"')
  }

  @Test void acceptsARecookOverAHandWrittenConfigurationThatHasWhatTheArgumentsNeed() {
    def kitchen = kitchen()
    write(kitchen, '.ralph-tui/config.toml', '''\
tracker = "beads-rust"
defaultAgent = "shim"

[[agents]]
name = "shim"
plugin = "claude"
command = "scripts/ralph/claude"
defaultFlags = ["--permission-prompts", "none", "--settings", "x.json"]

[agents.options]
skipPermissions = false
''')
    def before = new File(kitchen, '.ralph-tui/config.toml').text

    cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['claude:x.json']]).commit()

    assert new File(kitchen, '.ralph-tui/config.toml').text == before
    assert new File(kitchen, '.gitignore').readLines()[1].endsWith(' --harness=claude --worker-settings=claude:x.json')
  }

  @Test void acceptsARecookWithTheWorkerCheckTheConfigurationWasSeededWith() {
    def kitchen = kitchen()
    cook(kitchen, RALPH, [harness: ['claude', 'codex'], 'worker-check': ['scripts/check']]).commit()
    def config = new File(kitchen, '.ralph-tui/config.toml').text

    cook(kitchen, RALPH, [harness: ['claude', 'codex'], 'worker-check': ['scripts/other']]).commit()

    assert new File(kitchen, '.ralph-tui/config.toml').text == config
    assert new File(kitchen, '.ralph-tui/bin/worker').text.contains("\ncheck='scripts/other'\n")
  }

  @Test void refusesAConfigurationThatIsNotToml() {
    def kitchen = kitchen()
    write(kitchen, '.ralph-tui/config.toml', 'key = \n')

    def error = refusedRecook(kitchen, [harness: ['claude']])

    def lines = error.message.readLines()
    assert lines.size() == 3
    assert lines[0] == 'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: .ralph-tui/config.toml is not valid TOML'
    assert lines[1].startsWith('  ') && lines[1].trim()
    assert lines[2] == '  fix it, or delete it and cook again'
  }

  @Test void withoutAWorkerCheckTheWorkerIsWrittenAndRefusesEveryTask() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph/demo', '.worktrees/ralph/demo')

    def refused = launch(new File(kitchen, '.worktrees/ralph/demo'), '.ralph-tui/bin/worker', 'claude', '--print')

    assert refused.exit == 1
    assert refused.output.readLines() == [
      'worker: no check is set, so no task may complete',
      '  cook io.github.lmarqs.cookbook:ralph#ralph-tui again with --worker-check=<path>,',
      "  or point the agent entry's command in .ralph-tui/config.toml at the team's own wrapper",
    ]
    assert !new File(kitchen, '.ralph-tui/config.toml').text.contains('command')
  }

  @Test void refusesAWorkerCheckUnderRalphTui() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-check': ['.ralph-tui/check']])
    }

    assert error.message.readLines() == [
      "${RALPH}: --worker-check=.ralph-tui/check is under .ralph-tui/",
      '  a run worktree has there only the configuration, prompts and launcher this recipe writes',
      '  name a committed file outside .ralph-tui/, as --worker-check=scripts/check',
    ]*.toString()
    assert files(kitchen) == []
  }

  @Test void theWorkerHandsTheAgentsOutputToRalphTuiOnlyOnceTheCheckPasses() {
    def kitchen = workerRepository()
    def worktree = new File(kitchen, '.worktrees/ralph/demo')
    def log = new File(worktree, '.ralph-tui/worker-check.log')
    def stderr = new File(folder.root, 'stderr')
    def base = git(worktree, 'rev-parse', 'HEAD')

    def passed = launch(worktree, 'sh', '-c', "echo the prompt | .ralph-tui/bin/worker claude --print 2>${stderr}")

    assert passed.exit == 0
    assert passed.output.readLines() == ['args --print', 'prompt the prompt', '<promise>COMPLETE</promise>']
    assert stderr.text == ''
    assert log.text == "check ${base}\nrate limit exceeded, 429, overloaded\n".toString()

    def failed = launch(worktree, 'sh', '-c',
        "echo the prompt | CHECK_FAILS=1 .ralph-tui/bin/worker claude 2>${stderr}")

    assert failed.exit == 1
    assert failed.output == ''
    assert stderr.text == "worker: scripts/check did not pass, so the task stays open; its output is in " +
    "${worktree.canonicalPath}/.ralph-tui/worker-check.log\n"
    assert log.text.contains('rate limit exceeded')

    log.delete()
    def blocked = launch(worktree, 'sh', '-c', 'echo the prompt | AGENT_BLOCKED=1 .ralph-tui/bin/worker codex exec -')

    assert blocked.exit == 0
    assert blocked.output.readLines() == ['args exec -', 'prompt the prompt', 'BLOCKED: no tests']
    assert !log.exists()

    def broken = launch(worktree, 'sh', '-c', 'echo the prompt | AGENT_FAILS=1 .ralph-tui/bin/worker claude')

    assert broken.exit == 3
    assert broken.output == ''
    assert !log.exists()
    assert git(worktree, 'status', '--porcelain') == ''
  }

  @Test void theWorkerChecksEveryCompletionRalphTuiAccepts() {
    def worktree = new File(workerRepository(), '.worktrees/ralph/demo')
    def log = new File(worktree, '.ralph-tui/worker-check.log')

    [
      '<promise>complete</promise>',
      '<promise>\n COMPLETE\n</promise>',
      '{"type":"result","result":"<promise> Complete </promise>"}'
    ].each { promise ->
      log.delete()

      def failed = launch(worktree, 'sh', '-c',
          "echo the prompt | PROMISE='${promise}' CHECK_FAILS=1 .ralph-tui/bin/worker claude 2>/dev/null")

      assert failed.exit == 1: promise
      assert failed.output == '': promise
      assert log.text.startsWith('check '): promise
    }
  }

  @Test void theWorkerAsksEachHarnessForItsVersionAndRefusesTheMainCheckout() {
    def kitchen = workerRepository()

    def version = launch(new File(kitchen, '.worktrees/ralph/demo'), '.ralph-tui/bin/worker', '--version')

    assert version.exit == 0
    assert version.output == 'claude 2.1.306\ncodex-cli 0.160.1\n'

    def main = launch(kitchen, '.ralph-tui/bin/worker', 'claude')

    assert main.exit == 1
    assert main.output == "worker: ${kitchen.canonicalPath} is the main checkout; the worker runs only in a run " +
    'worktree, which .ralph-tui/bin/run creates\n'
  }

  @Test void stoppingTheWorkerStopsTheAgentsAndTheChecksProcessTrees() {
    def kitchen = workerRepository()
    def worktree = new File(kitchen, '.worktrees/ralph/demo')

    ['AGENT_SLEEPS', 'CHECK_SLEEPS'].each { variable ->
      def pid = new File(folder.root, variable)
      def builder = new ProcessBuilder('.ralph-tui/bin/worker', 'claude').directory(worktree).redirectErrorStream(true)
      builder.environment().putAll([
        PATH: "${new File(folder.root, 'bin')}:${System.getenv('PATH')}".toString(),
        (variable): pid.path,
      ])
      def worker = builder.start()
      worker.outputStream.close()
      eventually { pid.size() > 0 }
      def sleeper = ProcessHandle.of(pid.text.trim() as long).get()

      worker.destroy()

      assert worker.waitFor(10, TimeUnit.SECONDS)
      assert worker.exitValue() == 143
      eventually { !sleeper.alive }
    }
  }

  @Test void runCopiesTheWorkerWithAnIgnoredConfiguration() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    new File(kitchen, '.git/info/exclude') << '/.ralph-tui/\n'
    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo')

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines()[1].startsWith('Copied .ralph-tui/config.toml, templates/ and bin/worker from ')
    def worker = new File(worktree, '.ralph-tui/bin/worker')
    assert worker.text == new File(kitchen, '.ralph-tui/bin/worker').text
    assert Files.isExecutable(worker.toPath())
    assert git(worktree, 'status', '--porcelain') == ''
  }

  @Test void cleanRunsTheTeardownFromTheRunWorktreesRootBeforeRemovingIt() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-teardown': ['.worktrees/teardown']]).commit()
    executable(kitchen, '.worktrees/teardown', '#!/bin/sh\npwd -P >> ../../../teardown-ran\n')
    commitAll(kitchen)
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worktree-teardown=.worktrees/teardown'
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worktree = new File(kitchen, '.worktrees/ralph/demo').canonicalPath

    def clean = launch(kitchen, '.ralph-tui/bin/clean', 'demo')

    assert clean.exit == 0
    assert new File(kitchen, 'teardown-ran').readLines() == [worktree]
    assert !new File(worktree).exists()
    assert launch(kitchen, '.ralph-tui/bin/clean', 'demo').exit == 0
    assert new File(kitchen, 'teardown-ran').readLines() == [worktree]
  }

  @Test void cleanStopsAndRemovesNothingWhenTheTeardownIsMissingNotExecutableOrFails() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-teardown': ['scripts/teardown']]).commit()
    commitAll(kitchen)
    ['missing', 'plain', 'failing'].each {
      assert launch(kitchen, '.ralph-tui/bin/run', it, '--agent', 'claude', '--epic', 'ep-1').exit == 0
    }
    def worktree = { String name -> new File(kitchen, ".worktrees/ralph/${name}") }
    write(worktree('plain'), 'scripts/teardown', '#!/bin/sh\nexit 0\n')
    executable(worktree('failing'), 'scripts/teardown', '#!/bin/sh\nexit 3\n')
    ['plain', 'failing'].each {
      git(worktree(it), 'add', 'scripts/teardown')
      git(worktree(it), 'commit', '-q', '-m', 'teardown')
    }

    def missing = launch(kitchen, '.ralph-tui/bin/clean', 'missing')
    assert missing.exit == 1
    assert missing.output.readLines() == [
      "clean: scripts/teardown is not in ${worktree('missing').canonicalPath}",
      '  a worktree holds only what is committed',
      '  fix it, then run .ralph-tui/bin/clean missing again',
    ]*.toString()
    // --force passes the branches' unmerged commits, never the teardown.
    def plain = launch(kitchen, '.ralph-tui/bin/clean', 'plain', '--force')
    assert plain.exit == 1
    assert plain.output.readLines()[0] == 'clean: scripts/teardown is not executable'
    def failing = launch(kitchen, '.ralph-tui/bin/clean', 'failing', '--force')
    assert failing.exit == 1
    assert failing.output.readLines() == [
      'clean: scripts/teardown failed',
      "  in ${worktree('failing').canonicalPath}",
      '  nothing was removed',
      '  fix it, then run .ralph-tui/bin/clean failing again',
    ]*.toString()
    ['missing', 'plain', 'failing'].each {
      assert worktree(it).exists()
      assert git(kitchen, 'branch', '--list', "ralph/${it}")
    }
  }

  @Test void runRefusesBothTrackersAndNeedsOne() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads', 'json']]).commit()
    commitAll(kitchen)
    write(kitchen, 'prd.json', '{}\n')

    def both = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1', '--prd', 'prd.json')
    assert both.exit == 1
    assert both.output.startsWith('run: --epic and --prd are both given\n')
    def neither = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--headless')
    assert neither.exit == 2
    assert neither.output.startsWith('usage: .ralph-tui/bin/run <name> --agent <entry> (--epic <id> | --prd <file>)')
    def missing = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--prd', 'none.json')
    assert missing.exit == 1
    assert !new File(kitchen, '.worktrees').exists()
  }

  @Test void runRequiresAnAgent() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], tracker: ['beads', 'json']]).commit()
    commitAll(kitchen)

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1')

    assert run.exit == 2
    assert run.output.readLines() == [
      'usage: .ralph-tui/bin/run <name> --agent <entry> (--epic <id> | --prd <file>) [--base <ref>] ' +
      '[ralph-tui run arguments...]',
    ]
    assert !new File(kitchen, '.worktrees').exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  @Test void runRefusesAnAgentTheConfigurationHasNoEntryFor() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['codex']]).commit()
    commitAll(kitchen)

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1')

    assert run.exit == 1
    assert run.output.readLines().drop(1) == [
      'run: --agent claude matches no entry in .ralph-tui/config.toml',
      '  ralph-tui would run an installed plugin of that name with no options, and Claude without permission checks',
      '  give one of: codex',
    ]
  }

  @Test void runTakesTheAgentByEntryNameOrPluginInEitherForm() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    def config = new File(kitchen, '.ralph-tui/config.toml')
    config.text = config.text.replace('name = "claude"', 'name = "shim"')
    commitAll(kitchen)

    [['--agent', 'claude'], ['--agent=claude'], ['--agent', 'shim'], ['--agent=shim']].eachWithIndex { agent, index ->
      def run = launch(kitchen, '.ralph-tui/bin/run', "run${index}".toString(), *agent, '--epic', 'ep-1')

      assert run.exit == 0
      assert run.output.readLines()[3] ==
      "ralph-tui run --epic ep-1 --agent ${agent.last().replace('--agent=', '')}".toString()
    }
    def other = launch(kitchen, '.ralph-tui/bin/run', 'other', '--agent', 'shi', '--epic', 'ep-1')

    assert other.exit == 1
    assert other.output.readLines().last() == '  give one of: shim'
  }

  @Test void runTakesTheAgentOnlyFromAnAgentsEntryInAnyTomlForm() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    write(kitchen, '.ralph-tui/config.toml', '''\
tracker = "beads"

[trackerOptions]
name = "codex"

[[agents]]
  name = 'shim' # the team's entry
  plugin="claude"

[agents.options]
skipPermissions = false
''')
    commitAll(kitchen)

    ['shim', 'claude'].each { agent ->
      def run = launch(kitchen, '.ralph-tui/bin/run', agent, '--agent', agent, '--epic', 'ep-1')

      assert run.exit == 0
      assert run.output.readLines()[3] == "ralph-tui run --epic ep-1 --agent ${agent}".toString()
    }
    def codex = launch(kitchen, '.ralph-tui/bin/run', 'codex', '--agent', 'codex', '--epic', 'ep-1')

    assert codex.exit == 1
    assert codex.output.readLines().drop(1) == [
      'run: --agent codex matches no entry in .ralph-tui/config.toml',
      '  ralph-tui would run an installed plugin of that name with no options, and Claude without permission checks',
      '  give one of: shim',
    ]
  }

  @Test void runRefusesARepeatedEpicPrdOrBase() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)

    [
      '--agent': ['--agent', 'claude', '--agent=codex', '--epic', 'ep-1'],
      '--epic': ['--epic', 'ep-1', '--epic', 'ep-2'],
      '--epic ': ['--epic=ep-1', '--epic=ep-2'],
      '--prd': ['--prd', 'a.json', '--prd=b.json'],
      '--base': ['--base', 'HEAD', '--base', 'HEAD'],
      '--base ': ['--base=HEAD', '--epic', 'ep-1', '--base', 'HEAD'],
    ].each { flag, arguments ->
      flag = flag.trim()
      def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', *arguments)

      assert run.exit == 1
      assert run.output.readLines() == ["run: ${flag} is given twice", "  a run takes one ${flag}"]*.toString()
    }
    assert !new File(kitchen, '.worktrees').exists()
  }

  @Test void runRefusesABaseWhenTheRunBranchExists() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def branch = git(kitchen, 'rev-parse', 'ralph/demo')

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--base', 'HEAD', '--epic', 'ep-1')

    assert run.exit == 1
    assert run.output.readLines() == [
      'run: --base does not apply: ralph/demo exists',
      '  the run continues the branch where it is',
      '  run without --base, or run .ralph-tui/bin/clean demo first',
    ]*.toString()
    assert git(kitchen, 'rev-parse', 'ralph/demo') == branch
  }

  @Test void cleanRefusesUndeliveredOrUncommittedWorkUnlessForced() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worktree = new File(kitchen, '.worktrees/ralph/demo')
    write(worktree, 'work.txt', 'done\n')
    git(worktree, 'add', 'work.txt')
    git(worktree, 'commit', '-q', '-m', 'work')

    def ahead = launch(kitchen, '.ralph-tui/bin/clean', 'demo')
    assert ahead.exit == 1
    def lines = ahead.output.readLines()
    assert lines[0] == 'clean: ralph/demo has commits that neither HEAD nor origin/HEAD has'
    assert lines[1] ==~ /  [0-9a-f]+ work/
    assert lines[2].contains('a squash merge, or a commit changed while delivering it')
    assert lines[3] == '  deliver ralph/demo, or run with --force to delete them'

    git(kitchen, 'merge', '-q', '--ff-only', 'ralph/demo')
    write(worktree, 'draft.txt', 'not yet\n')
    def dirty = launch(kitchen, '.ralph-tui/bin/clean', 'demo')
    assert dirty.exit == 1
    assert dirty.output.startsWith("clean: ${worktree.canonicalPath} has uncommitted changes\n")
    assert worktree.exists()

    def forced = launch(kitchen, '.ralph-tui/bin/clean', 'demo', '--force')
    assert forced.exit == 0
    assert !worktree.exists()
    assert !new File(kitchen, '.worktrees/ralph').exists()
    assert git(kitchen, 'branch', '--list', 'ralph/*') == ''
  }

  @Test void cleanCountsACherryPickedOrRebasedBranchAsDeliveredAndPrefersOriginHead() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    def run = { String name ->
      assert launch(kitchen, '.ralph-tui/bin/run', name, '--agent', 'claude', '--epic', 'ep-1').exit == 0
      def worktree = new File(kitchen, ".worktrees/ralph/${name}")
      write(worktree, "${name}.txt", "${name}\n")
      git(worktree, 'add', '-A')
      git(worktree, 'commit', '-q', '-m', name)
      worktree
    }

    run('picked')
    write(kitchen, 'main.txt', 'moved on\n')
    commitAll(kitchen)
    git(kitchen, 'cherry-pick', 'ralph/picked')
    assert launch(kitchen, '.ralph-tui/bin/clean', 'picked').exit == 0

    run('rebased')
    write(kitchen, 'main2.txt', 'moved on again\n')
    commitAll(kitchen)
    // A forge's rebase merge: a copy rebased onto main, so main gets new commits with the same patches.
    git(kitchen, 'branch', 'copy', 'ralph/rebased')
    git(kitchen, 'rebase', '-q', 'main', 'copy')
    git(kitchen, 'checkout', '-q', 'main')
    git(kitchen, 'merge', '-q', '--ff-only', 'copy')
    assert launch(kitchen, 'git', 'merge-base', '--is-ancestor', 'ralph/rebased', 'main').exit == 1
    assert launch(kitchen, '.ralph-tui/bin/clean', 'rebased').exit == 0

    run('upstream')
    git(kitchen, 'update-ref', 'refs/remotes/origin/main', 'ralph/upstream')
    git(kitchen, 'symbolic-ref', 'refs/remotes/origin/HEAD', 'refs/remotes/origin/main')
    assert launch(kitchen, '.ralph-tui/bin/clean', 'upstream').exit == 0

    run('local')
    git(kitchen, 'merge', '-q', '--ff-only', 'ralph/local')
    assert launch(kitchen, '.ralph-tui/bin/clean', 'local').exit == 0
    assert git(kitchen, 'branch', '--list', 'ralph/*') == ''
  }

  @Test void cleanRemovesADeliveredRunAndTheParallelWorktreesRalphTuiMadeForIt() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    assert launch(kitchen, '.ralph-tui/bin/run', 'other', '--agent', 'claude', '--epic', 'ep-2').exit == 0
    def workers = new File(kitchen, '.worktrees/ralph/.ralph-worktrees/demo')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t1', new File(workers, 'worker-1').path)
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t2', new File(workers, 'worker 2').path)

    def clean = launch(kitchen, '.ralph-tui/bin/clean', 'demo')

    assert clean.exit == 0
    assert clean.output.readLines().containsAll([
      "Removed the worktree ${workers.canonicalPath}/worker-1 and the branch ralph-parallel/s1/t1.",
      "Removed the worktree ${workers.canonicalPath}/worker 2 and the branch ralph-parallel/s1/t2.",
    ]*.toString())
    assert !new File(kitchen, '.worktrees/ralph/demo').exists()
    assert !new File(kitchen, '.worktrees/ralph/.ralph-worktrees').exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == '+ ralph/other'
    def worktrees = git(kitchen, 'worktree', 'list', '--porcelain').readLines()
    assert worktrees.count { it.startsWith('worktree ') } == 2
    assert new File(kitchen, '.worktrees/ralph/other').exists()
  }

  @Test void cleanLeavesParallelWorktreesOutsideItsRunsDirectoryWithOrWithoutForce() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    def foreign = [
      'ralph-parallel/old/t1': new File(kitchen, '.worktrees/earlier-runner/t1'),
      'ralph-parallel/s2/t1': new File(kitchen, '.worktrees/ralph/.ralph-worktrees/demo2/worker-1'),
    ]
    foreign.each { branch, path ->
      git(kitchen, 'worktree', 'add', '-q', '-b', branch, path.path)
      write(path, 'unsaved.txt', 'not committed\n')
    }

    ['', '--force'].each { force ->
      assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
      def clean = launch(kitchen, *(['.ralph-tui/bin/clean', 'demo'] + (force ? [force] : [])))

      assert clean.exit == 0
      assert !clean.output.contains('ralph-parallel')
      assert !new File(kitchen, '.worktrees/ralph/demo').exists()
      foreign.each { branch, path ->
        assert new File(path, 'unsaved.txt').text == 'not committed\n'
        assert git(kitchen, 'branch', '--list', branch) == "+ ${branch}".toString()
      }
    }
  }

  @Test void cleanRefusesANameThatIsNotARunName() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def feature = new File(kitchen, '.worktrees/feature')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'feature', feature.path)
    write(feature, 'wip.txt', 'not committed\n')

    ['../feature', 'demo/'].each { name ->
      def clean = launch(kitchen, '.ralph-tui/bin/clean', name, '--force')

      assert clean.exit == 1
      assert clean.output.readLines() ==
      ["clean: ${name} is not a run name", "  ralph/${name} must be a valid branch name"]*.toString()
    }
    assert new File(feature, 'wip.txt').exists()
    assert new File(kitchen, '.worktrees/ralph/demo').exists()
  }

  @Test void cleanRefusesAParallelWorktreeOfItsRunWithChangesUnlessForced() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worker = new File(kitchen, '.worktrees/ralph/.ralph-worktrees/demo/worker-1')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t1', worker.path)
    write(worker, 'draft.txt', 'not yet\n')

    def dirty = launch(kitchen, '.ralph-tui/bin/clean', 'demo')
    assert dirty.exit == 1
    assert dirty.output.readLines() == [
      "clean: ${worker.canonicalPath} has uncommitted changes",
      '  they would be lost',
      '  commit them, or run with --force to delete them',
    ]*.toString()
    assert new File(worker, 'draft.txt').exists()
    assert new File(kitchen, '.worktrees/ralph/demo').exists()

    def forced = launch(kitchen, '.ralph-tui/bin/clean', 'demo', '--force')
    assert forced.exit == 0
    assert forced.output.readLines().contains(
    "Removed the worktree ${worker.canonicalPath} and the branch ralph-parallel/s1/t1.".toString())
    assert !worker.exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  @Test void cleanRefusesAWorkerBranchWithCommitsNeitherHeadNorOriginHeadHasUnlessForced() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worker = new File(kitchen, '.worktrees/ralph/.ralph-worktrees/demo/worker-1')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t1', worker.path, 'ralph/demo')
    write(worker, 'unmerged.txt', 'committed, never merged\n')
    git(worker, 'add', '-A')
    git(worker, 'commit', '-q', '-m', 'unmerged')

    def refused = launch(kitchen, '.ralph-tui/bin/clean', 'demo')
    assert refused.exit == 1
    def lines = refused.output.readLines()
    assert lines[0] == 'clean: ralph-parallel/s1/t1 has commits that neither HEAD nor origin/HEAD has'
    assert lines[1] ==~ /  [0-9a-f]+ unmerged/
    assert lines[3] == '  deliver ralph-parallel/s1/t1, or run with --force to delete them'
    assert worker.exists()

    git(new File(kitchen, '.worktrees/ralph/demo'), 'merge', '-q', '--no-edit', 'ralph-parallel/s1/t1')
    git(kitchen, 'merge', '-q', '--ff-only', 'ralph/demo')
    assert launch(kitchen, '.ralph-tui/bin/clean', 'demo').exit == 0
    assert !worker.exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  @Test void cleanRefusesAMergeWhoseResultDiffersFromGitsOwnUnlessDeliveredByMerge() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    // A run whose branch merges a worker's branch, as ralph-tui's parallel mode does, with or without a change of the
    // merge's own, such as a resolved conflict.
    def run = { String name, boolean resolved ->
      assert launch(kitchen, '.ralph-tui/bin/run', name, '--agent', 'claude', '--epic', 'ep-1').exit == 0
      def worktree = new File(kitchen, ".worktrees/ralph/${name}")
      git(worktree, 'checkout', '-q', '-b', "worker/${name}")
      write(worktree, "${name}-worker.txt", 'worker\n')
      git(worktree, 'add', '-A')
      git(worktree, 'commit', '-q', '-m', "${name} worker")
      git(worktree, 'checkout', '-q', "ralph/${name}")
      write(worktree, "${name}-run.txt", 'run\n')
      git(worktree, 'add', '-A')
      git(worktree, 'commit', '-q', '-m', "${name} run")
      git(worktree, 'merge', '-q', '--no-commit', "worker/${name}")

      if (resolved) {
        write(worktree, "${name}-worker.txt", 'resolved\n')
        git(worktree, 'add', '-A')
      }

      git(worktree, 'commit', '-q', '-m', "Merge ${name}")
      git(kitchen, 'branch', '-q', '-D', "worker/${name}")
    }
    // A forge's rebase merge, which drops merge commits and keeps the others' patches.
    def deliverByRebase = { String name ->
      git(kitchen, 'branch', 'copy', "ralph/${name}")
      git(kitchen, 'rebase', '-q', 'main', 'copy')
      git(kitchen, 'checkout', '-q', 'main')
      git(kitchen, 'merge', '-q', '--ff-only', 'copy')
      git(kitchen, 'branch', '-q', '-D', 'copy')
    }

    run('clean', false)
    deliverByRebase('clean')
    assert launch(kitchen, '.ralph-tui/bin/clean', 'clean').exit == 0

    run('resolved', true)
    deliverByRebase('resolved')
    def refused = launch(kitchen, '.ralph-tui/bin/clean', 'resolved')
    assert refused.exit == 1
    def lines = refused.output.readLines()
    assert lines[0] == 'clean: ralph/resolved has commits that neither HEAD nor origin/HEAD has'
    assert lines[1] ==~ /  [0-9a-f]+ Merge resolved/
    assert lines.size() == 4

    git(kitchen, 'merge', '-q', '--no-edit', '-X', 'theirs', 'ralph/resolved')
    assert launch(kitchen, '.ralph-tui/bin/clean', 'resolved').exit == 0
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  @Test void cleanListsOnlyTheCommitsThatNeitherHeadNorOriginHeadHas() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worktree = new File(kitchen, '.worktrees/ralph/demo')
    ['picked', 'pushed', 'neither'].each { subject ->
      write(worktree, "${subject}.txt", "${subject}\n")
      git(worktree, 'add', '-A')
      git(worktree, 'commit', '-q', '-m', subject)
    }
    git(kitchen, 'cherry-pick', 'ralph/demo~2')
    git(kitchen, 'branch', 'upstream', 'main~1')
    git(kitchen, 'checkout', '-q', 'upstream')
    git(kitchen, 'cherry-pick', 'ralph/demo~1')
    git(kitchen, 'checkout', '-q', 'main')
    git(kitchen, 'update-ref', 'refs/remotes/origin/main', 'upstream')
    git(kitchen, 'symbolic-ref', 'refs/remotes/origin/HEAD', 'refs/remotes/origin/main')

    def refused = launch(kitchen, '.ralph-tui/bin/clean', 'demo')

    assert refused.exit == 1
    def lines = refused.output.readLines()
    assert lines[0] == 'clean: ralph/demo has commits that neither HEAD nor origin/HEAD has'
    assert lines[1] ==~ /  [0-9a-f]+ neither/
    assert lines.size() == 4
  }

  @Test void cleanFindsTheParallelWorktreesOfARunWhoseNameHasASlash() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'team/demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worker = new File(kitchen, '.worktrees/ralph/team/.ralph-worktrees/demo/worker-1')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t1', worker.path)
    write(worker, 'draft.txt', 'not yet\n')

    def dirty = launch(kitchen, '.ralph-tui/bin/clean', 'team/demo')
    assert dirty.exit == 1
    assert dirty.output.startsWith("clean: ${worker.canonicalPath} has uncommitted changes\n")

    def forced = launch(kitchen, '.ralph-tui/bin/clean', 'team/demo', '--force')
    assert forced.exit == 0
    assert !worker.exists()
    assert !new File(kitchen, '.worktrees/ralph/team/.ralph-worktrees').exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  @Test void cleanFindsTheParallelWorktreesOfItsRunWhenStartedThroughASymbolicLink() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--agent', 'claude', '--epic', 'ep-1').exit == 0
    def worker = new File(kitchen, '.worktrees/ralph/.ralph-worktrees/demo/worker-1')
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph-parallel/s1/t1', worker.path)
    write(worker, 'draft.txt', 'not yet\n')
    def link = new File(folder.root, 'link')
    Files.createSymbolicLink(link.toPath(), kitchen.toPath())

    def dirty = launch(kitchen, "${link.path}/.ralph-tui/bin/clean".toString(), 'demo')
    assert dirty.exit == 1
    assert dirty.output.startsWith("clean: ${worker.canonicalPath} has uncommitted changes\n")

    def forced = launch(kitchen, "${link.path}/.ralph-tui/bin/clean".toString(), 'demo', '--force')
    assert forced.exit == 0
    assert !worker.exists()
    assert git(kitchen, 'branch', '--list', 'ralph*') == ''
  }

  // Re-cooks with arguments the seeded configuration cannot take and checks that the refusal wrote nothing.
  private Throwable refusedRecook(File kitchen, Map arguments) {
    def before = files(kitchen).collectEntries { [(it): new File(kitchen, it).bytes.toList()] }
    def error = shouldFail(IllegalArgumentException) { cook(kitchen, RALPH, arguments) }

    assert files(kitchen).collectEntries { [(it): new File(kitchen, it).bytes.toList()] } == before
    error
  }

  private File kitchen() {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'ralph')
    kitchen
  }

  // A kitchen that is a Git repository with one commit, whose published cookbook Git does not see.
  private File repository() {
    def kitchen = kitchen()
    git(kitchen, 'init', '-q', '-b', 'main')
    new File(kitchen, '.git/info/exclude') << '/repository/\n'
    write(kitchen, 'README.md', '# Kitchen\n')
    commitAll(kitchen)
    kitchen
  }

  private void commitAll(File kitchen) {
    git(kitchen, 'add', '-A')
    git(kitchen, 'commit', '-q', '-m', 'cook')
  }

  private String git(File directory, String... arguments) {
    def result = launch(directory, *(['git'] + arguments.toList()))
    assert result.exit == 0: result.output
    result.output.trim()
  }

  // Runs a command with a fake ralph-tui first on the PATH, which prints its arguments, directory and effort, and with
  // a HOME of its own, so the person's Git configuration and hooks stay out of the test.
  private Map launch(File directory, String... command) {
    launch([:], directory, command)
  }

  // As launch, with BEADS_DIR printed after the effort when set, and the variables in `environment` added.
  private Map launch(Map<String, String> environment, File directory, String... command) {
    def bin = new File(folder.root, 'bin')

    if (!bin.exists()) {
      write(bin, 'ralph-tui', '#!/bin/sh\necho "ralph-tui $*"\necho "cwd $(pwd -P)"\n' +
          'echo "effort ${CLAUDE_CODE_EFFORT_LEVEL:-}"\n[ -z "${BEADS_DIR:-}" ] || echo "beads $BEADS_DIR"\n')
      new File(bin, 'ralph-tui').setExecutable(true)
      new File(folder.root, 'home').mkdirs()
    }

    def builder = new ProcessBuilder(command).directory(directory).redirectErrorStream(true)
    builder.environment().remove('BEADS_DIR')
    builder.environment().putAll(environment)
    builder.environment().putAll([
      PATH: "${bin.path}:${System.getenv('PATH')}".toString(),
      HOME: new File(folder.root, 'home').path,
      GIT_CONFIG_NOSYSTEM: '1',
      GIT_AUTHOR_NAME: 'Cook',
      GIT_AUTHOR_EMAIL: 'cook@example.com',
      GIT_COMMITTER_NAME: 'Cook',
      GIT_COMMITTER_EMAIL: 'cook@example.com',
      CLAUDE_CODE_EFFORT_LEVEL: 'high',
    ])
    def process = builder.start()
    def output = process.inputStream.text
    [exit: process.waitFor(), output: output]
  }

  // A repository cooked with --worker-check, whose check prints words ralph-tui takes for a rate limit, with the run
  // worktree .worktrees/ralph/demo and fake claude and codex agents on the PATH launch gives.
  private File workerRepository() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude', 'codex'], 'worker-check': ['scripts/check']]).commit()
    executable(kitchen, 'scripts/check', '#!/bin/sh\necho "check $1"\necho "rate limit exceeded, 429, overloaded"\n' +
        'if [ -n "${CHECK_SLEEPS:-}" ]; then sleep 60 & echo $! > "$CHECK_SLEEPS"; wait; fi\n' +
        '[ -z "${CHECK_FAILS:-}" ]\n')
    commitAll(kitchen)
    git(kitchen, 'worktree', 'add', '-q', '-b', 'ralph/demo', '.worktrees/ralph/demo')
    ['claude': 'claude 2.1.306', 'codex': 'codex-cli 0.160.1'].each { agent, version ->
      executable(folder.root, "bin/${agent}", '#!/bin/sh\n' +
          "if [ \"\$*\" = --version ]; then echo '${version}'; exit; fi\n" +
          'echo "args $*"\necho "prompt $(cat)"\n[ -z "${AGENT_FAILS:-}" ] || exit 3\n' +
          'if [ -n "${AGENT_SLEEPS:-}" ]; then sleep 60 & echo $! > "$AGENT_SLEEPS"; wait; fi\n' +
          'if [ -n "${AGENT_BLOCKED:-}" ]; then echo "BLOCKED: no tests"\n' +
          'else printf \'%s\\n\' "${PROMISE:-<promise>COMPLETE</promise>}"; fi\n')
    }
    kitchen
  }

  private static void eventually(Closure<Boolean> condition) {
    def deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)

    while (!condition()) {
      assert System.nanoTime() < deadline: 'the condition did not hold within 10 seconds'
      Thread.sleep(50)
    }
  }

  private static void executable(File kitchen, String path, String text) {
    write(kitchen, path, text)
    new File(kitchen, path).setExecutable(true)
  }

  private static void write(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
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

  // The kitchen's files, leaving out the published repository and Git's own directory.
  private static List<String> files(File kitchen) {
    Kitchens.files(kitchen).findAll { !it.startsWith('repository/') && !it.startsWith('.git/') }
  }
}
