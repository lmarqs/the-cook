package thecook

import static thecook.Cookbook.COOKBOOK

import org.junit.Test
import thecook.cookbook.RecipeSource

class RecipeTextsTest {
  // The cook and help print a recipe's After cooking text two spaces in.
  private static final String PRINTED_INDENT = '  '
  private static final int WIDTH = 120

  @Test void everyRecipesAfterCookingTextFitsTheTerminalWidthAsPrinted() {
    def recipes = COOKBOOK.listFiles()
        .findAll { it.isDirectory() }
        .collectMany { artifact ->
          artifact.listFiles()
              .findAll { it.name.endsWith('.groovy') }
              .collect { file ->
                [name: "${artifact.name}/${file.name}", texts: RecipeSource.texts(file.text, file.name)]
              }
        }
        .findAll { it.texts?.yieldText != null }

    assert recipes
    assert recipes.collectMany { recipe ->
      recipe.texts.yieldText.readLines()
          .findAll { (PRINTED_INDENT + it).size() > WIDTH }
          .collect { "${recipe.name}: ${it}" }
    } == []
  }
}
