package thecook.cookbook

import groovy.json.JsonOutput
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import thecook.model.Diagnostic

/**
 * `cook.lock`: for each cooked recipe, its short name, its source, the source's sha256, its arguments and the sections,
 * keys, whole files and links it owns. The kitchen keeps and commits it as the only record of which recipes were
 * cooked and what each owns. Cook reads it to withdraw what a recipe no longer declares, and to tell the first time
 * a recipe is cooked, which refuses an existing file its seed writes, from a later one.
 */
final class CookLock {
  static final String PATH = 'cook.lock'
  private static final String HEADER = '# Written by the-cook; do not edit by hand.'
  // In the order the lock lists them, each read from its `<kind>s` group.
  private static final Map<String, Closure<Target>> KINDS = [
    section: { Map item -> new SectionTarget((String) item.file, (String) item.id) },
    key: { Map item -> new KeyTarget((String) item.file, ((List) item.path)*.toString()) },
    file: { String file -> new FileTarget(file) },
    link: { String file -> new LinkTarget(file) },
  ]

  private final Map<String, Entry> entries = new TreeMap<>()

  /** The lock in `text`, or an empty lock when `text` is `null`. */
  static CookLock parse(String text) {
    def lock = new CookLock()

    if (text == null) {
      return lock
    }

    Map data

    try {
      data = (Map) new Yaml(new SafeConstructor(new LoaderOptions())).load(text)
    } catch (RuntimeException error) {
      throw new IllegalArgumentException("${PATH} is not valid YAML: ${error.message}", error)
    }

    if (data?.version != 1 || !(data.recipes instanceof List)) {
      throw new IllegalArgumentException("${PATH} must have version: 1 and a list of recipes")
    }

    data.recipes.each { Map recipe ->
      def owns = (Map) (recipe.owns ?: [:])
      def targets = KINDS.collectMany { kind, create -> (owns["${kind}s"] ?: []).collect(create) }
      lock.entries[recipe.name] = new Entry(recipe.name, recipe.source, recipe.sha256, recipe.arguments, targets)
    }
    lock
  }

  /** The sha256 the recipe `name` was last cooked with from `source`, or `null` when it was not cooked from it. */
  String sha256(String name, String source) {
    entries[name]?.source == source ? entries[name].sha256 : null
  }

  /** The source `name` was last cooked from, or `null` when it was not cooked. */
  String source(String name) {
    entries[name]?.source
  }

  /**
   * The arguments `name` was last cooked with, each param's first name with its values, or `null` when they were not
   * recorded. Fails when the lock records them in another form, which cooking `name` again or discarding it replaces.
   */
  Map<String, List<String>> arguments(String name) {
    def recorded = entries[name]?.arguments

    if (recorded == null) {
      return null
    }

    if (!readable(recorded)) {
      throw new IllegalArgumentException(Diagnostic.text(
      "${PATH} records the arguments of ${name} in a form Cook cannot read",
      'each argument is a name with a list of values, such as "mode": ["fast"]',
      "cook ${name} again with its arguments, or discard it"))
    }

    ((Map<String, List<String>>) recorded).collectEntries { option, values -> [(option): new ArrayList<>(values)] }
  }

  /** Whether the recipe `name` was cooked before. */
  boolean cooked(String name) {
    entries.containsKey(name)
  }

  /** What the recipe `name` owned when it was last cooked; empty when it never was. */
  List<Target> owned(String name) {
    entries[name]?.targets ?: []
  }

  /** The recipes the lock lists, by short name. */
  Set<String> getNames() {
    entries.keySet()
  }

  /** Drops the recipe `name` and returns what it owned. */
  List<Target> forget(String name) {
    entries.remove(name)?.targets ?: []
  }

  /** The recipe that owns `target`, other than `except`, or `null`. */
  String ownerOf(Target target, String except) {
    entries.find { name, entry ->
      name != except && entry.targets.any {
        it.overlaps(target)
      }
    }?.key
  }

  /** Records `name` as cooked from `source` with `arguments`, owning `targets`, taking them from prior owners. */
  void record(String name, String source, String sha256, Map<String, List<String>> arguments, List<Target> targets) {
    entries.each { other, entry ->
      if (other != name) {
        entry.targets.removeAll { owned ->
          targets.any {
            it.overlaps(owned)
          }
        }
      }
    }
    entries[name] = new Entry(name, source, sha256, arguments, targets)
  }

  /** The lock's YAML, with every string quoted so no value is read as another type. */
  String render() {
    def lines = [HEADER, 'version: 1', entries ? 'recipes:' : 'recipes: []']
    entries.values().each { entry ->
      lines << "  - name: ${quote(entry.name)}"
      lines << "    source: ${quote(entry.source)}"
      lines << "    sha256: ${quote(entry.sha256)}"

      if (entry.arguments != null && !readable(entry.arguments)) {
        lines << "    arguments: ${JsonOutput.toJson(entry.arguments)}"
      } else if (entry.arguments != null) {
        lines << (entry.arguments ? '    arguments:' : '    arguments: {}')
        ((Map) entry.arguments).each { option, values ->
          lines << "      ${quote((String) option)}: ${JsonOutput.toJson(values)}"
        }
      }

      def groups = KINDS.keySet()
          .collectEntries { kind -> [(kind): entry.targets.findAll { it.kind == kind }] }
          .findAll { kind, targets -> targets }
      lines << (groups ? '    owns:' : '    owns: {}')
      groups.each { kind, List<Target> targets ->
        lines << "      ${kind}s:"
        targets.sort { it.toString() }.each { lines.addAll(item(it.entry)) }
      }
    }
    lines.join('\n') + '\n'
  }

  /** Whether `recorded` maps each param's name to a list of its values, the form Cook writes. */
  private static boolean readable(Object recorded) {
    recorded instanceof Map && ((Map) recorded).every { option, values ->
      option instanceof String && values instanceof List && ((List) values).every { it instanceof String }
    }
  }

  private static String quote(String value) {
    JsonOutput.toJson(value)
  }

  /** A target's lines in its group: its fields, or its path for a whole file or link. */
  private static List<String> item(Object entry) {
    if (!(entry instanceof Map)) {
      return ["        - ${JsonOutput.toJson(entry)}".toString()]
    }

    ((Map) entry).collect { field, value -> "${field}: ${JsonOutput.toJson(value)}".toString() }
    .withIndex()
    .collect { line, index -> (index ? '          ' : '        - ') + line }
  }

  private static final class Entry {
    final String name
    final String source
    final String sha256
    // As the lock records them: the form Cook writes, or any other value, which only replicate refuses.
    final Object arguments
    final List<Target> targets

    Entry(String name, String source, String sha256, Object arguments, List<Target> targets) {
      this.name = name
      this.source = source
      this.sha256 = sha256
      this.arguments = arguments
      this.targets = new ArrayList<>(targets)
    }
  }
}
