package io.github.lmarqs.cookbook.agents

/**
 * Models the project settings, instructions and skill links Claude Code reads in a repository.
 *
 * ## Writes for
 *
 * The Claude Code settings schema in `.agents/adapters/claude/settings.json`, which `.claude/settings.json` links to:
 * top-level JSON keys, including hooks and `permissions` with `defaultMode` and `allow`, `ask` and `deny` lists of
 * `Bash(<prefix> *)` rules. Markdown instructions in `.agents/adapters/claude/CLAUDE.md`, which `.claude/CLAUDE.md`
 * links to, and symbolic links from `.claude/skills`, `.claude/commands`, `.claude/hooks` and `.claude/memory` to the
 * shared directories complete the project setup. `.claude/` holds only links and Claude's local files, so it can be deleted and cooked again.
 *
 * ## Instances
 *
 * One instance covers the Claude Code setup in one directory. Use `at` for a separate setup in another
 * directory; shared instruction and skill paths are relative to that instance's directory.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.agents.Agents
 * import io.github.lmarqs.cookbook.agents.Claude
 *
 * @ingredient(Agents) def agents
 * @ingredient(Claude) def claude
 *
 * agents.instruct('Run tests before committing.')
 * agents.permit([claude])
 * agents.connect([claude])
 * ```
 *
 * ## With other models
 *
 * `io.github.lmarqs.cookbook.agents.Agents` drives this harness through `permit`, `readInstructions` and `readDirectories`.
 * It supplies the shared policy and paths; Claude translates them into its native files. The same command
 * prefixes do not mean identical enforcement across harnesses; review the generated permissions against
 * Claude's permission model. `localFiles` supplies ignore patterns for `io.github.lmarqs.cookbook.git.Git`.
 */
@Ingredient
class Claude implements Harness {
  /** The harness name `claude`, used to select Claude Code. */
  final String name = 'claude'
  /** `.claude`, which holds only links Cook makes and files Claude Code writes locally. */
  final String localDirectory = '.claude'
  private Map settings = [:]
  private List<String> instructions = []
  private List<String> sharedInstructions = []
  private Map<String, String> directoryLinks = [:]

  /** Destination-root ignore patterns for the local settings file Claude Code writes. */
  final List<String> localFiles = ['/.claude/settings.local.json']

  /**
   * Sets `permissions` from the shared policy: `acceptEdits` and one `Bash(<prefix> *)` entry per command
   * in its `allow`, `ask` or `deny` list. Replaces any permissions supplied by an earlier `settings` call.
   */
  void permit(Policy policy) {
    settings.permissions = [
      defaultMode: 'acceptEdits',
      allow: policy.allow.collect { rule(it) },
      ask: policy.ask.collect { rule(it) },
      deny: policy.deny.collect { rule(it) },
    ]
  }

  /** Adds top-level settings keys, hooks included; a supplied key replaces its earlier value whole. */
  void settings(Map more) { settings += more }

  /** Appends Markdown instruction lines to the managed section in `.agents/adapters/claude/CLAUDE.md`. */
  void instruct(String... lines) { instructions += lines.toList() }

  /**
   * Imports the shared instructions at `path`, relative to this instance's directory, into the adapter's `CLAUDE.md`.
   * Claude resolves an import from the file a link points to, so the import climbs out of `.agents/adapters/claude/`.
   */
  void readInstructions(String path) {
    sharedInstructions << path
    instruct("@../../../${path}")
  }

  /** Links `.claude/<name>` to each shared directory; each path is relative to this instance's directory. */
  void readDirectories(Map<String, String> paths) {
    paths.each { name, path -> directoryLinks[name] = "../${path}".toString() }
  }

  /**
   * Each supplied top-level key of `.agents/adapters/claude/settings.json`, owned whole, when settings or permissions
   * are set.
   */
  @Keys(path = '.agents/adapters/claude/settings.json')
  Map settingsKeys() { settings ?: null }

  /**
   * `.claude/settings.json` and `.claude/CLAUDE.md`, links to the adapter's files, when settings or instructions are
   * set.
   */
  @Links(path = '.claude')
  Map<String, String> adapterLinks() {
    Map<String, String> links = [:]
    if (settings) links['settings.json'] = '../.agents/adapters/claude/settings.json'
    if (instructions) links['CLAUDE.md'] = '../.agents/adapters/claude/CLAUDE.md'
    links ?: null
  }

  /**
   * The `instructions` section of `.agents/adapters/claude/CLAUDE.md`, when instruction lines or a shared instruction
   * path are added. Fails when the root `CLAUDE.md` imports the shared instructions outside an `instructions` section
   * of this model, since Claude would then read them twice.
   */
  @Section(path = '.agents/adapters/claude/CLAUDE.md', id = 'instructions')
  List<String> instructionsSection() {
    if (!instructions) {
      return null
    }
    refuseImportsElsewhere()
    instructions
  }

  /** One relative link in `.claude/` per shared directory, such as `skills`, when the directories are connected. */
  @Links(path = '.claude')
  Map<String, String> links() { directoryLinks ?: null }

  private void refuseImportsElsewhere() {
    def imports = sharedInstructions.collect { "@${it}".toString() } as Set
    boolean own = false
    current('CLAUDE.md')?.readLines()?.eachWithIndex { String line, int index ->
      if (line ==~ /.*cook:begin \S+#instructions(?:\s.*)?/) {
        own = true
      } else if (line ==~ /.*cook:end \S+#instructions(?:\s.*)?/) {
        own = false
      } else if (!own && line.trim() in imports) {
        throw new IllegalArgumentException(
          "CLAUDE.md already imports ${line.trim()} on line ${index + 1}; remove that line, since " +
          '.claude/CLAUDE.md imports it')
      }
    }
  }

  private static String rule(Command command) { "Bash(${command.prefix.join(' ')} *)" }
}
