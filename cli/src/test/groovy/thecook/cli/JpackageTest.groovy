package thecook.cli

import org.junit.Assume
import org.junit.Test
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class JpackageTest {
  @Test void bundledRuntimeAppliesRecipesOutsideTheSourceCheckout() {
    def root = Files.createTempDirectory('cook packaged test ').toFile()

    try {
      assert run(root, '--help').contains('Usage:')
      Fixtures.notesCookbook(root)
      new File(root, 'Notes.groovy').text = Fixtures.NOTES_MODEL
      new File(root, 'local.groovy').text = """@help('Adds a local line.')
import acme.notes.Notes
@ingredient(Notes) def notes
notes.add('local')
"""
      assert run(root, 'help', 'acme:notes:1.2.3#notes').contains('--line=<String>')
      assert run(root, 'help', 'local.groovy').contains('Adds a local line.')
      def recipes = ['acme:notes:1.2.3#notes', '--line', 'first', 'local.groovy']
      def refused = run(root, *recipes, 1)
      assert refused.contains('cook: approval required; nothing was written')
      assert refused.contains('  run interactively to review the diff, or run again with --batch-approve')
      assert run(root, '--dry-run', *recipes).contains('Would create NOTES.md')
      assert !new File(root, 'NOTES.md').exists()
      assert run(root, '--batch-approve', *recipes).contains('Created cook.lock')
      assert new File(root, 'NOTES.md').text.contains('<!-- cook:begin local/notes -->\nlocal\n')
      def nested = new File(root, 'nested'); nested.mkdirs()
      assert run(nested, '-C', root.path, *recipes).trim() == 'Already cooked.'
      assert run(root, 'init', 2).contains('Not a recipe: init')
    } finally {
      root.deleteDir()
    }
  }

  @Test void archiveHoldsTheImageWithAnExecutableLauncherThatRunsOnceUnpackedAndItsSha256() {
    def path = System.getProperty('cook.packageArchive')
    Assume.assumeNotNull(path)
    def archive = new File(path)
    def launcher = System.getProperty('cook.packageArchiveLauncher')
    def image = launcher.substring(0, launcher.indexOf('/') + 1)
    def entries = tar(archive, '-tzf').readLines()

    assert launcher in entries
    assert entries.every { it.startsWith(image) }
    def root = Files.createTempDirectory('cook archive ').toFile()

    try {
      tar(archive, '-xzf', '-C', root.path)
      def unpacked = new File(root, launcher)
      assert unpacked.canExecute()
      assert runLauncher(root, unpacked, '--help').contains('Usage:')
    } finally {
      root.deleteDir()
    }

    def sha256 = MessageDigest.getInstance('SHA-256')
        .digest(archive.bytes)
        .encodeHex()
    assert new File("${archive}.sha256").text == "${sha256}  ${archive.name}\n"
  }

  private static String tar(File archive, String... arguments) {
    def process = new ProcessBuilder(['tar', arguments[0], archive.path] + arguments.drop(1).toList())
    .redirectErrorStream(true)
    .start()
    def output = process.inputStream.text
    assert process.waitFor() == 0 : output
    output
  }

  private String run(File directory, Object... arguments) {
    runLauncher(directory, new File(System.getProperty('cook.packagedLauncher')), arguments)
  }

  private static String runLauncher(File directory, File launcher, Object... arguments) {
    def args = arguments.toList()
    def expected = args.last() instanceof Integer ? args.removeLast() : 0
    assert launcher.isFile(): "Packaged launcher missing: ${launcher}"
    def output = Files.createTempFile('cook-output-', '.txt').toFile()

    try {
      def builder = new ProcessBuilder(([launcher.absolutePath] + args) as String[])
      .directory(directory)
      .redirectErrorStream(true)
      .redirectOutput(output)
      ['JAVA_HOME', 'JRE_HOME', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'].each {
        builder.environment().remove(it)
      }
      builder.environment().put('PATH', '')
      def process = builder.start()

      if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        throw new AssertionError('Packaged CLI timed out')
      }

      assert process.exitValue() == expected: output.text
      output.getText('UTF-8')
    } finally {
      output.delete()
    }
  }
}
