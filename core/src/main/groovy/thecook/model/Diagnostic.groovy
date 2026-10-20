package thecook.model

/**
 * The text of an error as the message standard lays it out: what failed on the first line, then why it failed and what
 * to do next, each on an indented line when known.
 */
final class Diagnostic {
  private Diagnostic() { }

  static String text(String what, String why, String next = null) {
    ([what] + [why, next].findAll().collect { "  ${it}".toString() }).join('\n')
  }
}
