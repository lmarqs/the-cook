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

class RalphTuiCookTest {
  private static final String RALPH = 'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui'
  private static final String WORKTREES = 'io.github.lmarqs.cookbook:git:0.1.0#worktrees'
  private static final File SOURCES = new File(COOKBOOK, 'ralph')
  private static final String CONFIG = '''\
configVersion = "2.1"
tracker = "beads"
defaultAgent = "claude"
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
defaultFlags = ["-c", "approval_policy=never", "-c", "sandbox_workspace_write.network_access=false"]

[agents.options]
fullAuto = false
sandbox = "workspace-write"
'''
  private static final String CLAUDE_FLAGS =
  'defaultFlags = ["--permission-prompts", "none", "--permission-mode", "acceptEdits"]'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void seedsTheConfigurationAndPromptsAndOwnsTheLauncherOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['claude', 'codex']]).commit()

    assert files(kitchen) == [
      '.gitignore',
      '.ralph-tui/bin/clean',
      '.ralph-tui/bin/run',
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
# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex
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
"""
    // No actions is what `./cook` reports as `Already cooked.`.
    assert cook(kitchen, RALPH, [harness: ['claude', 'codex']]).actions == [:]
  }

  @Test void theFirstHarnessIsTheDefaultAgentAndEachGetsOneEntry() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['codex', 'codex']]).commit()

    def config = new File(kitchen, '.ralph-tui/config.toml').text
    assert config.contains('defaultAgent = "codex"\n')
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
    def prompt = new File(kitchen, '.ralph-tui/templates/beads.hbs')
    prompt.text = 'Our prompt for {{taskId}}\n'

    cook(kitchen, RALPH, [harness: ['claude', 'codex']]).commit()
    assert config.text.contains('maxIterations = 3')
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex'
    assert !config.text.contains('codex')
    assert prompt.text == 'Our prompt for {{taskId}}\n'
  }

  @Test void aRoleAddsItsRenderedSettingsToTheClaudeEntryOnly() {
    def kitchen = kitchen()
    write(kitchen, '.agents/adapters/claude/roles/worker.json', '{"permissions":{"deny":["Bash(git push *)"]}}\n')

    cook(kitchen, RALPH, [harness: ['claude', 'codex'], 'worker-role': ['worker']]).commit()

    def config = new File(kitchen, '.ralph-tui/config.toml').text
    assert config == CONFIG.replace(CLAUDE_FLAGS, CLAUDE_FLAGS.replace(']',
    ', "--settings", ".agents/adapters/claude/roles/worker.json"]'))
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worker-role=worker'
    assert new File(kitchen, '.agents/adapters/claude/roles/worker.json').text ==
    '{"permissions":{"deny":["Bash(git push *)"]}}\n'
  }

  @Test void refusesARoleWhoseRenderedSettingsAreNotInTheDraft() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-role': ['worker']])
    }

    assert error.message.readLines() == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role=worker needs ' +
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
      (RALPH): [harness: ['claude'], 'worker-role': ['worker']],
    ]).commit()

    assert new File(kitchen, '.agents/adapters/claude/roles/worker.json').text.contains('Bash(git push *)')
    assert new File(kitchen, '.ralph-tui/config.toml').text.contains(
    '"--settings", ".agents/adapters/claude/roles/worker.json"]')
  }

  @Test void aSettingsFileIsPassedAsGivenAndNeverWritten() {
    def kitchen = kitchen()

    cook(kitchen, RALPH, [harness: ['claude'], 'worker-settings': ['x.json']]).commit()

    assert new File(kitchen, '.ralph-tui/config.toml').text.contains(
    CLAUDE_FLAGS.replace(']', ', "--settings", "x.json"]\n'))
    assert !new File(kitchen, 'x.json').exists()
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worker-settings=x.json'
  }

  @Test void refusesARoleAndASettingsFileTogether() {
    def kitchen = kitchen()
    write(kitchen, '.agents/adapters/claude/roles/worker.json', '{}\n')

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['claude'], 'worker-role': ['worker'], 'worker-settings': ['x.json']])
    }

    assert error.message.readLines()[0] ==
    'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-role and --worker-settings are both given'
  }

  @Test void refusesARoleOrSettingsFileWithoutAClaudeEntry() {
    def kitchen = kitchen()

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, RALPH, [harness: ['codex'], 'worker-settings': ['x.json']])
    }

    assert error.message.readLines()[0..1] == [
      'io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui: --worker-settings=x.json applies to the claude entry',
      '  no --harness=claude is given',
    ]
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
    cook(kitchen, RALPH, [harness: ['claude']]).commit()

    ['run', 'clean'].each {
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
      (RALPH): [harness: ['claude', 'codex']],
    ]).commit()

    assert files(kitchen).findAll { it.startsWith('.ralph-tui/') || it.startsWith('.beads/') } == [
      '.beads/PRIME.md',
      '.ralph-tui/bin/clean',
      '.ralph-tui/bin/run',
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
    def arguments = [harness: ['claude'], instructions: ['AGENTS.md']]

    cook(kitchen, RALPH, arguments).commit()

    assert agents.text == '''\
# Agents
<!-- cook:begin io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui -->
<!-- io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --instructions=AGENTS.md -->
- Loop runs: `.ralph-tui/bin/run <name> --epic <id>`, or `--prd <file>`, runs ralph-tui in
  `.worktrees/ralph/<name>` on branch `ralph/<name>`; `.ralph-tui/bin/clean <name>` removes both once the
  branch is delivered.
<!-- cook:end io.github.lmarqs.cookbook:ralph#ralph-tui/ralph-tui -->
'''
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --instructions=AGENTS.md'
    assert cook(kitchen, RALPH, arguments).actions == [:]
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(SOURCES, 'ralph-tui.groovy').text, 'ralph-tui.groovy')

    assert texts.params*.names ==
    [['harness'], ['worker-role'], ['worker-settings'], ['worktree-setup'], ['worktree-teardown'], ['instructions']]
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

  @Test void theModelDocumentsTheEntriesToAddAndTheKnownLimits() {
    def text = new File(SOURCES, 'RalphTui.groovy').text
    [
      'skipPermissions = true',
      '"--settings", ".agents/adapters/claude/roles/worker.json"',
      'ralph-tui run --model',
      'CLAUDE_CODE_EFFORT_LEVEL',
      'model_reasoning_effort',
      '`sonnet`, `opus` and `haiku`',
      'fewer\n * than three tasks',
      '--dangerously-bypass-hook-trust',
      'neither read nor write the Beads\n * database',
      '`permissions.allow`',
      'prefix rules',
    ].each { assert text.contains(it) }
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

    def refused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1')
    assert refused.exit == 1
    assert refused.output.startsWith('run: .ralph-tui has uncommitted changes\n')
    assert !new File(kitchen, '.worktrees').exists()

    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo').canonicalPath
    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1', '--headless', '--model', 'opus')
    assert run.exit == 0
    assert run.output.readLines() == [
      "Created the worktree ${kitchen.canonicalPath}/.worktrees/ralph/demo on ralph/demo.",
      "worktree: ${kitchen.canonicalPath}/.worktrees/ralph/demo",
      'branch: ralph/demo',
      'ralph-tui run --epic ep-1 --headless --model opus',
      "cwd ${worktree}",
      'effort high',
    ]*.toString()
    assert git(kitchen, 'rev-parse', 'ralph/demo') == git(kitchen, 'rev-parse', 'HEAD')

    def reused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic=ep-1')
    assert reused.exit == 0
    assert reused.output.readLines()[0] == "Reusing the worktree ${worktree} on ralph/demo."
    assert reused.output.readLines()[3] == 'ralph-tui run --epic ep-1'
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
    def run = launch(tasks, '../.ralph-tui/bin/run', 'story', '--base', 'base', '--prd', 'prd.json')

    assert run.exit == 0
    assert run.output.readLines()[3] == "ralph-tui run --prd ${kitchen.canonicalPath}/tasks/prd.json".toString()
    assert git(kitchen, 'rev-parse', 'ralph/story') == git(kitchen, 'rev-parse', 'base')
  }

  @Test void runRunsNoSetupWithoutSetupEvenWhenWorktreesSetupIsExecutable() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    executable(kitchen, '.worktrees/setup', '#!/bin/sh\ntouch setup-ran\n')
    commitAll(kitchen)

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines()[3] == 'ralph-tui run --epic ep-1'
    assert !new File(kitchen, '.worktrees/ralph/demo/setup-ran').exists()
    assert !new File(kitchen, 'setup-ran').exists()
  }

  @Test void runRunsTheSetupFileOnceFromTheRootOfAWorktreeItCreatesAndNotOnReuse() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': ['.worktrees/setup']]).commit()
    executable(kitchen, '.worktrees/setup', '#!/bin/sh\npwd -P >> setup-ran\n')
    commitAll(kitchen)
    def worktree = new File(kitchen, '.worktrees/ralph/demo')

    def run = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1')

    assert run.exit == 0
    assert run.output.readLines()[3] == 'ralph-tui run --epic ep-1'
    assert new File(worktree, 'setup-ran').readLines() == [worktree.canonicalPath]
    assert !new File(kitchen, 'setup-ran').exists()

    def reused = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1')
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
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
    assert new File(kitchen, '.worktrees/ralph/demo/setup-ran').exists()
  }

  @Test void runFailsWhenTheSetupFileIsMissingNotExecutableOrFails() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-setup': ['scripts/setup']]).commit()
    commitAll(kitchen)

    def missing = launch(kitchen, '.ralph-tui/bin/run', 'missing', '--epic', 'ep-1')
    assert missing.exit == 1
    def worktree = new File(kitchen, '.worktrees/ralph/missing').canonicalPath
    assert missing.output.readLines()[1..-1] == [
      "run: scripts/setup is not in ${worktree}",
      '  a worktree holds only what is committed',
      '  fix it, then run .ralph-tui/bin/clean missing and run again',
    ]*.toString()

    write(kitchen, 'scripts/setup', '#!/bin/sh\nexit 0\n')
    commitAll(kitchen)
    def plain = launch(kitchen, '.ralph-tui/bin/run', 'plain', '--epic', 'ep-1')
    assert plain.exit == 1
    assert plain.output.readLines()[1] == 'run: scripts/setup is not executable'

    executable(kitchen, 'scripts/setup', '#!/bin/sh\nexit 3\n')
    commitAll(kitchen)
    def failing = launch(kitchen, '.ralph-tui/bin/run', 'failing', '--epic', 'ep-1')
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

  @Test void cleanRunsTheTeardownFromTheRunWorktreesRootBeforeRemovingIt() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude'], 'worktree-teardown': ['.worktrees/teardown']]).commit()
    executable(kitchen, '.worktrees/teardown', '#!/bin/sh\npwd -P >> ../../../teardown-ran\n')
    commitAll(kitchen)
    assert new File(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --worktree-teardown=.worktrees/teardown'
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
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
      assert launch(kitchen, '.ralph-tui/bin/run', it, '--epic', 'ep-1').exit == 0
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
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    write(kitchen, 'prd.json', '{}\n')

    def both = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1', '--prd', 'prd.json')
    assert both.exit == 1
    assert both.output.startsWith('run: --epic and --prd are both given\n')
    def neither = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--headless')
    assert neither.exit == 2
    assert neither.output.startsWith('usage: .ralph-tui/bin/run <name>')
    def missing = launch(kitchen, '.ralph-tui/bin/run', 'demo', '--prd', 'none.json')
    assert missing.exit == 1
    assert !new File(kitchen, '.worktrees').exists()
  }

  @Test void cleanRefusesUndeliveredOrUncommittedWorkUnlessForced() {
    def kitchen = repository()
    cook(kitchen, RALPH, [harness: ['claude']]).commit()
    commitAll(kitchen)
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
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
      assert launch(kitchen, '.ralph-tui/bin/run', name, '--epic', 'ep-1').exit == 0
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
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
    assert launch(kitchen, '.ralph-tui/bin/run', 'other', '--epic', 'ep-2').exit == 0
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
      assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
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
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
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
    assert launch(kitchen, '.ralph-tui/bin/run', 'demo', '--epic', 'ep-1').exit == 0
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
    def bin = new File(folder.root, 'bin')

    if (!bin.exists()) {
      write(bin, 'ralph-tui', '#!/bin/sh\necho "ralph-tui $*"\necho "cwd $(pwd -P)"\n' +
          'echo "effort ${CLAUDE_CODE_EFFORT_LEVEL:-}"\n')
      new File(bin, 'ralph-tui').setExecutable(true)
      new File(folder.root, 'home').mkdirs()
    }

    def builder = new ProcessBuilder(command).directory(directory).redirectErrorStream(true)
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
