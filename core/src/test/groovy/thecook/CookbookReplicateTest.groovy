package thecook

import static org.junit.Assert.assertThrows

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Argument
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookCook
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.CookbookReplicate
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository
import thecook.cookbook.CookLock
import thecook.cookbook.RecipeRequest

class CookbookReplicateTest {
  private static final String NAME = 'acme:text#write'
  private static final String SOURCE = 'acme:text:1.0.0#write'
  private static final String MODEL = '''package acme.text

@Ingredient
class Text {
  String text
  @File(path = 'OUT.md')
  String file() { text }
}
'''
  private static final String RECIPE = '''import acme.text.Text
@param(value = ['value', 'v'], doc = 'The text', many = true) List value
@param(value = ['enabled'], doc = 'Whether enabled', default = ['false']) boolean enabled
@param(value = ['suffix'], doc = 'The suffix', default = ['default']) String suffix
@ingredient(Text) def text
text.text = value.join('\\n') + ':' + enabled + ':' + suffix
'''

  @Rule public TemporaryFolder folder = new TemporaryFolder()
  private File kitchen
  private final Map<String, CookbookArtifact> artifacts = [:]
  private final List<String> loaded = []
  private CookbookRepositories repositories

  @Before void setUp() {
    kitchen = folder.newFolder('kitchen')
    repositories = new CookbookRepositories([
      { Coordinates coordinates ->
        loaded.add(coordinates.artifactAddress)
        artifacts[coordinates.artifactAddress]
      } as CookbookRepository
    ], 'test repository')
    publish()
  }

  @Test void restoresOwnedFilesWithRecordedArgumentsAndLeavesTheLockUnchanged() {
    cook(SOURCE, [
      Argument.separate('v', 'hello = "world"\\path\nnext'),
      Argument.inline('v', ''),
      Argument.inline('v', '--other=value'),
      Argument.bare('enabled'),
    ])
    def output = new File(kitchen, 'OUT.md')
    def original = output.text
    def lock = new File(kitchen, 'cook.lock').text
    assert output.delete()

    def proposal = replicate([NAME])

    assert proposal.changes == ['OUT.md']
    assert proposal.after('OUT.md') == original
    assert proposal.changedSources.empty
    assert !output.exists()
    proposal.commit()
    assert output.text == original
    assert new File(kitchen, 'cook.lock').text == lock
    assert replicate([NAME]).changes.empty
  }

  @Test void loadsTheRecordedVersionEvenWhenTheOperandNamesAnotherVersion() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    publish(RECIPE.replace("default = ['default']", "default = ['different']"), '2.0.0')
    loaded.clear()

    def proposal = replicate(['acme:text:2.0.0#write'])

    assert loaded == ['acme:text:1.0.0']
    assert proposal.recipes*.address == [SOURCE]
    assert proposal.changes.empty
    assert proposal.changedSources.empty
  }

  @Test void refusesAnEmptyOperandList() {
    def error = assertThrows(IllegalArgumentException) { replicate([]) }

    assert error.message == 'replicate needs a recipe\n  name a recipe from cook.lock'
    assert !new File(kitchen, 'cook.lock').exists()
  }

  @Test void refusesDuplicateNamesAcrossOperandFormsBeforeLoadingAnyRecipe() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    loaded.clear()
    def lock = new File(kitchen, 'cook.lock').text

    [[NAME, NAME], [NAME, 'acme:text:9.9.9#write']].each { operands ->
      def error = assertThrows(IllegalArgumentException) { replicate(operands) }
      assert error.message == "${NAME} is replicated twice\n  replicate it once per invocation"
    }
    assert loaded.empty
    assert new File(kitchen, 'cook.lock').text == lock
  }

  @Test void refusesUnknownNamesAndListsKnownNames() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    loaded.clear()

    def error = assertThrows(IllegalArgumentException) { replicate([NAME, 'acme:text#missing']) }

    assert error.message == 'cook.lock lists no recipe acme:text#missing; nothing was written\n' +
    '  replicate cooks only what cook.lock records for a recipe\n' +
    "  check the recipe's name; cook.lock lists ${NAME}"
    assert loaded.empty
  }

  @Test void refusesARecipeWhenThereIsNoLock() {
    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "cook.lock lists no recipe ${NAME}; nothing was written\n" +
    '  replicate cooks only what cook.lock records for a recipe\n' +
    "  check the recipe's name; cook.lock lists none"
  }

  @Test void refusesLegacyEntriesWithoutRecordedArguments() {
    def lock = new CookLock()
    lock.record(NAME, SOURCE, artifacts['acme:text:1.0.0'].sha256, null, [])
    new File(kitchen, 'cook.lock').text = lock.render()

    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "${NAME} has no recorded arguments in cook.lock\n  cook it once with its arguments"
    assert loaded.empty
  }

  @Test void replaysPathRecipesByTheirLockNamesAndPathsInOneBatch() {
    write('recipes/Text.groovy', MODEL)
    write('recipes/first.groovy', RECIPE)
    write('recipes/second.groovy', RECIPE.replace('text.text = ', "text.text = 'second:' + "))
    cook('recipes/first.groovy', [Argument.inline('value', 'first')])
    cook('recipes/second.groovy', [Argument.inline('value', 'second')])
    def original = new File(kitchen, 'OUT.md').text
    def lock = new File(kitchen, 'cook.lock').text
    assert new File(kitchen, 'OUT.md').delete()

    def proposal = replicate(['recipes/first', './recipes/second.groovy'])

    assert proposal.recipes*.address == ['recipes/first.groovy', 'recipes/second.groovy']
    assert proposal.after('OUT.md') == original
    proposal.commit()
    assert new File(kitchen, 'cook.lock').text == lock
    assert replicate(['recipes/first', 'recipes/second']).changes.empty
    def error = assertThrows(IllegalArgumentException) {
      replicate(['recipes/first', './recipes/first.groovy'])
    }
    assert error.message == 'recipes/first is replicated twice\n  replicate it once per invocation'
  }

  @Test void reportsChangedCoordinatesAndCooksTheirCurrentSource() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    def recorded = artifacts['acme:text:1.0.0'].sha256
    publish(RECIPE.replace('text.text = ', "text.text = 'changed:' + "))

    def proposal = replicate([NAME])

    assert proposal.after('OUT.md') == 'changed:original:false:default'
    assert proposal.changedSources*.address == [SOURCE]
    assert proposal.changedSources*.recorded == [recorded]
    assert proposal.changedSources*.current == [artifacts['acme:text:1.0.0'].sha256]
    proposal.commit()
    assert replicate([NAME]).changedSources.empty
  }

  @Test void reportsChangedPathSourcesAndCooksTheirCurrentSource() {
    write('recipes/Text.groovy', MODEL)
    write('recipes/write.groovy', RECIPE)
    cook('recipes/write.groovy', [Argument.inline('value', 'original')])
    def recorded = CookLock.parse(new File(kitchen, 'cook.lock').text)
        .sha256('recipes/write', 'recipes/write.groovy')
    write('recipes/write.groovy', RECIPE.replace('text.text = ', "text.text = 'changed:' + "))

    def proposal = replicate(['recipes/write'])

    assert proposal.after('OUT.md') == 'changed:original:false:default'
    assert proposal.changedSources*.address == ['recipes/write.groovy']
    assert proposal.changedSources*.recorded == [recorded]
    assert proposal.changedSources.first().current != recorded
    proposal.commit()
    assert CookLock.parse(new File(kitchen, 'cook.lock').text).sha256('recipes/write', 'recipes/write.groovy') ==
    proposal.changedSources.first().current
    assert replicate(['recipes/write']).changedSources.empty
  }

  @Test void addsRecordedArgumentsAndANextStepWhenAnArgumentIsNoLongerKnown() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    def lock = new File(kitchen, 'cook.lock').text
    publish(RECIPE.replace("['value', 'v']", "['line']"))

    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "${SOURCE}: Unknown argument --value\n  its arguments are --line, --enabled, --suffix\n" +
    '  arguments recorded in cook.lock: --value=original\n  cook it with its arguments'
    assert new File(kitchen, 'cook.lock').text == lock
  }

  @Test void addsRecordedArgumentsWhenANewRequiredParameterIsMissing() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    publish(RECIPE + "\n@param(value = ['required'], doc = 'A new requirement') String required\n")

    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "${SOURCE}: Missing --required: A new requirement\n" +
    '  arguments recorded in cook.lock: --value=original\n  cook it with its arguments'
  }

  @Test void acceptsRecordedEmptyArgumentsAndReportsThemWhenANewParameterIsRequired() {
    publish("@help('Does nothing.')\nimport java.util.List\n")
    cook(SOURCE, [])
    assert replicate([NAME]).changes.empty
    publish("@param(value = ['required'], doc = 'A new requirement') String required\n")

    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "${SOURCE}: Missing --required: A new requirement\n" +
    '  arguments recorded in cook.lock: \n  cook it with its arguments'
  }

  @Test void refusesUnreadableArgumentsWhichCookingAgainOrDiscardingReplaces() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    def lock = new File(kitchen, 'cook.lock')
    def recorded = lock.text
    lock.text = recorded.replace('      "value": ["original"]', '      "value": "original"')

    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "cook.lock records the arguments of ${NAME} in a form Cook cannot read\n" +
    '  each argument is a name with a list of values, such as "mode": ["fast"]\n' +
    "  cook ${NAME} again with its arguments, or discard it"
    cook(SOURCE, [Argument.inline('value', 'original')])
    assert lock.text == recorded
    lock.text = recorded.replace('      "value": ["original"]', '      "value": "original"')
    new CookbookDiscard(kitchen).propose([NAME]).commit()
    assert !lock.exists()
  }

  @Test void leavesRecipeBodyFailuresWithoutAnArgumentBindingHint() {
    cook(SOURCE, [Argument.inline('value', 'original')])
    publish(RECIPE + "\nthrow new IllegalStateException('cannot cook')\n")

    def error = assertThrows(IllegalArgumentException) { replicate([NAME]) }

    assert error.message == "${SOURCE}: cannot cook"
  }

  private void cook(String operand, List<Argument> arguments) {
    new CookbookCook(kitchen, repositories).propose([new RecipeRequest(operand, arguments)]).commit()
  }

  private def replicate(List<String> operands) {
    new CookbookReplicate(kitchen, repositories).propose(operands)
  }

  private void publish(String recipe = RECIPE, String version = '1.0.0') {
    def coordinates = Coordinates.parse("acme:text:${version}")
    artifacts[coordinates.artifactAddress] = new CookbookArtifact(coordinates, [
      'Text.groovy': MODEL.getBytes('UTF-8'),
      'write.groovy': recipe.getBytes('UTF-8'),
    ])
  }

  private void write(String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
  }
}
