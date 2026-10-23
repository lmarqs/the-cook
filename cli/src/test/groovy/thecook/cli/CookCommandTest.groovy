package thecook.cli

import org.junit.Test
import java.nio.file.Files

class CookCommandTest {
  @Test void missingUnknownAndRemovedOptionsAreUsageErrors() {
    def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
    def cli = command(stdout, stderr)
    assert cli.execute(['--help'] as String[]) == 0
    assert stdout.toString('UTF-8').contains('Cooks recipes')
    [
      ['--mystery', 'x.groovy'],
      ['-C'],
      ['help', '--kitchen='],
      ['init'],
      ['cook'],
      ['plan'],
      ['apply'],
      ['revert'],
      ['batch'],
      ['--library', 'lib', 'x.groovy'],
      ['--remove', 'x.groovy'],
      ['notes'],
    ].each { arguments ->
      stderr.reset()
      assert cli.execute(arguments as String[]) == 2 : arguments
      assert stderr.toString('UTF-8').startsWith('cook: ')
      assert stderr.toString('UTF-8').endsWith('\n  run: ./cook help\n')
      assert !stderr.toString('UTF-8').contains('Usage:')
    }
    stderr.reset()
    assert cli.execute(['init'] as String[]) == 2
    assert stderr.toString('UTF-8')
    .readLines()
    .first() ==
    'cook: Not a recipe: init'
    stderr.reset()
    assert cli.execute(['init'] as String[]) == 2
    assert stderr.toString('UTF-8') ==
    'cook: Not a recipe: init\n  a recipe is coordinates or a .groovy path\n  run: ./cook help\n'
  }

  @Test void versionPrintsTheVersionTheBuildWroteAndTakesNoOtherArguments() {
    def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
    def cli = command(stdout, stderr)
    assert cli.execute(['--version'] as String[]) == 0
    assert stdout.toString('UTF-8') == "cook ${System.getProperty('cook.version')}\n"
    assert cli.execute(['--version', 'x.groovy'] as String[]) == 2
    assert stderr.toString('UTF-8') == 'cook: --version takes no other arguments\n  run: ./cook help\n'
  }

  @Test void checkPrintsTheDryRunAndExitsOneUntilTheRecipesAreCooked() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def check = ['-C', repo.path, '--check', 'acme:notes:1.2.3#notes', '--line=first'] as String[]

      assert cli.execute(check) == 1
      assert stdout.toString('UTF-8').endsWith('\n\nWould create NOTES.md\nWould create cook.lock\n')
      assert stderr.toString('UTF-8') == 'cook: the recipes would change 2 files; nothing was written\n' +
      '  --check reports what a cook would change\n' +
      '  cook the same recipes without --check, review the diff and commit it\n'
      assert !new File(repo, 'NOTES.md').exists()

      def cooked = ['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes', '--line=first'] as String[]
      assert cli.execute(cooked) == 0
      stdout.reset(); stderr.reset()
      assert cli.execute(check) == 0
      assert stdout.toString('UTF-8') == 'Already cooked.\n'
      assert stderr.toString('UTF-8') == ''

      assert cli.execute(['-C', repo.path, '--check', '--batch-approve', 'acme:notes:1.2.3#notes'] as String[]) == 2
      assert stderr.toString('UTF-8') == 'cook: --check and --batch-approve exclude each other\n  run: ./cook help\n'
    }
  }

  @Test void kitchenOptionCooksAsIfStartedThereAndGoesBeforeTheRecipe() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      // A model and a recipe whose names differ only in case would be one file on macOS.
      new File(repo, 'NotesModel.groovy').text = Fixtures.NOTES_MODEL
      new File(repo, 'notes.groovy').text = """import acme.notes.Notes
@ingredient(Notes) def notes
notes.add('notes')
"""
      def notes = new File(repo, 'NOTES.md')
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, 'notes.groovy', '-C', repo.path] as String[]) == 2
      assert cli.execute(['-C', repo.path, 'notes.groovy', '--kitchen', repo.path] as String[]) == 2
      assert cli.execute(['-C', repo.path, '--kitchen', repo.path, 'notes.groovy'] as String[]) == 2
      assert cli.execute(['--kitchen', new File(root, 'missing').path, 'notes.groovy'] as String[]) == 1
      assert cli.execute(['--kitchen=' + repo.path, '--dry-run', 'notes.groovy'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert !notes.exists()
      assert cli.execute(['--kitchen', repo.path, '--batch-approve', 'notes.groovy'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert notes.text == '<!-- cook:begin notes/notes -->\nnotes\n<!-- cook:end notes/notes -->\n'
      stdout.reset()
      assert cli.execute(['-C', repo.path, 'notes.groovy'] as String[]) == 0 : stderr.toString('UTF-8')
      assert stdout.toString('UTF-8') == 'Already cooked.\n'
    }
  }

  @Test void helpCommandReadsEachRecipeFromItsTextsWithoutRunningIt() {
    workspace { root, repo ->
      new File(repo, 'notes.groovy').text = """@help('''
Seeds `NOTES.md`.
''')
@confirm('''
Nothing outside the repository changes.
''')
@yield('''
Edit `NOTES.md`.
''')
import example.notes.Notes

@param(value = ['owner'], doc = 'Who keeps the notes', choices = ['me', 'team']) String owner
@param(value = ['tag'], doc = 'A tag for the notes', default = []) String tag

@ingredient(Notes) def notes

throw new IllegalStateException('the recipe ran')
"""
      def expected = '''notes.groovy

Seeds `NOTES.md`.

Parameters
  --owner=<String>
      Who keeps the notes; required; choices: me, team
  --tag=<String>
      A tag for the notes; optional

Ingredients
  example.notes.Notes

Confirm
  Nothing outside the repository changes.

After cooking
  Edit `NOTES.md`.
'''
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, 'help', 'notes.groovy'] as String[]) == 0 : stderr.toString('UTF-8')
      assert stdout.toString('UTF-8') == expected
      stdout.reset()
      assert cli.execute(['help', '-C', repo.path, 'notes.groovy', 'notes.groovy'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8') == expected.trim() + '\n\n' + expected
      [
        ['notes.groovy', '--help'],
        ['--help', 'notes.groovy'],
        ['help', 'notes.groovy', '--owner=me'],
      ].each { misplaced ->
        stderr.reset()
        assert cli.execute((['-C', repo.path] + misplaced) as String[]) == 2
        assert stderr.toString('UTF-8')
        .readLines()
        .first() in [
          'cook: --help takes no other arguments',
          'cook: help: help takes recipes only: --owner=me',
        ]
      }
      new File(repo, 'bare.groovy').text = 'println "no help"\n'
      [
        [['help', 'bare.groovy'], 1, 'cook: help: bare.groovy has no help'],
        [['help', 'missing.groovy'], 1, 'cook: help: Recipe file not found: missing.groovy'],
        [['help', 'notes'], 2, 'cook: help: Not a recipe: notes'],
      ].each { arguments, status, message ->
        stderr.reset()
        assert cli.execute((['-C', repo.path] + arguments) as String[]) == status
        assert stderr.toString('UTF-8')
        .readLines()
        .first() == message
      }
      stdout.reset()
      assert cli.execute(['help'] as String[]) == 0 && stdout.toString('UTF-8').startsWith('Usage:')
      assert repo.list() as Set == ['notes.groovy', 'bare.groovy'] as Set
    }
  }

  @Test void theCookbookReadmesRenderedHelpIsWhatHelpPrints() {
    def repository = new File(System.getProperty('cook.repository'))
    def readme = new File(repository, 'cookbook/README.md').getText('UTF-8')
    def examples = (readme =~ /(?s)````text\n\$ \.\/cook help (\S+)\n(.*?)````\n/).collect { [it[1], it[2]] }
    assert examples

    examples.each { address, rendered ->
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      assert command(stdout, stderr).execute(['-C', repository.path, 'help', address] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8') == rendered
    }
  }

  @Test void helpReadsRecipesAndFrontPagesFromTheKitchensRepositories() {
    workspace { root, repo ->
      def jar = new File(repo, 'repository/acme/notes/1.2.3/notes-1.2.3.jar'); jar.parentFile.mkdirs()
      new java.util.jar.JarOutputStream(jar.newOutputStream()).withCloseable { out ->
        ['acme/notes/README.md': 'Notes for the repository.\n', 'acme/notes/Notes.groovy': 'class Notes {}\n',
          'acme/notes/notes.groovy': """@help(\'\'\'
Seeds `NOTES.md`.
\'\'\')
import acme.notes.Notes

@ingredient(Notes) def notes
"""].each { path, text ->
          out.putNextEntry(new java.util.jar.JarEntry(path)); out.write(text.getBytes('UTF-8')); out.closeEntry()
        }
      }
      new File(repo, 'cook.toml').text = 'repositories = ["repository"]\n'
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, 'help', 'acme:notes:1.2.3#notes', 'acme:notes:1.2.3'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8') == """acme:notes:1.2.3#notes

Seeds `NOTES.md`.

Ingredients
  acme.notes.Notes

acme:notes:1.2.3

Notes for the repository.

Recipes
  acme:notes:1.2.3#notes

Models
  acme.notes.Notes
"""
      assert cli.execute(['-C', repo.path, 'help', 'acme:notes:1.2.4'] as String[]) == 1
      assert stderr.toString('UTF-8').contains('cook: help: acme:notes:1.2.4 not found in the repositories in')
      assert cli.execute(['-C', repo.path, 'help', 'acme:notes:latest'] as String[]) == 1
      assert stderr.toString('UTF-8').contains('cook: help: Invalid recipe coordinates: acme:notes:latest')
      stderr.reset()
      assert cli.execute(['-C', repo.path, 'a:notes.groovy'] as String[]) == 1
      assert stderr.toString('UTF-8').contains('cook: a:notes.groovy: Recipe file not found: a:notes.groovy')
    }
  }

  @Test void appliesRecipeCoordinatesLeftToRightAndPrintsWhatToDoNext() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def notes = new File(repo, 'NOTES.md')

      def review = """Confirm acme:notes:1.2.3#notes
  Everyone reads `NOTES.md`.

--- /dev/null
+++ b/NOTES.md
@@ -0,0 +1,3 @@
+<!-- cook:begin acme:notes#notes/notes -->
+first
+<!-- cook:end acme:notes#notes/notes -->
"""
      assert cli.execute(['-C', repo.path, '--dry-run', 'acme:notes:1.2.3#notes', '--line=first'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').startsWith(review + '--- /dev/null\n+++ b/cook.lock\n')
      assert stdout.toString('UTF-8').endsWith('\n\nWould create NOTES.md\nWould create cook.lock\n')
      assert !notes.exists()

      stdout.reset()
      assert cli.execute(['-C', repo.path, 'acme:notes:1.2.3#notes', '--line=first'] as String[]) == 1
      assert stderr.toString('UTF-8') ==
      'cook: approval required; nothing was written\n' +
      '  there is no terminal to ask in, and --batch-approve was not given\n' +
      '  run interactively to review the diff, or run again with --batch-approve before the first recipe\n'
      assert !notes.exists()

      def declined = new ByteArrayOutputStream(); stderr.reset()
      assert command(declined, stderr, 'n\n')
      .execute(['-C', repo.path, 'acme:notes:1.2.3#notes', '--line=first'] as String[]) == 1
      assert declined.toString('UTF-8').endsWith('\n\nWrite these changes? [y/N] ')
      assert stderr.toString('UTF-8') == 'cook: nothing was written\n  the answer was not y or yes\n'
      assert !notes.exists()

      def accepted = new ByteArrayOutputStream()
      assert command(accepted, stderr, 'y\n')
      .execute(['-C', repo.path, 'acme:notes:1.2.3#notes', '--line=first'] as String[]) == 0
      assert accepted.toString('UTF-8').startsWith(review)
      assert accepted.toString('UTF-8').endsWith('Write these changes? [y/N] Created NOTES.md\nCreated cook.lock\n\n' +
      'After cooking acme:notes:1.2.3#notes\n  Read `NOTES.md`.\n')

      stdout.reset()
      def more = 'acme:notes:1.2.3#more'
      def cookBoth = ['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes', '--line=first', more]
      assert cli.execute(cookBoth as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').endsWith('Updated NOTES.md\nUpdated cook.lock\n\n' +
      'After cooking acme:notes:1.2.3#notes\n  Read `NOTES.md`.\n')
      assert notes.text == """<!-- cook:begin acme:notes#notes/notes -->
first
<!-- cook:end acme:notes#notes/notes -->
<!-- cook:begin acme:notes#more/notes -->
more
<!-- cook:end acme:notes#more/notes -->
"""
      stdout.reset()
      def recookBoth = ['-C', repo.path, 'acme:notes:1.2.3#notes', '--line=first', more]
      assert cli.execute(recookBoth as String[]) == 0
      assert stdout.toString('UTF-8') == 'Already cooked.\n'
      stdout.reset()
      def recook = ['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes', '--line=second']
      assert cli.execute(recook as String[]) == 0
      assert stdout.toString('UTF-8').contains('\n-first\n+second\n')
      assert stdout.toString('UTF-8').contains('\nUpdated NOTES.md\nUpdated cook.lock\n\nAfter cooking')
      stdout.reset()
      def loud = ['-C', repo.path, '--dry-run', 'acme:notes:1.2.3#notes', '--line=second', '--loud']
      assert cli.execute(loud as String[]) == 0
      assert stdout.toString('UTF-8').contains('\n-second\n+SECOND\n')
      stdout.reset()
      def quiet = ['-C', repo.path, '--dry-run', 'acme:notes:1.2.3#notes', '--line=second', '--loud=false']
      assert cli.execute(quiet as String[]) == 0
      def sha256 = (new File(repo, 'cook.lock').text =~ /sha256: "(\w+)"/)[0][1]
      assert stdout.toString('UTF-8') == """Confirm acme:notes:1.2.3#notes
  Everyone reads `NOTES.md`.

--- a/cook.lock
+++ b/cook.lock
@@ -12,7 +12,7 @@
   - name: "acme:notes#notes"
     source: "acme:notes:1.2.3#notes"
     sha256: "${sha256}"
-    arguments: ["--line=second"]
+    arguments: ["--line=second","--loud=false"]
     owns:
       sections:
         - file: "NOTES.md"

Would update cook.lock
"""
      stdout.reset()
      def loudSeparate = ['-C', repo.path, '--dry-run', 'acme:notes:1.2.3#notes', '--line', 'second', '--loud']
      assert cli.execute(loudSeparate as String[]) == 0
      assert stdout.toString('UTF-8').contains('\n-second\n+SECOND\n')
      new File(repo, 'Notes.groovy').text = Fixtures.NOTES_MODEL
      new File(repo, 'local.groovy').text = """import acme.notes.Notes
@ingredient(Notes) def notes
notes.add('local')
"""
      stdout.reset()
      def withLocal = ['-C', repo.path, '--dry-run', 'acme:notes:1.2.3#notes', '--line', 'second', 'local.groovy']
      assert cli.execute(withLocal as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').contains('\n+<!-- cook:begin local/notes -->\n+local\n' +
      '+<!-- cook:end local/notes -->\n')
      assert stdout.toString('UTF-8').endsWith('\nWould update NOTES.md\nWould update cook.lock\n')

      [
        [['acme:notes:1.2.3#notes', '--line', 'a:b'], 2, 'cook: Not a recipe: a:b'],
        [['acme:notes:1.2.3#notes', '--line', 'a', 'b'], 2, 'cook: Not a recipe: b'],
        [
          ['acme:notes:1.2.3#notes', '--line=a', '--loud', 'false'],
          1,
          'cook: acme:notes:1.2.3#notes: --loud takes no separate value'
        ],
        [
          ['acme:notes:1.2.3#notes', '--line'],
          1,
          'cook: acme:notes:1.2.3#notes: --line needs a value'
        ],
        [['acme:notes:1.2.3#notes', '--line='], 2, 'cook: Missing value for --line'],
        [
          ['acme:notes:1.2.3#notes', '--line=a', '--set=first'],
          1,
          'cook: acme:notes:1.2.3#notes: Unknown argument --set'
        ],
        [['acme:notes:1.2.3#notes', 'line=first'], 2, 'cook: Not a recipe: line=first'],
        [
          ['../outside.groovy'],
          1,
          'cook: ../outside.groovy: the recipe is outside the kitchen'
        ],
        [
          ['https://example.com/notes.groovy'],
          1,
          'cook: https://example.com/notes.groovy: a URL is not a recipe'
        ],
        [['acme:notes:1.2.3#notes', '--line=a', '--dry-run'], 2, 'cook: --dry-run goes before the first recipe'],
        [
          ['acme:notes:1.2.3#notes', '--line=a', '--batch-approve'],
          2,
          'cook: --batch-approve goes before the first recipe'
        ],
        [['acme:notes:1.2.3#notes', '--line=a', '--force'], 2, 'cook: --force goes before the first recipe'],
        [['acme:notes:1.2.3#notes', '--line=a', '--skip'], 2, 'cook: --skip goes before the first recipe'],
        [['--force', '--skip', 'acme:notes:1.2.3#notes', '--line=a'], 2, 'cook: --force and --skip exclude each other'],
        [
          ['acme:notes:1.2.3#more', 'acme:notes:1.2.3#more'],
          1,
          'cook: recipe 2 (acme:notes:1.2.3#more): acme:notes#more is named twice'
        ],
        [['acme:notes:1.2.3#notes'], 1, 'cook: acme:notes:1.2.3#notes: Missing --line: The line to add'],
      ].each { arguments, status, message ->
        stderr.reset()
        assert cli.execute((['-C', repo.path] + arguments) as String[]) == status
        assert stderr.toString('UTF-8')
        .readLines()
        .first() == message
      }
    }
  }

  @Test void appliesAReleaseWhoseSourcesChangedAfterSayingSoAboveTheDiff() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def lock = new File(repo, 'cook.lock')
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#more'] as String[]) == 0
      def recorded = (lock.text =~ /sha256: "(\w+)"/)[0][1]

      Fixtures.notesCookbook(repo, ['acme/notes/NOTICE.md': 'Changed under the same version.'])
      stdout.reset()
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#more'] as String[]) == 0 :
      stderr.toString('UTF-8')

      def current = (lock.text =~ /sha256: "(\w+)"/)[0][1]
      assert current != recorded
      assert stdout.toString('UTF-8').startsWith("""Changed source acme:notes:1.2.3#more
  cook.lock recorded sha256 ${recorded}; the release now has sha256 ${current}
  its sources changed under the same version; review the diff before approving

--- a/cook.lock
+++ b/cook.lock
""")
      assert stdout.toString('UTF-8').endsWith('\nUpdated cook.lock\n')
    }
  }

  @Test void discardsByLockNameButCookingStillRequiresAVersion() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes#notes'] as String[]) == 1
      assert stderr.toString('UTF-8') ==
      'cook: Invalid recipe coordinates: acme:notes#notes\n' +
      '  expected group:artifact:version[#recipe] with an exact release version, such as ' +
      'io.github.lmarqs.cookbook:agents:1.2.3#seed\n'
      stderr.reset()
      def cooked = ['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes', '--line=first'] as String[]
      assert cli.execute(cooked) == 0
      def notes = new File(repo, 'NOTES.md')
      notes.text += 'mine\n'

      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--batch-approve', 'acme:notes#notes'] as String[]) == 0 :
      stderr.toString('UTF-8')

      assert stdout.toString('UTF-8').endsWith('\nUpdated NOTES.md\nDeleted cook.lock\n')
      assert notes.text == 'mine\n'
      assert !new File(repo, 'cook.lock').exists()
    }
  }

  @Test void discardsWhatTheLockListsForEachRecipeAndKeepsTheRestOfTheFile() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def notes = new File(repo, 'NOTES.md'); def lock = new File(repo, 'cook.lock')
      def more = 'acme:notes:1.2.3#more'
      def cookBoth = ['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes', '--line=first', more]
      assert cli.execute(cookBoth as String[]) == 0
      notes.text += 'mine\n'

      def declined = new ByteArrayOutputStream(); stderr.reset()
      assert command(declined, stderr, 'n\n')
      .execute(['discard', '-C', repo.path, 'acme:notes:1.2.3#more'] as String[]) == 1
      assert declined.toString('UTF-8').contains('\n-<!-- cook:begin acme:notes#more/notes -->\n-more\n' +
      '-<!-- cook:end acme:notes#more/notes -->\n')
      assert stderr.toString('UTF-8') == 'cook: discard: nothing was written\n  the answer was not y or yes\n'

      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--dry-run', 'acme:notes:1.2.3#more'] as String[]) == 0
      assert stdout.toString('UTF-8').endsWith('\nWould update NOTES.md\nWould update cook.lock\n')
      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#more'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').endsWith('\nUpdated NOTES.md\nUpdated cook.lock\n')
      assert notes.text ==
      '<!-- cook:begin acme:notes#notes/notes -->\nfirst\n<!-- cook:end acme:notes#notes/notes -->\nmine\n'

      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').endsWith('\nUpdated NOTES.md\nDeleted cook.lock\n')
      assert notes.text == 'mine\n'
      assert !lock.exists()

      [
        [['discard'], 2, 'cook: discard: A recipe is required'],
        [['discard', 'notes'], 2, 'cook: discard: Not a recipe: notes'],
        [
          ['discard', 'acme:notes:1.2.3#notes', '--batch-approve'],
          2,
          'cook: discard: --batch-approve goes before the first recipe'
        ],
        [
          ['discard', 'acme:notes:1.2.3#notes', '--line=a'],
          2,
          'cook: discard: discard takes no recipe arguments: --line=a'
        ],
        [
          ['--dry-run', 'discard', 'acme:notes:1.2.3#notes'],
          2,
          'cook: discard: --dry-run, --check, --batch-approve, --force and --skip go before a recipe, ' +
          'not before a command'
        ],
        [
          ['discard', 'acme:notes:1.2.3#notes'],
          1,
          'cook: discard: cook.lock lists no recipe acme:notes#notes; nothing was written'
        ],
        [
          ['help', 'https://example.com/notes.groovy'],
          1,
          'cook: help: a URL is not a recipe'
        ],
        [
          ['discard', 'https://example.com/notes.groovy'],
          1,
          'cook: discard: a URL is not a recipe'
        ],
      ].each { arguments, status, message ->
        stderr.reset()
        assert cli.execute((arguments.take(1) + ['-C', repo.path] + arguments.drop(1)) as String[]) == status :
        arguments
        assert stderr.toString('UTF-8')
        .readLines()
        .first() == message
      }
      // The message standard's example for discard, line for line.
      stderr.reset()
      assert cli.execute(['discard', '-C', repo.path, 'acme:notes:1.2.3#notes'] as String[]) == 1
      assert stderr.toString('UTF-8') == '''\
cook: discard: cook.lock lists no recipe acme:notes#notes; nothing was written
  discard removes only what cook.lock records for a recipe
  check the recipe's name; cook.lock lists none
'''
    }
  }

  private static Object command(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr, String input = null) {
    def stdin = input == null ? null : new BufferedReader(new StringReader(input))
    def home = new File(System.getProperty('java.io.tmpdir'), 'the-cook-test-home')
    def repositories = { File kitchen -> thecook.infra.CookToml.repositories(kitchen, home) }
    new thecook.cli.terminal.CookCommand(new PrintWriter(stdout, true), new PrintWriter(stderr, true), repositories,
        stdin)
  }

  private static void workspace(Closure body) {
    def root = Files.createTempDirectory('cook-cli-').toFile()

    try {
      def repo = new File(root, 'repository'); repo.mkdirs()
      body(root, repo)
    } finally {
      root.deleteDir()
    }
  }
}
