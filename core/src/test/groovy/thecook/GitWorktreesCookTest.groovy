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
import java.nio.file.attribute.PosixFilePermissions

class GitWorktreesCookTest {
  private static final String WORKTREES = 'io.github.lmarqs.cookbook:git:0.1.0#worktrees'
  private static final File README = new File(COOKBOOK, 'git/templates/README.template.md')

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void ownsTheWorktreesSectionAndSeedsTheReadmeOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, WORKTREES).commit()

    def seeded = ['.worktrees/README.md', '.worktrees/setup', '.worktrees/teardown']
    assert files(kitchen) == ['.gitignore'] + seeded + ['cook.lock']
    assert new File(kitchen, '.gitignore').text == '''\
# cook:begin io.github.lmarqs.cookbook:git#worktrees/worktrees
# io.github.lmarqs.cookbook:git#worktrees
/.worktrees/*
!/.worktrees/README.md
!/.worktrees/setup
!/.worktrees/teardown
# cook:end io.github.lmarqs.cookbook:git#worktrees/worktrees
'''
    assert new File(kitchen, '.worktrees/README.md').bytes == README.bytes
    ['setup', 'teardown'].each { name ->
      def script = new File(kitchen, ".worktrees/${name}")
      assert script.bytes == new File(COOKBOOK, "git/templates/${name}.sh").bytes
      assert Files.getPosixFilePermissions(script.toPath()) == PosixFilePermissions.fromString('rwxr-xr-x')
      def run = new ProcessBuilder(script.path).directory(kitchen).start()
      assert run.waitFor() == 0
      assert run.inputStream.text == ''
    }
    assert new File(kitchen, 'cook.lock').text == """# Written by the-cook; do not edit by hand.
version: 1
recipes:
  - name: "io.github.lmarqs.cookbook:git#worktrees"
    source: "${WORKTREES}"
    sha256: "${sourcesSha256('git')}"
    owns:
      sections:
        - file: ".gitignore"
          id: "io.github.lmarqs.cookbook:git#worktrees/worktrees"
"""
    // No actions is what `./cook` reports as `Already cooked.`.
    assert cook(kitchen, WORKTREES).actions == [:]
  }

  @Test void keepsAHandEditedReadmeAndSetupAndTheTeamsOwnIgnoreRules() {
    def kitchen = kitchen()
    new File(kitchen, '.gitignore').text = '/build/\n'
    cook(kitchen, WORKTREES).commit()
    def readme = new File(kitchen, '.worktrees/README.md')
    readme.text = '# Our worktrees\n\nRun `.worktrees/setup` after `git worktree add`.\n'
    def setup = new File(kitchen, '.worktrees/setup')
    setup.text = '#!/bin/sh\nnpm ci\n'

    assert cook(kitchen, WORKTREES).actions == [:]
    assert setup.text == '#!/bin/sh\nnpm ci\n'
    assert readme.text == '# Our worktrees\n\nRun `.worktrees/setup` after `git worktree add`.\n'
    assert new File(kitchen, '.gitignore').text.startsWith('/build/\n# cook:begin ')
  }

  @Test void gitIgnoresEveryWorktreeAndKeepsTheReadmeAndScripts() {
    def kitchen = kitchen()
    assert ['git', 'init', '-q', kitchen.path].execute().waitFor() == 0
    cook(kitchen, WORKTREES).commit()

    ['.worktrees/x/a', '.worktrees/ralph/run/AGENTS.md'].each { assert ignored(kitchen, it) }
    ['.worktrees/README.md', '.worktrees/setup', '.worktrees/teardown'].each { assert !ignored(kitchen, it) }
  }

  @Test void theReadmeStatesTheRuleTheCommandsTheSetupScriptAndTheTraps() {
    def text = README.text
    [
      '`.worktrees/<branch>`',
      'git worktree add .worktrees/<branch> -b <branch>',
      'git worktree remove .worktrees/<branch>',
      'git branch -d <branch>',
      'git worktree prune',
      '`.worktrees/setup`',
      '`.worktrees/teardown`',
      '(cd .worktrees/<branch> && .worktrees/setup)',
      '(cd .worktrees/<branch> && .worktrees/teardown) && git worktree remove .worktrees/<branch>',
      'installing dependencies',
      'the stash',
    ].each { assert text.contains(it) }
  }

  @Test void statesTheRuleInEachInstructionsFileWhereverTheTeamMovesItAndRemovesItWhenNoLongerNamed() {
    def kitchen = kitchen()
    def agents = new File(kitchen, 'AGENTS.md')
    agents.text = '# Agents\n'
    def readme = new File(kitchen, 'README.md')
    readme.text = '# Project\n'
    def both = [instructions: ['README.md', 'AGENTS.md']]
    def line = 'io.github.lmarqs.cookbook:git#worktrees --instructions=AGENTS.md --instructions=README.md'
    def section = { String opening ->
      '<!-- cook:begin io.github.lmarqs.cookbook:git#worktrees/worktrees -->\n' +
          "<!-- ${opening} -->\n" +
          '- Worktrees: branch `<branch>` is checked out at `.worktrees/<branch>`; ' +
          '`.worktrees/README.md` says how to\n' +
          '  create one ready to run and remove it.\n' +
          '<!-- cook:end io.github.lmarqs.cookbook:git#worktrees/worktrees -->\n'
    }

    cook(kitchen, WORKTREES, both).commit()

    assert agents.text == '# Agents\n' + section(line)
    assert readme.text == '# Project\n' + section(line)
    assert new File(kitchen, '.gitignore').text.readLines()[1] == "# ${line}"
    assert new File(kitchen, 'cook.lock').text.contains('''\
        - file: "AGENTS.md"
          id: "io.github.lmarqs.cookbook:git#worktrees/worktrees"
        - file: "README.md"
          id: "io.github.lmarqs.cookbook:git#worktrees/worktrees"
''')
    assert cook(kitchen, WORKTREES, both).actions == [:]

    agents.text = section(line) + '# Agents\n'
    assert cook(kitchen, WORKTREES, both).actions == [:]

    cook(kitchen, WORKTREES, [instructions: ['AGENTS.md']]).commit()

    assert agents.text == section('io.github.lmarqs.cookbook:git#worktrees --instructions=AGENTS.md') + '# Agents\n'
    assert readme.text == '# Project\n'
  }

  @Test void refusesAnInstructionsFileOutsideTheRepositoryOrMissing() {
    def kitchen = kitchen()
    def outside = { String path ->
      [
        "${WORKTREES}: --instructions=${path} is outside the repository",
        '  a recipe reads and writes only inside the repository',
        '  name the file by its path from the repository root',
      ]*.toString()
    }
    [
      'AGENTS.md': [
        "${WORKTREES}: AGENTS.md section io.github.lmarqs.cookbook:git#worktrees/worktrees",
        '  AGENTS.md is missing',
        '    the section goes into a file the team keeps, and the recipe never creates it',
        '    create the file, or cook the recipe that writes it earlier in the same invocation',
      ]*.toString(),
      '/tmp/AGENTS.md': outside('/tmp/AGENTS.md'),
      'docs/../../AGENTS.md': outside('docs/../../AGENTS.md'),
      ' ': [
        "${WORKTREES}: --instructions needs a path",
        '  name the file by its path from the repository root',
      ]*.toString(),
    ].each { path, lines ->
      def error = shouldFail(IllegalArgumentException) { cook(kitchen, WORKTREES, [instructions: [path]]) }
      assert error.message.readLines() == lines
    }
    assert files(kitchen) == []
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'git/worktrees.groovy').text, 'worktrees.groovy')

    assert texts.params*.names == [['instructions']]
    assert texts.ingredients == ['io.github.lmarqs.cookbook.git.Git']
    assert texts.help.readLines().findAll { it.startsWith('## ') } ==
    ['## API', '## Consumes', '## Produces', '## Connections', '## Re-cooking', '## Traps']
    assert texts.help.contains('Never touches')
    assert texts.yieldText
  }

  @Test void theIgnoreSectionOpensWithTheArgumentLineItIsGiven() {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'git', ['patterns.groovy': '''@help('Ignores build output.')
import io.github.lmarqs.cookbook.git.Git

@ingredient(Git) def git

git.ignoredBy('io.github.lmarqs.cookbook:git#patterns')
git.ignore('/build/', '/build/')
'''])

    cook(kitchen, 'io.github.lmarqs.cookbook:git:0.1.0#patterns').commit()

    assert new File(kitchen, '.gitignore').text == '''\
# cook:begin io.github.lmarqs.cookbook:git#patterns/ignore
# io.github.lmarqs.cookbook:git#patterns
/build/
# cook:end io.github.lmarqs.cookbook:git#patterns/ignore
'''
  }

  private File kitchen() {
    def kitchen = folder.newFolder('kitchen')
    publishDirectory(kitchen, 'git')
    kitchen
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
