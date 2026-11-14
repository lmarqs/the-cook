package thecook

import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook
import static thecook.Kitchens.ignored

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.CookLock
import thecook.cookbook.SectionTarget

class BeadsViewerCookTest {
  private static final String VIEWER = 'io.github.lmarqs.cookbook:beads:0.1.0#viewer'
  private static final String NAME = 'io.github.lmarqs.cookbook:beads#viewer'
  private static final String TRACKER = 'io.github.lmarqs.cookbook:beads:0.1.0#tracker'
  private static final String TRACKER_NAME = 'io.github.lmarqs.cookbook:beads#tracker'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void standaloneViewerOwnsOnlyItsRootExportIgnoreByDefault() {
    def kitchen = kitchen()
    git(kitchen, 'init', '-q')

    cook(kitchen, VIEWER).commit()

    assert files(kitchen) == ['.gitignore', 'cook.lock']
    def expectedIgnore = ["# cook:begin ${NAME}/viewer", "# ${NAME}", '/.beads/issues.jsonl']
    expectedIgnore.add("# cook:end ${NAME}/viewer")
    assert new File(kitchen, '.gitignore').text.readLines() == expectedIgnore*.toString()
    assert ignored(kitchen, '.beads/issues.jsonl')
    assert !ignored(kitchen, '.beads/config.yaml')
    assert !ignored(kitchen, 'nested/.beads/issues.jsonl')
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.arguments(NAME) == [:]
    assert lock.owned(NAME) == [new SectionTarget('.gitignore', "${NAME}/viewer")]
    assert cook(kitchen, VIEWER).actions == [:]
  }

  @Test void explicitInstructionTargetsAreDistinctOwnedSectionsAndCanBeWithdrawn() {
    def kitchen = kitchen()
    write(kitchen, 'AGENTS.md', '# Agents\n\nTeam policy.\n')
    write(kitchen, 'README.md', '# Project\n')
    write(kitchen, '.gitignore', '/build/\n')
    def arguments = [instructions: ['README.md', 'AGENTS.md', 'AGENTS.md']]

    cook(kitchen, VIEWER, arguments).commit()

    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    def expected = ['.gitignore', 'AGENTS.md', 'README.md'].collect {
      new SectionTarget(it, "${NAME}/viewer")
    }
    assert lock.owned(NAME).toSet() == expected.toSet()
    assert lock.arguments(NAME) == [instructions: ['AGENTS.md', 'README.md']]
    assert new File(kitchen, 'AGENTS.md').text.startsWith('# Agents\n\nTeam policy.\n')
    assert new File(kitchen, '.gitignore').text.startsWith('/build/\n')
    assert cook(kitchen, VIEWER, arguments).actions == [:]

    cook(kitchen, VIEWER, [instructions: ['AGENTS.md']]).commit()

    assert new File(kitchen, 'README.md').text == '# Project\n'
    cook(kitchen, VIEWER).commit()
    assert new File(kitchen, 'AGENTS.md').text == '# Agents\n\nTeam policy.\n'
  }

  @Test void recookingEitherRecipePreservesTheOthersOwnershipAndSections() {
    ['team', 'clone'].each { share ->
      def kitchen = kitchen(share)
      write(kitchen, 'AGENTS.md', '# Agents\n')
      def trackerArguments = [share: [share], instructions: ['AGENTS.md']]
      def viewerArguments = [instructions: ['AGENTS.md']]
      cook(kitchen, TRACKER, trackerArguments).commit()
      def trackerConfig = new File(kitchen, '.beads/config.yaml').text
      def trackerIgnore = new File(kitchen, '.gitignore').text
      def trackerGuidance = new File(kitchen, 'AGENTS.md').text
      def trackerLock = CookLock.parse(new File(kitchen, 'cook.lock').text)

      cook(kitchen, VIEWER, viewerArguments).commit()

      assert new File(kitchen, '.beads/config.yaml').text == trackerConfig
      assert new File(kitchen, '.gitignore').text.startsWith(trackerIgnore)
      assert new File(kitchen, 'AGENTS.md').text.startsWith(trackerGuidance)
      def combinedIgnore = new File(kitchen, '.gitignore').text
      def combinedGuidance = new File(kitchen, 'AGENTS.md').text
      def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
      assert lock.owned(TRACKER_NAME) == trackerLock.owned(TRACKER_NAME)
      assert cook(kitchen, TRACKER, trackerArguments).actions == [:]
      assert cook(kitchen, VIEWER, viewerArguments).actions == [:]

      cook(kitchen, TRACKER, trackerArguments + [share: [share == 'team' ? 'clone' : 'team']]).commit()

      def ignoreSuffix = combinedIgnore.substring(trackerIgnore.length())
      def guidanceSuffix = combinedGuidance.substring(trackerGuidance.length())
      assert new File(kitchen, '.gitignore').text.endsWith(ignoreSuffix)
      assert new File(kitchen, 'AGENTS.md').text.endsWith(guidanceSuffix)
      def changedLock = CookLock.parse(new File(kitchen, 'cook.lock').text)
      assert changedLock.owned(NAME) == lock.owned(NAME)
      assert cook(kitchen, VIEWER, viewerArguments).actions == [:]
    }
  }

  @Test void viewerDiscardLeavesTrackerNativeStateAndUnrelatedContent() {
    def kitchen = kitchen()
    write(kitchen, '.gitignore', '/build/\n')
    write(kitchen, 'AGENTS.md', '# Agents\n')
    def nativeFiles = [
      '.beads/issues.jsonl': 'runtime export\n', '.beads/metadata.json': '{"database":"native"}\n',
      '.beads/embeddeddolt/task': 'task history\n', '.beads/README.md': 'Team seed\n',
      '.beads/config.local.yaml': 'native override\n', '.git/hooks/pre-commit': 'team hook\n',
      '.git/info/exclude': '/machine-local/\n',
    ]
    nativeFiles.each { path, text -> write(kitchen, path, text) }
    cook(kitchen, TRACKER, [instructions: ['AGENTS.md']]).commit()
    def trackerConfig = new File(kitchen, '.beads/config.yaml').text
    def trackerIgnore = new File(kitchen, '.gitignore').text
    def trackerGuidance = new File(kitchen, 'AGENTS.md').text
    cook(kitchen, VIEWER, [instructions: ['AGENTS.md']]).commit()

    new CookbookDiscard(kitchen).propose([NAME]).commit()

    assert new File(kitchen, '.beads/config.yaml').text == trackerConfig
    assert new File(kitchen, '.gitignore').text == trackerIgnore
    assert new File(kitchen, 'AGENTS.md').text == trackerGuidance
    nativeFiles.each { path, text -> assert new File(kitchen, path).text == text }
    assert cook(kitchen, TRACKER, [instructions: ['AGENTS.md']]).actions == [:]
  }

  @Test void discardingTrackerFirstLeavesStandaloneViewerAndRuntimeExport() {
    def kitchen = kitchen()
    write(kitchen, 'AGENTS.md', '# Agents\n')
    write(kitchen, '.beads/issues.jsonl', 'runtime export\n')
    cook(kitchen, VIEWER, [instructions: ['AGENTS.md']]).commit()
    def viewerIgnore = new File(kitchen, '.gitignore').text
    def viewerGuidance = new File(kitchen, 'AGENTS.md').text
    cook(kitchen, TRACKER, [instructions: ['AGENTS.md']]).commit()

    new CookbookDiscard(kitchen).propose([TRACKER_NAME]).commit()

    assert new File(kitchen, '.gitignore').text == viewerIgnore
    assert new File(kitchen, 'AGENTS.md').text == viewerGuidance
    assert new File(kitchen, '.beads/issues.jsonl').text == 'runtime export\n'
    assert cook(kitchen, VIEWER, [instructions: ['AGENTS.md']]).actions == [:]
  }

  @Test void missingOutsideAndEmptyInstructionTargetsWriteNothing() {
    def kitchen = kitchen()
    ['AGENTS.md', '/tmp/AGENTS.md', 'docs/../../AGENTS.md', ' '].each { path ->
      try {
        cook(kitchen, VIEWER, [instructions: [path]])
        throw new AssertionError("accepted ${path}")
      } catch (IllegalArgumentException error) {
        assert error.message.contains(VIEWER)
      }

      assert files(kitchen) == []
    }
  }

  @Test void generatedRefreshUsesOneStoreFromMainWorktreeOrExplicitExternalDirectory() {
    def kitchen = kitchen('main checkout')
    git(kitchen, 'init', '-q')
    write(kitchen, 'AGENTS.md', '# Agents\n')
    git(kitchen, 'add', 'AGENTS.md')
    git(kitchen, '-c', 'user.name=Test', '-c', 'user.email=test@example.invalid', 'commit', '-qm', 'fixture')
    def worktree = new File(folder.root, 'linked checkout')
    git(kitchen, 'worktree', 'add', '-qb', 'linked', worktree.path)
    write(worktree, '.beads/config.yaml', 'team: config shadows discovery\n')
    def external = folder.newFolder('external store')
    cook(kitchen, VIEWER, [instructions: ['AGENTS.md']]).commit()
    def command = refreshCommand(new File(kitchen, 'AGENTS.md').text)
    def nativeStore = new File(kitchen, '.beads')
    nativeStore.mkdirs()

    def cases = [[kitchen, null, nativeStore], [worktree, null, nativeStore], [worktree, external.path, external]]
    cases.each { directory, selected, expected ->
      def result = refresh(directory, command, selected, 0)

      assert result.status == 0: result.error
      assert result.calls == [
        'bd',
        '--db',
        expected.path,
        'export',
        '-o',
        "${expected.path}/issues.jsonl".toString(),
        'bv',
        '--db',
        expected.path,
        '--robot-triage',
      ]
      assert new File(expected, 'issues.jsonl').text == 'fresh snapshot\n'
    }
    assert !new File(worktree, '.beads/issues.jsonl').exists()
  }

  @Test void failedExplicitRefreshKeepsDiagnosticsAndNeverRunsViewerAgainstStaleOrMissingExport() {
    def kitchen = kitchen()
    write(kitchen, 'AGENTS.md', '# Agents\n')
    def external = folder.newFolder('external store')
    cook(kitchen, VIEWER, [instructions: ['AGENTS.md']]).commit()
    def command = refreshCommand(new File(kitchen, 'AGENTS.md').text)

    [false, true].each { existing ->
      if (existing) {
        write(external, 'issues.jsonl', 'stale snapshot\n')
      }

      def result = refresh(kitchen, command, external.path, 73)

      assert result.status == 73
      assert result.error.contains('synthetic export failure')
      assert result.calls == ['bd', '--db', external.path, 'export', '-o', "${external.path}/issues.jsonl".toString()]
      assert new File(external, 'issues.jsonl').exists() == existing

      if (existing) {
        assert new File(external, 'issues.jsonl').text == 'stale snapshot\n'
      }
    }
  }

  private File kitchen(String name = 'kitchen') {
    def kitchen = folder.newFolder(name)
    publishDirectory(kitchen, 'beads')
    kitchen
  }

  private Map refresh(File directory, String command, String selected, int exportStatus) {
    def bin = folder.newFolder()
    write(bin, 'bd', '''\
#!/bin/sh
printf '%s\n' bd "$@" >> "$VIEWER_TRACE"
if [ "$EXPORT_STATUS" != 0 ]; then
  echo 'synthetic export failure' >&2
  exit "$EXPORT_STATUS"
fi
printf 'fresh snapshot\n' > "$5"
''')
    write(bin, 'bv', '''\
#!/bin/sh
printf '%s\n' bv "$@" >> "$VIEWER_TRACE"
''')
    assert new File(bin, 'bd').setExecutable(true)
    assert new File(bin, 'bv').setExecutable(true)
    def trace = new File(bin, 'calls')
    def builder = new ProcessBuilder('sh', '-c', command).directory(directory)
    def environment = builder.environment()
    environment.remove('BEADS_DIR')
    environment.putAll([
      PATH: "${bin.path}:${environment.PATH}".toString(), VIEWER_TRACE: trace.path,
      EXPORT_STATUS: exportStatus.toString(), BEADS_DB: '/wrong/store',
    ])

    if (selected) {
      environment.BEADS_DIR = selected
    }

    def process = builder.start()
    def output = new StringBuffer()
    def error = new StringBuffer()
    process.waitForProcessOutput(output, error)
    [status: process.exitValue(), error: error.toString(), calls: trace.readLines()]
  }

  private static String refreshCommand(String instructions) {
    def matcher = instructions =~ /(?s)```sh\n(.*?)```/
    assert matcher.find(): 'Viewer instructions must offer an executable refresh sequence'
    matcher.group(1)
  }

  private static void git(File directory, String... arguments) {
    def process = (['git', '-C', directory.path] + arguments.toList()).execute()
    def output = new StringBuffer()
    def error = new StringBuffer()
    process.waitForProcessOutput(output, error)
    assert process.exitValue() == 0: error.toString()
  }

  private static void write(File directory, String path, String text) {
    def file = new File(directory, path)
    file.parentFile.mkdirs()
    file.text = text
  }

  private static List<String> files(File kitchen) {
    Kitchens.files(kitchen).findAll { !it.startsWith('repository/') && !it.startsWith('.git/') }
  }
}
