package thecook.infra

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Coordinates

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class GithubReleaseRepositoryTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void readsTheJarOfTheExactCoordinates() {
    release('1.0.0', ['org/acme/notes/hello.groovy': 'println "hello"'])

    def artifact = repository().artifact(Coordinates.parse('org.acme:notes:1.0.0'))

    assert artifact.recipes == ['hello']
    assert artifact.recipeSource('hello') == 'println "hello"'
  }

  @Test void holdsNoArtifactWhenTheJarIsMissing() {
    release('1.0.0', ['org/acme/notes/hello.groovy': 'println "hello"'])

    assert repository().artifact(Coordinates.parse('org.acme:notes:2.0.0')) == null
  }

  @Test void holdsNoArtifactWhenTheJarIsOfAnotherGroup() {
    release('1.0.0', ['org/other/notes/hello.groovy': 'println "hello"'])

    assert repository().artifact(Coordinates.parse('org.acme:notes:1.0.0')) == null
  }

  @Test void listsEveryVersionFromTheLatestReleaseMetadata() {
    metadata('''<metadata>
  <groupId>org.acme</groupId>
  <artifactId>notes</artifactId>
  <versioning><versions><version>1.0.0</version><version>1.1.0</version></versions></versioning>
</metadata>''')

    assert repository().versions('org.acme', 'notes') == ['1.0.0', '1.1.0']
  }

  @Test void listsNoVersionsWhenTheMetadataIsMissing() {
    assert repository().versions('org.acme', 'notes') == []
  }

  @Test void listsNoVersionsWhenTheMetadataIsOfAnotherGroup() {
    metadata('''<metadata>
  <groupId>org.other</groupId>
  <artifactId>notes</artifactId>
  <versioning><versions><version>1.0.0</version></versions></versioning>
</metadata>''')

    assert repository().versions('org.acme', 'notes') == []
  }

  @Test void isNamedAfterTheGithubRepository() {
    assert new GithubReleaseRepository('acme', 'notes').toString() == 'github:acme/notes'
  }

  private GithubReleaseRepository repository() {
    new GithubReleaseRepository('acme', 'notes', folder.root.toURI())
  }

  private void release(String version, Map<String, String> files) {
    def jar = new File(folder.root, "download/v${version}/notes-${version}.jar")
    jar.parentFile.mkdirs()
    new JarOutputStream(jar.newOutputStream()).withCloseable { out ->
      files.each { path, text ->
        out.putNextEntry(new JarEntry(path))
        out.write(text.getBytes('UTF-8'))
        out.closeEntry()
      }
    }
  }

  private void metadata(String text) {
    def file = new File(folder.root, 'latest/download/notes-maven-metadata.xml')
    file.parentFile.mkdirs()
    file.text = text
  }
}
