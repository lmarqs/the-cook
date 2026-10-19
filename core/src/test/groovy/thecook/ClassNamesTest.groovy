package thecook

import org.junit.Test

class ClassNamesTest {
  // macOS and Windows file systems ignore case, so two class files that differ only in case overwrite each other in
  // a classes directory.
  @Test void noTwoEngineClassesDifferOnlyInCase() {
    def directories = System.getProperty('java.class.path')
        .split(File.pathSeparator)
        .collect { new File(it) }
        .findAll { it.isDirectory() && new File(it, 'thecook').isDirectory() }
    assert directories

    directories.each { directory ->
      def root = directory.toPath()
      def names = []
      directory.eachFileRecurse(groovy.io.FileType.FILES) {
        names << root.relativize(it.toPath()).toString()
      }
      def clashes = names
          .groupBy { it.toLowerCase(Locale.ROOT) }
          .values()
          .findAll { it.size() > 1 }
      assert clashes == []
    }
  }
}
