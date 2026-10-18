package thecook.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CookbookPluginTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void publishesOneJarPerDirectoryAndTheBom() {
    def root = cookbook(['notes', 'review'])
    def project = project(root)
    project.extensions.extraProperties.set('releaseVersion', '1.2.3')

    project.pluginManager.apply('io.github.lmarqs.cookbook')

    assert project.group == 'acme' && project.version == '1.2.3'
    def publications = project.extensions.getByType(PublishingExtension).publications.withType(MavenPublication)
    assert publications*.artifactId.sort() == ['cookbook', 'notes', 'review']
    assert project.tasks.named('notesJar').get().archiveFileName.get() == 'notes-1.2.3.jar'
    assert publications.getByName('cookbook').pom.packaging == 'pom'
  }

  @Test void withoutAReleaseVersionTheVersionIsZero() {
    def project = project(cookbook(['notes']))

    project.pluginManager.apply('io.github.lmarqs.cookbook')

    assert project.version == '0.0.0'
    assert project.tasks.named('notesJar').get().archiveFileName.get() == 'notes-0.0.0.jar'
  }

  @Test void refusesADirectoryNamedLikeTheBom() {
    def project = project(cookbook(['cookbook']))

    try {
      project.pluginManager.apply('io.github.lmarqs.cookbook')
      throw new AssertionError('expected the plugin to refuse a directory named cookbook')
    } catch (Exception error) {
      def cause = error

      while (cause.cause != null && !(cause instanceof IllegalStateException)) {
        cause = cause.cause
      }

      assert cause.message == 'A cookbook directory cannot be named cookbook: the BOM uses that artifact name'
    }
  }

  private Project project(File root) {
    ProjectBuilder.builder()
        .withProjectDir(root)
        .withGradleUserHomeDir(folder.newFolder('home'))
        .build()
  }

  private File cookbook(List<String> directories) {
    def root = folder.newFolder('book')
    new File(root, 'cookbook.toml').text = 'group = "acme"\napi = 1\n'
    directories.each { new File(root, it).mkdirs() }
    root
  }
}
