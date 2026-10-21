package io.github.lmarqs.cookbook.agents

/**
 * Models the project configuration, hooks and rules Codex reads in a repository.
 *
 * ## Writes for
 *
 * Top-level TOML keys and `sandbox_workspace_write.network_access` in `config.toml`, top-level JSON keys and
 * `hooks.SessionStart` alone in `hooks.json`, whose command hooks' printed `hookSpecificOutput.additionalContext` Codex
 * adds to the session, and rule files under `rules/`, all in `.agents/adapters/codex/` and linked from `.codex/`.
 * Codex reads `AGENTS.md` and `.agents/skills/` natively, and hook settings reach the shared hook scripts by their
 * paths, so no other link is needed. Command rules use the
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
  private boolean hooksWritten

  /** The hooks file a team may write by hand, which Codex reads through `.codex/hooks.json`. */
  private static final String HOOKS_FILE = '.agents/adapters/codex/hooks.json'

  /** The key path the session-start entry owns in `hooks.json`. */
  private static final List<String> SESSION_START = ['hooks', 'SessionStart']

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
   * replace values supplied by an earlier `config` call. `cookedWith`, when given, opens the rules as a comment naming
   * the recipe and the arguments that cook them again.
   */
  void permit(Policy policy, String cookedWith = null) {
    config = config.findAll { key, value -> key != 'sandbox_workspace_write' } +
        [sandbox_mode: 'workspace-write', approval_policy: 'on-request', (NETWORK_ACCESS): false]
    commands = [allow: 'allow', ask: 'prompt', deny: 'forbidden'].collectMany { decision, codexDecision ->
      policy.commands[decision].collect { prefixRule(it, codexDecision) }
    }
    if (commands && policy.bypass == 'refuse') commands.add(0, UNREFUSED_RUNS)
    if (commands && cookedWith) commands.add(0, "# ${cookedWith}".toString())
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
   * Sets `hooks.SessionStart` in `hooks.json` to one entry for `startup|resume|clear` that runs `dispatcher`, a path
   * relative to this instance's directory, with the argument `codex`, and takes the `additionalContext` it prints.
   * The command finds the path from the repository root through `git rev-parse --show-toplevel`, because Codex runs a
   * relative hook path from the session's directory. The rest of `hooks` stays the team's.
   */
  void startSession(String dispatcher) {
    def path = Path.of(directory, dispatcher).normalize()
    hooks[SESSION_START] = [[matcher: 'startup|resume|clear', hooks: [[type: 'command',
            command: "sh -c 'exec \"\$(git rev-parse --show-toplevel)/${path}\" codex'".toString(),
            statusMessage: 'Loading session context']]]]
  }

  /**
   * Adds a whole rule file with `body` under `.agents/adapters/codex/rules/`, replacing an earlier rule with the same name.
   * The name accepts letters, digits, underscores, dots and hyphens; empty names, `.` and `..` are rejected.
   */
  void rule(String name, String body) {
    if (!(name ==~ /[A-Za-z0-9_.-]+/) || name in ['.', '..']) throw new IllegalArgumentException("Rule file needs a simple name: ${name}")
    rules[name] = body
  }

  /**
   * Checks that `skills` is at `.agents/skills`, where Codex discovers skills natively in its own directory; Codex
   * links no shared directory. Links `.codex/hooks.json` when the team wrote `.agents/adapters/codex/hooks.json`.
   */
  void readDirectories(Map<String, String> paths) {
    if (paths.skills) readsNatively(paths.skills, '.agents/skills')
    hooksWritten = current(HOOKS_FILE) != null
  }

  /** `config.toml`, `hooks.json` and `rules`, the links this instance may make. */
  final List<String> linkNames = ['config.toml', 'hooks.json', 'rules']

  /**
   * Each supplied top-level key of `.agents/adapters/codex/config.toml`, owned whole, when configuration or permissions
   * are set; permissions own `sandbox_workspace_write.network_access` alone, so the table's other keys stay the team's.
   */
  @Keys(path = '.agents/adapters/codex/config.toml')
  Map configKeys() { config ?: null }

  /**
   * Each supplied top-level key of `.agents/adapters/codex/hooks.json`, owned whole, when hooks keys are added, and
   * `hooks.SessionStart` alone once a session-start dispatcher is set.
   */
  @Keys(path = '.agents/adapters/codex/hooks.json')
  Map hookKeys() { hooks ?: null }

  /** One whole file under `.agents/adapters/codex/rules/` per named rule, when rules are added. */
  @Files(path = '.agents/adapters/codex/rules')
  Map<String, String> ruleFiles() { rules ?: null }

  /**
   * The `commands` section of `.agents/adapters/codex/rules/cook.rules`, in `#` comment syntax, when the policy has
   * command rules: the comment naming the recipe and arguments that cook it, when given, then the rules.
   */
  @Section(path = '.agents/adapters/codex/rules/cook.rules', id = 'commands', comment = '#')
  List<String> commandRules() { commands ?: null }

  /** One link in `.codex/` per adapter file or directory written: `config.toml`, `hooks.json` and `rules`. */
  @Links(path = '.codex')
  Map<String, String> links() {
    written().collectEntries { [it, "../.agents/adapters/codex/${it}".toString()] } ?: null
  }

  private List<String> written() {
    ['config.toml': config, 'hooks.json': hooks || hooksWritten, rules: rules || commands]
    .findAll { name, declared -> declared }.keySet() as List
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
