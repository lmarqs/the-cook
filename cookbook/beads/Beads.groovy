package io.github.lmarqs.cookbook.beads

/**
 * Models the files around a Beads tracker that bd leaves to the repository: the ignore rules for what stays out of
 * commits, the prime text bd prints as session context, and a session-start script that prints it.
 *
 * ## Writes for
 *
 * bd 1.3.1. Git ignore syntax in `.gitignore`. Markdown in `.beads/PRIME.md`, which `bd prime` prints in place of its
 * own workflow text. A POSIX `sh` script at the path the recipe names, for a session-start runner that runs it and adds
 * its standard output to the session. bd writes its own `.beads/config.yaml`, `metadata.json`, `.beads/.gitignore`,
 * git hooks and database, and this model none of them.
 *
 * ## Instances
 *
 * One instance covers one repository's tracker, at the repository's root, where bd keeps `.beads/`.
 *
 * ## Use
 *
 * ```groovy
 * import io.github.lmarqs.cookbook.beads.Beads
 *
 * @ingredient(Beads) def beads
 *
 * beads.sessionStart('.agents/hooks/session-start.d/beads.sh')
 * beads.track('clone', 'io.github.lmarqs.cookbook:beads#tracker --share=clone ' +
 *     '--session-start=.agents/hooks/session-start.d/beads.sh')
 * ```
 *
 * ## With other models
 *
 * None: the session-start script goes where the recipe names it, and the model reads no other model's files.
 */
@Ingredient
class Beads {
  /**
   * Who shares the issues: `team` commits bd's configuration and syncs through a Dolt remote; `clone` keeps them local.
   */
  static final List<String> SHARES = ['team', 'clone']

  private static final String EXAMPLE = '--session-start=.agents/hooks/session-start.d/beads.sh'

  private String share
  private String cookedWith
  private String sessionStart

  /**
   * Has the tracker write its session-start script at `path`, by its path from the repository root. Without it, the
   * tracker writes nothing for session start. Refused when blank, absolute or holding a `..` segment. Call before
   * `track`.
   */
  void sessionStart(String path) {
    if (!path?.trim()) {
      throw new IllegalArgumentException('--session-start needs a path\n' +
      '  beads writes its session-start script at the path it names\n' +
      "  name the file by its path from the repository root, as ${EXAMPLE}")
    }

    def segments = path.split('/').findAll { it && it != '.' }

    if (path.startsWith('/') || '..' in segments) {
      throw new IllegalArgumentException("--session-start=${path} is outside the repository\n" +
      '  beads writes the script into the repository, so every clone has it\n' +
      "  name the file by its path from the repository root, as ${EXAMPLE}")
    }

    sessionStart = path
  }

  /**
   * Requests the tracker's ignore rules and prime text for `share`, `team` or `clone`, and the session-start script
   * when `sessionStart` named a path. `cookedWith` names the recipe and its arguments, which the `beads` section's
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

  /**
   * The session-start script at the path `sessionStart` named, mode 0755, printing `bd prime` and nothing when bd or
   * its database is missing, when the tracker is requested with that path.
   */
  @File(path = '${sessionStart}', mode = '0755')
  String sessionStartScript() {
    cookedWith && sessionStart ? resource('hooks/beads.sh') : null
  }
}
