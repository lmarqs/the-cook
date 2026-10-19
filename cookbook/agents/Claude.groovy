package io.github.lmarqs.cookbook.agents

/**
 * Models the project settings and shared directory links Claude Code reads in a repository.
 *
 * ## Writes for
 *
 * The Claude Code settings schema in `.agents/adapters/claude/settings.json`, which `.claude/settings.json` links to:
 * top-level JSON keys, including hooks and `permissions` with `allow`, `ask` and `deny` lists of `Bash(<prefix> *)`
 * rules and tool patterns. One file per policy role under `.agents/adapters/claude/roles/`, holding only that role's
 * `permissions.deny`, which a run adds with `claude --settings <file>`. Symbolic links from `.claude/skills` and
 * `.claude/commands` to the shared directories complete the project setup: Claude Code finds skills and commands only
 * there. Hooks and memory are reached by their paths from the repository root, so they need no link, and Claude Code
 * reads `AGENTS.md` natively.
 * `.claude/` holds only links and Claude's local files, so it can be deleted and cooked again.
 *
 * ## Instances
 *
 * One instance covers the Claude Code setup in one directory. Use `at` for a separate setup in another
 * directory; shared directory paths are relative to that instance's directory.
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
 * agents.permit([claude])
 * agents.connect([claude])
 * ```
 *
 * ## With other models
 *
 * `io.github.lmarqs.cookbook.agents.Agents` drives this harness through `permit` and `readDirectories`.
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
  private Map<String, List<String>> roleDenies = [:]
  private Map<String, String> directoryLinks = [:]

  /** Destination-root ignore patterns for the local settings file Claude Code writes. */
  final List<String> localFiles = ['/.claude/settings.local.json']

  /**
   * The instruction files that make Claude Code skip `AGENTS.md` while any of them exists in the directory or above,
   * after a comment saying so.
   */
  final List<String> ignoredFiles = [
    '# Claude Code skips AGENTS.md while any of these exists, so they stay out of commits.',
    'CLAUDE.md',
    'CLAUDE.local.md',
    '**/.claude/CLAUDE.md',
  ]

  /**
   * Sets `permissions` from the shared policy: under `allow`, `ask` and `deny`, one `Bash(<words> *)` rule per
   * combination of a command entry's alternatives, `Bash(<pattern>)` for a Claude-only command entry, and each tool
   * pattern as it is. Writes one deny file per role. Sets no `defaultMode`: how a session asks is each person's choice.
   * Replaces any permissions supplied by an earlier `settings` call.
   */
  void permit(Policy policy) {
    settings.permissions = ['allow', 'ask', 'deny'].collectEntries { decision ->
      [decision, policy.commands[decision].collectMany { rules(it) } + policy.tools[decision]*.claude]
    }
    roleDenies = policy.roles.collectEntries { name, denies -> [name, denies.collectMany { rules(it) }] }
  }

  /** Adds top-level settings keys, hooks included; a supplied key replaces its earlier value whole. */
  void settings(Map more) { settings += more }

  /**
   * Links `.claude/skills` and `.claude/commands` to the shared directories of those names, the only ones Claude Code
   * finds by a fixed path; each path is relative to this instance's directory.
   */
  void readDirectories(Map<String, String> paths) {
    paths.findAll { name, path -> name in ['skills', 'commands'] }
    .each { name, path -> directoryLinks[name] = "../${path}".toString() }
  }

  /** `settings.json` when settings are set, then each shared directory linked. */
  List<String> getLinkNames() { (settings ? ['settings.json'] : []) + directoryLinks.keySet() }

  /**
   * Each supplied top-level key of `.agents/adapters/claude/settings.json`, owned whole, when settings or permissions
   * are set.
   */
  @Keys(path = '.agents/adapters/claude/settings.json')
  Map settingsKeys() { settings ?: null }

  /** `.claude/settings.json`, a link to the adapter's file, when settings or permissions are set. */
  @Links(path = '.claude')
  Map<String, String> adapterLinks() { settings ? ['settings.json': '../.agents/adapters/claude/settings.json'] : null }

  /**
   * `.agents/adapters/claude/roles/<role>.json` per policy role, holding the role's `permissions.deny`; Claude merges it
   * over the project settings when a run passes it with `--settings`.
   */
  @Files(path = '.agents/adapters/claude/roles')
  Map<String, String> roleFiles() {
    roleDenies.collectEntries { name, denies -> ["${name}.json".toString(), denyFile(denies)] } ?: null
  }

  /** One relative link in `.claude/` per shared directory, such as `skills`, when the directories are connected. */
  @Links(path = '.claude')
  Map<String, String> links() { directoryLinks ?: null }

  private static List<String> rules(Command command) {
    if (command.claude) return ["Bash(${command.claude})".toString()]
    combinations(command.prefix).collect { "Bash(${it.join(' ')} *)".toString() }
  }

  private static List<List<String>> combinations(List prefix) {
    prefix.inject([[]]) { List<List<String>> heads, item ->
      heads.collectMany { head -> (item instanceof List ? item : [item]).collect { head + it } }
    }
  }

  private static String denyFile(List<String> denies) {
    def list = denies ? '[\n' + denies.collect { "      ${groovy.json.JsonOutput.toJson(it)}" }.join(',\n') + '\n    ]' : '[]'
    "{\n  \"permissions\": {\n    \"deny\": ${list}\n  }\n}\n"
  }
}
