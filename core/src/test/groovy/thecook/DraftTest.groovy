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
    def kitchen = folder.newFolder()
    write(kitchen, 'agents/kept.md', 'kept\n')
    write(kitchen, 'agents/gone.md', 'gone\n')
    write(kitchen, 'agents/nested/inner.md', 'inner\n')
    Files.createSymbolicLink(new File(kitchen, 'agents/linked.md').toPath(), Paths.get('kept.md'))
    def draft = new Draft(kitchen)
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
    def names = draft.entries('agents').keySet()
    assert names as List == ['added.md', 'fresh', 'kept.md', 'linked.md', 'nested']
    assert draft.changes == ['agents/added.md', 'agents/fresh/deep/file.md', 'agents/gone.md']
  }

  @Test void listsNothingForAnAbsentDirectoryOrAFile() {
    def kitchen = folder.newFolder()
    write(kitchen, 'NOTES.md', 'notes\n')
    def draft = new Draft(kitchen)

    assert draft.entries('missing') == [:]
    assert draft.entries('NOTES.md') == [:]
  }

  @Test void listsTheDestinationRoot() {
    def kitchen = folder.newFolder()
    write(kitchen, 'NOTES.md', 'notes\n')
    def draft = new Draft(kitchen)
    draft.write('README.md', 'readme\n')

    assert draft.entries('.') == ['NOTES.md': 'file', 'README.md': 'file']
  }

  private static void write(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
  }
}
