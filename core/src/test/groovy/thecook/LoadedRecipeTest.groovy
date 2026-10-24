package thecook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Argument
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.Draft
import thecook.cookbook.FileTarget
import thecook.cookbook.RecipeLoader

class LoadedRecipeTest {
  private static final String NOTES_MODEL = '''package acme.notes

@Ingredient
class Notes {
  String text
  @File(path = 'NOTES.md')
  String notes() { text == null ? null : "${resource('header.txt')}${current('IN.md')}${text}\\n" }
}
'''

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void returnsWhatTheModelsDeclareAndTheArgumentsGivenWithoutChangingTheDraft() {
    def kitchen = kitchen()
    def draft = new Draft(kitchen)
    def recipe = new RecipeLoader(kitchen, new CookbookRepositories([], 'none')).load('recipes/notes.groovy')

    def evaluation = recipe.evaluate([Argument.inline('text', 'mine')], draft)

    assert evaluation.declared.owned*.target == [new FileTarget('NOTES.md')]
    assert evaluation.declared.owned*.value == ['Header\nIn\nmine\n']
    assert evaluation.arguments == [text: ['mine']]
    assert draft.changes == []
  }

  @Test void aModelReadsTheDraftAsTheRecipesBeforeItLeftIt() {
    def kitchen = kitchen()
    def draft = new Draft(kitchen)
    draft.write('IN.md', 'Earlier\n')
    def recipe = new RecipeLoader(kitchen, new CookbookRepositories([], 'none')).load('recipes/notes.groovy')

    def evaluation = recipe.evaluate([], draft)

    assert evaluation.declared.owned*.value == ['Header\nEarlier\ndefault\n']
    assert evaluation.arguments == [:]
  }

  private File kitchen() {
    def kitchen = folder.newFolder()
    write(kitchen, 'IN.md', 'In\n')
    write(kitchen, 'recipes/Notes.groovy', NOTES_MODEL)
    write(kitchen, 'recipes/header.txt', 'Header\n')
    write(kitchen, 'recipes/notes.groovy', '''import acme.notes.Notes
@param(value = ['text'], default = ['default'], doc = 'The text') String text
@ingredient(Notes) def notes
notes.text = text
''')
    kitchen
  }

  private static void write(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
  }
}
