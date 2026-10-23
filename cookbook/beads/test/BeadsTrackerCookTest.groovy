package thecook

import static thecook.Cookbook.COOKBOOK
import static thecook.Cookbook.publishDirectory
import static thecook.Cookbook.sourcesSha256
import static thecook.Kitchens.cook
import static thecook.Kitchens.ignored

import groovy.json.JsonOutput
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.RecipeSource

import java.nio.file.Files
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
    assert text(kitchen, 'cook.lock') == lock([share: ['clone']], '''\
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
    assert text(kitchen, 'cook.lock') == lock([share: ['team']], '''\
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
    assert cook(kitchen, TRACKER, [share: ['clone']]).actions == ['.gitignore': 'update', 'cook.lock': 'update']
    assert prime.text == '# Our Beads rules\n'
    assert text(kitchen, '.gitignore').startsWith('/build/\n# cook:begin ')
  }

  @Test void refusesAShareOutsideItsChoices() {
    def kitchen = kitchen()

    def fork = shouldFail(IllegalArgumentException) { cook(kitchen, TRACKER, [share: ['fork']]) }
    assert fork.message == "${TRACKER}: --share=fork is not one of: team, clone"
    assert files(kitchen) == []
  }

  @Test void theTeamShareIsTheDefault() {
    def kitchen = kitchen()

    cook(kitchen, TRACKER).commit()

    assert text(kitchen, '.gitignore').readLines()[2] == '/.beads/issues.jsonl'
    assert text(kitchen, '.beads/PRIME.md') == new File(COOKBOOK, 'beads/templates/PRIME-team.template.md').text
  }

  @Test void writesTheSessionStartScriptAtTheNamedPath() {
    def kitchen = kitchen()
    def path = 'tools/session/20-beads.sh'

    cook(kitchen, TRACKER, [share: ['clone'], 'session-start': [path]]).commit()

    def script = new File(kitchen, path)
    assert script.bytes == SCRIPT.bytes
    assert permissions(script) == 'rwxr-xr-x'
    assert files(kitchen) == ['.beads/PRIME.md', '.gitignore', 'cook.lock', path]
    assert text(kitchen, '.gitignore').readLines()[1] ==
    "# io.github.lmarqs.cookbook:beads#tracker --share=clone --session-start=${path}".toString()
    assert text(kitchen, 'cook.lock') == lock([share: ['clone'], 'session-start': [path.toString()]], """\
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:beads#tracker/beads"
      files:
        - "${path}"
""")
    assert cook(kitchen, TRACKER, [share: ['clone'], 'session-start': [path]]).actions == [:]
  }

  @Test void writesNothingForSessionStartWithoutItEvenWithTheLayoutsHooksInTheDraft() {
    def kitchen = kitchen()
    write(kitchen, '.agents/hooks/.gitkeep', '')
    write(kitchen, '.agents/hooks/session-start', '#!/bin/sh\n')
    def before = files(kitchen)

    cook(kitchen, TRACKER, [share: ['clone']]).commit()

    assert files(kitchen) == (before + ['.beads/PRIME.md', '.gitignore', 'cook.lock']).sort()
    assert text(kitchen, '.gitignore').readLines()[1] == '# io.github.lmarqs.cookbook:beads#tracker --share=clone'
    assert text(kitchen, 'cook.lock') == lock([share: ['clone']], '''\
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:beads#tracker/beads"
''')
    assert cook(kitchen, TRACKER, [share: ['clone']]).actions == [:]
  }

  @Test void anotherPathMovesTheScriptAndLeavingItOutOrDiscardRemovesIt() {
    def kitchen = kitchen()
    cook(kitchen, TRACKER, [share: ['team'], 'session-start': [HOOK]]).commit()

    cook(kitchen, TRACKER, [share: ['team'], 'session-start': ['scripts/beads.sh']]).commit()
    assert !new File(kitchen, HOOK).exists()
    assert new File(kitchen, 'scripts/beads.sh').bytes == SCRIPT.bytes

    cook(kitchen, TRACKER, [share: ['team']]).commit()
    assert files(kitchen) == ['.beads/PRIME.md', '.gitignore', 'cook.lock']

    cook(kitchen, TRACKER, [share: ['team'], 'session-start': [HOOK]]).commit()
    new CookbookDiscard(kitchen).propose([TRACKER]).commit()
    assert files(kitchen) == ['.beads/PRIME.md']
  }

  @Test void tellsAgentsInEachInstructionsFileAndRefusesAMissingOne() {
    def kitchen = kitchen()
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Agents\n'
    def arguments = [share: ['clone'], instructions: ['AGENTS.md']]

    cook(kitchen, TRACKER, arguments).commit()

    assert agents.text == '''\
# Agents
<!-- cook:begin io.github.lmarqs.cookbook:beads#tracker/beads -->
<!-- io.github.lmarqs.cookbook:beads#tracker --share=clone --instructions=AGENTS.md -->
- Issues: Beads tracks this project's work; `bd prime` prints its rules, which a session-start hook may already
  have printed.
<!-- cook:end io.github.lmarqs.cookbook:beads#tracker/beads -->
'''
    assert text(kitchen, '.gitignore').readLines()[1] ==
    '# io.github.lmarqs.cookbook:beads#tracker --share=clone --instructions=AGENTS.md'
    assert cook(kitchen, TRACKER, arguments).actions == [:]

    def error = shouldFail(IllegalArgumentException) {
      cook(kitchen, TRACKER, [share: ['clone'], instructions: ['CLAUDE.md']])
    }
    assert error.message.readLines()[1] == '  CLAUDE.md is missing'
  }

  @Test void refusesABlankAbsoluteOrOutsideSessionStartPath() {
    def kitchen = kitchen()
    def next = '  name the file by its path from the repository root'

    ['', ' ', '.', './'].each { path ->
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, TRACKER, [share: ['clone'], 'session-start': [path]])
      }
      assert error.message.readLines() == ["${TRACKER}: --session-start needs a path", next]*.toString()
    }
    ['/etc/beads.sh', '../beads.sh', 'a/../../beads.sh', 'hooks/../beads.sh'].each { path ->
      def error = shouldFail(IllegalArgumentException) {
        cook(kitchen, TRACKER, [share: ['clone'], 'session-start': [path]])
      }
      assert error.message.readLines() == [
        "${TRACKER}: --session-start=${path} is outside the repository",
        '  a recipe reads and writes only inside the repository',
        next,
      ]*.toString()
    }
    assert files(kitchen) == []
  }

  @Test void cooksWithAgentsConnectInEitherOrder() {
    def connectFirst = layoutKitchen('first')
    def tracker = [share: ['clone'], 'session-start': [HOOK]]
    cook(connectFirst, [(CONNECT): [harness: ['claude', 'codex']], (TRACKER): tracker]).commit()
    def beadsFirst = layoutKitchen('second')
    cook(beadsFirst, [(TRACKER): tracker, (CONNECT): [harness: ['claude', 'codex']]]).commit()

    [connectFirst, beadsFirst].each { kitchen ->
      assert new File(kitchen, HOOK).bytes == SCRIPT.bytes
      def ignore = text(kitchen, '.gitignore')
      assert ignore.contains("# io.github.lmarqs.cookbook:beads#tracker --share=clone --session-start=${HOOK}\n" +
      '/.beads/*\n')
      assert ignore.contains('# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex\n')
      assert Files.isSymbolicLink(new File(kitchen, '.claude/settings.json').toPath())
      assert cook(kitchen, [(CONNECT): [harness: ['claude', 'codex']], (TRACKER): tracker]).actions == [:]
    }
    // Only the order of the sections in `.gitignore` and of the recipes in `cook.lock` follows the invocation.
    def unordered = { File kitchen ->
      contents(kitchen).findAll {
        !(it.key in ['.gitignore', 'cook.lock'])
      }
    }
    assert unordered(connectFirst) == unordered(beadsFirst)
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

    assert texts.params*.names == [['share'], ['session-start'], ['instructions']]
    assert texts.params[0].choices == ['team', 'clone']
    assert texts.ingredients == ['io.github.lmarqs.cookbook.beads.Beads']
    assert texts.help.readLines().findAll { it.startsWith('## ') } ==
    ['## API', '## Consumes', '## Produces', '## Connections', '## Run-time effects', '## Re-cooking', '## Traps']
    [
      'Never touches',
      '--session-start=.agents/hooks/session-start.d/beads.sh',
      'The prime text at session start | The person, by `--session-start`',
      '5.5 KB',
      '--check',
    ].each { assert texts.help.contains(it) }
    assert !texts.help.contains('Cook, automatically')
    [
      'mise use -g aqua:steveyegge/beads@1.3.1',
      'bd init --prefix <p> --skip-hooks --skip-agents --non-interactive',
      'bd config set sync.remote <url>',
      'bd config set export.auto true',
      'bd hooks install --beads',
      'git config core.hooksPath .beads/hooks',
      'if command -v bd >/dev/null; then bd prime; fi',
      'bd setup claude',
      'bd setup codex',
      '--instructions=AGENTS.md',
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

  private static String lock(Map<String, List<String>> arguments, String owns) {
    def recorded = arguments.collect { name, values ->
      "      ${JsonOutput.toJson(name)}: ${JsonOutput.toJson(values)}"
    }
    """# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "io.github.lmarqs.cookbook:beads#tracker"
    source: "${TRACKER}"
    sha256: "${sourcesSha256('beads')}"
    arguments:
${recorded.join('\n')}
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
