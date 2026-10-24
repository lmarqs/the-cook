package thecook.formats

/** A text file holding marked blocks, each between a `cook:begin <id>` and a `cook:end <id>` line. */
class BlockDocument implements Document {
  String text
  private final String open
  private final String close

  /** `open` and `close` surround each marker line: `# ` and nothing by default, `<!-- ` and ` -->` in Markdown. */
  BlockDocument(String text, String open = '# ', String close = '') {
    this.text = text; this.open = open; this.close = close; validateMarkers()
  }
  boolean isEmpty() {
    text.isEmpty()
  }
  private String newline() {
    text.contains('\r\n') ? '\r\n' : '\n'
  }
  private String begin(String id) {
    "${open}cook:begin ${id}${close}"
  }
  private String end(String id) {
    "${open}cook:end ${id}${close}"
  }
  private String markedId(String line, String word) {
    def prefix = "${open}cook:${word} "
    line.startsWith(prefix) && line.endsWith(close) && line.length() >= prefix.length() + close.length() ?
        line.substring(prefix.length(), line.length() - close.length()) : null
  }
  private void validateMarkers() {
    String unclosed = null
    def seen = new HashSet()
    text.readLines().each { line ->
      def begun = markedId(line, 'begin')

      if (begun != null) {
        if (!(begun ==~ /[a-zA-Z0-9][a-zA-Z0-9_.:\/#-]*/)) {
          throw new IllegalArgumentException('Invalid Cook marker identity')
        }

        if (unclosed || !seen.add(begun)) {
          throw new IllegalArgumentException('Nested or duplicate Cook markers')
        }

        unclosed = begun
      }

      def ended = markedId(line, 'end')

      if (ended != null) {
        if (unclosed != ended) {
          throw new IllegalArgumentException('Mismatched Cook markers')
        }

        unclosed = null
      }
    }

    if (unclosed) {
      throw new IllegalArgumentException('Unterminated Cook marker')
    }
  }
  private List<Integer> bounds(String id) {
    def first = java.util.regex.Pattern.compile('(?m)^' + java.util.regex.Pattern.quote(begin(id)) + '\\r?$')
        .matcher(text)

    if (!first.find()) {
      return null
    }

    def last = java.util.regex.Pattern.compile('(?m)^' + java.util.regex.Pattern.quote(end(id)) + '\\r?$').matcher(text)

    if (!last.find(first.end())) {
      throw new IllegalArgumentException('Missing Cook end marker')
    }

    def finish = text.indexOf('\n', last.end())
    [first.start(), finish < 0 ? text.length() : finish + 1]
  }
  /** The text between the block's markers; `null` when the document holds no block `id`. */
  String read(String id) {
    def range = bounds(id)

    if (!range) {
      return null
    }

    def first = text.indexOf('\n', range[0]) + 1
    def marker = java.util.regex.Pattern.compile('(?m)^' + java.util.regex.Pattern.quote(end(id)) + '\\r?$')
        .matcher(text)
    marker.find(first)
    def last = marker.start()
    text.substring(first, last)
  }
  String render(Object value) {
    def result = value.toString()

    if (result.contains("${open}cook:begin ") || result.contains("${open}cook:end ")) {
      throw new IllegalArgumentException('Reserved marker in rendered content')
    }

    result = result.replace('\r\n', '\n').replace('\n', newline())
    result ? result.replaceAll(/(?:\r?\n)+$/, '') + newline() : ''
  }
  /** Replaces the block `id` with `content`, as `render` lays it out, or appends the block when absent. */
  void write(String id, String content) {
    def range = bounds(id)
    def block = begin(id) + newline() + content + end(id) + newline()

    if (range) {
      splice(range, block); return
    }

    text += (text && !text.endsWith('\n') ? newline() : '') + block
  }
  /** Removes the block `id` with its markers. */
  void remove(String id) {
    def range = bounds(id)

    if (range) {
      splice(range, '')
    }
  }
  private void splice(List<Integer> range, String replacement) {
    text = text.substring(0, range[0]) + replacement + text.substring(range[1])
  }
}
