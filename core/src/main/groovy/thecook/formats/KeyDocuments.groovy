package thecook.formats

/** Opens the key document of a file by its extension. */
final class KeyDocuments {
  private static final Map<String, Closure<KeyDocument>> OPENERS = [
    json: { String text -> new JsonDocument(text) },
    toml: { String text -> new TomlDocument(text) },
    yaml: { String text -> new YamlDocument(text) },
  ]

  private KeyDocuments() { }

  /** `json`, `toml` or `yaml` by the file's extension; `null` for any other file. */
  static String format(String file) {
    def extension = file.contains('.') ? file.substring(file.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : ''
    [json: 'json', toml: 'toml', yaml: 'yaml', yml: 'yaml'][extension]
  }

  /** The document of `file` holding `text`, or an empty one when `text` is `null`, as for a file not created yet. */
  static KeyDocument open(String file, String text) {
    def opener = OPENERS[format(file)]

    if (!opener) {
      throw new IllegalArgumentException("${file} is not a JSON, TOML or YAML file")
    }

    opener.call(text)
  }
}
