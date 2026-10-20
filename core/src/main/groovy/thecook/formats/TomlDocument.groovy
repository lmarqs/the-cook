package thecook.formats

import thecook.model.Diagnostic
import thecook.rendering.RegionLocation
import thecook.rendering.KeyLocation

import org.tomlj.Toml

class TomlDocument implements Document {
  String text
  final List<Map> createdTables = []
  TomlDocument(String text) {
    this.text = text; parsed()
  }
  boolean isEmpty() {
    parsed().isEmpty() && !text.trim()
  }
  private def parsed() {
    def result = Toml.parse(text)

    if (result.hasErrors()) {
      throw new IllegalArgumentException("TOML: ${result.errors()}")
    }

    result
  }
  private int offset(def position) {
    int index = 0

    for (int line = 1; line < position.line(); line++) {
      index = text.indexOf('\n', index) + 1
    }

    index + position.column() - 1
  }
  private List<Integer> bounds(List<String> path) {
    def result = parsed()
    def position = result.inputPositionOf(path)

    if (!position) {
      return null
    }

    int start = offset(position)
    def shape = tableShape(path, result, start)

    if (shape) {
      throw new IllegalArgumentException(Diagnostic.text("key ${tableName(path)} is written as ${shape}",
      "Cook owns ${tableName(path)} whole and writes it as one inline table",
      "replace it with ${inlineForm(path)}, then cook again"))
    }

    ValueSpan.afterEquals(text, start)
  }
  // A key Cook owns whole is one value it replaces; a header or dotted keys spread it over lines it cannot replace.
  private String tableShape(List<String> path, def result, int start) {
    def header = text.substring(text.lastIndexOf('\n', start - 1) + 1) =~ /^[ \t]*(\[\[[^\]\r\n]*]]|\[[^\]\r\n]*])/

    if (header.find()) {
      return header.group(1).startsWith('[[') ? "a ${header.group(1)} array of tables" : "a ${header.group(1)} table"
    }

    if (result.isTable(path) && text.charAt(ValueSpan.afterEquals(text, start)[0]) != '{' as char) {
      return "a dotted key ${text.substring(start, text.indexOf('=', start)).trim()}"
    }

    null
  }
  private static String inlineForm(List<String> path) {
    path.size() == 1 ? "${keyName(path[0])} = { … } above the first [table]" :
        "${keyName(path[-1])} = { … } in the [${tableName(path.dropRight(1))}] table"
  }
  @Override
  String read(String id, RegionLocation location) {
    def range = bounds(((KeyLocation) location).path)
    range ? text.substring(range[0], range[1]) : null
  }
  String render(Object value) {
    if (value instanceof Map) {
      return '{ ' + value.collect { key, item -> keyName(key.toString()) + ' = ' + render(item) }.join(', ') + ' }'
    }

    if (value instanceof Collection) {
      return '[' + value.collect { render(it) }.join(', ') + ']'
    }

    if (value instanceof String || value instanceof GString) {
      return quote(value.toString())
    }

    if (value instanceof Boolean || value instanceof Number) {
      return value.toString()
    }

    throw new IllegalArgumentException("Unsupported TOML value: ${value}")
  }
  private static String quote(String value) {
    new groovy.json.JsonGenerator.Options().disableUnicodeEscaping()
        .build()
        .toJson(value)
  }
  private static String keyName(String key) {
    key ==~ /[a-zA-Z0-9_-]+/ ? key : quote(key)
  }
  private static String tableName(List<String> path) {
    path.collect { keyName(it) }.join('.')
  }
  @Override
  void write(String id, RegionLocation location, String content) {
    def range = bounds(((KeyLocation) location).path)

    if (range) {
      splice(range[0], range[1], content); return
    }

    def table = ((KeyLocation) location).path.dropRight(1)
    def header = findHeader(table)

    if (table && !header) {
      appendTable(table); header = findHeader(table)
    }

    int insertion = header ? header[1] : 0
    def ending = text.contains('\r\n') ? '\r\n' : '\n'
    splice(insertion, insertion, keyName(((KeyLocation) location).path[-1]) + ' = ' + content + ending)
  }
  private List<Integer> findHeader(List<String> path) {
    if (!path) {
      return null
    }

    def matcher = text =~ /(?m)^[ \t]*\[(?!\[)([^\r\n]+)\][ \t]*(?:#[^\r\n]*)?\r?\n?/

    while (matcher.find()) {
      def probe = Toml.parse(matcher.group() + '\n__cook_table_probe = 1\n')

      if (!probe.hasErrors() && probe.getTable(path)?.contains('__cook_table_probe')) {
        return [matcher.start(), matcher.end()]
      }
    }

    null
  }
  private void appendTable(List<String> table) {
    def ending = text.contains('\r\n') ? '\r\n' : '\n'
    def separator = text ? (text.endsWith('\n') ? ending : ending + ending) : ''
    text += separator + '[' + tableName(table) + ']' + ending
    createdTables.add([path: table, separator: separator])
    parsed()
  }
  @Override
  void remove(String id, RegionLocation location) {
    def range = bounds(((KeyLocation) location).path)

    if (range) {
      int start = text.lastIndexOf('\n', range[0] - 1) + 1
      int end = text.indexOf('\n', range[1]); end = end < 0 ? text.length() : end + 1
      def suffix = text.substring(range[1], end)
      splice(start, end, suffix.contains('#') ? suffix.substring(suffix.indexOf('#')) : '')
    }

    cleanupTables()
  }
  private void cleanupTables() {
    createdTables.toList().reverseEach { table ->
      def header = findHeader(table.path)

      if (!header) {
        return
      }

      def following = (text.substring(header[1]) =~ /(?m)^[ \t]*\[/)
      int end = following.find() ? header[1] + following.start() : text.length()

      if (text.substring(header[1], end).trim()) {
        return
      }

      int start = header[0]

      if (table.separator && start >= table.separator.length() &&
          text.substring(start - table.separator.length(), start) == table.separator) {
        start -= table.separator.length()
      }

      // Following whitespace may be the next table's separator; only this header is ours.
      splice(start, header[1], '')
      createdTables.remove(table)
    }
  }
  private void splice(int start, int end, String value) {
    text = text.substring(0, start) + value + text.substring(end); parsed()
  }
}
