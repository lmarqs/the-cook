package thecook

import groovy.json.JsonSlurper
import org.junit.Test
import thecook.formats.BlockDocument
import thecook.formats.Document
import thecook.formats.Documents
import thecook.rendering.BlockLocation
import thecook.rendering.KeyLocation

class DocumentsTest {
  @Test void markdownReadsBackABlockWhoseIdContainsHashes() {
    def id = 'acme:notes#more/notes'
    def written = new BlockDocument('# Notes\n', '<!-- ', ' -->')
    written.write(id, new BlockLocation(), written.render('more'))
    assert written.text ==
    '# Notes\n<!-- cook:begin acme:notes#more/notes -->\nmore\n<!-- cook:end acme:notes#more/notes -->\n'

    def reopened = new BlockDocument(written.text, '<!-- ', ' -->')
    assert reopened.read(id, new BlockLocation()) == 'more\n'
    reopened.remove(id, new BlockLocation())
    assert reopened.text == '# Notes\n'
  }

  @Test void jsonChangesOneNestedValueAndKeepsEveryOtherByte() {
    def original = '{\n "name" : "fixture", "devDependencies" : { "@types/node": "^20.0.0", "other" : "keep" }\n}\n'
    def document = open('json', original)
    set(document, ['devDependencies', '@types/node'], '^22.0.0')
    assert document.text == original.replace('^20.0.0', '^22.0.0')
  }

  @Test void jsonCreatesMissingParentsAndRemovesOnlyTheEmptyOnesItCreated() {
    def document = open('json', '{"name":"fixture","kept":{}}')
    set(document, ['devDependencies', '@types/node'], '^22.0.0')
    set(document, ['kept', 'value'], 1)
    assert new JsonSlurper().parseText(document.text) ==
    [name: 'fixture', kept: [value: 1], devDependencies: ['@types/node': '^22.0.0']]
    remove(document, ['devDependencies', '@types/node'])
    remove(document, ['kept', 'value'])
    assert new JsonSlurper().parseText(document.text) == [name: 'fixture', kept: [:]]
  }

  @Test void jsonRemovesObjectsLeftEmptyByRemovingTheirLastKeyAndKeepsOnesWithOtherKeys() {
    def emptied = open('json', '{\n  "permissions": {},\n  "hooks": {\n    "SessionStart": []\n  }\n}\n')
    remove(emptied, ['hooks', 'SessionStart'])
    assert emptied.text == '{\n  "permissions": {}\n}\n'

    def nested = open('json', '{"a": {"b": {"c": 1}}}')
    remove(nested, ['a', 'b', 'c'])
    assert new JsonSlurper().parseText(nested.text) == [:]

    def shared = open('json', '{"hooks": {"SessionStart": [], "PreToolUse": []}}')
    remove(shared, ['hooks', 'SessionStart'])
    assert new JsonSlurper().parseText(shared.text) == [hooks: [PreToolUse: []]]
  }

  @Test void jsonKeepsNullsArraysEscapedStringsAndSlashedKeysApart() {
    def document = open('json', '{}')
    set(document, ['a/b'], null)
    set(document, ['a', 'b'], [true, false, 'quote"'])
    assert new JsonSlurper().parseText(document.text) == ['a/b': null, a: [b: [true, false, 'quote"']]]
  }

  @Test void jsonWritesAValueIntoAnIndentedFileAtTheDepthOfItsKey() {
    def document = open('json', '{\n  "permissions": {\n    "allow": [\n      "a"\n    ]\n  },\n  "other": 1\n}\n')
    set(document, ['permissions'], [allow: ['a', 'b'], deny: []])
    set(document, ['hooks'], [stop: ['x']])
    assert document.text == '''\
{
  "permissions": {
    "allow": [
      "a",
      "b"
    ],
    "deny": []
  },
  "other": 1,
  "hooks": {
    "stop": [
      "x"
    ]
  }
}
'''
  }

  @Test void jsonWritesANestedKeyIntoAnIndentedFileAtTheDepthOfItsKeyThroughNewAndEmptyParents() {
    def document = open('json', '{\n  "permissions": {},\n  "empty": {}\n}\n')
    set(document, ['hooks', 'SessionStart'], [[type: 'command']])
    set(document, ['empty', 'key'], 1)
    assert document.text == '''\
{
  "permissions": {},
  "empty": {
    "key": 1
  },
  "hooks": {
    "SessionStart": [
      {
        "type": "command"
      }
    ]
  }
}
'''
  }

  @Test void jsonKeepsAOneLineFileOnOneLine() {
    def document = open('json', '{"a": {"b": 1}, "c": 2}\n')
    set(document, ['a'], [b: 2])
    set(document, ['d'], [3])
    assert document.text == '{"a": {"b":2}, "c": 2, "d":[3]}\n'
  }

  @Test void jsonRefusesDuplicateKeys() {
    def error = shouldFail { open('json', '{"x":1,"x":2}') }
    assert error.message.contains('Duplicate JSON key')
  }

  @Test void yamlRemovesMappingsLeftEmptyByRemovingTheirLastKeyInALaterDocument() {
    def written = open('yaml', 'name: fixture\nkept: {}\n')
    set(written, ['hooks', 'stop', 'command'], 'x')
    assert written.text == '"hooks": \n  "stop": \n    "command": "x"\nname: fixture\nkept: {}\n'

    def reopened = open('yaml', written.text)
    remove(reopened, ['hooks', 'stop', 'command'])
    assert reopened.text == 'name: fixture\nkept: {}\n'

    def shared = open('yaml', 'hooks:\n  stop: 1\n  start: 2\n')
    remove(shared, ['hooks', 'stop'])
    assert shared.text == 'hooks:\n  start: 2\n'
  }

  @Test void tomlUpdatesAPinWithoutTouchingCommentsOrOtherKeys() {
    def original = '# Team\n[tools]\nnode = "20" # pin\npython = "3.13"\n\n[env]\nAPP_MODE = "dev"\n'
    def document = open('toml', original)
    set(document, ['tools', 'node'], '22')
    assert document.text == original.replace('node = "20"', 'node = "22"')
  }

  @Test void tomlCreatesAnInlineTableAndRemovesTheTablesItCreated() {
    def original = '# Team\n[env]\nAPP_MODE = "dev"\n'
    def document = open('toml', original)
    set(document, ['tools', 'python'], '3.13')
    set(document, ['env', '_', 'python', 'venv'], [path: '.venv', create: true])
    assert document.text.contains('create = true')
    remove(document, ['env', '_', 'python', 'venv'])
    remove(document, ['tools', 'python'])
    assert document.text == original
  }

  @Test void tomlEditsAnExistingDottedKeyWithoutAddingATable() {
    def document = open('toml', '[env]\n_.python.venv = { path = ".venv", create = false } # keep\n')
    set(document, ['env', '_', 'python', 'venv'], [path: '.venv', create: true])
    assert document.text.contains('} # keep')
    assert document.text.contains('create = true')
    assert !document.text.contains('[env._.python]')
  }

  @Test void tomlRefusesAnOwnedKeyWrittenAsATableNamingTheShapeAndTheInlineForm() {
    def shapes = [
      '[k]\nx = 1\n': 'key k is written as a [k] table',
      '[k] # a = b\nx = 1\n': 'key k is written as a [k] table',
      '[k.sub]\nx = 1\n': 'key k is written as a [k.sub] table',
      'k.x = 1\n': 'key k is written as a dotted key k.x',
      '[[k]]\nx = 1\n': 'key k is written as a [[k]] array of tables',
    ]
    shapes.each { text, what ->
      def table = [
        what,
        '  Cook owns k whole and writes it as one inline table',
        '  replace it with k = { … } above the first [table], then cook again'
      ].join('\n')
      def line = [
        what,
        '  Cook owns k whole and writes it as one line, k = …',
        '  replace it with k = … above the first [table], then cook again'
      ].join('\n')
      def unknown = [
        what,
        '  Cook owns k whole as one line, k = …',
        '  replace it with k = … above the first [table], then cook again'
      ].join('\n')
      assert shouldFail { set(open('toml', text), ['k'], [x: 2]) }.message == table
      assert shouldFail { set(open('toml', text), ['k'], 'on') }.message == line
      assert shouldFail { set(open('toml', text), ['k'], [1, 2]) }.message == line
      def document = open('toml', text)
      assert shouldFail { document.readBefore('k', new KeyLocation(['k']), document.render([x: 2])) }.message == table
      assert shouldFail { document.readBefore('k', new KeyLocation(['k']), document.render(true)) }.message == line
      assert shouldFail { open('toml', text).read('k', new KeyLocation(['k'])) }.message == unknown
      assert shouldFail { remove(open('toml', text), ['k']) }.message == unknown
    }
  }

  @Test void tomlRefusesANestedOwnedKeyWrittenAsATableNamingItsTable() {
    def error = shouldFail { set(open('toml', '[a.k]\nx = 1\n'), ['a', 'k'], [x: 2]) }
    assert error.message == [
      'key a.k is written as a [a.k] table',
      '  Cook owns a.k whole and writes it as one inline table',
      '  replace it with k = { … } in the [a] table, then cook again'
    ].join('\n')
    def scalar = shouldFail { set(open('toml', '[a.k]\nx = 1\n'), ['a', 'k'], 2) }
    assert scalar.message == [
      'key a.k is written as a [a.k] table',
      '  Cook owns a.k whole and writes it as one line, k = …',
      '  replace it with k = … in the [a] table, then cook again'
    ].join('\n')
  }

  @Test void tomlStillWritesAnInlineTableAPlainKeyAndALeafInsideATable() {
    def inline = open('toml', 'k = { x = 1 } # keep\n')
    set(inline, ['k'], [x: 2])
    assert inline.text == 'k = { x = 2 } # keep\n'

    def plain = open('toml', 'k = 1\n[t]\ny = 2\n')
    set(plain, ['k'], 3)
    assert plain.text == 'k = 3\n[t]\ny = 2\n'

    def leaf = open('toml', '[k] # a = b\nx = 1\n')
    set(leaf, ['k', 'x'], 2)
    assert leaf.text == '[k] # a = b\nx = 2\n'
  }

  @Test void tomlRemovesATableLeftEmptyByRemovingItsLastKeyAndKeepsOneWithOtherContent() {
    def emptied = open('toml', 'a = 1\n\n[t]\nk = 1\n\n[u]\nx = 1\n')
    remove(emptied, ['t', 'k'])
    assert emptied.text == 'a = 1\n\n[u]\nx = 1\n'

    def commented = open('toml', 'a = 1\n\n[t]\n# ours\nk = 1\n')
    remove(commented, ['t', 'k'])
    assert commented.text == 'a = 1\n\n[t]\n# ours\n'

    def nested = open('toml', '[t]\nk = 1\n[t.sub]\nx = 1\n')
    remove(nested, ['t', 'k'])
    assert nested.text == '[t]\n[t.sub]\nx = 1\n'
  }

  private static Document open(String format, String text) {
    new Documents().open(format, text)
  }

  private static void set(Document document, List<String> path, Object value) {
    document.write(path.join('.'), new KeyLocation(path), document.render(value))
  }

  private static void remove(Document document, List<String> path) {
    document.remove(path.join('.'), new KeyLocation(path))
  }

  private static Throwable shouldFail(Closure action) {
    try {
      action()
    } catch (IllegalArgumentException error) {
      return error
    }

    throw new AssertionError('Expected an IllegalArgumentException')
  }
}
