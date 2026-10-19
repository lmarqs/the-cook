package io.github.lmarqs.cookbook.agents

/**
 * Models the project settings and shared directory links Claude Code reads in a repository.
 *
 * ## Writes for
 *
 * The Claude Code settings schema in `.agents/adapters/claude/settings.json`, which `.claude/settings.json` links to:
 * top-level JSON keys, including hooks and `permissions` with `defaultMode` and `allow`, `ask` and `deny` lists of
 * `Bash(<prefix> *)` rules. Symbolic links from `.claude/skills`, `.claude/commands`, `.claude/hooks` and
 * `.claude/memory` to the shared directories complete the project setup. Claude Code reads `AGENTS.md` natively.
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

  /** `.claude/settings.json`, a link to the adapter's file, when settings or permissions are set. */
  @Links(path = '.claude')
  Map<String, String> adapterLinks() { settings ? ['settings.json': '../.agents/adapters/claude/settings.json'] : null }

  /** One relative link in `.claude/` per shared directory, such as `skills`, when the directories are connected. */
  @Links(path = '.claude')
  Map<String, String> links() { directoryLinks ?: null }

  private static String rule(Command command) { "Bash(${command.prefix.join(' ')} *)" }
}
