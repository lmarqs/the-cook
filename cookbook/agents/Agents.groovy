package io.github.lmarqs.cookbook.agents

/**
 * Models the project instructions, policy and skills shared by coding agents in a repository.
 *
 * ## Writes for
 *
 * Markdown instructions in `AGENTS.md`, seeded from a section template that an agent fills in when asked, whose
 * line under each heading says what the section holds; version 2 of the YAML policy at
 * `.agents/permission/policy.yaml`; skill file trees under `.agents/skills/`, each with `SKILL.md` and its supporting
 * resources; the shared directories `.agents/skills/`, `.agents/commands/`, `.agents/hooks/` and
 * `.agents/memory/`, described in `.agents/README.md`; and the POSIX `sh` dispatcher `.agents/hooks/session-start`,
 * which turns the output of the scripts in `.agents/hooks/session-start.d/` into the JSON session context Claude Code
 * 2.1.306 and Codex 0.160.1 both take.
 *
 * ## Instances
 *
 * One instance covers the shared setup in one directory. Use `at` for a separate setup in another directory;
 * each connected harness must be able to read the shared paths from its own directory.
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
 * agents.instruct('Run the project checks before delivery.')
 * agents.skills('agents-layout')
 * agents.permit([claude])
 * agents.connect([claude])
 * ```
 *
 * ## With other models
 *
 * `Harness` models, such as `Claude` and `Codex` in this artifact or a harness from another cookbook, translate
 * the shared policy and paths into their own native files.
 * Agents owns the shared layout and computes the relative paths it passes to each harness, the session-start
 * dispatcher's included. A tool's recipe adds session context by a script in `.agents/hooks/session-start.d/`, at the
 * path the person names by a parameter of that recipe, never a harness's session-start key.
 */
@Ingredient
class Agents {
  /** The shared directories under `.agents/` that `seed` creates and `connect` links each harness to. */
  static final List<String> DIRECTORIES = ['skills', 'commands', 'hooks', 'memory']

  /**
   * The session-start dispatcher each connected harness runs at session start; it runs every script in
   * `.agents/hooks/session-start.d/`.
   */
  static final String SESSION_START = '.agents/hooks/session-start'

  /** The hook Claude Code runs to create and remove its worktrees, once a worktree root is named. */
  static final String WORKTREE_HOOK = '.agents/hooks/worktree'

  /** The seeded files' templates, inside the `agents-layout` skill so a review can compare the seeds with them. */
  static final String TEMPLATES = 'skills/agents-layout/templates'

  private List<String> instructions = []
  private List<String> skills = []
  private boolean instructionsUsed = false
  private boolean policyUsed = false
  private boolean layoutUsed = false
  private boolean sessionStarted = false
  private Map<String, String> worktreePaths = [:]
  private List<Harness> ignoredHarnesses = []
  private String ignoredBy

  /**
   * Requests the seeds of `AGENTS.md`, `.agents/permission/policy.yaml` and the shared layout, written only when absent
   * and never owned.
   */
  void seed() {
    seedInstructions()
    seedLayout()
    policyUsed = true
  }

  /**
   * Requests the seeds of the shared directories, each kept in Git by an empty `.gitkeep`, and of `.agents/README.md`,
   * which says what each holds; `connect` then links every harness to all of them.
   */
  void seedLayout() {
    layoutUsed = true
  }

  /** Requests the seed of `AGENTS.md`, written from the template only when absent and never owned. */
  void seedInstructions() {
    instructionsUsed = true
  }

  /** Adds individual Markdown lines to the shared instructions and requests the instruction file's seed. */
  void instruct(String... lines) {
    instruct(lines as List)
  }

  /** Adds a collection of Markdown lines to the shared instructions and requests the instruction file's seed. */
  void instruct(Collection<String> lines) {
    instructions += lines
    instructionsUsed = true
  }

  /**
   * Adds skills packaged with this artifact to the shared skill directory, by name: `agents-layout`, which explains
   * the shared layout, how to adopt and change it, and how to investigate misuse. Rejects any other name. Add skills
   * before calling `connect`.
   */
  void skills(String... names) {
    names.each {
      if (!resources("skills/${it}")) {
        throw new IllegalArgumentException("No packaged skill named ${it}")
      }
    }
    skills += names.toList()
  }

  /** The shared agent policy at `.agents/permission/policy.yaml`, or the packaged starter when the file is absent. */
  Policy policy() {
    policyUsed = true
    Policy.parse(current(Policy.PATH) ?: resource("${TEMPLATES}/policy.template.yaml"))
  }

  /**
   * Renders the shared policy into each harness's own permission model. `cookedWith`, when given, names the recipe and
   * the arguments that cook the rendered sections again, such as
   * `io.github.lmarqs.cookbook:agents#connect --harness=codex`; each section opens with it as a comment, because
   * `cook.lock` records no arguments.
   */
  void permit(Collection<Harness> harnesses, String cookedWith = null) {
    def shared = policy()
    harnesses.each { it.permit(shared, cookedWith) }
  }

  /**
   * Points each harness at the shared directories, by paths relative to where it sits: every directory once the layout
   * is seeded, otherwise `.agents/skills` once skills are added. Each harness reads `AGENTS.md` natively. Once the
   * layout is seeded, also makes each harness run the session-start dispatcher at session start. Call it after adding
   * skills.
   */
  void connect(Collection<Harness> harnesses) {
    def shared = layoutUsed ? DIRECTORIES : skills ? ['skills'] : []

    if (!shared) {
      return
    }

    harnesses.each { harness ->
      harness.readDirectories(shared.collectEntries { [it, reach(harness, ".agents/${it}")] })
    }

    if (!layoutUsed || !harnesses) {
      return
    }

    sessionStarted = true
    harnesses.each { it.startSession(reach(it, SESSION_START)) }
  }

  /**
   * Has each harness that can run worktree hooks create its worktrees at `<root>/<name>` on the branch `<name>` and
   * remove them, through `.agents/hooks/worktree`: after creating one, the hook runs `setup` from its root, and before
   * removing one, `teardown`; each is a path from the repository root, or null for none. Refuses `setup` or `teardown`
   * without `root`, and `root` when no harness runs worktree hooks.
   */
  void worktrees(Collection<Harness> harnesses, String root, String setup, String teardown) {
    if (root == null) {
      def orphan = [setup: setup, teardown: teardown].find { name, path -> path != null }

      if (orphan) {
        throw new IllegalArgumentException("--worktree-${orphan.key}=${orphan.value} needs --worktree-root\n" +
        '  the worktree hook runs it, and the hook is written only for a worktree root\n' +
        '  name the root too, as --worktree-root=.worktrees')
      }

      return
    }

    def hooked = harnesses.findAll { it.createWorktrees(reach(it, WORKTREE_HOOK)) }

    if (!hooked) {
      throw new IllegalArgumentException("--worktree-root=${root} needs a harness that runs worktree hooks\n" +
      '  Claude Code runs them, and Codex 0.160.1 has no worktree hook event\n' +
      '  add --harness=claude')
    }

    worktreePaths = [root: root, setup: setup, teardown: teardown]
  }

  /**
   * Keeps each harness's local directory, such as `.claude/`, out of Git except for the links Cook makes there, so a
   * fresh clone reads the shared setup with no cooking while the files the harness writes locally stay out of commits.
   * Also keeps out the files that would make a harness skip the shared setup. `cookedWith`, when given, names the
   * recipe and the arguments that cook the section again, such as
   * `io.github.lmarqs.cookbook:agents#connect --harness=claude`; it opens the section as a comment, because `cook.lock`
   * records no arguments.
   */
  void ignoreLocalDirectories(Collection<Harness> harnesses, String cookedWith = null) {
    ignoredHarnesses = (ignoredHarnesses + harnesses).unique()
    ignoredBy = cookedWith
  }

  /**
   * Seeds `AGENTS.md` with the `agents-layout` skill's `templates/AGENTS.template.md`: a note telling agents how to
   * fill the file in when the developer asks, then short sections whose every blank is a `[[fill: …]]` and whose fixed
   * text states the rules and word budgets that hold in any repository.
   * The `agents-layout` skill's `references/agents-md.md` states why the template is shaped so and names the sources.
   * Written once instructions are added or the seed is requested; when `AGENTS.md` already exists, cooking the recipe
   * the first time fails unless Cook runs with `--force`, which replaces it, or `--skip`, which keeps it; never owned.
   */
  @Seed(path = 'AGENTS.md', existing = 'fail')
  String starter() {
    instructionsUsed ? resource("${TEMPLATES}/AGENTS.template.md") : null
  }

  /**
   * Seeds `.agents/permission/policy.yaml` with the `agents-layout` skill's `templates/policy.template.yaml` when
   * absent, once the policy is read: a policy that grants nothing, with comments explaining each field and an
   * example; never owned.
   */
  @Seed(path = '.agents/permission/policy.yaml')
  String starterPolicy() {
    policyUsed ? resource("${TEMPLATES}/policy.template.yaml") : null
  }

  /** Seeds `.agents/README.md`, which says what each shared directory holds, once the layout is seeded. */
  @Seed(path = '.agents/README.md')
  String layoutReadme() {
    layoutUsed ? resource("${TEMPLATES}/README.template.md") : null
  }

  /** Seeds `.agents/skills/.gitkeep` so Git keeps the directory, once the layout is seeded. */
  @Seed(path = '.agents/skills/.gitkeep')
  String keepSkills() {
    keep()
  }

  /** Seeds `.agents/commands/.gitkeep` so Git keeps the directory, once the layout is seeded. */
  @Seed(path = '.agents/commands/.gitkeep')
  String keepCommands() {
    keep()
  }

  /** Seeds `.agents/hooks/.gitkeep` so Git keeps the directory, once the layout is seeded. */
  @Seed(path = '.agents/hooks/.gitkeep')
  String keepHooks() {
    keep()
  }

  /** Seeds `.agents/memory/.gitkeep` so Git keeps the directory, once the layout is seeded. */
  @Seed(path = '.agents/memory/.gitkeep')
  String keepMemory() {
    keep()
  }

  /** The `instructions` section of `AGENTS.md`, when instruction lines are added. */
  @Section(path = 'AGENTS.md', id = 'instructions')
  List<String> instructionsSection() {
    instructions ?: null
  }

  /**
   * The `harnesses` section of `.gitignore`: the comment naming the recipe and arguments that cook it, when given;
   * then, for each harness `ignoreLocalDirectories` was given, its local directory re-included, such as
   * `!/.claude/`, since Git cannot re-include a file whose parent directory an earlier rule excludes; then the
   * directory's contents, such as `/.claude/*`, and a negation per link Cook makes there, such as `!/.claude/skills`;
   * then the files each harness would read in place of the shared setup.
   */
  @Section(path = '.gitignore', id = 'harnesses')
  List<String> ignoredDirectories() {
    def lines = ignoredHarnesses.collectMany { harness ->
      def local = ignored(harness)
      ["!${local}", "${local}*"] + harness.linkNames.collect { "!${local}${it}" }
    } + ignoredHarnesses.collectMany { it.ignoredFiles }

    if (!lines) {
      return null
    }

    ((ignoredBy ? ["# ${ignoredBy}"] : []) + lines)*.toString()
  }

  /**
   * `.agents/hooks/session-start`, executable, once harnesses are connected to a seeded layout: run as
   * `session-start <harness>`, it runs every `.agents/hooks/session-start.d/*.sh` in name order with the harness name
   * as `$1` and prints their joined output as `hookSpecificOutput.additionalContext` in one JSON object, the session
   * context both Claude Code and Codex take; it needs only `sh` and `awk`, and prints nothing for no output.
   */
  @File(path = '.agents/hooks/session-start', mode = '0755')
  String sessionStartDispatcher() {
    sessionStarted ? resource('hooks/session-start') : null
  }

  /**
   * `.agents/hooks/worktree`, executable, once `worktrees` names a root: run as `worktree create` with Claude Code's
   * `WorktreeCreate` input, it adds the worktree `<root>/<name>` under the main checkout on the branch `<name>`, runs
   * the setup there and prints the worktree's path, or removes the worktree when the setup fails; run as
   * `worktree remove` with `WorktreeRemove`'s input, it runs the teardown, keeping the worktree when that fails,
   * removes the worktree, and deletes its branch once merged. The root, setup and teardown paths are shell-quoted into
   * its variables; it needs only `sh`, `sed` and `git`.
   */
  @File(path = '.agents/hooks/worktree', mode = '0755')
  String worktreeHook() {
    if (!worktreePaths) {
      return null
    }

    worktreePaths.findAll { name, path -> path }.inject(resource('hooks/worktree')) { String script, name, path ->
      script.replace("\n${name}=''\n", "\n${name}=${shellQuoted(path)}\n")
    }
  }

  /** Whole files under `.agents/skills/<name>/`, when skills are added. */
  @Files(path = '.agents/skills')
  Map<String, String> skillFiles() {
    skills.collectEntries { name ->
      resources("skills/${name}").collectEntries { path, body -> ["${name}/${path}", body] }
    } ?: null
  }

  private static String shellQuoted(String value) {
    "'${value.replace("'", "'\\''")}'"
  }

  private String keep() {
    layoutUsed ? '' : null
  }

  private String ignored(Harness harness) {
    def relative = Path.of(directory)
        .normalize()
        .relativize(Path.of(harness.directory, harness.localDirectory).normalize())
    "/${relative}/"
  }

  private String reach(Harness harness, String path) {
    Path.of(harness.directory)
        .normalize()
        .relativize(Path.of(directory, path).normalize())
        .toString()
  }
}

/** An agent program that reads the shared setup through its own native files. Agents drives it. */
interface Harness {
  /** The harness name used to select the agent program, such as `claude` or `codex`. */
  String getName()

  /** The harness instance's directory relative to the destination, with `.` at its root. */
  String getDirectory()

  /**
   * The directory, relative to the harness instance's, that holds only links Cook makes and files the harness writes
   * locally, such as `.claude`; Agents ignores it in Git.
   */
  String getLocalDirectory()

  /**
   * Renders the shared policy into the harness's own permission model; `cookedWith`, when not null, opens each section
   * it owns as a comment.
   */
  void permit(Policy policy, String cookedWith)

  /** The names of the links Cook can make in the local directory, such as `settings.json`, which stay in Git. */
  List<String> getLinkNames()

  /**
   * `.gitignore` lines, a comment saying why first, for files the harness would read in place of the shared setup;
   * empty when there are none.
   */
  List<String> getIgnoredFiles()

  /**
   * Makes the harness read the shared directories it supports, keyed by name, such as `skills` or `hooks`, each at a
   * path relative to its directory.
   */
  void readDirectories(Map<String, String> paths)

  /**
   * Makes the harness run `dispatcher`, a path relative to its directory, with its own name as the only argument at the
   * start of every session, and take what it prints as session context.
   */
  void startSession(String dispatcher)

  /**
   * Makes the harness create and remove its worktrees by running `hook`, a path relative to its directory, with
   * `create` or `remove`, and returns true; returns false and sets nothing when the harness has no worktree hook event.
   */
  boolean createWorktrees(String hook)
}

/**
 * Version 2 of the shared policy: command and tool entries with a decision each, and roles that add denies to the
 * command entries for one kind of run.
 */
class Policy {
  /** Where the policy lives, relative to the shared setup's directory. */
  static final String PATH = '.agents/permission/policy.yaml'

  private static final List<String> DECISIONS = ['allow', 'ask', 'deny']

  /** Command entries every session follows, under `allow`, `ask` and `deny`. */
  Map<String, List<Command>> commands = [allow: [], ask: [], deny: []]

  /** Tool entries, each a harness entry, under `allow`, `ask` and `deny`. */
  Map<String, List<Command>> tools = [allow: [], ask: [], deny: []]

  /** The command entries each role denies on top of `commands`, by role name. */
  Map<String, List<Command>> roles = [:]

  /** `refuse`, the default, keeps every run within the policy; `allow` lets a run start in a harness's bypass mode. */
  String bypass = 'refuse'

  /**
   * Parses YAML policy text; rejects a version other than 2, unknown fields, decisions other than `allow`, `ask` and
   * `deny`, invalid entries, tool entries that are not harness entries, roles that do more than deny, and a `bypass`
   * other than `refuse` or `allow`.
   */
  static Policy parse(String text) {
    def data = new YamlSlurper().parseText(text)

    if (!(data instanceof Map)) {
      fail('the policy must be a YAML map')
    }

    if (data.version != 2) {
      fail('version must be 2')
    }

    def unknown = data.keySet() - ['version', 'bypass', 'commands', 'tools', 'roles']

    if (unknown) {
      fail("unknown field ${unknown}")
    }

    if (!(data.bypass in [null, 'refuse', 'allow'])) {
      fail('bypass must be refuse or allow')
    }

    new Policy(commands: decisions(data, 'commands', false), tools: decisions(data, 'tools', true),
    roles: roles(data.roles), bypass: data.bypass ?: 'refuse')
  }

  private static Map<String, List<Command>> decisions(Map data, String field, boolean toolsOnly) {
    def value = data[field] ?: [:]

    if (!(value instanceof Map)) {
      fail("${field} must map allow, ask and deny to lists of entries")
    }

    def unknown = value.keySet() - DECISIONS

    if (unknown) {
      fail("${field} has unknown decision ${unknown}; use allow, ask or deny")
    }

    DECISIONS.collectEntries { decision -> [decision, entries(value[decision], "${field}.${decision}", toolsOnly)] }
  }

  private static List<Command> entries(Object value, String field, boolean toolsOnly) {
    if (value == null) {
      return []
    }

    if (!(value instanceof List)) {
      fail("${field} must be a list of entries")
    }

    value.collect { entry ->
      def command = Command.of(entry)

      if (!command) {
        fail("${field}: ${entry} is not an entry; write the literal words a command starts with, a nested list for " +
            'alternatives of one word, or {claude: "<pattern>"}')
      }

      if (toolsOnly && !command.claude) {
        fail("${field}: ${entry} is not a tool entry; write {claude: \"<pattern>\"}")
      }

      command
    }
  }

  private static Map<String, List<Command>> roles(Object value) {
    if (value == null) {
      return [:]
    }

    if (!(value instanceof Map)) {
      fail('roles must map each role name to its deny entries')
    }

    value.collectEntries { name, role ->
      if (!(name ==~ /[a-z][a-z0-9-]*/)) {
        fail("roles: ${name} is not a role name; use lower-case letters, digits and hyphens, starting with a letter")
      }

      if (!(role instanceof Map) || role.keySet() != ['deny'] as Set) {
        fail("roles.${name}: a role only adds denies; list them under deny, and keep allow and ask under commands")
      }

      [name, entries(role.deny, "roles.${name}.deny", false)]
    }
  }

  private static void fail(String message) {
    throw new IllegalArgumentException("${PATH}: ${message}")
  }
}

/**
 * One policy entry, translated by each harness into its native permission rules: either the literal words a command
 * starts with, where an item that is itself a list holds alternatives for that word, or a pattern only Claude can
 * express.
 */
class Command {
  /** The literal words, with a list of alternatives in place of a word; null for a harness entry. */
  List prefix

  /** A Claude permission pattern, where `*` matches anything; null for a prefix. */
  String claude

  /** The entry `value` describes, or null when it is neither a prefix of literal words nor `{claude: "<pattern>"}`. */
  static Command of(Object value) {
    if (value instanceof Map) {
      boolean harness = value.keySet() == ['claude'] as Set && value.claude instanceof String && value.claude
      return harness ? new Command(claude: value.claude) : null
    }

    boolean prefix = value instanceof List && value && value.every { item ->
      item instanceof List ? item && item.every { word(it) } : word(item)
    }
    prefix ? new Command(prefix: value) : null
  }

  // Letters, digits, underscore, dot, slash, colon, equals, at sign, plus and hyphen: no shell syntax or wildcards.
  private static boolean word(Object value) {
    value instanceof String && value ==~ /[a-zA-Z0-9_.\/:=@+-]+/
  }
}
