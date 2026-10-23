package thecook

import org.junit.Test
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.Coordinates

class CookbookArtifactTest {
  private static final Coordinates COORDINATES = Coordinates.parse('acme:text:1.0.0')

  @Test void itsTestDirectoryHoldsNoRecipeModelOrResourceAndIsNoSourceInTheHash() {
    Map<String, byte[]> sources = [
      'Text.groovy': bytes('class Text {}\n'),
      'setup.groovy': bytes("@help('Writes a file.')\n"),
      'setup.md': bytes('packaged\n'),
      'templates/test/setup.md': bytes('a resource\n'),
    ]
    def artifact = new CookbookArtifact(COORDINATES, sources + [
      'test/SetupTest.groovy': bytes('class SetupTest {}\n'),
      'test/fixtures/setup.md': bytes('a fixture\n'),
    ])

    assert artifact.recipes == ['setup']
    assert artifact.models == ['Text']
    assert artifact.resource('test/fixtures/setup.md') == null
    assert artifact.resources('test') == [:]
    assert artifact.resource('templates/test/setup.md') == 'a resource\n'
    assert artifact.sha256 == CookbookArtifact.sourcesSha256(sources)
  }

  private static byte[] bytes(String text) {
    text.getBytes('UTF-8')
  }
}
