package thecook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Draft

import java.nio.file.Files
import java.nio.file.Paths

class DraftTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void listsADirectorysEntriesAsTheInvocationLeftThem() {
    def destination = folder.newFolder()
    write(destination, 'agents/kept.md', 'kept\n')
    write(destination, 'agents/gone.md', 'gone\n')
    write(destination, 'agents/nested/inner.md', 'inner\n')
    Files.createSymbolicLink(new File(destination, 'agents/linked.md').toPath(), Paths.get('kept.md'))
    def draft = new Draft(destination)
    draft.delete('agents/gone.md')
    draft.write('agents/added.md', 'added\n')
    draft.write('agents/fresh/deep/file.md', 'deep\n')

    assert draft.entries('agents') == [
      'added.md': 'file',
      fresh: 'directory',
      'kept.md': 'file',
      'linked.md': 'symlink',
      nested: 'directory',
    ]
    assert draft.entries('agents').keySet().toList() == ['added.md', 'fresh', 'kept.md', 'linked.md', 'nested']
    assert draft.changes == ['agents/added.md', 'agents/fresh/deep/file.md', 'agents/gone.md']
  }

  @Test void listsNothingForAnAbsentDirectoryOrAFile() {
    def destination = folder.newFolder()
    write(destination, 'NOTES.md', 'notes\n')
    def draft = new Draft(destination)

    assert draft.entries('missing') == [:]
    assert draft.entries('NOTES.md') == [:]
  }

  @Test void listsTheDestinationRoot() {
    def destination = folder.newFolder()
    write(destination, 'NOTES.md', 'notes\n')
    def draft = new Draft(destination)
    draft.write('README.md', 'readme\n')

    assert draft.entries('.') == ['NOTES.md': 'file', 'README.md': 'file']
  }

  private static void write(File destination, String path, String text) {
    def file = new File(destination, path)
    file.parentFile.mkdirs()
    file.text = text
  }
}
