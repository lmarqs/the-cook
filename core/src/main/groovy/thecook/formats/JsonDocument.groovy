package thecook.formats

import thecook.rendering.RegionLocation
import thecook.rendering.KeyLocation

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class JsonDocument implements Document {
  String text
  final List<List<String>> createdParents = []
  JsonDocument(String text) {
    this.text = text ?: '{}\n'; root()
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
  @SuppressWarnings('UnusedMethodParameter')
  String readBefore(String id, RegionLocation location, String content) {
    read(id, location)
  }
  @Override
  String read(String id, RegionLocation location) {
    def node = at(((KeyLocation) location).path)
    node ? text.substring(node.start, node.end) : null
  }
  String render(Object value) {
    JsonOutput.toJson(value)
  }
  /** `value` as JSON, each object member and array item on its own line, two spaces deeper than `indent`. */
  static String indented(Object value, String indent) {
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
  @Override
  void write(String id, RegionLocation location, String content) {
    def path = ((KeyLocation) location).path
    ensureParents(path.dropRight(1))
    def node = at(path)

    if (node) {
      def indent = lineIndent(at(path.dropRight(1)).members[path[-1]].keyStart)
      splice(node.start, node.end, indent == null ? content : indented(new JsonSlurper().parseText(content), indent))
      return
    }

    insert(path, content)
  }
  private void ensureParents(List<String> path) {
    for (int count = 1; count <= path.size(); count++) {
      def parent = path.take(count)

      if (!at(parent)) {
        insert(parent, '{}'); createdParents.add(parent)
      }

      if (at(parent).kind != 'object') {
        throw new IllegalArgumentException("Expected JSON object at ${parent}")
      }
    }
  }
  private void insert(List<String> path, String content) {
    def parent = at(path.dropRight(1))
    def members = parent.members.values().toList()
    int offset = members ? members[-1].value.end : parent.start + 1
    def indent = members ? lineIndent(members[-1].keyStart) : null
    def entry = indent == null ? (members ? ', ' : '') + JsonOutput.toJson(path[-1]) + ':' + content :
        ',\n' + indent + JsonOutput.toJson(path[-1]) + ': ' + indented(new JsonSlurper().parseText(content), indent)
    splice(offset, offset, entry)
  }
  // The whitespace before `offset` on its line, or null when other text precedes it there, as in a one-line file.
  private String lineIndent(int offset) {
    def before = text.substring(text.lastIndexOf('\n', offset - 1) + 1, offset)
    before.trim() ? null : before
  }
  @Override
  void remove(String id, RegionLocation location) {
    deleteKey(((KeyLocation) location).path)
    def deepestFirst = createdParents.sort { -it.size() }.toList()

    deepestFirst.each { path ->
      def node = at(path)

      if (node?.kind == 'object' && node.members.isEmpty()) {
        deleteKey(path)
        createdParents.remove(path)
      }
    }
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
