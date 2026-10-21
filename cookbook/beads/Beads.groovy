package io.github.lmarqs.cookbook.beads

/**
 * Models the files around a Beads tracker that bd leaves to the repository: the ignore rules for what stays out of
 * commits, the prime text bd prints as session context, and the session-start hooks that print it.
 *
 * ## Writes for
 *
 * bd 1.3.1. Git ignore syntax in `.gitignore`. Markdown in `.beads/PRIME.md`, which `bd prime` prints in place of its
 * own workflow text. A `hooks.SessionStart` key in Claude Code's `.claude/settings.json` running `bd prime`, and in
 * Codex's `.codex/hooks.json` running `bd codex-hook SessionStart` (Claude Code 2.1.306, Codex 0.160.1). A POSIX `sh`
 * script in the agents layout's `.agents/hooks/session-start.d/`, whose dispatcher runs it with the harness name as
 * `$1` and hands its plain output to that harness. bd writes its own `.beads/config.yaml`, `metadata.json`,
 * `.beads/.gitignore`, git hooks and database, and this model none of them.
 *
 * ## Instances
 *
 * One instance covers one repository's tracker, at the repository's root, where bd keeps `.beads/` and the harnesses
 * read their settings.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.beads.Beads
 *
 * @ingredient(Beads) def beads
 *
 * beads.track('clone', 'io.github.lmarqs.cookbook:beads#tracker --share=clone --harness=claude')
 * beads.prime(['claude'])
 * ```
 *
 * ## With other models
 *
 * It names the agents layout only by its paths: `.agents/hooks/` in the draft is how it finds the layout, and
 * `.agents/hooks/session-start.d/` is where it adds its script. It never reads or writes the layout's adapters.
 */
@Ingredient
class Beads {
  /**
   * Who shares the issues: `team` commits bd's configuration and syncs through a Dolt remote; `clone` keeps them local.
   */
  static final List<String> SHARES = ['team', 'clone']

  /** The harnesses whose own hook file can run `bd prime`, in the order the argument line names them. */
  static final List<String> HARNESSES = ['claude', 'codex']

  /** The script the agents layout's session-start dispatcher runs. */
  static final String SESSION_START_SCRIPT = '.agents/hooks/session-start.d/beads.sh'

  /** The files that show the agents layout's `.agents/hooks/` is in the draft: its keep file and its dispatcher. */
  static final List<String> LAYOUT_HOOKS = ['.agents/hooks/.gitkeep', '.agents/hooks/session-start']

  private static final String CONNECT = 'io.github.lmarqs.cookbook:agents#connect'

  private String share
  private String cookedWith
  private boolean layout
  private List<String> harnesses = []

  /**
   * Requests the tracker's ignore rules and prime text for `share`, `team` or `clone`, and the session-start script
   * once `.agents/hooks/` is in the draft. `cookedWith` names the recipe and its arguments, which the `beads` section's
   * first line records; it must not be blank. Rejects any other share.
   */
  void track(String share, String cookedWith) {
    if (!(share in SHARES)) {
      throw new IllegalArgumentException("Beads is shared by ${SHARES.join(' or ')}, not ${share}")
    }

    if (!cookedWith?.trim()) {
      throw new IllegalArgumentException('track needs the recipe it is cooked with')
    }

    this.share = share
    this.cookedWith = cookedWith
    layout = LAYOUT_HOOKS.any { current(it) != null }
  }

  /**
   * Requests the `hooks.SessionStart` key that runs bd's prime text in each named harness's own hook file: `claude` or
   * `codex`. Call it after `track`. Refuses any harness once the agents layout is in the draft, since the layout links
   * those files to its adapters and the session-start script already reaches every harness it connects.
   */
  void prime(Collection<String> names) {
    def unknown = names - HARNESSES

    if (unknown) {
      throw new IllegalArgumentException("Beads primes ${HARNESSES.join(' or ')}, not ${unknown.join(', ')}")
    }

    def selected = HARNESSES.findAll { it in names }

    if (selected && layout) {
      def flags = selected.collect { "--harness=${it}" }.join(' ')
      throw new IllegalArgumentException("${flags} writes the harness's own hook file, which the agents layout " +
      "connects\n  the agents layout is in the draft, and ${SESSION_START_SCRIPT} gives bd prime to every harness " +
      "it connects\n  leave out --harness, and cook ${CONNECT} ${flags}")
    }

    harnesses = selected
  }

  /**
   * The `beads` section of `.gitignore`: a comment naming the recipe, then `/.beads/*` and `!/.beads/PRIME.md` for
   * `clone`, or `/.beads/issues.jsonl` for `team`, whose other files bd's own `.beads/.gitignore` covers.
   */
  @Section(path = '.gitignore', id = 'beads')
  List<String> ignoreSection() {
    if (!cookedWith) {
      return null
    }

    def rules = share == 'clone' ? ['/.beads/*', '!/.beads/PRIME.md'] : ['/.beads/issues.jsonl']
    (["# ${cookedWith}"] + rules)*.toString()
  }

  /**
   * `.beads/PRIME.md`, seeded when absent and never owned: the workflow and rules for the share, which `bd prime`
   * prints.
   */
  @Seed(path = '.beads/PRIME.md')
  String primeText() {
    share ? resource("templates/PRIME-${share}.template.md") : null
  }

  /** `hooks.SessionStart` in `.claude/settings.json`, one command hook running `bd prime`, when `claude` is primed. */
  @Key(path = '.claude/settings.json', segments = ['hooks', 'SessionStart'])
  List<Map> claudeSessionStart() {
    'claude' in harnesses ? [[hooks: [[type: 'command', command: 'bd prime']]]] : null
  }

  /**
   * `hooks.SessionStart` in `.codex/hooks.json`, one entry matching `startup|resume|clear` that runs
   * `bd codex-hook SessionStart`, when `codex` is primed.
   */
  @Key(path = '.codex/hooks.json', segments = ['hooks', 'SessionStart'])
  List<Map> codexSessionStart() {
    'codex' in harnesses ?
        [[matcher: 'startup|resume|clear', hooks: [[type: 'command', command: 'bd codex-hook SessionStart']]]] : null
  }

  /**
   * `.agents/hooks/session-start.d/beads.sh`, mode 0755, printing `bd prime` and nothing when bd or its database is
   * missing, when the tracker is requested and `.agents/hooks/` is in the draft.
   */
  @File(path = '.agents/hooks/session-start.d/beads.sh', mode = '0755')
  String sessionStartScript() {
    cookedWith && layout ? resource('hooks/beads.sh') : null
  }
}
