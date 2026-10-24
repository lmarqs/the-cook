package thecook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookLock
import thecook.cookbook.Declaration
import thecook.cookbook.Declared
import thecook.cookbook.Deletion
import thecook.cookbook.Draft
import thecook.cookbook.ExistingSeed
import thecook.cookbook.FileTarget
import thecook.cookbook.KeyTarget
import thecook.cookbook.Ownership
import thecook.cookbook.SectionTarget
import thecook.cookbook.Seeding

class OwnershipTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void withdrawsWhatTheRecipeOwnedAndNoLongerDeclares() {
    def kitchen = kitchen(['old.txt': 'old\n'], [a: [files: ['old.txt']]])
    def draft = new Draft(kitchen)
    def ownership = new Ownership(draft)

    ownership.cook('a', declared(file('new.txt', 'new\n')), 'a.groovy', 'sha', [:])
    ownership.save()

    assert draft.after('old.txt') == null
    assert draft.after('new.txt') == 'new\n'
    assert lock(draft).owned('a') == [new FileTarget('new.txt')]
  }

  @Test void theLaterRecipeTakesOwnershipOfAKeyBothWrite() {
    def draft = new Draft(kitchen())
    def ownership = new Ownership(draft)
    def key = new KeyTarget('settings.json', ['test'])

    ownership.cook('a', declared(new Declaration('A.test', key, 'A')), 'a.groovy', 'sha', [:])
    ownership.cook('b', declared(new Declaration('B.test', key, 'B')), 'b.groovy', 'sha', [:])
    ownership.save()

    assert draft.after('settings.json').contains('"B"')
    assert lock(draft).owned('a') == []
    assert lock(draft).owned('b') == [key]
  }

  @Test void refusesARecipeWhosePartsOverlap() {
    def ownership = new Ownership(new Draft(kitchen()))

    def error = shouldFail(IllegalArgumentException) {
      ownership.cook('a', declared(
          new Declaration('A.whole', new FileTarget('notes.md'), 'text'),
          new Declaration('A.notes', new SectionTarget('notes.md', 'a/notes'), ['line'])), 'a.groovy', 'sha', [:])
    }

    assert error.message == 'A.whole and A.notes both declare notes.md'
  }

  @Test void refusesToDeleteAFileAnotherRecipeOwns() {
    def ownership = new Ownership(new Draft(kitchen(['notes.md': 'notes\n'], [b: [files: ['notes.md']]])))
    def deleting = new Declared()
    deleting.deletes << new Deletion('A.gone', 'notes.md')

    def error = shouldFail(IllegalArgumentException) {
      ownership.cook('a', deleting, 'a.groovy', 'sha', [:])
    }

    assert error.message == 'A.gone deletes notes.md, which b owns'
  }

  @Test void refusesAnExistingSeededFileOnlyTheFirstTimeTheRecipeIsCooked() {
    def seeding = new Declared()
    seeding.seeds << new Seeding('A.seed', 'SEED.md', 'seed\n', true, null)

    def first = new Ownership(new Draft(kitchen(['SEED.md': 'mine\n'])))
    first.cook('a', seeding, 'a.groovy', 'sha', [:])
    def error = shouldFail(IllegalArgumentException) { first.save() }
    assert error.message.startsWith('SEED.md already exists\n')

    def again = new Ownership(new Draft(kitchen(['SEED.md': 'mine\n'], [a: [:]])))
    again.cook('a', seeding, 'a.groovy', 'sha', [:])
    again.save()

    def kept = new Ownership(new Draft(kitchen(['SEED.md': 'mine\n'])), ExistingSeed.KEEP)
    kept.cook('a', seeding, 'a.groovy', 'sha', [:])
    kept.save()
    assert kept.seeds.kept == ['SEED.md'] as Set
  }

  @Test void discardWithdrawsEverythingTheRecipeOwnedAndDeletesALockLeftEmpty() {
    def notes = 'kept\n# cook:begin a/notes\nline\n# cook:end a/notes\n'
    def draft = new Draft(kitchen(['notes.txt': notes, 'out.txt': 'out\n'],
    [a: [files: ['out.txt'], sections: [[file: 'notes.txt', id: 'a/notes']]]]))
    def ownership = new Ownership(draft)

    ownership.discard('a')
    ownership.save()

    assert draft.after('notes.txt') == 'kept\n'
    assert draft.after('out.txt') == null
    assert draft.after(CookLock.PATH) == null
  }

  @Test void reportsTheSha256RecordedForARecipeFromTheSameSource() {
    def ownership = new Ownership(new Draft(kitchen([:], [a: [:]])))

    assert ownership.sha256('a', 'a.groovy') == 'recorded'
    assert ownership.sha256('a', 'other.groovy') == null
    assert ownership.names == ['a'] as Set
  }

  private File kitchen(Map<String, String> files = [:], Map<String, Map<String, List>> recipes = [:]) {
    def kitchen = folder.newFolder()
    files.each { path, text -> new File(kitchen, path).text = text }

    if (recipes) {
      def lock = new CookLock()
      recipes.each { name, owns ->
        def targets = (owns.files ?: []).collect { new FileTarget((String) it) } +
        (owns.sections ?: []).collect { Map it -> new SectionTarget((String) it.file, (String) it.id) }
        lock.record(name, "${name}.groovy", 'recorded', [:], targets)
      }
      new File(kitchen, CookLock.PATH).text = lock.render()
    }

    kitchen
  }

  private static Declared declared(Declaration... parts) {
    def declared = new Declared()
    declared.owned.addAll(parts)
    declared
  }

  private static Declaration file(String path, String text) {
    new Declaration("A.${path}", new FileTarget(path), text)
  }

  private static CookLock lock(Draft draft) {
    CookLock.parse(draft.after(CookLock.PATH))
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
