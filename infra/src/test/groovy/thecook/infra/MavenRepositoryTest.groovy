package thecook.infra

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MavenRepositoryTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void listsEveryVersionFromMavenMetadataInItsPublishedOrder() {
    metadata('maven-metadata.xml', '''<metadata>
  <version>outside-the-list</version>
  <versioning>
    <latest>3.0.0</latest>
    <release>2.0.0</release>
    <versions>
      <version>2.0.0</version>
      <version>1.0.0</version>
      <version>3.0.0-SNAPSHOT</version>
    </versions>
  </versioning>
</metadata>''')

    assert repository().versions('org.acme', 'notes') == ['2.0.0', '1.0.0', '3.0.0-SNAPSHOT']
  }

  @Test void fallsBackToMavenLocalMetadata() {
    metadata('maven-metadata-local.xml', '''<metadata>
  <versioning><versions><version>1.2.3</version><version>1.2.4</version></versions></versioning>
</metadata>''')

    assert repository().versions('org.acme', 'notes') == ['1.2.3', '1.2.4']
  }

  @Test void prefersMavenMetadataWhenBothFilesExist() {
    metadata('maven-metadata.xml', '''<metadata>
  <versioning><versions><version>2.0.0</version></versions></versioning>
</metadata>''')
    metadata('maven-metadata-local.xml', '''<metadata>
  <versioning><versions><version>1.0.0</version></versions></versioning>
</metadata>''')

    assert repository().versions('org.acme', 'notes') == ['2.0.0']
  }

  @Test void listsNoVersionsWhenNeitherMetadataFileExists() {
    assert repository().versions('org.acme', 'notes') == []
  }

  @Test void readsMetadataWithoutResolvingAnExternalEntity() {
    def external = folder.newFile('external.txt')
    external.text = 'external-content'
    metadata('maven-metadata.xml', """<!DOCTYPE metadata [
  <!ENTITY external SYSTEM "${external.toURI()}">
]>
<metadata>
  <versioning><versions><version>&external;</version><version>1.2.3</version></versions></versioning>
</metadata>""")

    assert repository().versions('org.acme', 'notes') == ['', '1.2.3']
  }

  @Test void readsMetadataWithoutLoadingAnExternalDtd() {
    def missing = new File(folder.root, 'missing.dtd')
    metadata('maven-metadata.xml', """<!DOCTYPE metadata SYSTEM "${missing.toURI()}">
<metadata>
  <versioning><versions><version>1.2.3</version></versions></versioning>
</metadata>""")

    assert repository().versions('org.acme', 'notes') == ['1.2.3']
  }

  private MavenRepository repository() {
    new MavenRepository(folder.root.toURI())
  }

  private void metadata(String name, String text) {
    def file = new File(folder.root, "org/acme/notes/${name}")
    file.parentFile.mkdirs()
    file.text = text
  }
}
