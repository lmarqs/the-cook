package thecook

import org.junit.Test
import thecook.cookbook.RecipeSource
import thecook.model.RecipeParam

class RecipeSourceTest {
  @Test void readsTextsAndDeclarationsWithoutResolvingImports() {
    def texts = RecipeSource.texts('''@help(\'\'\'
Connects each selected harness.
\'\'\')
@yield(\'\'\'
Review the rendered permissions.
\'\'\')
import acme.agents.Agents
import acme.agents.Claude

@param(value = ['harness'], choices = ['claude', 'codex'], many = true, sorted = true,
    doc = 'Harnesses to connect')
    String[] harness
@param(value = ['worktree-root'], default = [], path = true,
    doc = "The directory holding Claude Code's worktrees")
    String worktreeRoot

@ingredient(Agents) def agents
@ingredient(Claude) def claude
''', 'connect.groovy')
    assert texts.help == 'Connects each selected harness.'
    assert texts.confirm == null
    assert texts.yieldText == 'Review the rendered permissions.'
    assert texts.params.first() == new RecipeParam(['harness'], 'String[]', 'Harnesses to connect', null,
    ['claude', 'codex'], true)
    assert texts.params*.names == [['harness'], ['worktree-root']]
    assert texts.ingredients == ['acme.agents.Agents', 'acme.agents.Claude']
  }

  @Test void readsAnEmptyDefaultAsAnOptionalParamWithNoValue() {
    def texts = RecipeSource.texts('''@help('Records one requirement.')
import acme.python.Python

@param(value = ['dependency'], doc = 'One requirement') String dependency
@param(value = ['group'], default = [], doc = 'Optional dependency group') String group
''', 'dependency.groovy')
    assert texts.params[1] == new RecipeParam(['group'], 'String', 'Optional dependency group', '', [], false)
  }

  @Test void keepsEscapedLineBreaksInTexts() {
    def texts = RecipeSource.texts('''@help(\'\'\'
./cook acme:github:1.0.0#aws-oidc --job=deploy \\\\
       --region=us-east-1
\'\'\')
@confirm(\'\'\'
The selected job's next run assumes the selected IAM role.
\'\'\')
import acme.aws.Aws
''', 'aws-oidc.groovy')
    assert texts.confirm == "The selected job's next run assumes the selected IAM role."
    assert texts.help.contains('--job=deploy \\\n')
  }

  @Test void recipeWithoutHelpHasNoTexts() {
    assert RecipeSource.texts('import a.B\nreturn { k, b, o -> }\n', 'plain.groovy') == null
    assert RecipeSource.texts('return { k, b, o -> }\n', 'plain.groovy') == null
  }
}
