package thecook

import org.junit.Test
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.Coordinates

class CookbookArtifactTest {
  private static final Coordinates COORDINATES = Coordinates.parse('acme:text:1.0.0')

  @Test void aTestFileIsNeitherARecipeNorAModelNorPartOfTheSourcesHash() {
    Map<String, byte[]> sources = [
      'Text.groovy': bytes('class Text {}\n'),
      'setup.groovy': bytes("@help('Writes a file.')\n"),
      'setup.md': bytes('packaged\n'),
    ]
    def artifact = new CookbookArtifact(COORDINATES, sources + [
      'setup.test.groovy': bytes('class SetupTest {}\n'),
      'Text.test.groovy': bytes('class TextTest {}\n'),
      'docs/Other.test.groovy': bytes('class OtherTest {}\n'),
    ])

    assert artifact.recipes == ['setup']
    assert artifact.models == ['Text']
    assert artifact.resource('setup.test.groovy') == null
    assert artifact.sha256 == CookbookArtifact.sourcesSha256(sources)
  }

  private static byte[] bytes(String text) {
    text.getBytes('UTF-8')
  }
}
