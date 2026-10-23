package io.github.lmarqs.cookbook.ralph

/**
 * Models ralph-tui's project files: its configuration, the prompts it renders for each task, and the launcher that runs
 * it in a worktree of its own.
 *
 * ## Writes for
 *
 * ralph-tui 0.12.0, which reads `.ralph-tui/config.toml` (config version 2.1) and `.ralph-tui/templates/<tracker>.hbs`
 * from the directory it runs in, with Claude Code 2.1.306 and codex-cli 0.160.1 as its workers. The configuration has
 * one `[[agents]]` entry per harness: `defaultFlags` sits at the top of the entry and the plugin's settings under
 * `[agents.options]`, because ralph-tui 0.12 replaces a `defaultFlags` it finds under the options and silently drops
 * keys it does not know at the top of an entry. The launcher scripts are POSIX `sh`.
 *
 * Model, effort and further entries are the team's, in the seeded configuration. A model goes per run
 * (`ralph-tui run --model`) or per entry (`model` under `[agents.options]`); ralph-tui 0.12's Claude plugin accepts
 * only `sonnet`, `opus` and `haiku`. Claude's effort goes per run through `CLAUDE_CODE_EFFORT_LEVEL`, which the
 * launcher and ralph-tui pass to every worker; Codex's goes per entry, as `"-c", "model_reasoning_effort=high"` in
 * `defaultFlags`. Entries a team may add, launched with `.ralph-tui/bin/run <run> --agent <name>`:
 *
 * ```toml
 * # A Claude worker bound to a role's denies, beside the seeded entries.
 * [[agents]]
 * name = "claude-worker"
 * plugin = "claude"
 * defaultFlags = ["--permission-prompts", "none", "--permission-mode", "acceptEdits",
 *                 "--settings", ".agents/adapters/claude/roles/worker.json"]
 *
 * [agents.options]
 * skipPermissions = false
 *
 * # A Claude worker that bypasses every permission check, for a sandboxed machine.
 * [[agents]]
 * name = "claude-bypass"
 * plugin = "claude"
 *
 * [agents.options]
 * skipPermissions = true
 * ```
 *
 * A Markdown section in each instruction file the recipe names, such as `AGENTS.md`, gives the launcher's commands.
 *
 * ## Instances
 *
 * One instance covers one repository's ralph-tui setup, at its root: the launcher puts run worktrees under the
 * repository's `<worktree root>/ralph/`, `.worktrees/ralph/` by default.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.ralph.RalphTui
 *
 * @ingredient(RalphTui) def ralph
 *
 * ralph.harnesses(['claude', 'codex'])
 * ralph.workerRoles(['claude:worker'])
 * ralph.workerSettings(['codex:ralph-codex.toml'])
 * ralph.worktreeRoot('.worktrees')
 * ralph.worktreeSetup('.worktrees/setup')
 * ralph.install('io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex ' +
 *     '--worker-role=claude:worker --worker-settings=codex:ralph-codex.toml --worktree-root=.worktrees ' +
 *     '--worktree-setup=.worktrees/setup')
 * ```
 *
 * ## With other models
 *
 * None: ralph-tui names the other tools only by path. Run worktrees follow `io.github.lmarqs.cookbook:git#worktrees`'s
 * rule `.worktrees/<branch>` by default, and `worktreeRoot` moves them as `io.github.lmarqs.cookbook:agents#connect
 * --worktree-root` moves Claude Code's; the launcher runs a setup file only when `setup` names one, such as that
 * recipe's `.worktrees/setup`; `--worker-role` reads the Claude role files `io.github.lmarqs.cookbook:agents#connect`
 * renders.
 */
@Ingredient
class RalphTui {
  /** The harnesses ralph-tui drives, by their ralph-tui agent plugin names. */
  static final List<String> HARNESSES = ['claude', 'codex']

  /**
   * The trackers a run takes tasks from: `beads` or `beads-bv`, which has `bv` pick the next bead, with `--epic`, and
   * `json` with `--prd`.
   */
  static final List<String> TRACKERS = ['beads', 'beads-bv', 'json']

  /** The trackers when `trackers` is not called. */
  static final List<String> DEFAULT_TRACKERS = ['json']

  /** The trackers a run with `--epic` takes its beads from; one of them at most. */
  static final List<String> EPIC_TRACKERS = ['beads', 'beads-bv']

  /** Where run worktrees go when `worktreeRoot` is not called, following `.worktrees/<branch>`. */
  static final String WORKTREE_ROOT = '.worktrees'

  private static final String CONFIG_PATH = '.ralph-tui/config.toml'

  /** The worker wrapper `workerCheck` writes, which each seeded entry then names as its `command`. */
  static final String WORKER = '.ralph-tui/bin/worker'

  private static final Map<String, Map<String, List<String>>> AGENTS = [
    claude: [
      flags: ['--permission-prompts', 'none', '--permission-mode', 'acceptEdits'],
      options: ['skipPermissions = false'],
    ],
    codex: [
      flags: ['-c', 'approval_policy=never', '-c', 'sandbox_workspace_write.network_access=false'],
      options: ['fullAuto = false', 'sandbox = "workspace-write"'],
    ],
  ]

  private List<String> harnesses = []
  private List<String> trackers = DEFAULT_TRACKERS
  private List<String> workerRoles = []
  private List<String> workerSettings = []
  private Map<String, WorkerBinding> workerBindings = [:]
  private String worktreeRoot = WORKTREE_ROOT
  private String worktreeSetup
  private String worktreeTeardown
  private String workerCheck
  private String cookedWith
  private List<String> instructions = []

  /**
   * Sets the `[[agents]]` entries the seeded configuration gets, one per harness in the order given; a repeated
   * harness counts once. `null` means none.
   */
  void harnesses(List<String> names) {
    harnesses = names ?: []
  }

  /**
   * Sets what is seeded and offered to the trackers in `names`, each `beads`, `beads-bv` or `json`, with at most one of
   * `beads` and `beads-bv`: their prompts, the configuration's `tracker` line, and the run flags `bin/run`'s usage line
   * and the instructions section name. `json` without it, and for `null`. A run still picks its tracker with `--epic`
   * or `--prd`.
   */
  void trackers(List<String> names) {
    trackers = names ?: DEFAULT_TRACKERS
  }

  /**
   * Binds roles to harnesses, each value `<harness>:<role>`, where the harness is `claude`: the Claude entry passes
   * the settings `io.github.lmarqs.cookbook:agents#connect` renders for the role,
   * `.agents/adapters/claude/roles/<role>.json`, as `"--settings", "<that path>"`. Codex has no roles, since it loads
   * no rules for one run. `null` binds none. Stored as given; `install` resolves them.
   */
  void workerRoles(List<String> roles) {
    workerRoles = roles ?: []
  }

  /**
   * Binds settings files to harnesses, each value `<harness>:<path>`, the path from the repository root: the `claude`
   * entry passes `"--settings", "<path>"`, and nothing reads or writes the file at cook time; the `codex` entry passes
   * one `"-c", "<key>=<value>"` per key of the TOML file, which is read from the draft when cooking. `null` binds none.
   * Stored as given; `install` resolves them.
   */
  void workerSettings(List<String> files) {
    workerSettings = files ?: []
  }

  /**
   * Has `bin/run` put each run's worktree at `<path>/ralph/<name>`, `path` being a directory by its path from the
   * repository root, and `bin/clean` remove it from there; ralph-tui puts the run's parallel workers beside it, in
   * `.ralph-worktrees/<last part of the name>/`. `.worktrees` without it, and for `null`.
   */
  void worktreeRoot(String path) {
    worktreeRoot = path ?: WORKTREE_ROOT
  }

  /**
   * Has `bin/run` run the file at `path`, by its path from the repository root, from the root of each worktree it
   * creates, and fail the run when that file is missing, not executable or fails. Without it, and for `null`,
   * `bin/run` runs no setup.
   */
  void worktreeSetup(String path) {
    worktreeSetup = path
  }

  /**
   * Has `bin/clean` run the file at `path`, by its path from the repository root, from the root of the run worktree
   * before removing anything, and stop, removing nothing, when that file is missing, not executable or fails. Without
   * it, and for `null`, `bin/clean` runs no teardown.
   */
  void worktreeTeardown(String path) {
    worktreeTeardown = path
  }

  /**
   * Has `.ralph-tui/bin/worker` hand the agent's output to ralph-tui only once the file at `path`, by its path from the
   * repository root, passes; and seeds each `[[agents]]` entry with the worker as its `command`, its harness as the
   * worker's first argument, and retries for a failed iteration. Without it, and for `null`, the worker refuses every
   * task.
   */
  void workerCheck(String path) {
    workerCheck = path
  }

  /**
   * Has the launcher's commands stated in each Markdown file in `paths`, by its path from the repository root, in a
   * `ralph-tui` section. A file the draft lacks is refused when the sections are written. `null` names none.
   */
  void instructions(List<String> paths) {
    instructions = paths ?: []
  }

  /**
   * Requests the owned launcher, `bin/run` and `bin/clean`, the worker `bin/worker`, and the `ralph-tui` section of
   * `.gitignore`, and makes the settings above the state the outputs render from. `cookedWith` names the recipe and its
   * arguments, which the section's first line records. Refused, in this order, when:
   *
   * 1. `cookedWith` is blank;
   * 2. a harness is not `claude` or `codex`, or none is given;
   * 3. a tracker is not `beads`, `beads-bv` or `json`, or none is given, or both `beads` and `beads-bv` are;
   * 4. a worker role or settings value is not `<harness>:<x>`, or names a harness that is not `claude` or `codex` or
   *    that no `--harness` gives, or a role binds `codex`, or one param names a harness twice;
   * 5. a role and a settings file both bind one harness;
   * 6. a role name is not a role name, or its `.agents/adapters/claude/roles/<name>.json` is not in the draft;
   * 7. a settings path is absolute or has a `..` segment, or a `codex` settings file is not in the draft, is not TOML,
   *    or holds a key or value `-c <key>=<value>` cannot carry;
   * 8. the worktree setup, worktree teardown or worker check is under `.ralph-tui/`, which in a run worktree holds only
   *    what this recipe writes;
   * 9. `.ralph-tui/config.toml` exists and is not TOML, or lacks what an argument needs, checked in this order: an
   *    `[[agents]]` entry with each harness's plugin; one with each worker role or settings file's flags as a run in
   *    its `defaultFlags`; one with the worker as its `command` and the harness first in its `defaultFlags`, for
   *    each harness, with a worker check; and a `tracker` or `defaultTracker` starting with `beads` with the `beads`
   *    tracker, or equal to `beads-bv` with the `beads-bv` tracker. The configuration is seeded once, so a refused cook
   *    writes nothing.
   */
  void install(String cookedWith) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('install needs the recipe it is cooked with')
    }

    checkHarnesses()
    checkTrackers()
    workerBindings = resolveWorkers()
    outsideRalphTui('worktree-setup', worktreeSetup, '.worktrees/setup')
    outsideRalphTui('worktree-teardown', worktreeTeardown, '.worktrees/teardown')
    outsideRalphTui('worker-check', workerCheck, 'scripts/check')

    harnesses = harnesses.unique(false)
    trackers = TRACKERS.findAll { it in trackers }
    checkExistingConfiguration()
    this.cookedWith = cookedWith
  }

  /**
   * `.ralph-tui/config.toml`, seeded when absent and never owned: config version 2.1, the epic tracker, if any, ten
   * iterations, `autoCommit`, skipping a failed task, or with `workerCheck` retrying it twice first, direct merges in
   * parallel mode, and one `[[agents]]` entry per harness, when installed.
   */
  @Seed(path = CONFIG_PATH)
  String config() {
    if (!cookedWith) {
      return null
    }

    def lines = ['configVersion = "2.1"',] + (epicTracker() ? ["tracker = \"${epicTracker()}\"".toString()] : []) + [
      '# ralph-tui stops a run after this many iterations, each one agent run on one task; 0 means no limit.',
      'maxIterations = 10',
      'autoCommit = true',
      '',
      '[errorHandling]',
    ] + errorHandling() + ['', '[parallel]', 'directMerge = true',]
    harnesses.each { harness -> lines += [''] + entry(harness) }
    lines.join('\n') + '\n'
  }

  /**
   * `.ralph-tui/templates/beads.hbs`, the prompt for a run with `--epic`, seeded when absent and never owned, when
   * installed.
   */
  @Seed(path = '.ralph-tui/templates/beads.hbs')
  String beadsPrompt() {
    cookedWith && 'beads' in trackers ? resource('templates/beads.hbs') : null
  }

  /**
   * `.ralph-tui/templates/beads-bv.hbs`, the prompt for a run with `--epic` under the `beads-bv` tracker, seeded when
   * absent and never owned, when installed.
   */
  @Seed(path = '.ralph-tui/templates/beads-bv.hbs')
  String beadsBvPrompt() {
    cookedWith && 'beads-bv' in trackers ? resource('templates/beads-bv.hbs') : null
  }

  /**
   * `.ralph-tui/templates/json.hbs`, the prompt for a run with `--prd`, seeded when absent and never owned, when
   * installed.
   */
  @Seed(path = '.ralph-tui/templates/json.hbs')
  String jsonPrompt() {
    cookedWith && 'json' in trackers ? resource('templates/json.hbs') : null
  }

  /**
   * The executable `.ralph-tui/bin/run`, which runs ralph-tui in `<worktreeRoot>/ralph/<name>`, with the `worktreeRoot`
   * and `worktreeSetup` paths, the tracker arguments and the epic tracker shell-quoted into its `worktrees`, `setup`,
   * `flags` and `epic_tracker` variables, when installed.
   */
  @File(path = '.ralph-tui/bin/run', mode = '0755')
  String run() {
    if (!cookedWith) {
      return null
    }

    def script = assign(resource('bin/run.sh'), 'worktrees', worktreeRoot)
    script = assign(script, 'setup', worktreeSetup ?: '')
    script = assign(script, 'flags', runFlags())
    assign(script, 'epic_tracker', epicTracker() ?: 'beads')
  }

  /**
   * The executable `.ralph-tui/bin/clean`, which removes a run's worktrees and branches, with the `worktreeRoot` and
   * `worktreeTeardown` paths shell-quoted into its `worktrees` and `teardown` variables, when installed.
   */
  @File(path = '.ralph-tui/bin/clean', mode = '0755')
  String clean() {
    if (!cookedWith) {
      return null
    }

    def script = assign(resource('bin/clean.sh'), 'worktrees', worktreeRoot)
    assign(script, 'teardown', worktreeTeardown ?: '')
  }

  /**
   * The executable `.ralph-tui/bin/worker`, which gates each agent run on the `workerCheck` file, with that path and
   * the harnesses shell-quoted into its `check` and `agents` variables, when installed; without `workerCheck`, its
   * `check` is empty and it refuses every task.
   */
  @File(path = '.ralph-tui/bin/worker', mode = '0755')
  String worker() {
    if (!cookedWith) {
      return null
    }

    def script = assign(resource('bin/worker.sh'), 'check', workerCheck ?: '')
    assign(script, 'agents', harnesses.join(' '))
  }

  /**
   * The `ralph-tui` section of `.gitignore`: a comment naming the recipe and its arguments, then everything under
   * `.ralph-tui/` but the configuration, prompts and launcher, and the run worktrees, when installed.
   */
  @Section(path = '.gitignore', id = 'ralph-tui')
  List<String> ignoreSection() {
    if (!cookedWith) {
      return null
    }

    [
      "# ${cookedWith}".toString(),
      '/.ralph-tui/*',
      '!/.ralph-tui/config.toml',
      '!/.ralph-tui/templates/',
      '!/.ralph-tui/bin/',
      "/${worktreeRoot}/ralph/".toString(),
    ]
  }

  /**
   * The `ralph-tui` section of each file `instructions` named, when installed: the recipe and its arguments as an HTML
   * comment, then a list item giving `bin/run` and `bin/clean` and where a run's worktree goes.
   */
  @Sections(path = '.', id = 'ralph-tui', comment = '<!--', missing = 'fail')
  Map<String, List<String>> instructionsSections() {
    if (!cookedWith || !instructions) {
      return null
    }

    def run = epicTracker() && 'json' in trackers ?
        '`.ralph-tui/bin/run <name> --agent <entry> --epic <id>`, or `--prd <file>`,' :
        "`.ralph-tui/bin/run <name> --agent <entry> ${runFlags()}`"
    def lines = [
      "<!-- ${cookedWith} -->",
      "- Loop runs: ${run} runs ralph-tui in",
      "  `${worktreeRoot}/ralph/<name>` on branch `ralph/<name>`; `.ralph-tui/bin/clean <name>` removes both once the",
      '  branch is delivered.',
    ]*.toString()
    instructions.collectEntries { [(it): lines] }
  }

  private static String assign(String script, String variable, String value) {
    def lines = script.split('\n', -1).toList()
    def index = lines.findIndexOf { it.startsWith("${variable}=") }

    if (index < 0) {
      throw new IllegalStateException("${variable}= is not in the script")
    }

    lines[index] = "${variable}=${shellQuoted(value)}".toString()
    lines.join('\n')
  }

  // A failed check exits the worker non-zero, which ralph-tui retries only under the retry strategy.
  private List<String> errorHandling() {
    workerCheck ? ['strategy = "retry"', 'maxRetries = 2', 'continueOnNonZeroExit = false'] : ['strategy = "skip"']
  }

  private String runFlags() {
    if (!epicTracker()) {
      return '--prd <file>'
    }

    'json' in trackers ? '(--epic <id> | --prd <file>)' : '--epic <id>'
  }

  private String epicTracker() {
    trackers.find { it in EPIC_TRACKERS }
  }

  private List<String> entry(String harness) {
    def defaultFlags = ((workerCheck ? [harness] : []) + flags(harness)).collect { quoted(it) }
    ['[[agents]]', "name = \"${harness}\"", "plugin = \"${harness}\""] +
    (workerCheck ? ["command = \"${WORKER}\""] : []) +
    ["defaultFlags = [${defaultFlags.join(', ')}]", '', '[agents.options]'] + AGENTS[harness].options
  }

  private List<String> flags(String harness) {
    AGENTS[harness].flags + (workerBindings[harness]?.flags ?: [])
  }

  private void checkHarnesses() {
    def unknown = harnesses - HARNESSES

    if (unknown) {
      throw new IllegalArgumentException("--harness=${unknown.first()} is not one of: ${HARNESSES.join(', ')}")
    }

    if (!harnesses) {
      throw new IllegalArgumentException('ralph-tui needs at least one --harness')
    }
  }

  private void checkTrackers() {
    def unknown = trackers - TRACKERS

    if (unknown || !trackers) {
      throw new IllegalArgumentException("--tracker=${unknown ? unknown.first() : ''} is not one of: " +
      TRACKERS.join(', '))
    }

    if (EPIC_TRACKERS.every { it in trackers }) {
      throw new IllegalArgumentException('--tracker=beads and --tracker=beads-bv are both given\n  both run with ' +
      '--epic, and the configuration names one tracker\n  give one of them')
    }
  }

  private void checkExistingConfiguration() {
    def text = current(CONFIG_PATH)

    if (text == null) {
      return
    }

    def config = parsedToml(CONFIG_PATH, text, ['fix it, or delete it and cook again'])
    def agents = config.agents instanceof List ? config.agents.findAll { it instanceof Map } : []

    harnesses.each { harness ->
      if (!agents.any { it.plugin == harness }) {
        notApplied("--harness=${harness}", "no [[agents]] entry there has plugin = \"${harness}\"")
      }
    }

    harnesses.each { harness ->
      def binding = workerBindings[harness]

      if (binding?.flags && !agents.any { it.plugin == harness && containsRun(it.defaultFlags, binding.flags) }) {
        def flags = binding.flags.collect { quoted(it) }.join(', ')
        notApplied(binding.argument(), "no [[agents]] entry there with plugin = \"${harness}\" has ${flags} in " +
            'its defaultFlags')
      }
    }

    harnesses.each { harness ->
      if (workerCheck && !agents.any { it.plugin == harness && it.command == WORKER && firstFlag(it) == harness }) {
        notApplied("--worker-check=${workerCheck}", "no [[agents]] entry there with plugin = \"${harness}\" has " +
            "command = \"${WORKER}\" and \"${harness}\" first in its defaultFlags")
      }
    }

    def tracker = config.tracker ?: config.defaultTracker

    def set = tracker == null ? 'it sets no tracker' : "its tracker is ${quoted(tracker.toString())}"

    if ('beads' in trackers && !(tracker instanceof CharSequence && tracker.toString().startsWith('beads'))) {
      notApplied('--tracker=beads', "${set}, and a run with --epic needs tracker = \"beads\"")
    }

    if ('beads-bv' in trackers && tracker != 'beads-bv') {
      notApplied('--tracker=beads-bv', "${set}, and --tracker=beads-bv needs tracker = \"beads-bv\"")
    }
  }

  private static Map parsedToml(String path, String text, List<String> next) {
    try {
      new TomlSlurper().parseText(text)
    } catch (Exception e) {
      def cause = (e.message ?: e.class.name).readLines()[0]
      throw new IllegalArgumentException((["${path} is not valid TOML", cause] + next).join('\n  '))
    }
  }

  private static boolean containsRun(Object defaultFlags, List<String> run) {
    defaultFlags instanceof List && Collections.indexOfSubList(defaultFlags, run) >= 0
  }

  private static Object firstFlag(Map agent) {
    agent.defaultFlags instanceof List && agent.defaultFlags ? agent.defaultFlags.first() : null
  }

  private static void notApplied(String argument, String cause) {
    throw new IllegalArgumentException("${argument} is not applied in ${CONFIG_PATH}\n  ${cause}\n  the " +
    'configuration is seeded once and never rewritten, so a later cook cannot add it\n  add it there, or ' +
    "delete ${CONFIG_PATH} and cook again")
  }

  /** One binding of a worker role or settings file to a harness, with the argument as the person gave it. */
  private static class WorkerBinding {
    String param
    String value
    String harness
    String operand
    List<String> flags = []

    String argument() {
      "--${param}=${value}"
    }
  }

  private Map<String, WorkerBinding> resolveWorkers() {
    def roles = bindings('worker-role', workerRoles, '<role>', 'the role binds', 'claude:worker')
    def settings = bindings('worker-settings', workerSettings, '<path>', 'the file binds', 'claude:ralph-claude.json')

    (roles + settings).each { binding ->
      if (!(binding.harness in HARNESSES)) {
        throw new IllegalArgumentException("${binding.argument()} names ${binding.harness}, which is not one of: " +
        HARNESSES.join(', '))
      }

      if (!(binding.harness in harnesses)) {
        throw new IllegalArgumentException("${binding.argument()} names ${binding.harness}, which no --harness " +
        'gives\n  add --harness=' + binding.harness + ', or leave it out')
      }

      if (binding.param == 'worker-role' && binding.harness == 'codex') {
        throw new IllegalArgumentException("${binding.argument()} binds a role to codex, which has no roles\n" +
        '  Codex loads no rules for one run, so a role\'s denies reach Claude only\n' +
        '  give the Codex entry settings with --worker-settings=codex:<path>')
      }
    }

    checkOnePerHarness(roles + settings)
    roles.each { it.flags = roleFlags(it) }
    settings.each { it.flags = settingsFlags(it, it.operand) }
    (roles + settings).collectEntries { [(it.harness): it] }
  }

  private static List<WorkerBinding> bindings(String param, List<String> values, String operand, String binds,
      String example) {
    values.collect { value ->
      def colon = value.indexOf(':')
      def harness = colon < 0 ? '' : value.substring(0, colon)
      def rest = colon < 0 ? '' : value.substring(colon + 1)

      if (!harness.trim() || !rest.trim()) {
        throw new IllegalArgumentException("--${param}=${value} is not <harness>:${operand}\n  name the harness " +
        "${binds}, as --${param}=${example}")
      }

      new WorkerBinding(param: param, value: value, harness: harness, operand: rest)
    }
  }

  private static void checkOnePerHarness(List<WorkerBinding> bindings) {
    bindings.groupBy { it.harness }.each { harness, bound ->
      def repeated = bound.find { binding -> bound.count { it.param == binding.param } > 1 }

      if (repeated) {
        throw new IllegalArgumentException("--${repeated.param} names ${harness} twice\n  give one file per harness")
      }

      if (bound.size() > 1) {
        throw new IllegalArgumentException("${bound[0].argument()} and ${bound[1].argument()} both set ${harness}'s " +
        'settings\n  give one of them; to combine a role with more settings, write a settings file that holds both')
      }
    }
  }

  private List<String> roleFlags(WorkerBinding binding) {
    def role = binding.operand

    if (!(role ==~ /[a-z][a-z0-9-]*/)) {
      throw new IllegalArgumentException("${binding.argument()} is not a role name\n" +
      '  a role is a key under roles in .agents/permission/policy.yaml: lower-case letters, digits and hyphens, ' +
      'starting with a letter')
    }

    def file = ".agents/adapters/claude/roles/${role}.json".toString()

    if (current(file) == null) {
      throw new IllegalArgumentException("${binding.argument()} needs ${file}\n  the role file is not in the " +
      "draft\n  define roles.${role} in .agents/permission/policy.yaml, then cook " +
      'io.github.lmarqs.cookbook:agents#connect --harness=claude')
    }

    settingsFlags(binding, file)
  }

  private List<String> settingsFlags(WorkerBinding binding, String given) {
    def segments = given.split('/').findAll { it && it != '.' }

    if (given.startsWith('/') || '..' in segments || !segments) {
      throw new IllegalArgumentException("${binding.argument()} is not a path from the repository root\n" +
      '  name a file inside the repository, without .. segments')
    }

    def path = segments.join('/')
    binding.harness == 'claude' ? ['--settings', path] : codexFlags(binding, path)
  }

  private List<String> codexFlags(WorkerBinding binding, String path) {
    def text = current(path)

    if (text == null) {
      throw new IllegalArgumentException("${binding.argument()} needs ${path} in the draft\n" +
      '  Codex settings are read when cooking\n  write the file, then cook again')
    }

    def flags = []
    flatten(parsedToml(path, text, []), '', path).each { key, value ->
      def rendered = render(value)

      if (rendered == null) {
        throw new IllegalArgumentException("${path}: ${key} cannot be passed as -c ${key}=<value>\n" +
        '  use strings, numbers, booleans or lists of them')
      }

      flags += ['-c', "${key}=${rendered}".toString()]
    }
    flags
  }

  // A -c key is a dotted path to nested tables, and in <key>=<value> a key's own = reads as the separator, so a TOML
  // key holding either, such as "my.server", would set another key than the file's.
  private static Map<String, Object> flatten(Map table, String prefix, String path) {
    def flat = [:]
    table.each { key, value ->
      def name = "${prefix}${key}".toString()

      if (!key || key.contains('.') || key.contains('=')) {
        throw new IllegalArgumentException("${path}: ${prefix}${quoted(key.toString())} cannot be passed as " +
        '-c <key>=<value>\n  -c reads each . in a key as a nested table and an = as the end of the key; use keys ' +
        'without them')
      }

      if (value instanceof Map) {
        flat += flatten(value, "${name}.", path)
      } else {
        flat[name] = value
      }
    }
    flat
  }

  private static String render(Object value) {
    if (value instanceof CharSequence) {
      return quoted(value.toString())
    }

    if (value instanceof Number || value instanceof Boolean) {
      return value.toString()
    }

    if (value instanceof List) {
      def items = value.collect { it instanceof List ? null : render(it) }
      return null in items ? null : "[${items.join(', ')}]".toString()
    }

    null
  }

  // In a run worktree, .ralph-tui/ holds only what this recipe writes, so a team's script there would never be found.
  private static void outsideRalphTui(String param, String path, String example) {
    if (path != null && path.split('/').first() == '.ralph-tui') {
      throw new IllegalArgumentException("--${param}=${path} is under .ralph-tui/\n" +
      '  a run worktree has there only the configuration, prompts and launcher this recipe writes\n' +
      "  name a committed file outside .ralph-tui/, as --${param}=${example}")
    }
  }

  private static String shellQuoted(String value) {
    "'${value.replace("'", "'\\''")}'"
  }

  private static String quoted(String value) {
    groovy.json.JsonOutput.toJson(value)
  }
}
