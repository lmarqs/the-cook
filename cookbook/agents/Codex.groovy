package io.github.lmarqs.cookbook.agents

/**
 * Models the project configuration, hooks and rules Codex reads in a repository.
 *
 * ## Writes for
 *
 * Top-level TOML keys in `config.toml`, top-level JSON keys in `hooks.json` and rule files under `rules/`, all in
 * `.agents/adapters/codex/` and linked from `.codex/`, and a `.codex/hooks` link to the shared hook scripts. Codex reads
 * `AGENTS.md` and `.agents/skills/` natively. Command rules use the
 * Codex `prefix_rule` syntax: `prefix_rule(pattern = ["git", "status"], decision = "allow")`, with `allow`, `prompt`
 * or `forbidden` decisions. `.codex/` holds only links, so it can be deleted and cooked again.
 *
 * ## Instances
 *
 * One instance covers the Codex setup in one directory. Use `at` for a separate setup in another directory;
 * Codex reads `AGENTS.md` and skills only from that instance's own directory.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.agents.Agents
 * import io.github.lmarqs.cookbook.agents.Codex
 *
 * @ingredient(Agents) def agents
 * @ingredient(Codex) def codex
 *
 * agents.permit([codex])
 * agents.connect([codex])
 * ```
 *
 * ## With other models
 *
 * `io.github.lmarqs.cookbook.agents.Agents` drives this harness through `permit` and `readDirectories`.
 * It supplies the shared policy and paths; Codex translates the policy into its native settings and rules,
 * and checks that the shared paths are the ones it reads natively. The same command prefixes do not mean
 * identical enforcement across harnesses; review the generated rules against Codex's permission model.
 */
@Ingredient
class Codex implements Harness {
  /** The harness name `codex`, used to select Codex. */
  final String name = 'codex'
  /** `.codex`, which holds only links Cook makes. */
  final String localDirectory = '.codex'
  private Map config = [:]
  private Map hooks = [:]
  private Map<String, String> rules = [:]
  private List<String> commands = []
  private String hooksPath

  /**
   * Sets `sandbox_mode` to `workspace-write`, `approval_policy` to `on-request` and
   * `sandbox_workspace_write.network_access` to false. Replaces the command rules with one `prefix_rule` per
   * shared policy command: `allow` stays `allow`, `ask` becomes `prompt` and `deny` becomes `forbidden`.
   * These settings keys replace values supplied by an earlier `config` call.
   */
  void permit(Policy policy) {
    config += [sandbox_mode: 'workspace-write', approval_policy: 'on-request',
               sandbox_workspace_write: [network_access: false]]
    commands = policy.allow.collect { prefixRule(it, 'allow') } +
      policy.ask.collect { prefixRule(it, 'prompt') } +
      policy.deny.collect { prefixRule(it, 'forbidden') }
  }

  /** Adds top-level configuration keys; a supplied key replaces its earlier value whole. */
  void config(Map more) { config += more }

  /** Adds top-level hooks keys; a supplied key replaces its earlier value whole. */
  void hooks(Map more) { hooks += more }

  /**
   * Adds a whole rule file with `body` under `.agents/adapters/codex/rules/`, replacing an earlier rule with the same name.
   * The name accepts letters, digits, underscores, dots and hyphens; empty names, `.` and `..` are rejected.
   */
  void rule(String name, String body) {
    if (!(name ==~ /[A-Za-z0-9_.-]+/) || name in ['.', '..']) throw new IllegalArgumentException("Rule file needs a simple name: ${name}")
    rules[name] = body
  }

  /**
   * Checks that `skills` is at `.agents/skills`, where Codex discovers skills natively in its own directory, and links
   * `.codex/hooks` to `hooks`; Codex has no project commands or memory, so it reads no other directory.
   */
  void readDirectories(Map<String, String> paths) {
    if (paths.skills) readsNatively(paths.skills, '.agents/skills')
    hooksPath = paths.hooks
  }

  /**
   * Each supplied top-level key of `.agents/adapters/codex/config.toml`, owned whole, when configuration or permissions
   * are set.
   */
  @Keys(path = '.agents/adapters/codex/config.toml')
  Map configKeys() { config ?: null }

  /** Each supplied top-level key of `.agents/adapters/codex/hooks.json`, owned whole, when hooks keys are added. */
  @Keys(path = '.agents/adapters/codex/hooks.json')
  Map hookKeys() { hooks ?: null }

  /** One whole file under `.agents/adapters/codex/rules/` per named rule, when rules are added. */
  @Files(path = '.agents/adapters/codex/rules')
  Map<String, String> ruleFiles() { rules ?: null }

  /**
   * The `commands` section of `.agents/adapters/codex/rules/cook.rules`, in `#` comment syntax, when the policy has
   * command rules.
   */
  @Section(path = '.agents/adapters/codex/rules/cook.rules', id = 'commands', comment = '#')
  List<String> commandRules() { commands ?: null }

  /**
   * One link in `.codex/` per adapter file or directory written, `config.toml`, `hooks.json` and `rules`, and `hooks`
   * to the shared hook scripts when connected.
   */
  @Links(path = '.codex')
  Map<String, String> links() {
    def written = [
      'config.toml': config,
      'hooks.json': hooks,
      rules: rules || commands,
    ].findAll { name, declared -> declared }
    Map<String, String> links = written.collectEntries { name, declared -> [name, "../.agents/adapters/codex/${name}"] }
    if (hooksPath) links.hooks = "../${hooksPath}".toString()
    links ?: null
  }

  private static void readsNatively(String path, String expected) {
    if (path != expected) throw new IllegalArgumentException("Codex reads ${expected} in its own directory, not ${path}")
  }

  private static String prefixRule(Command command, String decision) {
    def pattern = command.prefix.collect { "\"${it}\"" }.join(', ')
    "prefix_rule(pattern = [${pattern}], decision = \"${decision}\")"
  }
}
