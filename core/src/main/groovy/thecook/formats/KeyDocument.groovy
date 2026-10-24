package thecook.formats

/** A JSON, TOML or YAML document whose keys Cook edits by their path of segments, keeping the rest of the text. */
interface KeyDocument extends Document {
  /** Writes `value` at `path`, creating its parents; `false`, changing nothing, when the key holds it already. */
  boolean set(List<String> path, Object value)

  /** Removes the key at `path` and the parents it leaves empty, unless they were empty before. */
  void remove(List<String> path)
}
