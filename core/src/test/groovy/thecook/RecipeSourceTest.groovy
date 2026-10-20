package thecook

import org.junit.Test
import thecook.cookbook.RecipeSource
import thecook.model.RecipeParam

class RecipeSourceTest {
  private static final File COOKBOOK = new File(System.getProperty('cook.cookbook'))

  @Test void readsTextsAndDeclarationsWithoutResolvingImports() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'agents/connect.groovy').text, 'connect.groovy')
    assert texts.help.startsWith('Connects each selected harness to the shared agent layout')
    assert texts.confirm == null
    assert texts.yieldText.startsWith('Review the rendered permissions against each harness')
    assert texts.params == [
      new RecipeParam(['harness'], 'String[]', 'Harnesses to connect to the shared layout', null,
      ['claude', 'codex'], true)
    ]
    assert texts.ingredients == [
      'io.github.lmarqs.cookbook.agents.Agents',
      'io.github.lmarqs.cookbook.agents.Claude',
      'io.github.lmarqs.cookbook.agents.Codex',
    ]
  }

  @Test void readsAnEmptyDefaultAsAnOptionalParamWithNoValue() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'agents/claude-settings.groovy').text, 'claude-settings.groovy')
    assert texts.params == [
      new RecipeParam(['settings'], 'String',
      'JSON file whose top-level keys are written into .agents/adapters/claude/settings.json', '', [], false)
    ]
  }

  @Test void keepsEscapedLineBreaksInTexts() {
    def texts = RecipeSource.texts(new File(COOKBOOK, 'github/aws-oidc.groovy').text, 'aws-oidc.groovy')
    assert texts.confirm.startsWith("The selected job's next run assumes the selected IAM role")
    assert texts.help.contains('--job=deploy \\\n')
  }

  @Test void recipeWithoutHelpHasNoTexts() {
    assert RecipeSource.texts('import a.B\nreturn { k, b, o -> }\n', 'plain.groovy') == null
    assert RecipeSource.texts('return { k, b, o -> }\n', 'plain.groovy') == null
  }
}
