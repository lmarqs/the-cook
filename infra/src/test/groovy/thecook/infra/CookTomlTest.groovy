package thecook.infra

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookRepositories

import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class CookTomlTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void readsTheArtifactFromTheFirstListedRepositoryHoldingIt() {
    def kitchen = folder.newFolder('kitchen')
    publish(new File(kitchen, 'second'), [
      'acme/notes/README.md': 'Notes for the repository.',
      'acme/notes/notes.groovy': 'second notes',
      'acme/notes/review.groovy': 'review',
      'acme/notes/Notes.groovy': 'class Notes {}',
      'acme/notes/templates/NOTES.md': 'resource',
      'acme/other/other.groovy': 'another artifact',
    ])
    new File(kitchen, 'cook.toml').text = 'repositories = ["first", "second"]\n'
    new File(kitchen, 'first').mkdirs()

    def artifact = repositories(kitchen).artifact(Coordinates.parse('acme:notes:1.2.3'))

    assert artifact.readme == 'Notes for the repository.'
    assert artifact.recipes == ['notes', 'review']
    assert artifact.models == ['Notes']
    assert artifact.recipeSource('notes') == 'second notes'
    def error = shouldFail(IllegalArgumentException) { artifact.recipeSource('other') }
    assert error.message == 'No recipe other in acme:notes:1.2.3\n  its recipes are: notes, review'
  }

  @Test void hashesTheArtifactsSourcesWhateverTheJarsEntryOrder() {
    def kitchen = folder.newFolder('kitchen')
    def files = [
      'acme/notes/notes.groovy': 'notes',
      'acme/notes/templates/a b.md': 'resource',
      'acme/notes/README.md': '',
    ]
    publish(new File(kitchen, 'forward'), files)
    publish(new File(kitchen, 'reverse'), files.entrySet()
        .toList()
        .reverse()
        .collectEntries())
    def coordinates = Coordinates.parse('acme:notes:1.2.3')
    new File(kitchen, 'cook.toml').text = 'repositories = ["forward"]\n'
    def forward = repositories(kitchen).artifact(coordinates).sha256
    new File(kitchen, 'cook.toml').text = 'repositories = ["reverse"]\n'
    def reverse = repositories(kitchen).artifact(coordinates).sha256

    def lines = ['README.md', 'notes.groovy', 'templates/a b.md'].collect { path ->
      def text = files['acme/notes/' + path]
      "${digest(text.getBytes('UTF-8'))}  ${path}\n"
    }
    assert forward == digest(lines.join().getBytes('UTF-8'))
    assert reverse == forward
  }

  @Test void readsAnArtifactFromACookbookSourceDirectoryAtAnyVersion() {
    def kitchen = folder.newFolder('kitchen')
    source(new File(folder.root, 'book'), [
      'src/notes/notes.groovy': 'source notes',
      'src/notes/Note.groovy': 'class Note {}',
      'src/notes/templates/NOTES.md': 'resource',
    ])
    new File(kitchen, 'cook.toml').text = 'repositories = ["../book"]\n'

    def artifact = repositories(kitchen).artifact(Coordinates.parse('acme:notes:1.2.3'))

    assert artifact.recipes == ['notes']
    assert artifact.models == ['Note']
    assert artifact.recipeSource('notes') == 'source notes'
    assert repositories(kitchen).artifact(Coordinates.parse('acme:notes:9.9.9')).sha256 == artifact.sha256
    assert repositories(kitchen).repositories*.class == [CookbookDirectory]
  }

  @Test void aCookbookSourceDirectoryServesOnlyItsGroupsArtifacts() {
    def kitchen = folder.newFolder('kitchen')
    source(new File(kitchen, 'book'), ['src/other/other.groovy': 'source other'])
    source(new File(kitchen, 'elsewhere'), ['src/notes/notes.groovy': 'other group notes'], 'other')
    publish(new File(kitchen, 'repository'), ['acme/notes/notes.groovy': 'published notes'])
    new File(kitchen, 'cook.toml').text = 'repositories = ["book", "elsewhere", "repository"]\n'

    def artifact = repositories(kitchen).artifact(Coordinates.parse('acme:notes:1.2.3'))

    assert artifact.recipeSource('notes') == 'published notes'
  }

  @Test void missingArtifactNamesTheRepositoriesSearched() {
    def kitchen = folder.newFolder('kitchen')
    new File(kitchen, 'cook.toml').text = 'repositories = ["repository"]\n'
    def error = shouldFail(IllegalArgumentException) {
      repositories(kitchen).artifact(Coordinates.parse('acme:notes:1.2.4'))
    }
    assert error.message
    .startsWith("acme:notes:1.2.4 not found in the repositories in ${new File(kitchen, 'cook.toml')}")
    assert error.message.endsWith('publish it there, or list its repository in cook.toml')
  }

  @Test void readsARepositoryWhosePathHoldsASpace() {
    def kitchen = folder.newFolder('kitchen')
    publish(new File(kitchen, 'my repository'), ['acme/notes/notes.groovy': 'published notes'])
    new File(kitchen, 'cook.toml').text = 'repositories = ["my repository"]\n'

    def artifact = repositories(kitchen).artifact(Coordinates.parse('acme:notes:1.2.3'))

    assert artifact.recipeSource('notes') == 'published notes'
  }

  @Test void withoutCookTomlTheRepositoryIsMavenCentral() {
    assert repositories(folder.newFolder('kitchen')).repositories*.toString() == [CookToml.CENTRAL.toString()]
  }

  @Test void readsPathsStartingWithTildeSlashFromTheUsersHome() {
    def kitchen = folder.newFolder('kitchen')
    def home = folder.newFolder('home')
    new File(kitchen, 'cook.toml').text = 'repositories = ["~/.m2/repository", "~user/x"]\n'

    assert repositories(kitchen).repositories*.toString() == [
      new File(home, '.m2/repository').toURI()
      .toString()
      .replaceAll(/\/?$/, '/'),
      new File(kitchen, '~user/x').toURI()
      .toString()
      .replaceAll(/\/?$/, '/'),
    ]
  }

  @Test void refusesRepositoriesThatAreNeitherHttpsNorPaths() {
    def kitchen = folder.newFolder('kitchen')
    new File(kitchen, 'cook.toml').text = 'repositories = ["http://example.com/maven2"]\n'
    def error = shouldFail(IllegalArgumentException) { repositories(kitchen) }
    assert error.message.endsWith('repository http://example.com/maven2 must be an https:// URL or a path')
    [
      'repositories = "repository"\n',
      '[[repositories]]\ngit = "https://github.com/acme/book"\ndir = "cookbook"\n',
    ].each { manifest ->
      new File(kitchen, 'cook.toml').text = manifest
      error = shouldFail(IllegalArgumentException) { repositories(kitchen) }
      assert error.message ==
      "${new File(kitchen, 'cook.toml')}: repositories must be a list of https:// URLs and paths"
    }
  }

  private static void publish(File repository, Map<String, String> files) {
    def jar = new File(repository, 'acme/notes/1.2.3/notes-1.2.3.jar')
    jar.parentFile.mkdirs()
    new JarOutputStream(jar.newOutputStream()).withCloseable { out ->
      files.each { path, text ->
        out.putNextEntry(new JarEntry(path))
        out.write(text.getBytes('UTF-8'))
        out.closeEntry()
      }
    }
  }

  private static void source(File cookbook, Map<String, String> files, String group = 'acme') {
    cookbook.mkdirs()
    new File(cookbook, 'cookbook.toml').text = "group = \"${group}\"\n"
    files.each { path, text ->
      def file = new File(cookbook, path)
      file.parentFile.mkdirs()
      file.text = text
    }
  }

  private CookbookRepositories repositories(File kitchen) {
    CookToml.repositories(kitchen, new File(folder.root, 'home'))
  }

  private static String digest(byte[] bytes) {
    MessageDigest.getInstance('SHA-256')
        .digest(bytes)
        .encodeHex()
        .toString()
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
