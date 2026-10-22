package thecook

import static thecook.Kitchens.COOKBOOK
import static thecook.Kitchens.cook
import static thecook.Kitchens.publishDirectory

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookLock
import thecook.cookbook.FileTarget
import thecook.cookbook.KeyTarget

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class AgentsWorktreeCookTest {
  private static final String CONNECT = 'io.github.lmarqs.cookbook:agents:0.1.0#connect'
  private static final String HOOK = '.agents/hooks/worktree'
  private static final String SETTINGS = '.agents/adapters/claude/settings.json'
  private static final Map<String, List<String>> NAMED = [
    harness: ['claude'],
    'worktree-root': ['.worktrees'],
    'worktree-setup': ['.worktrees/setup'],
    'worktree-teardown': ['.worktrees/teardown'],
  ]

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void ownsTheHookAndBothWorktreeKeysAndRemovesThemWhenCookedWithoutARoot() {
    def kitchen = kitchen()
    cook(kitchen, CONNECT, NAMED).commit()

    def hook = new File(kitchen, HOOK)
    assert hook.text == new File(COOKBOOK, 'agents/hooks/worktree').text
    .replace("\nroot=''\n", "\nroot='.worktrees'\n")
    .replace("\nsetup=''\n", "\nsetup='.worktrees/setup'\n")
    .replace("\nteardown=''\n", "\nteardown='.worktrees/teardown'\n")
    assert Files.getPosixFilePermissions(hook.toPath()) == PosixFilePermissions.fromString('rwxr-xr-x')
    def settings = new File(kitchen, SETTINGS)
    def command = { String action -> [type: 'command', command: "\"\$CLAUDE_PROJECT_DIR\"/${HOOK} ${action}"] }
    def hooks = new JsonSlurper().parse(settings).hooks
    assert hooks.WorktreeCreate == [[hooks: [command('create')]]]
    assert hooks.WorktreeRemove == [[hooks: [command('remove')]]]
    def owned = CookLock.parse(new File(kitchen, 'cook.lock').text).owned('io.github.lmarqs.cookbook:agents#connect')
    assert new FileTarget(HOOK) in owned
    ['WorktreeCreate', 'WorktreeRemove'].each { assert new KeyTarget(SETTINGS, ['hooks', it]) in owned }
    assert new File(kitchen, '.gitignore').readLines()[1] == '# io.github.lmarqs.cookbook:agents#connect ' +
    '--harness=claude --worktree-root=.worktrees --worktree-setup=.worktrees/setup ' +
    '--worktree-teardown=.worktrees/teardown'

    def json = new JsonSlurper().parse(settings)
    def preToolUse = [[matcher: 'Bash', hooks: [[type: 'command', command: '.agents/hooks/guard.sh']]]]
    json.hooks.PreToolUse = preToolUse
    settings.text = JsonOutput.prettyPrint(JsonOutput.toJson(json)) + '\n'
    assert cook(kitchen, CONNECT, NAMED).changes == []

    cook(kitchen, CONNECT, [harness: ['claude']]).commit()
    assert !hook.exists()
    assert new JsonSlurper().parse(settings).hooks.keySet() == ['SessionStart', 'PreToolUse'] as Set
  }

  @Test void refusesASetupOrTeardownWithoutARootAndARootWithoutClaude() {
    def kitchen = kitchen()

    ['setup', 'teardown'].each { name ->
      def error = shouldFail { cook(kitchen, CONNECT, [harness: ['claude'], ("worktree-${name}".toString()): ['x']]) }
      assert error.message.readLines() == [
        "${CONNECT}: --worktree-${name}=x needs --worktree-root",
        '  the worktree hook runs it, and the hook is written only for a worktree root',
        '  name the root too, as --worktree-root=.worktrees',
      ]*.toString()
    }
    def error = shouldFail { cook(kitchen, CONNECT, [harness: ['codex'], 'worktree-root': ['.worktrees']]) }
    assert error.message.readLines() == [
      "${CONNECT}: --worktree-root=.worktrees needs a harness that runs worktree hooks",
      '  Claude Code runs them, and Codex 0.160.1 has no worktree hook event',
      '  add --harness=claude',
    ]*.toString()
    assert !new File(kitchen, '.gitignore').exists()
  }

  @Test void createAddsTheWorktreeOnItsBranchUnderTheMainCheckoutAndPrintsItsPathLast() {
    def kitchen = repository([harness: ['claude'], 'worktree-root': ['.worktrees']])
    def worktree = new File(kitchen, '.worktrees/feature/a').canonicalPath

    def created = hook(kitchen, 'create', [hook_event_name: 'WorktreeCreate', cwd: kitchen.path, name: 'feature/a'])

    assert created.exit == 0
    assert created.output == "${worktree}\n"
    assert git(new File(worktree), 'symbolic-ref', '--short', 'HEAD') == 'feature/a'

    def nested = hook(new File(worktree), 'create', [name: 'b'])
    assert nested.exit == 0
    assert nested.output == "${new File(kitchen, '.worktrees/b').canonicalPath}\n"

    git(kitchen, 'branch', 'kept')
    def existing = hook(kitchen, 'create', [name: 'kept'])
    assert existing.exit == 0
    assert git(new File(kitchen, '.worktrees/kept'), 'rev-parse', 'HEAD') == git(kitchen, 'rev-parse', 'kept')

    def refused = hook(kitchen, 'create', [name: '../out'])
    assert refused.exit == 1
    assert refused.output == ''
    assert refused.error.startsWith('worktree: ../out is not a branch name\n')
  }

  @Test void createRunsTheSetupFromTheWorktreesRootAndRemovesWhatItMadeWhenTheSetupFails() {
    def kitchen = repository(NAMED) {
      executable(it, '.worktrees/setup', '#!/bin/sh\necho set up\npwd -P > setup-ran\n')
    }

    def created = hook(kitchen, 'create', [name: 'demo'])
    def worktree = new File(kitchen, '.worktrees/demo')
    assert created.exit == 0
    assert created.output == "${worktree.canonicalPath}\n"
    assert created.error.contains('set up\n')
    assert new File(worktree, 'setup-ran').text == "${worktree.canonicalPath}\n"

    executable(kitchen, '.worktrees/setup', '#!/bin/sh\nexit 3\n')
    commitAll(kitchen)
    git(kitchen, 'branch', 'kept')
    ['fresh', 'kept'].each { name ->
      def failed = hook(kitchen, 'create', [name: name])
      def path = new File(kitchen, ".worktrees/${name}")
      assert failed.exit == 1
      assert failed.output == ''
      assert failed.error.contains("worktree: .worktrees/setup failed\n  in ${path.canonicalPath}\n")
      assert failed.error.endsWith("worktree: removed the worktree ${path.canonicalPath}, whose setup failed\n")
      assert !path.exists()
    }
    assert git(kitchen, 'branch', '--list', 'fresh') == ''
    assert git(kitchen, 'branch', '--list', 'kept') == 'kept'
  }

  @Test void removeRunsTheTeardownKeepsTheWorktreeWhenItFailsAndDeletesOnlyAMergedBranch() {
    def kitchen = repository(NAMED) {
      executable(it, '.worktrees/setup', '#!/bin/sh\n')
      executable(it, '.worktrees/teardown', '#!/bin/sh\n[ ! -f fail ] || exit 1\npwd -P >> ../../teardown-ran\n')
    }
    ['merged', 'unmerged'].each { assert hook(kitchen, 'create', [name: it]).exit == 0 }
    def merged = new File(kitchen, '.worktrees/merged')
    def unmerged = new File(kitchen, '.worktrees/unmerged')
    new File(unmerged, 'work.txt').text = 'work\n'
    git(unmerged, 'add', 'work.txt')
    git(unmerged, 'commit', '-q', '-m', 'work')

    new File(merged, 'fail').text = ''
    def failed = hook(kitchen, 'remove', [hook_event_name: 'WorktreeRemove', worktree_path: merged.canonicalPath])
    assert failed.exit == 1
    assert failed.error == "worktree: .worktrees/teardown failed\n  in ${merged.canonicalPath}\n"
    assert merged.exists()

    new File(merged, 'fail').delete()
    [merged, unmerged].each { assert hook(kitchen, 'remove', [worktree_path: it.canonicalPath]).exit == 0 }
    assert !merged.exists()
    assert !unmerged.exists()
    assert new File(kitchen, 'teardown-ran').readLines() == [merged.canonicalPath, unmerged.canonicalPath]
    assert git(kitchen, 'branch', '--list', 'merged') == ''
    assert git(kitchen, 'branch', '--list', 'unmerged') == 'unmerged'

    def main = hook(kitchen, 'remove', [worktree_path: kitchen.canonicalPath])
    assert main.exit == 1
    assert main.error.startsWith("worktree: ${kitchen.canonicalPath} is the main checkout\n")
  }

  private File kitchen() {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'agents')
    kitchen
  }

  // A Git repository with the recipe cooked as `arguments` and committed with what `prepare` writes, whose published
  // cookbook Git does not see.
  private File repository(Map<String, List<String>> arguments, Closure prepare = {}) {
    def kitchen = kitchen()
    git(kitchen, 'init', '-q', '-b', 'main')
    new File(kitchen, '.git/info/exclude') << '/repository/\n/.worktrees/*\n!/.worktrees/setup\n!/.worktrees/teardown\n'
    cook(kitchen, CONNECT, arguments).commit()
    prepare(kitchen)
    commitAll(kitchen)
    kitchen
  }

  private void commitAll(File kitchen) {
    git(kitchen, 'add', '-A')
    git(kitchen, 'commit', '-q', '-m', 'cook')
  }

  private String git(File directory, String... arguments) {
    def result = launch(directory, null, *(['git'] + arguments.toList()))
    assert result.exit == 0: result.error
    result.output.trim()
  }

  private Map hook(File directory, String action, Map input) {
    def kitchen = new File(git(directory, 'rev-parse', '--show-toplevel'))
    launch(directory, JsonOutput.toJson(input), new File(kitchen, HOOK).path, action)
  }

  // Runs a command with `input` on standard input and a HOME of its own, so the person's Git configuration stays out
  // of the test.
  private Map launch(File directory, String input, String... command) {
    def home = new File(folder.root, 'home')
    home.mkdirs()
    def builder = new ProcessBuilder(command).directory(directory)
    builder.environment().putAll([
      HOME: home.path,
      GIT_CONFIG_NOSYSTEM: '1',
      GIT_AUTHOR_NAME: 'Cook',
      GIT_AUTHOR_EMAIL: 'cook@example.com',
      GIT_COMMITTER_NAME: 'Cook',
      GIT_COMMITTER_EMAIL: 'cook@example.com',
    ])
    def process = builder.start()
    process.outputStream.withWriter { it << (input ?: '') }
    def error = new StringBuilder()
    def reader = Thread.start { error << process.errorStream.text }
    def output = process.inputStream.text
    def exit = process.waitFor()
    reader.join()
    [exit: exit, output: output, error: error.toString()]
  }

  private static void executable(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
    file.setExecutable(true)
  }

  private static Throwable shouldFail(Closure action) {
    try {
      action()
    } catch (IllegalArgumentException error) {
      return error
    }

    throw new AssertionError('Expected IllegalArgumentException')
  }
}
