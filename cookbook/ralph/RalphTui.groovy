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
 * `defaultFlags`. Entries a team may add, launched with `ralph-tui run --agent <name>`:
 *
 * ```toml
 * # A Claude worker bound to a role's denies, beside the default entry.
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
 * Known limits of those versions: ralph-tui's parallel mode starts children that are still blocked and refuses fewer
 * than three tasks; Codex runs no project hooks in a worktree, because it trusts hooks per checkout path, unless
 * started with `--dangerously-bypass-hook-trust`; a Codex `workspace-write` worker can neither read nor write the Beads
 * database, nor write `.git`, so it cannot commit; Claude Code ignores the project's `permissions.allow` in a workspace
 * nobody has trusted, so an unattended Claude worker's allows come through `--worker-role` or `--worker-settings`;
 * deny rules are prefix rules that `git -C . push` gets past, a guardrail, and the structural limit is a worker without
 * push credentials.
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
 * ralph.workerRole('worker')
 * ralph.worktreeRoot('.worktrees')
 * ralph.worktreeSetup('.worktrees/setup')
 * ralph.install('io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worker-role=worker ' +
 *     '--worktree-root=.worktrees --worktree-setup=.worktrees/setup')
 * ```
 *
 * ## With other models
 *
 * None: ralph-tui names the other tools only by path. Run worktrees follow `io.github.lmarqs.cookbook:git#worktrees`'s
 * rule `.worktrees/<branch>` by default, and `worktreeRoot` moves them as `io.github.lmarqs.cookbook:agents#connect
 * --worktree-root` moves Claude Code's; the launcher runs a setup file only when `setup` names one, such as that
 * recipe's `.worktrees/setup`; `--worker-role` reads the role files `io.github.lmarqs.cookbook:agents#connect` renders.
 */
@Ingredient
class RalphTui {
  /** The harnesses ralph-tui drives, by their ralph-tui agent plugin names. */
  static final List<String> HARNESSES = ['claude', 'codex']

  /** The trackers a run takes tasks from: `beads` with `--epic`, `json` with `--prd`. */
  static final List<String> TRACKERS = ['beads', 'json']

  /** Where run worktrees go when `worktreeRoot` is not called, following `.worktrees/<branch>`. */
  static final String WORKTREE_ROOT = '.worktrees'

  /** Where `io.github.lmarqs.cookbook:agents#connect` renders each role's Claude settings, as `<role>.json`. */
  static final String ROLES = '.agents/adapters/claude/roles'

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
  private List<String> trackers = TRACKERS
  private String workerRole
  private String workerSettings
  private String worktreeRoot = WORKTREE_ROOT
  private String worktreeSetup
  private String worktreeTeardown
  private String workerCheck
  private String cookedWith
  private List<String> instructions = []

  /**
   * Sets the `[[agents]]` entries the seeded configuration gets, one per harness in the order given, the first as
   * `defaultAgent`; a repeated harness counts once. `null` means none.
   */
  void harnesses(List<String> names) {
    harnesses = names ?: []
  }

  /**
   * Narrows what is seeded and offered to the trackers in `names`, each `beads` or `json`: their prompts, the
   * configuration's `tracker = "beads"`, and the run flags `bin/run`'s usage line and the instructions section name.
   * Both by default, and for `null`. A run still picks its tracker with `--epic` or `--prd`.
   */
  void trackers(List<String> names) {
    trackers = names ?: TRACKERS
  }

  /**
   * Adds `"--settings", ".agents/adapters/claude/roles/<name>.json"` to the Claude entry's `defaultFlags`. `null` adds
   * none.
   */
  void workerRole(String name) {
    workerRole = name
  }

  /**
   * Adds `"--settings", "<path>"` to the Claude entry's `defaultFlags`; the file is the team's, and nothing reads or
   * writes it at cook time. `null` adds none.
   */
  void workerSettings(String path) {
    workerSettings = path
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
   * 3. a tracker is not `beads` or `json`, or none is given;
   * 4. the worker role is not a role name;
   * 5. the worker role or settings is given with no `claude` harness;
   * 6. both the worker role and the worker settings are given;
   * 7. the role's `.agents/adapters/claude/roles/<name>.json` is not in the draft;
   * 8. the worker settings path is blank;
   * 9. the worktree setup, worktree teardown or worker check is under `.ralph-tui/`, which in a run worktree holds only
   *    what this recipe writes.
   */
  void install(String cookedWith) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('install needs the recipe it is cooked with')
    }

    checkHarnesses()
    checkTrackers()
    checkWorker()
    outsideRalphTui('worktree-setup', worktreeSetup, '.worktrees/setup')
    outsideRalphTui('worktree-teardown', worktreeTeardown, '.worktrees/teardown')
    outsideRalphTui('worker-check', workerCheck, 'scripts/check')

    harnesses = harnesses.unique(false)
    trackers = TRACKERS.findAll { it in trackers }
    this.cookedWith = cookedWith
  }

  /**
   * `.ralph-tui/config.toml`, seeded when absent and never owned: config version 2.1, the `beads` tracker, the first
   * harness as `defaultAgent`, ten iterations, `autoCommit`, skipping a failed task, or with `workerCheck` retrying it
   * twice first, direct merges in parallel mode, and one `[[agents]]` entry per harness, when installed.
   */
  @Seed(path = '.ralph-tui/config.toml')
  String config() {
    if (!cookedWith) {
      return null
    }

    def lines = ['configVersion = "2.1"',] + ('beads' in trackers ? ['tracker = "beads"'] : []) + [
      "defaultAgent = \"${harnesses.first()}\"",
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
   * `.ralph-tui/templates/json.hbs`, the prompt for a run with `--prd`, seeded when absent and never owned, when
   * installed.
   */
  @Seed(path = '.ralph-tui/templates/json.hbs')
  String jsonPrompt() {
    cookedWith && 'json' in trackers ? resource('templates/json.hbs') : null
  }

  /**
   * The executable `.ralph-tui/bin/run`, which runs ralph-tui in `<worktreeRoot>/ralph/<name>`, with the `worktreeRoot`
   * and `worktreeSetup` paths shell-quoted into its `worktrees` and `setup` variables, when installed.
   */
  @File(path = '.ralph-tui/bin/run', mode = '0755')
  String run() {
    if (!cookedWith) {
      return null
    }

    def script = withWorktreeRoot(resource('bin/run.sh')).replace('(--epic <id> | --prd <file>)', runFlags())
    worktreeSetup ? script.replace("\nsetup=''\n", "\nsetup=${shellQuoted(worktreeSetup)}\n") : script
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

    def script = withWorktreeRoot(resource('bin/clean.sh'))
    worktreeTeardown ? script.replace("\nteardown=''\n", "\nteardown=${shellQuoted(worktreeTeardown)}\n") : script
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

    def script = resource('bin/worker.sh').replace("\nagents=''\n", "\nagents=${shellQuoted(harnesses.join(' '))}\n")
    workerCheck ? script.replace("\ncheck=''\n", "\ncheck=${shellQuoted(workerCheck)}\n") : script
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

    def run = trackers == TRACKERS ? '`.ralph-tui/bin/run <name> --epic <id>`, or `--prd <file>`,' :
        "`.ralph-tui/bin/run <name> ${runFlags()}`"
    def lines = [
      "<!-- ${cookedWith} -->",
      "- Loop runs: ${run} runs ralph-tui in",
      "  `${worktreeRoot}/ralph/<name>` on branch `ralph/<name>`; `.ralph-tui/bin/clean <name>` removes both once the",
      '  branch is delivered.',
    ]*.toString()
    instructions.collectEntries { [(it): lines] }
  }

  private String withWorktreeRoot(String script) {
    script.replace("\nworktrees='.worktrees'\n", "\nworktrees=${shellQuoted(worktreeRoot)}\n")
  }

  // A failed check exits the worker non-zero, which ralph-tui retries only under the retry strategy.
  private List<String> errorHandling() {
    workerCheck ? ['strategy = "retry"', 'maxRetries = 2', 'continueOnNonZeroExit = false'] : ['strategy = "skip"']
  }

  private String runFlags() {
    trackers == TRACKERS ? '(--epic <id> | --prd <file>)' : trackers == ['beads'] ? '--epic <id>' : '--prd <file>'
  }

  private List<String> entry(String harness) {
    def defaultFlags = ((workerCheck ? [harness] : []) + flags(harness)).collect { quoted(it) }
    ['[[agents]]', "name = \"${harness}\"", "plugin = \"${harness}\""] +
    (workerCheck ? ["command = \"${WORKER}\""] : []) +
    ["defaultFlags = [${defaultFlags.join(', ')}]", '', '[agents.options]'] + AGENTS[harness].options
  }

  private List<String> flags(String harness) {
    AGENTS[harness].flags + (extraFlags()[harness] ?: [])
  }

  private Map<String, List<String>> extraFlags() {
    def settings = workerRole ? "${ROLES}/${workerRole}.json".toString() : workerSettings
    settings ? [claude: ['--settings', settings]] : [:]
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
  }

  private void checkWorker() {
    if (workerRole != null && !(workerRole ==~ /[a-z][a-z0-9-]*/)) {
      throw new IllegalArgumentException("--worker-role=${workerRole} is not a role name\n" +
      '  a role is a key under roles in .agents/permission/policy.yaml: lower-case letters, digits and hyphens, ' +
      'starting with a letter')
    }

    if (workerRole != null) {
      claudeOnly("--worker-role=${workerRole}")
    }

    if (workerSettings != null) {
      claudeOnly("--worker-settings=${workerSettings}")
    }

    if (workerRole != null && workerSettings != null) {
      throw exclusive()
    }

    if (workerRole != null) {
      def file = "${ROLES}/${workerRole}.json"

      if (current(file) == null) {
        throw new IllegalArgumentException("--worker-role=${workerRole} needs ${file}\n  the role file is not in the " +
        "draft\n  define roles.${workerRole} in .agents/permission/policy.yaml, then cook " +
        'io.github.lmarqs.cookbook:agents#connect --harness=claude')
      }
    }

    if (workerSettings != null && !workerSettings.trim()) {
      throw new IllegalArgumentException('--worker-settings needs a path')
    }
  }

  private void claudeOnly(String argument) {
    if (!('claude' in harnesses)) {
      throw new IllegalArgumentException("${argument} applies to the claude entry\n  no --harness=claude is given\n" +
      '  add --harness=claude, or leave it out')
    }
  }

  private static IllegalArgumentException exclusive() {
    new IllegalArgumentException('--worker-role and --worker-settings are both given\n  each sets the one --settings ' +
        'file of the claude entry\n  give one of them; to combine a role with more settings, write a settings file ' +
        'that holds both')
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
