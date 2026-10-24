package thecook.formats

/** The text of a file Cook edits in place. */
interface Document {
  String getText()

  /** Whether nothing is left, so the file is deleted. */
  boolean isEmpty()
}
