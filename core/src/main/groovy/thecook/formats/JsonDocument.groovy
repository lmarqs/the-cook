package thecook.formats

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class JsonDocument implements KeyDocument {
  String text
  // A file Cook creates is indented; one that exists keeps its layout.
  private final boolean created
  private final List<List<String>> emptyWhenOpened
  JsonDocument(String text) {
    created = text == null
    this.text = text ?: '{}\n'
    emptyWhenOpened = emptyObjects(root(), [])
  }
  boolean isEmpty() {
    root().members.isEmpty()
  }
  private Map root() {
    new JsonSyntax(text).parse()
  }
  private Map at(List<String> path) {
    def node = root()

    for (part in path) {
      if (node.kind != 'object') {
        throw new IllegalArgumentException("Expected JSON object before ${part}")
      }

      node = node.members[part]?.value

      if (!node) {
        return null
      }
    }

    node
  }
  @Override
  boolean set(List<String> path, Object value) {
    def rendered = JsonOutput.toJson(value)
    def node = at(path)

    if (node && parsed(text.substring(node.start, node.end)) == parsed(rendered)) {
      return false
    }

    write(path, rendered)

    if (created) {
      text = indented(parsed(text), '') + '\n'
    }

    true
  }
  private static Object parsed(String json) {
    new JsonSlurper().parseText(json)
  }
  // `value` as JSON, each object member and array item on its own line, two spaces deeper than `indent`.
  private static String indented(Object value, String indent) {
    def inner = indent + '  '

    if (value instanceof Map && value) {
      return '{\n' + value.collect { key, item ->
        inner + JsonOutput.toJson(key.toString()) + ': ' + indented(item, inner)
      }.join(',\n') + '\n' + indent + '}'
    }

    if (value instanceof List && value) {
      return '[\n' + value.collect { inner + indented(it, inner) }.join(',\n') + '\n' + indent + ']'
    }

    JsonOutput.toJson(value)
  }
  private void write(List<String> path, String content) {
    ensureParents(path.dropRight(1))
    def node = at(path)

    if (node) {
      def indent = lineIndent(at(path.dropRight(1)).members[path[-1]].keyStart)
      splice(node.start, node.end, indent == null ? content : indented(parsed(content), indent))
      return
    }

    insert(path, content)
  }
  private void ensureParents(List<String> path) {
    for (int count = 1; count <= path.size(); count++) {
      def parent = path.take(count)

      if (!at(parent)) {
        insert(parent, '{}')
      }

      if (at(parent).kind != 'object') {
        throw new IllegalArgumentException("Expected JSON object at ${parent}")
      }
    }
  }
  private void insert(List<String> path, String content) {
    def parent = at(path.dropRight(1))
    def members = parent.members.values().toList()
    def parentIndent = !members && path.size() > 1 ?
        lineIndent(at(path.dropRight(2)).members[path[-2]].keyStart) : null

    if (parentIndent != null) {
      def inner = parentIndent + '  '
      splice(parent.start, parent.end, '{\n' + inner + JsonOutput.toJson(path[-1]) + ': ' +
          indented(parsed(content), inner) + '\n' + parentIndent + '}')
      return
    }

    int offset = members ? members[-1].value.end : parent.start + 1
    def indent = members ? lineIndent(members[-1].keyStart) : null
    def entry = indent == null ? (members ? ', ' : '') + JsonOutput.toJson(path[-1]) + ':' + content :
        ',\n' + indent + JsonOutput.toJson(path[-1]) + ': ' + indented(parsed(content), indent)
    splice(offset, offset, entry)
  }
  // The whitespace before `offset` on its line, or null when other text precedes it there, as in a one-line file.
  private String lineIndent(int offset) {
    def before = text.substring(text.lastIndexOf('\n', offset - 1) + 1, offset)
    before.trim() ? null : before
  }
  @Override
  void remove(List<String> path) {
    deleteKey(path)
    removeEmptiedParents(path.dropRight(1))
  }
  // An object whose last key was removed is dropped, as TOML drops an emptied table, so withdrawing the key Cook wrote
  // into an object it added leaves no parent behind in a later invocation; one that was empty when opened stays.
  private void removeEmptiedParents(List<String> path) {
    for (def parent = path; parent; parent = parent.dropRight(1)) {
      def node = at(parent)

      if (node?.kind != 'object' || !node.members.isEmpty() || parent in emptyWhenOpened) {
        return
      }

      deleteKey(parent)
    }
  }
  private static List<List<String>> emptyObjects(Map node, List<String> path) {
    if (node.kind != 'object') {
      return []
    }

    def nested = node.members.collectMany { key, member -> emptyObjects(member.value, path + [key]) }
    node.members.isEmpty() && path ? [path] + nested : nested
  }
  private void deleteKey(List<String> path) {
    def parent = at(path.dropRight(1))

    if (!parent) {
      return
    }

    def entry = parent.members[path[-1]]

    if (!entry) {
      return
    }

    def members = parent.members.values().toList()
    int index = members.indexOf(entry)

    if (index > 0) {
      splice(members[index - 1].value.end, entry.value.end, ''); return
    }

    int end = members.size() > 1 ? members[1].keyStart : entry.value.end
    splice(entry.keyStart, end, '')
  }
  private void splice(int start, int end, String replacement) {
    text = text.substring(0, start) + replacement + text.substring(end); root()
  }
}
