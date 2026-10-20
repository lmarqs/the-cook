package thecook.infra

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookArtifact

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CookbookDirectoryTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void servesAnArtifactAtAnyVersionFromItsFilesLeavingHiddenOnesOut() {
    def book = cookbook([
      'notes/notes.groovy': 'notes',
      'notes/Note.groovy': 'class Note {}',
      'notes/templates/NOTES.md': 'resource',
      'notes/.DS_Store': 'hidden',
      'notes/.cache/x.groovy': 'hidden',
    ])

    def artifact = book.artifact(Coordinates.parse('acme:notes:1.2.3'))

    assert artifact.recipes == ['notes']
    assert artifact.models == ['Note']
    assert artifact.resources('templates') == ['NOTES.md': 'resource']
    assert artifact.resource('.DS_Store') == null
    assert book.artifact(Coordinates.parse('acme:notes:9.9.9')).sha256 == artifact.sha256
    def jar = zip([
      'acme/notes/notes.groovy': 'notes',
      'acme/notes/Note.groovy': 'class Note {}',
      'acme/notes/templates/NOTES.md': 'resource',
    ])
    def coordinates = Coordinates.parse('acme:notes:1.2.3')
    assert artifact.sha256 == CookbookArtifact.fromZip(coordinates, jar, coordinates.packagePath).sha256
  }

  @Test void holdsOnlyItsGroupsArtifacts() {
    def book = cookbook(['notes/notes.groovy': 'notes', 'build/build.groovy': 'packaging output'])

    assert book.artifact(Coordinates.parse('other:notes:1.2.3')) == null
    assert book.artifact(Coordinates.parse('acme:missing:1.2.3')) == null
    assert book.artifact(Coordinates.parse('acme:build:1.2.3')) == null
    assert book.toString() == new File(folder.root, 'book').toURI().toString()
  }

  @Test void refusesACookbookTomlWithoutAGroup() {
    def book = folder.newFolder('book')
    new File(book, 'cookbook.toml').text = 'api = 1\n'

    def error = shouldFail(IllegalArgumentException) { new CookbookDirectory(book.toURI()) }

    assert error.message == "${new File(book, 'cookbook.toml')} must name the cookbook's group"
  }

  private CookbookDirectory cookbook(Map<String, String> files) {
    def book = folder.newFolder('book')
    new File(book, 'cookbook.toml').text = 'group = "acme"\n'
    files.each { path, text ->
      def file = new File(book, path)
      file.parentFile.mkdirs()
      file.text = text
    }
    new CookbookDirectory(book.toURI())
  }

  private static byte[] zip(Map<String, String> files) {
    def bytes = new ByteArrayOutputStream()
    new ZipOutputStream(bytes).withCloseable { out ->
      files.each { path, text ->
        out.putNextEntry(new ZipEntry(path))
        out.write(text.getBytes('UTF-8'))
        out.closeEntry()
      }
    }
    bytes.toByteArray()
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
