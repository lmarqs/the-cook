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

class GitWorktreesCookTest {
  private static final String WORKTREES = 'io.github.lmarqs.cookbook:git:0.1.0#worktrees'
  private static final File README = new File(COOKBOOK, 'git/templates/README.template.md')

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void ownsTheWorktreesSectionAndSeedsTheReadmeOnAnEmptyKitchen() {
    def kitchen = kitchen()

    cook(kitchen, WORKTREES).commit()

    assert files(kitchen) == ['.gitignore', '.worktrees/README.md', 'cook.lock']
    assert new File(kitchen, '.gitignore').text == '''\
# cook:begin io.github.lmarqs.cookbook:git#worktrees/worktrees
# io.github.lmarqs.cookbook:git#worktrees
/.worktrees/*
!/.worktrees/README.md
# cook:end io.github.lmarqs.cookbook:git#worktrees/worktrees
'''
    assert new File(kitchen, '.worktrees/README.md').bytes == README.bytes
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

  @Test void keepsAHandEditedReadmeAndTheTeamsOwnIgnoreRules() {
    def kitchen = kitchen()
    new File(kitchen, '.gitignore').text = '/build/\n'
    cook(kitchen, WORKTREES).commit()
    def readme = new File(kitchen, '.worktrees/README.md')
    readme.text = '# Our worktrees\n\nRun `.worktrees/setup` after `git worktree add`.\n'

    assert cook(kitchen, WORKTREES).actions == [:]
    assert readme.text == '# Our worktrees\n\nRun `.worktrees/setup` after `git worktree add`.\n'
    assert new File(kitchen, '.gitignore').text.startsWith('/build/\n# cook:begin ')
  }

  @Test void gitIgnoresEveryWorktreeAndKeepsTheReadme() {
    def kitchen = kitchen()
    assert ['git', 'init', '-q', kitchen.path].execute().waitFor() == 0
    cook(kitchen, WORKTREES).commit()

    ['.worktrees/x/a', '.worktrees/ralph/run/AGENTS.md', '.worktrees/setup'].each { assert ignored(kitchen, it) }
    assert !ignored(kitchen, '.worktrees/README.md')
  }

  @Test void theReadmeStatesTheRuleTheCommandsTheSetupScriptAndTheTraps() {
    def text = README.text
    [
      '`.worktrees/<branch>`',
      'git worktree add .worktrees/<branch> -b <branch>',
      'git worktree remove .worktrees/<branch>',
      'git worktree prune',
      '`.worktrees/setup`',
      'mise trust',
      'the stash',
    ].each { assert text.contains(it) }
  }

  @Test void theHelpFollowsTheRecipeDocumentationStandard() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'git/worktrees.groovy').text, 'worktrees.groovy')

    assert texts.params == []
    assert texts.ingredients == ['io.github.lmarqs.cookbook.git.Git']
    assert texts.help.readLines().findAll { it.startsWith('#') } ==
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

  // The kitchen's files, leaving out the published repository and Git's own directory.
  private static List<String> files(File kitchen) {
    Kitchens.files(kitchen).findAll { !it.startsWith('repository/') && !it.startsWith('.git/') }
  }
}
