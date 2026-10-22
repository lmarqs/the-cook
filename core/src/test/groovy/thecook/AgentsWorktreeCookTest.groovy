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

  @Test void ownsTheHookAndBothWorktreeKeysAndRemovesThemWhenCookedWithoutAWorktreeParam() {
    def kitchen = kitchen()
    cook(kitchen, CONNECT, NAMED).commit()

    def hook = new File(kitchen, HOOK)
    assert hook.text == rendered(root: '.worktrees', setup: '.worktrees/setup', teardown: '.worktrees/teardown',
    base: 'fresh', fetch: 'stale', remote: 'origin')
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

  @Test void rendersEveryWorktreeParamAndRootsTheWorktreesAtDotWorktreesWhenAnyIsGiven() {
    def kitchen = kitchen()
    cook(kitchen, CONNECT, [harness: ['claude'], 'worktree-base': ['head'], 'worktree-fetch': ['never'],
      'worktree-remote': ['upstream'], 'worktree-branch-prefix': ['worktree-']]).commit()

    assert new File(kitchen, HOOK).text == rendered(root: '.worktrees', base: 'head', fetch: 'never',
    remote: 'upstream', branch_prefix: 'worktree-')

    cook(kitchen, CONNECT, [harness: ['claude'], 'worktree-setup': ['.worktrees/setup']]).commit()
    assert new File(kitchen, HOOK).text == rendered(root: '.worktrees', setup: '.worktrees/setup', base: 'fresh',
    fetch: 'stale', remote: 'origin')
  }

  @Test void refusesAWorktreeParamWithoutClaudeAndAnUnknownBaseOrFetch() {
    def kitchen = kitchen()

    ['root', 'setup', 'remote'].each { name ->
      def error = shouldFail { cook(kitchen, CONNECT, [harness: ['codex'], ("worktree-${name}".toString()): ['x']]) }
      assert error.message.readLines() == [
        "${CONNECT}: --worktree-${name}=x needs a harness that runs worktree hooks",
        '  Claude Code runs them, and Codex 0.160.1 has no worktree hook event',
        '  add --harness=claude',
      ]*.toString()
    }
    ['base', 'fetch'].each { name ->
      def error = shouldFail { cook(kitchen, CONNECT, [harness: ['claude'], ("worktree-${name}".toString()): ['x']]) }
      assert error.message.contains("--worktree-${name}=x is not one of: ")
    }
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

  @Test void createReusesItsWorktreeWithoutSettingItUpAgainAndRecreatesOneWhoseDirectoryIsGone() {
    def kitchen = repository(NAMED) {
      executable(it, '.worktrees/setup', '#!/bin/sh\npwd -P >> ../../setup-ran\n')
    }
    def worktree = new File(kitchen, '.worktrees/demo')

    assert hook(kitchen, 'create', [name: 'demo']).exit == 0
    def reused = hook(kitchen, 'create', [name: 'demo'])
    assert reused.exit == 0
    assert reused.output == "${worktree.canonicalPath}\n"
    assert reused.error == "worktree: reusing ${worktree.canonicalPath}\n"
    assert new File(kitchen, 'setup-ran').readLines() == [worktree.canonicalPath]

    worktree.deleteDir()
    def recreated = hook(kitchen, 'create', [name: 'demo'])
    assert recreated.exit == 0
    assert recreated.output == "${worktree.canonicalPath}\n"
    assert git(worktree, 'symbolic-ref', '--short', 'HEAD') == 'demo'
  }

  @Test void createStartsANewBranchAtTheRemotesDefaultBranchFetchedAsTheFetchParamSaysWithoutTrackingIt() {
    def stale = repository([harness: ['claude'], 'worktree-root': ['.worktrees']], 'stale')
    def origin = remote(stale, 'origin')
    def local = git(stale, 'rev-parse', 'HEAD')
    def first = push(origin, 'first')

    assert hook(stale, 'create', [name: 'fetched']).exit == 0
    assert git(stale, 'rev-parse', 'fetched') == first
    assert git(stale, 'rev-parse', 'main') == local
    assert launch(stale, null, 'git', 'config', '--get', 'branch.fetched.merge').exit == 1

    push(origin, 'second')
    assert hook(stale, 'create', [name: 'cached']).exit == 0
    assert git(stale, 'rev-parse', 'cached') == first
    def fetchedAt = new File(stale, '.git/FETCH_HEAD')
    fetchedAt.lastModified = System.currentTimeMillis() - 25 * 60 * 60 * 1000
    def third = push(origin, 'third')
    assert hook(stale, 'create', [name: 'refetched']).exit == 0
    assert git(stale, 'rev-parse', 'refetched') == third

    def always = repository([harness: ['claude'], 'worktree-fetch': ['always'], 'worktree-remote': ['upstream']],
    'always')
    def upstream = remote(always, 'upstream')
    assert hook(always, 'create', [name: 'one']).exit == 0
    def latest = push(upstream, 'latest')
    assert hook(always, 'create', [name: 'two']).exit == 0
    assert git(always, 'rev-parse', 'two') == latest

    def never = repository([harness: ['claude'], 'worktree-fetch': ['never']], 'never')
    def unfetched = remote(never, 'origin')
    def cached = git(never, 'rev-parse', 'origin/main')
    push(unfetched, 'unseen')
    git(never, 'remote', 'set-head', 'origin', 'main')
    assert hook(never, 'create', [name: 'offline']).exit == 0
    assert git(never, 'rev-parse', 'offline') == cached

    def alone = repository([harness: ['claude'], 'worktree-root': ['.worktrees']], 'alone')
    def created = hook(alone, 'create', [name: 'local'])
    assert created.exit == 0
    assert created.error.contains("worktree: no default branch of origin is known, so local starts at HEAD\n")
    assert git(alone, 'rev-parse', 'local') == git(alone, 'rev-parse', 'HEAD')
  }

  @Test void createWithBaseHeadStartsAtTheSessionsCheckoutOnTheBranchItsPrefixNames() {
    def kitchen = repository([harness: ['claude'], 'worktree-base': ['head'], 'worktree-branch-prefix': ['worktree-']])
    def first = new File(kitchen, '.worktrees/worktree-first')

    def created = hook(kitchen, 'create', [cwd: kitchen.path, name: 'first'])
    assert created.exit == 0
    assert created.output == "${first.canonicalPath}\n"
    git(first, 'commit', '-q', '--allow-empty', '-m', 'work in progress')

    assert hook(kitchen, 'create', [cwd: first.path, name: 'second']).exit == 0
    assert git(kitchen, 'rev-parse', 'worktree-second') == git(first, 'rev-parse', 'HEAD')
    assert hook(kitchen, 'create', [name: 'main']).exit == 0
    assert git(new File(kitchen, '.worktrees/worktree-main'), 'symbolic-ref', '--short', 'HEAD') == 'worktree-main'
  }

  @Test void removeDeletesOnlyTheWorktreesOwnBranchOnceItsPatchesAreOnHeadOrTheRemotesDefaultBranch() {
    def kitchen = repository([harness: ['claude'], 'worktree-root': ['.worktrees']])
    def origin = remote(kitchen, 'origin')
    ['landed', 'switched', 'feature/nested'].each { assert hook(kitchen, 'create', [name: it]).exit == 0 }
    def landed = new File(kitchen, '.worktrees/landed')
    new File(landed, 'landed.txt').text = 'landed\n'
    git(landed, 'add', 'landed.txt')
    git(landed, 'commit', '-q', '-m', 'landed')
    push(origin, 'teammate')
    git(landed, 'fetch', '-q', 'origin')
    git(landed, 'rebase', '-q', 'origin/main')
    git(landed, 'push', '-q', 'origin', 'HEAD:main')
    def switched = new File(kitchen, '.worktrees/switched')
    git(kitchen, 'branch', 'develop')
    git(switched, 'checkout', '-q', 'develop')

    ['landed', 'switched', 'feature/nested'].each { name ->
      assert hook(kitchen, 'remove', [worktree_path: new File(kitchen, ".worktrees/${name}").canonicalPath]).exit == 0
    }

    assert git(kitchen, 'branch', '--list', 'landed', 'switched', 'feature/nested') == ''
    assert git(kitchen, 'branch', '--list', 'develop') == 'develop'
    assert !new File(kitchen, '.worktrees/feature').exists()
  }

  @Test void skipsTheScriptsABranchPredatesAndStillFailsOnesNeverCommitted() {
    def kitchen = repository(NAMED) {
      executable(it, '.worktrees/setup', '#!/bin/sh\n')
      executable(it, '.worktrees/teardown', '#!/bin/sh\n')
    }
    git(kitchen, 'checkout', '-q', '-b', 'old')
    git(kitchen, 'rm', '-q', '.worktrees/setup', '.worktrees/teardown')
    git(kitchen, 'commit', '-q', '-m', 'before the scripts')
    git(kitchen, 'checkout', '-q', 'main')
    def old = new File(kitchen, '.worktrees/old')

    def created = hook(kitchen, 'create', [name: 'old'])
    assert created.exit == 0
    assert created.error.contains("worktree: skipped .worktrees/setup, which this branch predates\n")
    def removed = hook(kitchen, 'remove', [worktree_path: old.canonicalPath])
    assert removed.exit == 0
    assert removed.error.contains("worktree: skipped .worktrees/teardown, which this branch predates\n")
    assert !old.exists()

    def uncommitted = repository(NAMED, 'uncommitted')
    executable(uncommitted, '.worktrees/setup', '#!/bin/sh\n')
    def failed = hook(uncommitted, 'create', [name: 'demo'])
    assert failed.exit == 1
    def demo = new File(uncommitted, '.worktrees/demo')
    assert failed.error.contains("worktree: .worktrees/setup is not in ${demo.canonicalPath}\n")
  }

  private File kitchen(String name = 'kitchen') {
    def kitchen = folder.newFolder(name)
    publishDirectory(kitchen, 'agents')
    kitchen
  }

  // A Git repository with the recipe cooked as `arguments` and committed with what `prepare` writes, whose published
  // cookbook Git does not see.
  private File repository(Map<String, List<String>> arguments, String name = 'kitchen', Closure prepare = {}) {
    def kitchen = kitchen(name)
    git(kitchen, 'init', '-q', '-b', 'main')
    new File(kitchen, '.git/info/exclude') << '/repository/\n/.worktrees/*\n!/.worktrees/setup\n!/.worktrees/teardown\n'
    cook(kitchen, CONNECT, arguments).commit()
    prepare(kitchen)
    commitAll(kitchen)
    kitchen
  }

  private File repository(Map<String, List<String>> arguments, Closure prepare) {
    repository(arguments, 'kitchen', prepare)
  }

  // A bare repository the kitchen adds as the remote `name` and pushes main to, leaving `<name>/HEAD` unset as a clone
  // of an empty repository does.
  private File remote(File kitchen, String name) {
    def bare = new File(folder.root, "${kitchen.name}-${name}.git")
    git(folder.root, 'init', '-q', '--bare', '-b', 'main', bare.path)
    git(kitchen, 'remote', 'add', name, bare.path)
    git(kitchen, 'push', '-q', name, 'main')
    bare
  }

  // Commits a file named `message` to the remote's main from a teammate's clone, and returns the commit.
  private String push(File bare, String message) {
    def clone = new File(folder.root, "${bare.name}-teammate")

    if (clone.exists()) {
      git(clone, 'pull', '-q', '--rebase')
    } else {
      git(folder.root, 'clone', '-q', bare.path, clone.path)
    }

    new File(clone, "${message}.txt").text = "${message}\n"
    git(clone, 'add', "${message}.txt")
    git(clone, 'commit', '-q', '-m', message)
    git(clone, 'push', '-q', 'origin', 'HEAD:main')
    git(clone, 'rev-parse', 'HEAD')
  }

  // The hook as the recipe renders it with `values`, each shell-quoted into the variable of its name.
  private static String rendered(Map<String, String> values) {
    values.inject(new File(COOKBOOK, 'agents/hooks/worktree').text) { String script, name, value ->
      script.replace("\n${name}=''\n", "\n${name}='${value}'\n")
    }
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
