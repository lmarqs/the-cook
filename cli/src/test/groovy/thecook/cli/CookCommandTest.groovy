package thecook.cli

import org.junit.Test
import java.nio.file.Files

class CookCommandTest {
  @Test void missingUnknownAndRemovedOptionsAreUsageErrors() {
    def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
    def cli = command(stdout, stderr)
    assert cli.execute(['--help'] as String[]) == 0
    assert stdout.toString('UTF-8').contains('Cooks recipes')
    assert stdout.toString('UTF-8').contains('replicate  Cook recipes again from cook.lock.')
    assert stdout.toString('UTF-8').contains(
    'update     Cook recipes from cook.lock again at their newest release.')
    assert stdout.toString('UTF-8').contains('check      Compare the kitchen with what cook.lock records')
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
      ['--check', 'x.groovy'],
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

  @Test void checkComparesTheKitchenWithWhatTheLockRecordsAndExitsOneOnDrift() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)

      assert cli.execute(['check', '-C', repo.path] as String[]) == 2
      assert stderr.toString('UTF-8') == 'cook: check: there is no cook.lock\n' +
      '  cook.lock records the recipes to cook again\n  run in the kitchen that holds it\n'

      def cooked = ['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#notes', '--line=first'] as String[]
      assert cli.execute(cooked) == 0
      stdout.reset(); stderr.reset()
      assert cli.execute(['check', '-C', repo.path] as String[]) == 0
      assert stdout.toString('UTF-8') == 'Already cooked.\n'
      assert stderr.toString('UTF-8') == ''

      def notes = new File(repo, 'NOTES.md')
      notes.text = notes.text.replace('\nfirst\n', '\nedited\n')
      stdout.reset()
      assert cli.execute(['check', '-C', repo.path] as String[]) == 1
      assert stdout.toString('UTF-8').startsWith('--- a/NOTES.md\n')
      assert stdout.toString('UTF-8').endsWith('\n\nWould update NOTES.md\n')
      assert stderr.toString('UTF-8') ==
      'cook: check: 1 file differs from what cook.lock records; nothing was written\n' +
      '  check cooks each recipe again from the source and arguments cook.lock records\n' +
      "  run ./cook replicate -C ${repo.path}, review the diff and commit it\n"
      assert notes.text.contains('\nedited\n')

      stderr.reset()
      assert cli.execute(['check', '-C', repo.path, 'acme:notes#notes'] as String[]) == 1
      assert stderr.toString('UTF-8')
      .endsWith("\n  run ./cook replicate -C ${repo.path} acme:notes#notes, review the diff and commit it\n")
    }
  }

  @Test void aRecipeThatFailsAnAssertionExitsTwo() {
    workspace { root, repo ->
      new File(repo, 'failing.groovy').text = "assert 1 == 2\n"
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()

      assert command(stdout, stderr).execute(['-C', repo.path, '--dry-run', 'failing.groovy'] as String[]) == 2
      assert stderr.toString('UTF-8').startsWith('cook: ')
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
      assert cli.execute(['--kitchen', new File(root, 'missing').path, 'notes.groovy'] as String[]) == 2
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
        [['help', 'bare.groovy'], 2, 'cook: help: bare.groovy has no help'],
        [['help', 'missing.groovy'], 2, 'cook: help: Recipe file not found: missing.groovy'],
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
      assert stdout.toString('UTF-8').contains('replicate  Cook recipes again from cook.lock.')
      assert stdout.toString('UTF-8').contains(
      'update     Cook recipes from cook.lock again at their newest release.')
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
      assert cli.execute(['-C', repo.path, 'help', 'acme:notes:1.2.4'] as String[]) == 2
      assert stderr.toString('UTF-8').contains('cook: help: acme:notes:1.2.4 not found in the repositories in')
      assert cli.execute(['-C', repo.path, 'help', 'acme:notes:latest'] as String[]) == 2
      assert stderr.toString('UTF-8').contains('cook: help: Invalid recipe coordinates: acme:notes:latest')
      stderr.reset()
      assert cli.execute(['-C', repo.path, 'a:notes.groovy'] as String[]) == 2
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
@@ -14,6 +14,7 @@
     sha256: "${sha256}"
     arguments:
       "line": ["second"]
+      "loud": ["false"]
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
          2,
          'cook: acme:notes:1.2.3#notes: --loud takes no separate value'
        ],
        [
          ['acme:notes:1.2.3#notes', '--line'],
          2,
          'cook: acme:notes:1.2.3#notes: --line needs a value'
        ],
        [['acme:notes:1.2.3#notes', '--line='], 2, 'cook: Missing value for --line'],
        [
          ['acme:notes:1.2.3#notes', '--line=a', '--set=first'],
          2,
          'cook: acme:notes:1.2.3#notes: Unknown argument --set'
        ],
        [['acme:notes:1.2.3#notes', 'line=first'], 2, 'cook: Not a recipe: line=first'],
        [
          ['../outside.groovy'],
          2,
          'cook: ../outside.groovy: the recipe is outside the kitchen'
        ],
        [
          ['https://example.com/notes.groovy'],
          2,
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
          2,
          'cook: recipe 2 (acme:notes:1.2.3#more): acme:notes#more is named twice'
        ],
        [['acme:notes:1.2.3#notes'], 2, 'cook: acme:notes:1.2.3#notes: Missing --line: The line to add'],
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

  @Test void replicatesNamedRecipesWithRecordedArgumentsThroughTheReviewAndWriteFlow() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def cooked = [
        '-C',
        repo.path,
        '--batch-approve',
        'acme:notes:1.2.3#notes',
        '--line',
        'two words',
        '--loud',
        'acme:notes:1.2.3#more'
      ]
      assert cli.execute(cooked as String[]) == 0 : stderr.toString('UTF-8')
      def notes = new File(repo, 'NOTES.md')
      def expected = notes.text
      def lock = new File(repo, 'cook.lock')
      def recorded = lock.text
      assert notes.delete()
      def operands = ['acme:notes#notes', 'acme:notes#more']

      stdout.reset()
      assert cli.execute((['replicate', '--kitchen=' + repo.path, '--dry-run'] + operands) as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').startsWith('Confirm acme:notes:1.2.3#notes\n  Everyone reads `NOTES.md`.\n')
      assert stdout.toString('UTF-8').contains('\n+TWO WORDS\n')
      assert stdout.toString('UTF-8').endsWith('\nWould create NOTES.md\n')
      assert !notes.exists()
      assert lock.text == recorded

      assert cli.execute((['replicate', '-C', repo.path] + operands) as String[]) == 1
      assert stderr.toString('UTF-8').startsWith('cook: replicate: approval required; nothing was written\n')
      assert !notes.exists()
      stderr.reset()
      def declined = new ByteArrayOutputStream()
      assert command(declined, stderr, 'n\n')
      .execute((['replicate', '-C', repo.path] + operands) as String[]) == 1
      assert stderr.toString('UTF-8') == 'cook: replicate: nothing was written\n  the answer was not y or yes\n'
      assert !notes.exists()
      assert lock.text == recorded

      stdout.reset(); stderr.reset()
      assert cli.execute((['-C', repo.path, 'replicate', '--batch-approve'] + operands) as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').endsWith('Created NOTES.md\n\n' +
      'After cooking acme:notes:1.2.3#notes\n  Read `NOTES.md`.\n')
      assert notes.text == expected
      assert lock.text == recorded
      stdout.reset()
      assert cli.execute((['replicate', '-C', repo.path] + operands) as String[]) == 0
      assert stdout.toString('UTF-8') == 'Already cooked.\n'
      assert stderr.toString('UTF-8') == ''
    }
  }

  @Test void updateDryRunReviewsEveryMoveWithoutWritingAndApprovalWritesTheWholeBatch() {
    workspace { root, repo ->
      Fixtures.notesReleases(repo, '1.2.3', '1.3.0', '2.0.0')
      Fixtures.notesCookbook(repo, ['acme/notes/more.groovy': '''import acme.notes.Notes
@ingredient(Notes) def notes
notes.add('newer')
'''], '1.3.0')
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def recipes = ['acme:notes:1.2.3#notes', '--line=recorded', 'acme:notes:1.2.3#more']
      assert cli.execute((['-C', repo.path, '--batch-approve'] + recipes) as String[]) == 0
      def lock = new File(repo, 'cook.lock'); def notes = new File(repo, 'NOTES.md')
      def recorded = lock.text; def content = notes.text

      stdout.reset()
      assert cli.execute(['update', '--kitchen=' + repo.path, '--dry-run', '--batch-approve'] as String[]) == 1 :
      stderr.toString('UTF-8')
      def moves = 'Update acme:notes#more 1.2.3 -> 1.3.0\nUpdate acme:notes#notes 1.2.3 -> 1.3.0\n'
      assert stdout.toString('UTF-8').startsWith(moves + 'Confirm acme:notes:1.3.0#notes\n')
      assert stdout.toString('UTF-8').contains('\n-more\n+newer\n')
      assert stdout.toString('UTF-8').contains('--- a/cook.lock\n+++ b/cook.lock\n')
      assert stdout.toString('UTF-8').endsWith('Would update NOTES.md\nWould update cook.lock\n')
      assert !stdout.toString('UTF-8').contains('Write these changes?')
      assert !stdout.toString('UTF-8').contains('Holding ')
      assert lock.text == recorded
      assert notes.text == content
      assert stderr.toString('UTF-8') == ''

      stdout.reset()
      assert command(stdout, stderr, 'yes\n').execute(['-C', repo.path, 'update'] as String[]) == 0
      assert stdout.toString('UTF-8').startsWith(moves + 'Confirm acme:notes:1.3.0#notes\n')
      assert stdout.toString('UTF-8').count('Write these changes? [y/N]') == 1
      assert stdout.toString('UTF-8').contains('Updated NOTES.md\nUpdated cook.lock\n')
      assert notes.text.contains('\nrecorded\n')
      assert notes.text.contains('\nnewer\n')
      assert !lock.text.contains('1.2.3')
    }
  }

  @Test void updateRequiresApprovalAndBatchApproveWritesAfterReview() {
    workspace { root, repo ->
      Fixtures.notesReleases(repo, '1.2.3', '1.2.4')
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#more'] as String[]) == 0
      def lock = new File(repo, 'cook.lock'); def recorded = lock.text
      def notes = new File(repo, 'NOTES.md'); def content = notes.text

      stdout.reset()
      assert cli.execute(['update', '-C', repo.path] as String[]) == 1
      assert stdout.toString('UTF-8').startsWith('Update acme:notes#more 1.2.3 -> 1.2.4\n')
      assert stdout.toString('UTF-8').contains('--- a/cook.lock\n+++ b/cook.lock\n')
      assert stderr.toString('UTF-8').startsWith('cook: update: approval required; nothing was written\n')
      assert lock.text == recorded
      assert notes.text == content

      stdout.reset(); stderr.reset()
      assert command(stdout, stderr, 'n\n').execute(['update', '-C', repo.path] as String[]) == 1
      assert stdout.toString('UTF-8').endsWith('Write these changes? [y/N] ')
      assert stderr.toString('UTF-8') == 'cook: update: nothing was written\n  the answer was not y or yes\n'
      assert lock.text == recorded
      assert notes.text == content

      stdout.reset(); stderr.reset()
      assert cli.execute(['update', '-C', repo.path, '--batch-approve'] as String[]) == 0
      assert stdout.toString('UTF-8').contains('--- a/cook.lock\n+++ b/cook.lock\n')
      assert stdout.toString('UTF-8').endsWith('Updated cook.lock\n')
      assert !stdout.toString('UTF-8').contains('Write these changes?')
      assert lock.text.contains('acme:notes:1.2.4#more')
      assert stderr.toString('UTF-8') == ''
      [[], ['--dry-run']].each { options ->
        stdout.reset()
        assert cli.execute((['update', '-C', repo.path] + options) as String[]) == 0
        assert stdout.toString('UTF-8') == 'Already at the newest releases.\n'
      }
    }
  }

  @Test void updateHoldsANewerMajorUntilMajorIsRequested() {
    workspace { root, repo ->
      Fixtures.notesReleases(repo, '1.2.3', '2.0.0')
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#more'] as String[]) == 0
      def lock = new File(repo, 'cook.lock'); def recorded = lock.text
      [[], ['--dry-run']].each { options ->
        stdout.reset()
        assert cli.execute((['update', '-C', repo.path] + options) as String[]) == 0
        assert stdout.toString('UTF-8') ==
        'Holding acme:notes#more 1.2.3; 2.0.0 needs --major\nAlready at the newest releases.\n'
        assert lock.text == recorded
      }
      stdout.reset()
      assert cli.execute(['update', '-C', repo.path, '--major', '--dry-run'] as String[]) == 1
      assert stdout.toString('UTF-8').startsWith('Update acme:notes#more 1.2.3 -> 2.0.0\n')
      assert lock.text == recorded
      assert cli.execute(['update', '-C', repo.path, '--major', '--batch-approve'] as String[]) == 0
      assert lock.text.contains('acme:notes:2.0.0#more')
      assert stderr.toString('UTF-8') == ''
    }
  }

  @Test void updateTakesOnlyTheNamedLockRecipes() {
    workspace { root, repo ->
      Fixtures.notesReleases(repo, '1.2.3', '1.3.0')
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def recipes = ['acme:notes:1.2.3#notes', '--line=recorded', 'acme:notes:1.2.3#more']
      assert cli.execute((['-C', repo.path, '--batch-approve'] + recipes) as String[]) == 0
      stdout.reset()
      assert cli.execute(['update', '-C', repo.path, '--batch-approve', 'acme:notes#more'] as String[]) == 0
      assert stdout.toString('UTF-8').startsWith('Update acme:notes#more 1.2.3 -> 1.3.0\n')
      assert !stdout.toString('UTF-8').contains('Update acme:notes#notes')
      def lock = new File(repo, 'cook.lock')
      assert lock.text.contains('acme:notes:1.2.3#notes')
      assert lock.text.contains('acme:notes:1.3.0#more')
      assert stderr.toString('UTF-8') == ''
    }
  }

  @Test void updateWarnsAboutUnknownReleasesAndContinuesWithTheOtherRecipes() {
    workspace { root, repo ->
      Fixtures.notesReleases(repo, '1.2.3', '1.3.0')
      def cookbook = new File(repo, 'sources'); cookbook.mkdirs()
      new File(cookbook, 'cookbook.toml').text = 'group = "acme"\n'
      def artifact = new File(cookbook, 'src/local'); artifact.mkdirs()
      new File(artifact, 'local.groovy').text = 'assert true\n'
      new File(repo, 'cook.toml').text = 'repositories = ["sources", "repository"]\n'
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      def recipes = ['acme:local:1.0.0#local', 'acme:notes:1.2.3#more']
      assert cli.execute((['-C', repo.path, '--batch-approve'] + recipes) as String[]) == 0
      stdout.reset()
      assert cli.execute(['update', '-C', repo.path, '--batch-approve'] as String[]) == 0
      assert stdout.toString('UTF-8').startsWith('Update acme:notes#more 1.2.3 -> 1.3.0\n')
      assert new File(repo, 'cook.lock').text.contains('acme:notes:1.3.0#more')
      def warning = stderr.toString('UTF-8')
      assert warning.startsWith('cook: update: warning: skipping acme:local#local; releases are unknown\n')
      assert warning.contains("  repository ${cookbook.toURI()} cannot list releases\n")
      assert warning.contains('  cook the release you want by its coordinates, such as ./cook acme:local:1.0.0#local\n')
      [[], ['--dry-run']].each { options ->
        stdout.reset(); stderr.reset()
        assert cli.execute((['update', '-C', repo.path] + options) as String[]) == 0
        assert stdout.toString('UTF-8') == 'Already at the newest releases.\n'
        assert stderr.toString('UTF-8') == warning
      }
      stderr.reset()
      assert cli.execute(['update', '-C', repo.path, 'acme:local#local'] as String[]) == 2
      assert stderr.toString('UTF-8').startsWith('cook: update: acme:local#local has no known releases')
    }
  }

  @Test void updateRejectsRecipeArgumentsVersionedNamesAndMisplacedOrUnsupportedOptions() {
    workspace { root, repo ->
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['update', '-C', repo.path, '--dry-run'] as String[]) == 2
      assert stderr.toString('UTF-8').startsWith('cook: update: there is no cook.lock\n')
      Fixtures.notesReleases(repo, '1.2.3')
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes:1.2.3#more'] as String[]) == 0
      [
        [['update', 'acme:notes:1.2.3#more'], 'acme:notes:1.2.3#more is not a name cook.lock writes'],
        [['update', 'missing'], 'cook.lock lists no recipe missing; nothing was written'],
        [['update', 'acme:notes#more', '--line=a'], 'update takes no recipe arguments: --line=a'],
        [['update', 'acme:notes#more', '--batch-approve'], '--batch-approve goes before the first recipe'],
        [['update', 'acme:notes#more', '--dry-run'], '--dry-run goes before the first recipe'],
        [['update', '--force'], "Unknown option: '--force'"],
        [['update', '--skip'], "Unknown option: '--skip'"],
      ].each { arguments, message ->
        stderr.reset()
        assert cli.execute((['-C', repo.path] + arguments) as String[]) == 2 : arguments
        assert stderr.toString('UTF-8').startsWith("cook: update: ${message}\n")
      }
      ['--dry-run', '--batch-approve', '--force', '--skip'].each { option ->
        stderr.reset()
        assert cli.execute(['-C', repo.path, option, 'update'] as String[]) == 2
        assert stderr.toString('UTF-8') == 'cook: update: --dry-run, --batch-approve, --force and --skip ' +
        'go before a recipe, not before a command\n  run: ./cook help\n'
      }
    }
  }

  @Test void replicateReviewsChangedPathSourcesBeforeWriting() {
    workspace { root, repo ->
      new File(repo, 'NotesModel.groovy').text = Fixtures.NOTES_MODEL
      def recipe = new File(repo, 'local.groovy')
      recipe.text = """import acme.notes.Notes
@param(value = ['line']) String line
@ingredient(Notes) def notes
notes.add(line)
"""
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, '--batch-approve', 'local.groovy', '--line=recorded'] as String[]) == 0
      def lock = new File(repo, 'cook.lock')
      def recorded = (lock.text =~ /sha256: "(\w+)"/)[0][1]
      recipe.text = recipe.text.replace('notes.add(line)', "notes.add(line + ' again')")

      stdout.reset()
      assert cli.execute(['check', '-C', repo.path, 'local'] as String[]) == 1
      assert stdout.toString('UTF-8').contains('\n  its sources changed; review the diff\n\n')

      stdout.reset()
      assert command(stdout, stderr, 'yes\n')
      .execute(['replicate', '-C', repo.path, 'local'] as String[]) == 0 : stderr.toString('UTF-8')
      def current = (lock.text =~ /sha256: "(\w+)"/)[0][1]
      assert current != recorded
      assert stdout.toString('UTF-8').startsWith("""Changed source local.groovy
  cook.lock recorded sha256 ${recorded}; the source now has sha256 ${current}
  its sources changed; review the diff before approving

--- a/NOTES.md
+++ b/NOTES.md
""")
      assert stdout.toString('UTF-8').contains('Write these changes? [y/N] Updated NOTES.md\nUpdated cook.lock\n')
      assert new File(repo, 'NOTES.md').text.contains('\nrecorded again\n')
    }
  }

  @Test void discardsByLockNameButCookingStillRequiresAVersion() {
    workspace { root, repo ->
      Fixtures.notesCookbook(repo)
      def stdout = new ByteArrayOutputStream(); def stderr = new ByteArrayOutputStream()
      def cli = command(stdout, stderr)
      assert cli.execute(['-C', repo.path, '--batch-approve', 'acme:notes#notes'] as String[]) == 2
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
      .execute(['discard', '-C', repo.path, 'acme:notes#more'] as String[]) == 1
      assert declined.toString('UTF-8').contains('\n-<!-- cook:begin acme:notes#more/notes -->\n-more\n' +
      '-<!-- cook:end acme:notes#more/notes -->\n')
      assert stderr.toString('UTF-8') == 'cook: discard: nothing was written\n  the answer was not y or yes\n'

      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--dry-run', 'acme:notes#more'] as String[]) == 0
      assert stdout.toString('UTF-8').endsWith('\nWould update NOTES.md\nWould update cook.lock\n')
      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--batch-approve', 'acme:notes#more'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').endsWith('\nUpdated NOTES.md\nUpdated cook.lock\n')
      assert notes.text ==
      '<!-- cook:begin acme:notes#notes/notes -->\nfirst\n<!-- cook:end acme:notes#notes/notes -->\nmine\n'

      stdout.reset()
      assert cli.execute(['discard', '-C', repo.path, '--batch-approve', 'acme:notes#notes'] as String[]) == 0 :
      stderr.toString('UTF-8')
      assert stdout.toString('UTF-8').endsWith('\nUpdated NOTES.md\nDeleted cook.lock\n')
      assert notes.text == 'mine\n'
      assert !lock.exists()

      [
        [['replicate'], 2, 'cook: replicate: there is no cook.lock'],
        [['replicate', 'notes'], 2, 'cook: replicate: cook.lock lists no recipe notes; nothing was written'],
        [
          ['replicate', 'acme:notes#notes', '--batch-approve'],
          2,
          'cook: replicate: --batch-approve goes before the first recipe'
        ],
        [
          ['replicate', 'acme:notes#notes', '--line=a'],
          2,
          'cook: replicate: replicate takes no recipe arguments: --line=a'
        ],
        [
          ['replicate', 'acme:notes#notes', '-x'],
          2,
          'cook: replicate: replicate takes no recipe arguments: -x'
        ],
        [
          ['--dry-run', 'replicate', 'acme:notes#notes'],
          2,
          'cook: replicate: --dry-run, --batch-approve, --force and --skip go before a recipe, not before a command'
        ],
        [
          ['replicate', 'acme:notes#notes'],
          2,
          'cook: replicate: cook.lock lists no recipe acme:notes#notes; nothing was written'
        ],
        [
          ['replicate', 'acme:notes:1.2.3#notes'],
          2,
          'cook: replicate: acme:notes:1.2.3#notes is not a name cook.lock writes'
        ],
        [
          ['replicate', 'https://example.com/notes.groovy'],
          2,
          'cook: replicate: a URL is not a recipe'
        ],
        [['replicate', '--check'], 2, "cook: replicate: Unknown option: '--check'"],
        [['replicate', '--force'], 2, "cook: replicate: Unknown option: '--force'"],
        [['replicate', '--skip'], 2, "cook: replicate: Unknown option: '--skip'"],
        [['check'], 2, 'cook: check: there is no cook.lock'],
        [['check', 'notes.groovy'], 2, 'cook: check: notes.groovy is not a name cook.lock writes'],
        [['check', 'acme:notes#notes', '--line=a'], 2, 'cook: check: check takes no recipe arguments: --line=a'],
        [['check', '--batch-approve'], 2, "cook: check: Unknown option: '--batch-approve'"],
        [['check', '--dry-run'], 2, "cook: check: Unknown option: '--dry-run'"],
        [['discard'], 2, 'cook: discard: A recipe is required'],
        [['discard', 'notes'], 2, 'cook: discard: cook.lock lists no recipe notes; nothing was written'],
        [
          ['discard', 'acme:notes#notes', '--batch-approve'],
          2,
          'cook: discard: --batch-approve goes before the first recipe'
        ],
        [
          ['discard', 'acme:notes#notes', '--line=a'],
          2,
          'cook: discard: discard takes no recipe arguments: --line=a'
        ],
        [
          ['--dry-run', 'discard', 'acme:notes#notes'],
          2,
          'cook: discard: --dry-run, --batch-approve, --force and --skip go before a recipe, not before a command'
        ],
        [
          ['discard', 'acme:notes:1.2.3#notes'],
          2,
          'cook: discard: acme:notes:1.2.3#notes is not a name cook.lock writes'
        ],
        [
          ['help', 'https://example.com/notes.groovy'],
          2,
          'cook: help: a URL is not a recipe'
        ],
        [
          ['discard', 'https://example.com/notes.groovy'],
          2,
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
      assert cli.execute(['discard', '-C', repo.path, 'acme:notes#notes'] as String[]) == 2
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
