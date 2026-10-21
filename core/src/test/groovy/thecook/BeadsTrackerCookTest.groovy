package thecook

import static thecook.Kitchens.COOKBOOK
import static thecook.Kitchens.cook
import static thecook.Kitchens.ignored
import static thecook.Kitchens.publishDirectory
import static thecook.Kitchens.sourcesSha256

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.RecipeSource

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

class BeadsTrackerCookTest {
  private static final String TRACKER = 'io.github.lmarqs.cookbook:beads:0.1.0#tracker'
  private static final String SEED = 'io.github.lmarqs.cookbook:agents:0.1.0#seed'
  private static final String CONNECT = 'io.github.lmarqs.cookbook:agents:0.1.0#connect'
  private static final File BEADS = new File(COOKBOOK, 'beads')
  private static final File SCRIPT = new File(BEADS, 'hooks/beads.sh')
  private static final String HOOK = '.agents/hooks/session-start.d/beads.sh'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void cloneKeepsTheIssuesInTheCloneOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, TRACKER, [share: ['clone']]).commit()

    assert files(kitchen) == ['.beads/PRIME.md', '.gitignore', 'cook.lock']
    assert text(kitchen, '.gitignore') == '''\
# cook:begin io.github.lmarqs.cookbook:beads#tracker/beads
# io.github.lmarqs.cookbook:beads#tracker --share=clone
/.beads/*
!/.beads/PRIME.md
# cook:end io.github.lmarqs.cookbook:beads#tracker/beads
'''
    assert new File(kitchen, '.beads/PRIME.md').bytes == new File(BEADS, 'templates/PRIME-clone.template.md').bytes
    assert text(kitchen, 'cook.lock') == lock('''\
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:beads#tracker/beads"
''')
    // No actions is what `./cook` reports as `Already cooked.`.
    assert cook(kitchen, TRACKER, [share: ['clone']]).actions == [:]
  }

  @Test void teamIgnoresOnlyTheExportOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, TRACKER, [share: ['team']]).commit()

    assert files(kitchen) == ['.beads/PRIME.md', '.gitignore', 'cook.lock']
    assert text(kitchen, '.gitignore') == '''\
# cook:begin io.github.lmarqs.cookbook:beads#tracker/beads
# io.github.lmarqs.cookbook:beads#tracker --share=team
/.beads/issues.jsonl
# cook:end io.github.lmarqs.cookbook:beads#tracker/beads
'''
    assert new File(kitchen, '.beads/PRIME.md').bytes == new File(BEADS, 'templates/PRIME-team.template.md').bytes
    assert text(kitchen, 'cook.lock') == lock('''\
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:beads#tracker/beads"
''')
    assert cook(kitchen, TRACKER, [share: ['team']]).actions == [:]
  }

  @Test void gitIgnoresWhatEachShareKeepsOutOfCommits() {
    def kitchen = kitchen()
    assert ['git', 'init', '-q', kitchen.path].execute().waitFor() == 0

    cook(kitchen, TRACKER, [share: ['clone']]).commit()
    ['.beads/config.yaml', '.beads/issues.jsonl', '.beads/embeddeddolt/x'].each { assert ignored(kitchen, it) }
    assert !ignored(kitchen, '.beads/PRIME.md')

    cook(kitchen, TRACKER, [share: ['team']]).commit()
    assert ignored(kitchen, '.beads/issues.jsonl')
    ['.beads/config.yaml', '.beads/PRIME.md'].each { assert !ignored(kitchen, it) }
  }

  @Test void keepsAHandEditedPrimeTextAndTheTeamsOwnIgnoreRules() {
    def kitchen = kitchen()
    new File(kitchen, '.gitignore').text = '/build/\n'
    cook(kitchen, TRACKER, [share: ['team']]).commit()
    def prime = new File(kitchen, '.beads/PRIME.md')
    prime.text = '# Our Beads rules\n'

    assert cook(kitchen, TRACKER, [share: ['team']]).actions == [:]
    assert cook(kitchen, TRACKER, [share: ['clone']]).actions == ['.gitignore': 'update']
    assert prime.text == '# Our Beads rules\n'
    assert text(kitchen, '.gitignore').startsWith('/build/\n# cook:begin ')
  }

  @Test void refusesAShareOutsideItsChoicesAndAMissingShare() {
    def kitchen = kitchen()

    def fork = shouldFail(IllegalArgumentException) { cook(kitchen, TRACKER, [share: ['fork']]) }
    assert fork.message == "${TRACKER}: --share=fork is not one of: team, clone"
    def missing = shouldFail(IllegalArgumentException) { cook(kitchen, TRACKER) }
    assert missing.message.startsWith("${TRACKER}: Missing --share: ")
    def harness = shouldFail(IllegalArgumentException) {
      cook(kitchen, TRACKER, [share: ['clone'], harness: ['gemini']])
    }
    assert harness.message == "${TRACKER}: --harness=gemini is not one of: claude, codex"
    assert files(kitchen) == []
  }

  @Test void ownsEachHarnessSessionStartHookOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, TRACKER, [share: ['clone'], harness: ['codex', 'claude']]).commit()

    assert files(kitchen) ==
    ['.beads/PRIME.md', '.claude/settings.json', '.codex/hooks.json', '.gitignore', 'cook.lock']
    assert text(kitchen, '.claude/settings.json') == '''\
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "bd prime"
          }
        ]
      }
    ]
  }
}
'''
    assert text(kitchen, '.codex/hooks.json') == '''\
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "startup|resume|clear",
        "hooks": [
          {
            "type": "command",
            "command": "bd codex-hook SessionStart"
          }
        ]
      }
    ]
  }
}
'''
    assert text(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:beads#tracker --share=clone --harness=claude --harness=codex'
    assert text(kitchen, 'cook.lock') == lock('''\
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:beads#tracker/beads"
      keys:
        - file: ".claude/settings.json"
          path: ["hooks","SessionStart"]
        - file: ".codex/hooks.json"
          path: ["hooks","SessionStart"]
''')
    assert cook(kitchen, TRACKER, [share: ['clone'], harness: ['claude', 'codex']]).actions == [:]
  }

  @Test void keepsTheTeamsOtherHooksAndDiscardRestoresTheirFiles() {
    def kitchen = kitchen()
    def claude = write(kitchen, '.claude/settings.json', '''\
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [{"type": "command", "command": "./check"}]
      }
    ]
  },
  "env": {"A": "1"}
}
''')
    def codex = write(kitchen, '.codex/hooks.json', '''\
{
  "hooks": {
    "Stop": [{"hooks": [{"type": "command", "command": "./stop"}]}]
  }
}
''')
    def claudeBefore = claude.text
    def codexBefore = codex.text

    cook(kitchen, TRACKER, [share: ['team'], harness: ['claude', 'codex']]).commit()

    assert claude.text == '''\
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [{"type": "command", "command": "./check"}]
      }
    ],
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "bd prime"
          }
        ]
      }
    ]
  },
  "env": {"A": "1"}
}
'''
    assert codex.text == '''\
{
  "hooks": {
    "Stop": [{"hooks": [{"type": "command", "command": "./stop"}]}],
    "SessionStart": [
      {
        "matcher": "startup|resume|clear",
        "hooks": [
          {
            "type": "command",
            "command": "bd codex-hook SessionStart"
          }
        ]
      }
    ]
  }
}
'''
    assert cook(kitchen, TRACKER, [share: ['team'], harness: ['claude', 'codex']]).actions == [:]

    cook(kitchen, TRACKER, [share: ['team'], harness: ['codex']]).commit()
    assert claude.text == claudeBefore

    new CookbookDiscard(kitchen).propose([TRACKER]).commit()
    assert claude.text == claudeBefore
    assert codex.text == codexBefore
    assert files(kitchen) == ['.beads/PRIME.md', '.claude/settings.json', '.codex/hooks.json']
  }

  @Test void theEngineRefusesAHookThroughALinkedSettingsFile() {
    def kitchen = kitchen()
    write(kitchen, 'shared.json', '{}\n')
    Files.createDirectories(new File(kitchen, '.claude').toPath())
    Files.createSymbolicLink(new File(kitchen, '.claude/settings.json').toPath(), Paths.get('../shared.json'))

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, TRACKER, [share: ['clone'], harness: ['claude']])
    }

    assert error.message == "${TRACKER}: .claude/settings.json key hooks.SessionStart\n" +
    '  .claude/settings.json is a symbolic link to ../shared.json\n' +
    '    Cook writes sections and keys only into regular files'
    assert text(kitchen, 'shared.json') == '{}\n'
  }

  @Test void ownsTheSessionStartScriptOnceTheLayoutIsInTheDraft() {
    def kitchen = layoutKitchen('kitchen')

    cook(kitchen, TRACKER, [share: ['clone']]).commit()

    def script = new File(kitchen, HOOK)
    assert script.bytes == SCRIPT.bytes
    assert permissions(script) == 'rwxr-xr-x'
    assert !new File(kitchen, '.claude').exists()
    assert !new File(kitchen, '.codex').exists()
    assert text(kitchen, 'cook.lock').contains('''\
  - name: "io.github.lmarqs.cookbook:beads#tracker"
    source: "io.github.lmarqs.cookbook:beads:0.1.0#tracker"
    sha256: "''' + sourcesSha256('beads') + '''"
    owns:
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:beads#tracker/beads"
      files:
        - ".agents/hooks/session-start.d/beads.sh"
''')
    assert cook(kitchen, TRACKER, [share: ['clone']]).actions == [:]
  }

  @Test void refusesAHarnessOnceTheLayoutIsInTheDraftNamingWhatToCook() {
    def kitchen = layoutKitchen('kitchen')
    def before = files(kitchen)

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, TRACKER, [share: ['clone'], harness: ['codex', 'claude']])
    }

    assert error.message == "${TRACKER}: --harness=claude --harness=codex writes the harness's own hook file, " +
    'which the agents layout connects\n' +
    "  the agents layout is in the draft, and ${HOOK} gives bd prime to every harness it connects\n" +
    '  leave out --harness, and cook io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex'
    assert files(kitchen) == before
  }

  @Test void cooksWithAgentsConnectInEitherOrder() {
    def connectFirst = layoutKitchen('first')
    cook(connectFirst, [(CONNECT): [harness: ['claude', 'codex']], (TRACKER): [share: ['clone']]]).commit()
    def beadsFirst = layoutKitchen('second')
    cook(beadsFirst, [(TRACKER): [share: ['clone']], (CONNECT): [harness: ['claude', 'codex']]]).commit()

    [connectFirst, beadsFirst].each { kitchen ->
      assert new File(kitchen, HOOK).bytes == SCRIPT.bytes
      def ignore = text(kitchen, '.gitignore')
      assert ignore.contains('# io.github.lmarqs.cookbook:beads#tracker --share=clone\n/.beads/*\n')
      assert ignore.contains('# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex\n')
      assert Files.isSymbolicLink(new File(kitchen, '.claude/settings.json').toPath())
      assert cook(kitchen, [(CONNECT): [harness: ['claude', 'codex']], (TRACKER): [share: ['clone']]]).actions == [:]
    }
    // Only the order of the sections in `.gitignore` and of the recipes in `cook.lock` follows the invocation.
    def unordered = { File kitchen ->
      contents(kitchen).findAll {
        !(it.key in ['.gitignore', 'cook.lock'])
      }
    }
    assert unordered(connectFirst) == unordered(beadsFirst)
  }

  @Test void addsTheScriptOnTheNextCookWhenTheLayoutCameLater() {
    def kitchen = kitchen()
    publishDirectory(kitchen, 'agents')

    cook(kitchen, [(TRACKER): [share: ['clone']], (SEED): [:], (CONNECT): [harness: ['claude']]]).commit()
    assert !new File(kitchen, HOOK).exists()

    assert cook(kitchen, TRACKER, [share: ['clone']]).actions == [(HOOK): 'create', 'cook.lock': 'update']
  }

  @Test void theScriptPrintsBdPrimeAndNothingWithoutBdOrItsDatabase() {
    def bin = folder.newFolder('bin')
    assert run(['/bin/sh', '-n', SCRIPT.path], bin) == [0, '']
    assert run(['/bin/sh', SCRIPT.path, 'claude'], bin) == [0, '']

    write(bin.parentFile, 'bin/bd', '#!/bin/sh\n[ "$1" = prime ] && [ $# -eq 1 ] && echo "Beads rules"\n')
    new File(bin, 'bd').setExecutable(true)
    assert run(['/bin/sh', SCRIPT.path, 'codex'], bin) == [0, 'Beads rules\n']

    write(bin.parentFile, 'bin/bd', '#!/bin/sh\necho "no beads database" >&2\nexit 1\n')
    assert run(['/bin/sh', SCRIPT.path, 'claude'], bin) == [0, '']
  }

  @Test void eachPrimeTextStatesItsShareAndTheWorkerRule() {
    ['team', 'clone'].each { share ->
      def prime = new File(BEADS, "templates/PRIME-${share}.template.md").text
      assert prime.contains('A worker never closes its bead or pushes')
      assert prime.contains('bd ready')
    }
    assert new File(BEADS, 'templates/PRIME-team.template.md').text.contains('`bd sync` pulls')
    assert new File(BEADS, 'templates/PRIME-clone.template.md').text.contains('The issues have no remote')
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(BEADS, 'tracker.groovy').text, 'tracker.groovy')

    assert texts.params*.names == [['share'], ['harness']]
    assert texts.params*.choices == [['team', 'clone'], ['claude', 'codex']]
    assert texts.ingredients == ['io.github.lmarqs.cookbook.beads.Beads']
    assert texts.help.readLines().findAll { it.startsWith('## ') } ==
    ['## API', '## Consumes', '## Produces', '## Connections', '## Run-time effects', '## Re-cooking', '## Traps']
    ['Never touches', 'leave out `--harness`', '5.5 KB', '--check',].each { assert texts.help.contains(it) }
    [
      'bd init --prefix <p> --skip-hooks --skip-agents --non-interactive',
      'bd config set sync.remote <url>',
      'bd config set export.auto true',
      'bd hooks install --beads',
      'git config core.hooksPath .beads/hooks',
      'bd setup',
      'io.github.lmarqs.cookbook:agents:<version>#connect',
      'run `bd prime`',
    ].each { assert texts.yieldText.contains(it) }
  }

  private File kitchen(String name = 'kitchen') {
    def kitchen = folder.newFolder(name)
    publishDirectory(kitchen, 'beads')
    kitchen
  }

  private File layoutKitchen(String name) {
    def kitchen = kitchen(name)
    publishDirectory(kitchen, 'agents')
    cook(kitchen, SEED).commit()
    kitchen
  }

  private static String lock(String owns) {
    """# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "io.github.lmarqs.cookbook:beads#tracker"
    source: "${TRACKER}"
    sha256: "${sourcesSha256('beads')}"
    owns:
""" + owns
  }

  private static String text(File kitchen, String path) {
    new File(kitchen, path).text
  }

  private static File write(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
    file
  }

  private static String permissions(File file) {
    PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath()))
  }

  // Runs `command` with only `bin` on the PATH, returning its exit status and standard output.
  private static List run(List<String> command, File bin) {
    def builder = new ProcessBuilder(command)
    builder.environment().put('PATH', bin.path)
    def process = builder.start()
    def out = process.inputStream.text
    [process.waitFor(), out]
  }

  // The kitchen's files, leaving out the published repository and Git's own directory.
  private static List<String> files(File kitchen) {
    Kitchens.files(kitchen).findAll { !it.startsWith('repository/') && !it.startsWith('.git/') }
  }

  // Each of the kitchen's files with its bytes, read as Latin-1 text so any byte compares; links are followed.
  private static Map<String, String> contents(File kitchen) {
    files(kitchen).collectEntries { [it, new File(kitchen, it).getText('ISO-8859-1')] }
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
