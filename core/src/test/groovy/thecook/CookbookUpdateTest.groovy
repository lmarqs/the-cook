package thecook

import static org.junit.Assert.assertThrows

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository
import thecook.cookbook.CookbookUpdate
import thecook.cookbook.CookLock

class CookbookUpdateTest {
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
@ingredient(Text) def text
text.text = value.join(':')
'''

  @Rule public TemporaryFolder folder = new TemporaryFolder()
  private File kitchen

  @Before void setUp() {
    kitchen = folder.newFolder('kitchen')
    publish('1.0.0')
  }

  @Test void proposesTheNewestCompatibleReleaseWithRecordedArgumentsAndUpdatesItsLockIdentity() {
    def arguments = [value: ['hello = "world"\\path\nnext', '', '--other=value']]
    Kitchens.cook(kitchen, SOURCE, arguments).commit()
    def originalLock = lockFile().text
    publish('1.1.0')
    publish('1.9.0', RECIPE.replace('text.text = ', "text.text = 'new:' + "))
    publish('2.0.0')
    def update = updater()

    def plan = update.plan([], false)

    assert plan*.name == [NAME]
    assert plan*.known == [true]
    assert plan*.from == ['1.0.0']
    assert plan*.to == ['1.9.0']
    assert plan*.newestOverall == ['2.0.0']
    def proposal = update.propose(plan)
    def target = 'acme:text:1.9.0#write'
    assert proposal.recipes*.address == [target]
    assert proposal.after('OUT.md') == 'new:' + arguments.value.join(':')
    assert lockFile().text == originalLock
    assert new File(kitchen, 'OUT.md').text == arguments.value.join(':')
    def proposedLock = CookLock.parse(proposal.after('cook.lock'))
    assert proposedLock.source(NAME) == target
    assert proposedLock.arguments(NAME) == arguments
    assert proposedLock.sha256(NAME, target) ==
    Kitchens.repositories(kitchen).artifact(Coordinates.parse(target)).sha256
    assert proposedLock.sha256(NAME, SOURCE) == null
    proposal.commit()
    assert lockFile().text == proposedLock.render()
  }

  @Test void holdsAtTheMajorBoundaryUnlessMajorIsRequested() {
    cook()
    publish('2.0.0')
    def update = updater()

    def compatible = update.plan([NAME], false)

    assert compatible*.known == [true]
    assert compatible*.to == [null]
    assert compatible*.newestOverall == ['2.0.0']
    assert update.propose(compatible).recipes.empty
    assert update.propose(compatible).changes.empty
    def major = update.plan([NAME], true)
    assert major*.known == [true]
    assert major*.to == ['2.0.0']
    assert major*.newestOverall == [null]
    assert update.propose(major).recipes*.address == ['acme:text:2.0.0#write']
  }

  @Test void leavesACurrentRecipeUncookedEvenWhenItsOutputIsMissing() {
    cook()
    assert new File(kitchen, 'OUT.md').delete()
    def update = updater()

    def plan = update.plan([], false)

    assert plan*.known == [true]
    assert plan*.to == [null]
    assert plan*.newestOverall == [null]
    assert update.propose(plan).changes.empty
    assert update.propose(plan).recipes.empty
    assert !new File(kitchen, 'OUT.md').exists()
  }

  @Test void plansInLockOrderWithoutNamesAndInOperandOrderWithNamesSkippingPathRecipes() {
    publish('1.0.0', RECIPE, 'other')
    publish('1.1.0', RECIPE, 'other')
    Kitchens.cook(kitchen, 'acme:other:1.0.0#write', [value: ['first']]).commit()
    cook()
    publish('1.1.0')
    new File(kitchen, 'local.groovy').text = "@help('Local recipe')\nimport java.util.List\n"
    Kitchens.cook(kitchen, 'local.groovy').commit()
    def update = updater()

    def plan = update.plan([], false)

    assert plan*.name == ['acme:other#write', NAME]
    assert update.propose(plan).recipes*.address == ['acme:other:1.1.0#write', 'acme:text:1.1.0#write']
    def named = update.plan([NAME, 'acme:other#write'], false)
    assert named*.name == [NAME, 'acme:other#write']
    assert update.propose(named).recipes*.address == ['acme:text:1.1.0#write', 'acme:other:1.1.0#write']
    assert lock().source('local') == 'local.groovy'
  }

  @Test void refusesARenamedArgumentAndWritesNoneOfTheBatch() {
    publish('1.0.0', RECIPE, 'other')
    Kitchens.cook(kitchen, 'acme:other:1.0.0#write', [value: ['first']]).commit()
    cook()
    publish('1.1.0', RECIPE.replace('text.text = ', "text.text = 'new:' + "), 'other')
    publish('1.1.0', RECIPE.replace("['value', 'v']", "['line']"))
    def before = lockFile().text
    def output = new File(kitchen, 'OUT.md').text
    def update = updater()

    def error = assertThrows(IllegalArgumentException) { update.propose(update.plan([], false)) }

    assert error.message == 'recipe 2 (acme:text:1.1.0#write): Unknown argument --value\n' +
    '  its arguments are --line\n  arguments recorded in cook.lock: --value=original\n' +
    '  read `./cook help acme:text:1.1.0#write` and cook it with its new arguments'
    assert lockFile().text == before
    assert new File(kitchen, 'OUT.md').text == output
  }

  @Test void reportsANewRequiredArgumentWithAcceptedArgumentsAndRecordedLine() {
    cook()
    publish('1.1.0', RECIPE + "\n@param(value = ['required'], doc = 'A new requirement') String required\n")
    def update = updater()
    def before = lockFile().text

    def error = assertThrows(IllegalArgumentException) { update.propose(update.plan([], false)) }

    assert error.message == 'acme:text:1.1.0#write: Missing --required: A new requirement\n' +
    '  its arguments are --value, --v, --required\n  arguments recorded in cook.lock: --value=original\n' +
    '  read `./cook help acme:text:1.1.0#write` and cook it with its new arguments'
    assert lockFile().text == before
  }

  @Test void refusesDuplicateNames() {
    cook()

    def error = assertThrows(IllegalArgumentException) { updater().plan([NAME, NAME], false) }

    assert error.message == "${NAME} is named twice\n  name it once per invocation"
    assert lock().source(NAME) == SOURCE
  }

  @Test void refusesNamesAbsentFromTheLockAndListsItsNames() {
    cook()

    def error = assertThrows(IllegalArgumentException) { updater().plan(['acme:text#missing'], false) }

    assert error.message == 'cook.lock lists no recipe acme:text#missing; nothing was written\n' +
    '  only a recipe cook.lock records can be updated\n' +
    "  check the recipe's name; cook.lock lists ${NAME}"
  }

  @Test void refusesPathRecipesNamedExplicitly() {
    new File(kitchen, 'local.groovy').text = "@help('Local recipe')\nimport java.util.List\n"
    Kitchens.cook(kitchen, 'local.groovy').commit()

    def error = assertThrows(IllegalArgumentException) { updater().plan(['local'], false) }

    assert error.message.contains('local has no version to update')
  }

  @Test void refusesVersionedCoordinatesAsReplicateDoes() {
    cook()

    def error = assertThrows(IllegalArgumentException) { updater().plan([SOURCE], false) }

    assert error.message == "${SOURCE} is not a name cook.lock writes\n  write ${NAME}"
  }

  @Test void refusesAMissingLockWithAndWithoutNames() {
    [[], [NAME]].each { names ->
      def error = assertThrows(IllegalArgumentException) { updater().plan(names, false) }

      assert error.message == 'there is no cook.lock\n  cook.lock records the recipes to update\n' +
      '  run in the kitchen that holds it'
    }
  }

  @Test void refusesAMovingRecipeWithoutRecordedArgumentsButLeavesACurrentOneAlone() {
    def legacy = new CookLock()
    legacy.record(NAME, SOURCE, 'old-sha', null, [])
    lockFile().text = legacy.render()
    def update = updater()
    assert update.propose(update.plan([], false)).changes.empty
    publish('1.1.0')

    def error = assertThrows(IllegalArgumentException) { update.propose(update.plan([], false)) }

    assert error.message == "${NAME} has no recorded arguments in cook.lock\n  cook it once with its arguments"
    assert lockFile().text == legacy.render()
  }

  @Test void movesARecipeWithRecordedEmptyArguments() {
    def recipe = "@help('Needs no arguments')\nimport java.util.List\n"
    publish('1.0.0', recipe)
    Kitchens.cook(kitchen, SOURCE).commit()
    publish('1.1.0', recipe)
    def update = updater()

    def proposal = update.propose(update.plan([]))

    assert proposal.recipes*.address == ['acme:text:1.1.0#write']
    proposal.commit()
    assert lock().arguments(NAME) == [:]
    assert lock().source(NAME) == 'acme:text:1.1.0#write'
  }

  @Test void reportsUnknownListingsInTheWholeScopeButRefusesOneNamedExplicitly() {
    cook()
    def unknown = [
      artifact: { Coordinates coordinates -> throw new AssertionError("loaded ${coordinates}") },
      versions: { String group, String artifact -> null },
    ] as CookbookRepository
    def update = new CookbookUpdate(kitchen, new CookbookRepositories([unknown], 'unknown repository'))

    def plan = update.plan([], false)

    assert plan*.name == [NAME]
    assert plan*.known == [false]
    assert plan*.unlisted == [[unknown]]
    assert plan*.to == [null]
    assert update.propose(plan).recipes.empty
    assert update.propose(plan).changes.empty
    def error = assertThrows(IllegalArgumentException) { update.plan([NAME], false) }
    assert error.message.contains("${NAME} has no known releases")
    assert lock().source(NAME) == SOURCE
  }

  private CookbookUpdate updater() {
    new CookbookUpdate(kitchen, Kitchens.repositories(kitchen))
  }

  private void cook() {
    Kitchens.cook(kitchen, SOURCE, [value: ['original']]).commit()
  }

  private void publish(String version, String recipe = RECIPE, String artifact = 'text') {
    Kitchens.publish(kitchen, "acme:${artifact}:${version}", ['Text.groovy': MODEL, 'write.groovy': recipe])
  }

  private File lockFile() {
    new File(kitchen, 'cook.lock')
  }

  private CookLock lock() {
    CookLock.parse(lockFile().text)
  }
}
