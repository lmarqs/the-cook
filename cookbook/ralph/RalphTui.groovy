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
 * repository's `.worktrees/ralph/`.
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
 * ralph.worktreeSetup('.worktrees/setup')
 * ralph.install('io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worker-role=worker ' +
 *     '--worktree-setup=.worktrees/setup')
 * ```
 *
 * ## With other models
 *
 * None: ralph-tui names the other tools only by path. Run worktrees follow `io.github.lmarqs.cookbook:git#worktrees`'s
 * rule `.worktrees/<branch>`, and the launcher runs a setup file only when `setup` names one, such as that recipe's
 * `.worktrees/setup`; `--worker-role` reads the role files `io.github.lmarqs.cookbook:agents#connect` renders.
 */
@Ingredient
class RalphTui {
  /** The harnesses ralph-tui drives, by their ralph-tui agent plugin names. */
  static final List<String> HARNESSES = ['claude', 'codex']

  /** Where `io.github.lmarqs.cookbook:agents#connect` renders each role's Claude settings, as `<role>.json`. */
  static final String ROLES = '.agents/adapters/claude/roles'

  private static final Map<String, List<String>> FLAGS = [
    claude: ['--permission-prompts', 'none', '--permission-mode', 'acceptEdits'],
    codex: ['-c', 'approval_policy=never', '-c', 'sandbox_workspace_write.network_access=false'],
  ]
  private static final Map<String, List<String>> OPTIONS = [
    claude: ['skipPermissions = false'],
    codex: ['fullAuto = false', 'sandbox = "workspace-write"'],
  ]

  private List<String> harnesses = []
  private String workerRole
  private String workerSettings
  private String worktreeSetup
  private String worktreeTeardown
  private String cookedWith
  private List<String> instructions = []

  /**
   * Sets the `[[agents]]` entries the seeded configuration gets, one per harness in the order given, the first as
   * `defaultAgent`; a repeated harness counts once. Each must be `claude` or `codex`, and at least one is required.
   */
  void harnesses(List<String> names) {
    def unknown = names - HARNESSES

    if (unknown) {
      throw new IllegalArgumentException("--harness=${unknown.first()} is not one of: ${HARNESSES.join(', ')}")
    }

    if (!names) {
      throw new IllegalArgumentException('ralph-tui needs at least one --harness')
    }

    harnesses = names.unique(false)
  }

  /**
   * Adds `"--settings", ".agents/adapters/claude/roles/<name>.json"` to the Claude entry's `defaultFlags`. Refused when
   * that file is not in the draft, naming the policy role to define and the recipe to cook; when `workerSettings` was
   * called; and when no Claude entry is requested. Call after `harnesses`.
   */
  void workerRole(String name) {
    if (!(name ==~ /[a-z][a-z0-9-]*/)) {
      throw new IllegalArgumentException("--worker-role=${name} is not a role name\n  a role is a key under roles in " +
      '.agents/permission/policy.yaml: lower-case letters, digits and hyphens, starting with a letter')
    }

    claudeOnly("--worker-role=${name}")

    if (workerSettings) {
      throw exclusive()
    }

    def file = "${ROLES}/${name}.json"

    if (current(file) == null) {
      throw new IllegalArgumentException("--worker-role=${name} needs ${file}\n  the role file is not in the draft\n" +
      "  define roles.${name} in .agents/permission/policy.yaml, then cook " +
      'io.github.lmarqs.cookbook:agents#connect --harness=claude')
    }

    workerRole = name
  }

  /**
   * Adds `"--settings", "<path>"` to the Claude entry's `defaultFlags`; the file is the team's, and nothing reads or
   * writes it at cook time. Refused when `workerRole` was called, and when no Claude entry is requested. Call after
   * `harnesses`.
   */
  void workerSettings(String path) {
    if (!path?.trim()) {
      throw new IllegalArgumentException('--worker-settings needs a path')
    }

    claudeOnly("--worker-settings=${path}")

    if (workerRole) {
      throw exclusive()
    }

    workerSettings = path
  }

  /**
   * Has `bin/run` run the file at `path`, by its path from the repository root, from the root of each worktree it
   * creates, and fail the run when that file is missing, not executable or fails. Without it, `bin/run` runs no setup.
   * Refused under `.ralph-tui/`, which in a run worktree holds only what this recipe writes.
   */
  void worktreeSetup(String path) {
    worktreeSetup = outsideRalphTui('worktree-setup', path)
  }

  /**
   * Has `bin/clean` run the file at `path`, by its path from the repository root, from the root of the run worktree
   * before removing anything, and stop, removing nothing, when that file is missing, not executable or fails. Without
   * it, `bin/clean` runs no teardown. Refused under `.ralph-tui/`, as `worktreeSetup` is.
   */
  void worktreeTeardown(String path) {
    worktreeTeardown = outsideRalphTui('worktree-teardown', path)
  }

  /**
   * Has the launcher's commands stated in each Markdown file in `paths`, by its path from the repository root, in a
   * `ralph-tui` section. A file the draft lacks is refused when the sections are written.
   */
  void instructions(List<String> paths) {
    instructions = paths.unique(false)
  }

  /**
   * Requests the owned launcher, `bin/run` and `bin/clean`, and the `ralph-tui` section of `.gitignore`. `cookedWith`
   * names the recipe and its arguments, which the section's first line records; it must not be blank. Call after
   * `harnesses`, `workerRole`, `workerSettings`, `worktreeSetup`, `worktreeTeardown` and `instructions`.
   */
  void install(String cookedWith) {
    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('install needs the recipe it is cooked with')
    }

    this.cookedWith = cookedWith
  }

  /**
   * `.ralph-tui/config.toml`, seeded when absent and never owned: config version 2.1, the `beads` tracker, the first
   * harness as `defaultAgent`, ten iterations, `autoCommit`, skipping a failed task, direct merges in parallel mode,
   * and one `[[agents]]` entry per harness, when harnesses are set.
   */
  @Seed(path = '.ralph-tui/config.toml')
  String config() {
    if (!harnesses) {
      return null
    }

    def lines = [
      'configVersion = "2.1"',
      'tracker = "beads"',
      "defaultAgent = \"${harnesses.first()}\"",
      '# ralph-tui stops a run after this many iterations, each one agent run on one task; 0 means no limit.',
      'maxIterations = 10',
      'autoCommit = true',
      '',
      '[errorHandling]',
      'strategy = "skip"',
      '',
      '[parallel]',
      'directMerge = true',
    ]
    harnesses.each { harness ->
      def defaultFlags = flags(harness).collect { quoted(it) }
      lines += [
        '',
        '[[agents]]',
        "name = \"${harness}\"",
        "plugin = \"${harness}\"",
        "defaultFlags = [${defaultFlags.join(', ')}]",
        '',
        '[agents.options]',
      ] + OPTIONS[harness]
    }
    lines.join('\n') + '\n'
  }

  /**
   * `.ralph-tui/templates/beads.hbs`, the prompt for a run with `--epic`, seeded when absent and never owned, when
   * harnesses are set.
   */
  @Seed(path = '.ralph-tui/templates/beads.hbs')
  String beadsPrompt() {
    harnesses ? resource('templates/beads.hbs') : null
  }

  /**
   * `.ralph-tui/templates/json.hbs`, the prompt for a run with `--prd`, seeded when absent and never owned, when
   * harnesses are set.
   */
  @Seed(path = '.ralph-tui/templates/json.hbs')
  String jsonPrompt() {
    harnesses ? resource('templates/json.hbs') : null
  }

  /**
   * The executable `.ralph-tui/bin/run`, which runs ralph-tui in `.worktrees/ralph/<name>`, with the `worktreeSetup`
   * path shell-quoted into its `setup` variable, when installed.
   */
  @File(path = '.ralph-tui/bin/run', mode = '0755')
  String run() {
    if (!cookedWith) {
      return null
    }

    def script = resource('bin/run.sh')
    worktreeSetup ? script.replace("\nsetup=''\n", "\nsetup=${shellQuoted(worktreeSetup)}\n") : script
  }

  /**
   * The executable `.ralph-tui/bin/clean`, which removes a run's worktrees and branches, with the `worktreeTeardown`
   * path shell-quoted into its `teardown` variable, when installed.
   */
  @File(path = '.ralph-tui/bin/clean', mode = '0755')
  String clean() {
    if (!cookedWith) {
      return null
    }

    def script = resource('bin/clean.sh')
    worktreeTeardown ? script.replace("\nteardown=''\n", "\nteardown=${shellQuoted(worktreeTeardown)}\n") : script
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
      '/.worktrees/ralph/',
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

    def lines = [
      "<!-- ${cookedWith} -->",
      '- Loop runs: `.ralph-tui/bin/run <name> --epic <id>`, or `--prd <file>`, runs ralph-tui in',
      '  `.worktrees/ralph/<name>` on branch `ralph/<name>`; `.ralph-tui/bin/clean <name>` removes both once the',
      '  branch is delivered.',
    ]*.toString()
    instructions.collectEntries { [(it): lines] }
  }

  private List<String> flags(String harness) {
    def extra = harness == 'claude' && workerRole ? ['--settings', "${ROLES}/${workerRole}.json".toString()] :
    harness == 'claude' && workerSettings ? ['--settings', workerSettings] : []
    FLAGS[harness] + extra
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
  private static String outsideRalphTui(String param, String path) {
    if (path.split('/').first() == '.ralph-tui') {
      throw new IllegalArgumentException("--${param}=${path} is under .ralph-tui/\n" +
      '  a run worktree has there only the configuration, prompts and launcher this recipe writes\n' +
      "  name a committed file outside .ralph-tui/, as --${param}=.worktrees/${param - 'worktree-'}")
    }

    path
  }

  private static String shellQuoted(String value) {
    "'${value.replace("'", "'\\''")}'"
  }

  private static String quoted(String value) {
    groovy.json.JsonOutput.toJson(value)
  }
}
