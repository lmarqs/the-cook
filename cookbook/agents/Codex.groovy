package io.github.lmarqs.cookbook.agents

/**
 * Models the project configuration, hooks and rules Codex reads in a repository.
 *
 * ## Writes for
 *
 * Top-level TOML keys and `sandbox_workspace_write.network_access` in `config.toml`, top-level JSON keys in
 * `hooks.json` and rule files under `rules/`, all in `.agents/adapters/codex/` and linked from `.codex/`. Codex reads
 * `AGENTS.md` and `.agents/skills/` natively, and hook settings reach the shared hook scripts by their paths, so no
 * other link is needed. Command rules use the
 * Codex `prefix_rule` syntax: `prefix_rule(pattern = ["git", ["status", "diff"]], decision = "allow")`, where a nested
 * list holds alternatives for one word, with `allow`, `prompt` or `forbidden` decisions. `.codex/` holds only links, so it can be deleted and cooked again.
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

  /** The key path permissions own in `config.toml`. */
  private static final List<String> NETWORK_ACCESS = ['sandbox_workspace_write', 'network_access']

  /** The runs that skip or approve these rules under Codex 0.160.1, which has no setting that refuses them. */
  private static final String UNREFUSED_RUNS = '# bypass: refuse reaches Claude only: --ignore-rules or an untrusted ' +
      'project skips these rules, --approve-for-me approves prompts'

  /** None: Codex reads no instruction file in place of `AGENTS.md`. */
  final List<String> ignoredFiles = []

  /**
   * Sets `sandbox_mode` to `workspace-write`, `approval_policy` to `on-request` and
   * `sandbox_workspace_write.network_access` to false, owning only that key of the `[sandbox_workspace_write]` table,
   * which Cook adds when absent. Replaces the command rules with one `prefix_rule` per
   * shared policy command entry, alternatives kept as nested lists: `allow` stays `allow`, `ask` becomes `prompt` and
   * `deny` becomes `forbidden`. A Claude-only entry becomes a comment naming it, since Codex cannot enforce it, and
   * tool entries and roles render nothing: tools are named per harness, and a Codex run cannot add a rules file of its
   * own. While the policy's `bypass` is `refuse`, the rules start with a comment naming the runs that step outside
   * them, since Codex has no setting that refuses those. These settings keys, and a whole `sandbox_workspace_write`,
   * replace values supplied by an earlier `config` call.
   */
  void permit(Policy policy) {
    config = config.findAll { key, value -> key != 'sandbox_workspace_write' } +
        [sandbox_mode: 'workspace-write', approval_policy: 'on-request', (NETWORK_ACCESS): false]
    commands = [allow: 'allow', ask: 'prompt', deny: 'forbidden'].collectMany { decision, codexDecision ->
      policy.commands[decision].collect { prefixRule(it, codexDecision) }
    }
    if (commands && policy.bypass == 'refuse') commands.add(0, UNREFUSED_RUNS)
  }

  /**
   * Adds top-level configuration keys; a supplied key replaces its earlier value whole, and a supplied
   * `sandbox_workspace_write` replaces the `network_access` that permissions set.
   */
  void config(Map more) {
    config = config.findAll { key, value -> !(key == NETWORK_ACCESS && 'sandbox_workspace_write' in more) } + more
  }

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
   * Adds the top-level keys of the TOML file at `path`, relative to this instance's directory, as `config` does. The
   * file is read as the recipes before this one in the invocation left it; a missing file is rejected.
   */
  void configFrom(String path) { config((Map) new TomlSlurper().parseText(input(path))) }

  /** Adds the top-level keys of the JSON file at `path` as `hooks` does, read as `configFrom` reads its file. */
  void hooksFrom(String path) { hooks((Map) new JsonSlurper().parseText(input(path))) }

  /** Adds the file at `path` as the rule named by its file name, as `rule` does, read as `configFrom` reads its file. */
  void ruleFrom(String path) { rule(path.tokenize('/').last(), input(path)) }

  /**
   * Checks that `skills` is at `.agents/skills`, where Codex discovers skills natively in its own directory; Codex
   * links no shared directory.
   */
  void readDirectories(Map<String, String> paths) {
    if (paths.skills) readsNatively(paths.skills, '.agents/skills')
  }

  /** `config.toml`, `hooks.json` and `rules`, each when written. */
  List<String> getLinkNames() { written() }

  /**
   * Each supplied top-level key of `.agents/adapters/codex/config.toml`, owned whole, when configuration or permissions
   * are set; permissions own `sandbox_workspace_write.network_access` alone, so the table's other keys stay the team's.
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

  /** One link in `.codex/` per adapter file or directory written: `config.toml`, `hooks.json` and `rules`. */
  @Links(path = '.codex', instead = 'the inputs of #codex-settings')
  Map<String, String> links() {
    written().collectEntries { [it, "../.agents/adapters/codex/${it}".toString()] } ?: null
  }

  private List<String> written() {
    ['config.toml': config, 'hooks.json': hooks, rules: rules || commands].findAll { name, declared -> declared }
    .keySet() as List
  }

  private String input(String path) {
    def text = current(path)
    if (text == null) throw new IllegalArgumentException("${path} must exist")
    text
  }

  private static void readsNatively(String path, String expected) {
    if (path != expected) throw new IllegalArgumentException("Codex reads ${expected} in its own directory, not ${path}")
  }

  private static String prefixRule(Command command, String decision) {
    if (command.claude) return "# Not enforced by Codex, Claude-only: ${command.claude}"
    "prefix_rule(pattern = ${pattern(command.prefix)}, decision = \"${decision}\")"
  }

  private static String pattern(List words) {
    '[' + words.collect { it instanceof List ? pattern(it) : "\"${it}\"" }.join(', ') + ']'
  }
}
