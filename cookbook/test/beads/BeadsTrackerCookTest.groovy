package thecook

import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook
import static thecook.Kitchens.ignored

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.CookLock
import thecook.cookbook.KeyTarget
import thecook.cookbook.SectionTarget
import thecook.formats.YamlDocument

class BeadsTrackerCookTest {
  private static final String TRACKER = 'io.github.lmarqs.cookbook:beads:0.1.0#tracker'
  private static final String NAME = 'io.github.lmarqs.cookbook:beads#tracker'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void defaultsToSharedConfigurationWithoutInitializingNativeState() {
    def kitchen = kitchen()

    cook(kitchen, TRACKER).commit()

    assert files(kitchen) == ['.beads/config.yaml', '.gitignore', 'cook.lock']
    policy(kitchen, false)
    assert new File(kitchen, '.gitignore').text.readLines() == [
      "# cook:begin ${NAME}/tracker",
      "# ${NAME}",
      '/.beads/*',
      '!/.beads/config.yaml',
      '.dolt/',
      '*.db',
      '.beads-credential-key',
      '.beads/proxieddb/',
      '*.gate.lock*',
      "# cook:end ${NAME}/tracker",
    ]*.toString()
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.arguments(NAME) == [:]
    def policyPaths = [['dolt', 'local-only'], ['dolt', 'auto-push'], ['no-git-ops']]
    policyPaths.addAll([['export', 'auto'], ['export', 'git-add']])
    def policyTargets = policyPaths.collect { new KeyTarget('.beads/config.yaml', it) }
    def expectedTargets = [new SectionTarget('.gitignore', "${NAME}/tracker")] + policyTargets
    assert lock.owned(NAME).toSet() == expectedTargets.toSet()
    assert cook(kitchen, TRACKER).actions == [:]
  }

  @Test void eachSharingModeIgnoresRuntimeStateAndOnlyTeamSharesConfiguration() {
    ['team', 'clone'].each { share ->
      def kitchen = kitchen(share)
      assert ['git', 'init', '-q', kitchen.path].execute().waitFor() == 0

      cook(kitchen, TRACKER, [share: [share]]).commit()

      policy(kitchen, share == 'clone')
      [
        '.beads/issues.jsonl',
        '.beads/metadata.json',
        '.beads/embeddeddolt/task',
        '.beads/future-local-file',
        '.beads/proxieddb/task',
        '.beads.gate.lock',
        '.beads.gate.lock.tmp',
        '.dolt/config.json',
        'nested/.dolt/config.json',
        'nested/tasks.db',
        '.beads-credential-key',
      ].each { assert ignored(kitchen, it): it }
      assert ignored(kitchen, '.beads/config.yaml') == (share == 'clone')
      assert !ignored(kitchen, 'README.md')
    }
  }

  @Test void modeChangesReconcileOwnedInstructionsAndPreserveTheTeamsNativeState() {
    def kitchen = kitchen()
    write(kitchen, 'AGENTS.md', '# Agents\n\nTeam policy.\n')
    write(kitchen, 'README.md', '# Project\n')
    write(kitchen, '.gitignore', '/build/\n')
    write(kitchen, '.beads/config.yaml', '''\
# Native destination
sync:
  remote: git+ssh://example.invalid/team.git
custom:
  unrelated: preserve
dolt:
  auto-push: true
  custom: keep
''')
    def nativeFiles = [
      '.beads/embeddeddolt/task': 'task history\n', '.beads/metadata.json': '{"database":"native"}\n',
      '.beads/.gitignore': 'native ignores\n', '.beads/README.md': 'Team seed\n',
      '.beads.gate.lock': 'runtime\n', '.git/hooks/pre-commit': 'team hook\n',
      '.git/info/exclude': '/machine-local/\n',
    ]
    nativeFiles.each { path, text -> write(kitchen, path, text) }
    def instructions = [instructions: ['README.md', 'AGENTS.md', 'AGENTS.md']]

    cook(kitchen, TRACKER, instructions).commit()
    def teamGuidance = new File(kitchen, 'AGENTS.md').text
    assert teamGuidance.contains('bd dolt push')
    assert !teamGuidance.contains('Remote sync: disabled')

    cook(kitchen, TRACKER, instructions + [share: ['clone']]).commit()

    policy(kitchen, true)
    def cloneGuidance = new File(kitchen, 'AGENTS.md').text
    assert cloneGuidance.startsWith('# Agents\n\nTeam policy.\n')
    assert cloneGuidance.contains('Remote sync: disabled')
    assert !cloneGuidance.contains('bd dolt push')
    assert new File(kitchen, 'README.md').text.contains('Remote sync: disabled')
    assert new File(kitchen, '.gitignore').text.startsWith('/build/\n')
    assert cook(kitchen, TRACKER, instructions + [share: ['clone']]).actions == [:]

    cook(kitchen, TRACKER, instructions).commit()

    assert new File(kitchen, 'AGENTS.md').text == teamGuidance
    policy(kitchen, false)
    def config = new File(kitchen, '.beads/config.yaml').text
    assert config.contains('sync:\n  remote: git+ssh://example.invalid/team.git\n')
    assert config.contains('custom:\n  unrelated: preserve\n')
    assert config.contains('  custom: keep\n')
    nativeFiles.each { path, text -> assert new File(kitchen, path).text == text }
    def lock = CookLock.parse(new File(kitchen, 'cook.lock').text)
    assert lock.owned(NAME).count {
      it instanceof SectionTarget && it.file in ['AGENTS.md', 'README.md']
    } == 2
  }

  @Test void droppingAnInstructionTargetWithdrawsOnlyItsTrackerSection() {
    def kitchen = kitchen()
    write(kitchen, 'AGENTS.md', '# Agents\n')
    write(kitchen, 'README.md', '# Project\n')
    cook(kitchen, TRACKER, [instructions: ['AGENTS.md', 'README.md']]).commit()

    cook(kitchen, TRACKER, [instructions: ['AGENTS.md']]).commit()

    assert new File(kitchen, 'README.md').text == '# Project\n'
    assert new File(kitchen, 'AGENTS.md').text.contains("<!-- cook:begin ${NAME}/tracker -->")
    cook(kitchen, TRACKER).commit()
    assert new File(kitchen, 'AGENTS.md').text == '# Agents\n'
  }

  @Test void discardRemovesOnlyOwnedPartsAndLeavesTasksRemoteAndTeamSeeds() {
    def kitchen = kitchen()
    write(kitchen, '.beads/config.yaml', 'sync:\n  remote: git+ssh://example.invalid/team.git\n')
    write(kitchen, '.beads/README.md', 'Team seed\n')
    write(kitchen, '.beads/embeddeddolt/task', 'Private task\n')
    write(kitchen, '.gitignore', '/build/\n')
    write(kitchen, 'AGENTS.md', '# Agents\n')
    cook(kitchen, TRACKER, [share: ['clone'], instructions: ['AGENTS.md']]).commit()

    new CookbookDiscard(kitchen).propose([NAME]).commit()

    assert new File(kitchen, '.beads/config.yaml').text == 'sync:\n  remote: git+ssh://example.invalid/team.git\n'
    assert new File(kitchen, '.beads/README.md').text == 'Team seed\n'
    assert new File(kitchen, '.beads/embeddeddolt/task').text == 'Private task\n'
    assert new File(kitchen, '.gitignore').text == '/build/\n'
    assert new File(kitchen, 'AGENTS.md').text == '# Agents\n'
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void invalidShareAndMissingOrOutsideInstructionTargetsWriteNothing() {
    def kitchen = kitchen()
    [
      [share: ['personal']],
      [share: ['']],
      [share: ['team', 'clone']],
      [instructions: ['AGENTS.md']],
      [instructions: ['/tmp/AGENTS.md']],
      [instructions: ['docs/../../AGENTS.md']],
      [instructions: [' ']],
    ].each { arguments ->
      try {
        cook(kitchen, TRACKER, arguments)
        throw new AssertionError("accepted ${arguments}")
      } catch (IllegalArgumentException error) {
        assert error.message.contains(TRACKER)
      }

      assert files(kitchen) == []
    }
  }

  private File kitchen(String name = 'kitchen') {
    def kitchen = folder.newFolder(name)
    publishDirectory(kitchen, 'beads')
    kitchen
  }

  private static void policy(File kitchen, boolean localOnly) {
    def config = new YamlDocument(new File(kitchen, '.beads/config.yaml').text)
    assert !config.set(['dolt', 'local-only'], localOnly)
    assert !config.set(['dolt', 'auto-push'], false)
    assert !config.set(['no-git-ops'], true)
    assert !config.set(['export', 'auto'], false)
    assert !config.set(['export', 'git-add'], false)
  }

  private static void write(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
  }

  private static List<String> files(File kitchen) {
    Kitchens.files(kitchen).findAll { !it.startsWith('repository/') && !it.startsWith('.git/') }
  }
}
