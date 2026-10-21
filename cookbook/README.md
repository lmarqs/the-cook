# Cookbook

This directory shows the shape of recipes and ingredients. The engine runs the `agents` artifact; the other
directories are a design sketch in Groovy that has not been compiled.

## Units

A cookbook directory is one artifact holding its models, recipes, resources and one `README.md`. A model describes
how a program reads its files, or supplies content another model consumes; a recipe chooses a setup through models.
Models may depend on other models for their types and on ordinary libraries. Recipes depend on models.

A cookbook releases as jars in a Maven-layout repository. The `io.github.lmarqs.cookbook` Gradle plugin packages one
jar per directory into a Maven-layout repository, with a POM for each jar and a bill of materials (BOM),
`<group>:cookbook:<version>`, listing the artifacts at that release, so no directory is named `cookbook`. Each jar
holds the directory's sources, resources and README under its package path, such as `io/github/lmarqs/cookbook/ralph/`:
everything a reader needs without the source checkout. Each release of this repository publishes the `agents`
artifact to Maven Central as `io.github.lmarqs.cookbook:agents:<version>`, so it cooks with no `cook.toml`; the other
directories are not published.

A recipe is addressed as `group:artifact:version#recipe`, such as `io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui`. Without
`#recipe` the address names the artifact, which `./cook help` describes and nothing cooks.
Only exact release versions are accepted; branches, commits, snapshots and version ranges are refused. `cook.lock`
records the address and a sha256 of the artifact's sources.

A loose recipe compiles against the models in its own directory, the upper-case `.groovy` files beside it, as a
recipe in an artifact compiles against that artifact's models.

A committed `cook.toml` at the destination lists the repositories to resolve from, in order, as `repositories`: each
an `https://` URL or a path, one starting with `~/` relative to the user's home and any other to the destination. A
URL or path names a Maven-layout repository, or a path names a cookbook's source directory, the one holding
`cookbook.toml`. Without a `cook.toml`, Cook resolves from Maven Central.
[ADR 0029](../docs/adrs/0029-cookbook-distribution.md) records the reasons.

```toml
repositories = [
  "https://maven.example.com/releases/",
  "https://repo.maven.apache.org/maven2/",
]
```

The engine does not find recipes. People find them in a cookbook's repository, on an artifact host or through a
teammate's link. Given the coordinates:

```sh
./cook help io.github.lmarqs.cookbook:agents:0.0.0           # the artifact's front page
./cook help io.github.lmarqs.cookbook:agents:0.0.0#connect   # the recipe's help, params and ingredients
./cook io.github.lmarqs.cookbook:agents:0.0.0#seed io.github.lmarqs.cookbook:agents:0.0.0#connect --harness=claude
```

`help` on `group:artifact:version` prints the artifact's front page; on `group:artifact:version#recipe` it prints
that recipe's help. Recipes named in one invocation compose left to right by file content. Each recipe is evaluated
in its own classloader. A recipe never composes another recipe.

## Names

A model's package is its cookbook's group plus its directory name: `cookbook/docker/Compose.groovy` declares
`io.github.lmarqs.cookbook.docker.Compose` in the artifact `io.github.lmarqs.cookbook:docker`. A recipe imports every type it injects, so
its imports are its visible dependency list and `@ingredient(Compose)` resolves through them.

Recipe names are noun phrases naming the state the destination gets, without repeating the directory's topic:
`node/project.groovy`, `docker/base-image.groovy` and `agents/connect.groovy`. A recipe that configures one of
several programs in its directory names the program first: `ralph/ralph-tui.groovy`.

The engine imports its own annotations, `groovy.json`, `groovy.yaml`, `groovy.toml` and `java.nio.file` into every
unit, as the Groovy shell imports `java.util`. A unit imports nothing from the engine and nothing for JSON, YAML,
TOML or paths. The annotations come first, so `File` and `Files` name them; write `java.io.File` and
`java.nio.file.Files` in full.

## The author's API

An author's code reaches the engine only through annotations. It imports nothing from the engine.

### In an ingredient

| Annotation | On | Declares |
| --- | --- | --- |
| `@Ingredient` | class | A model or content. The engine adds `directory`, the instance's base inside the destination with `.` at its root; `resource(name)` and `resources(dir)`, which read one file or every file packaged in the jar, or in the directory of a model beside a loose recipe; and `current(path)`, which reads a destination file. |
| `@Section(path, id, comment)` | member returning `List<String>` | The marked section `id` of a shared text file. `comment` names the comment syntax when the extension does not. |
| `@Key(path, segments)` | member | One key path in a JSON, TOML or YAML document, owned whole. A segment can select a list entry. |
| `@Keys(path, under)` | member returning `Map` | One owned key per entry, under the key path `under`. A `List` map key is a deeper path. |
| `@Lines(path)` | member returning `Map<String, String>` | One line per entry, owned on its own. The key is a regular expression: the value replaces the one line it matches, or is appended when none does. More than one match is an error. |
| `@File(path, mode)` | member returning `String` | The whole file. |
| `@Files(path, mode)` | member returning `Map<String, String>` | One whole file per entry, under `path`. |
| `@Links(path, instead)` | member returning `Map<String, String>` | One symbolic link per entry, under `path`, pointing at the value. A link already at the path is replaced; a file or directory there is refused, and `instead` names where its contents belong, by default the link's target. |
| `@Seed(path, existing)` | member returning `String` | Written only when the file is absent, and never owned. With `existing = 'fail'`, cooking the recipe the first time fails when the file exists, unless Cook runs with `--force`, which replaces the file, or `--skip`, which keeps it. |
| `@Delete(path)` | member returning `boolean` | Deletes a file the recipe does not own, when true. |

Rules:

- A member that returns `null` declares nothing. A pristine instance declares nothing: only what the recipe asked for
  is written. The engine evaluates a pristine instance of every ingredient it loads and rejects the ingredient, naming
  the member, when anything is declared.
- A declaration member may throw to reject the request, after the guard that keeps a pristine instance declaring
  nothing.
- Sections, keys and lines are written only into regular files. When the path is a symbolic link, cooking fails,
  naming the link and its target, and writes nothing.
- `path` joins to the instance's `directory`. `${property}` in a path reads the instance, for files the recipe names.
- Declare the entry the recipe means, never the list or file around it: a step, a requirement, a line. Ownership then
  covers only that entry, and cooking again finds it in place.
- A `Map` segment in a key path selects one entry of a list: `[id: 'cache']` the map entry whose `id` is `cache`,
  `[matches: regex]` the string entry the expression matches. An absent entry is inserted at `place`: `start`, `end`
  by default, `before:<value>` or `after:<value>`, compared the same way. An absent list is created. A path that
  continues past a selector requires the entry to exist. Lock and docs write the entry as `steps[id=cache]`.
- The engine owns every format: markers in the right comment syntax, key merge that preserves unrelated keys, order
  and comments, link and mode handling, the destination boundary.
- An ingredient reads destination content only through `current(path)`, which returns the file as the recipes before
  this one in the invocation left it, or `null` when absent. It never reads the disk, the lock or the engine's state.
- Two declarations of one target in one recipe are an error. Across recipes, the last one wins and owns the target.
- A model exposes facts for other models through plain methods, such as the files it leaves behind. The recipe wires
  them. Ingredients never call each other's outputs.
- A model that knows how to run something returns a command, `[run: [step...], dir: String, env: Map]`, where a step
  is an argument list or shell text and `dir` is relative to the destination. The consumer, such as `mise`, renders
  and quotes it; models never quote.
- A model that several programs read through their own files, such as `agents`, owns that shared layout and drives
  each program through one interface, such as `Harness`. Each program's model only translates.

### In a recipe

| Annotation | On | Declares |
| --- | --- | --- |
| `@help('''...''')` | first import | The recipe's description, in Markdown. Required. |
| `@confirm('''...''')` | first import | A consequence the diff cannot show, displayed above the diff when asking for approval. Optional. |
| `@yield('''...''')` | first import | What to do next, printed after the writes succeed. Optional. |
| `@ingredient(Type, at)` | field | An injected instance, at the destination root or at the directory `at`. |
| `@resource('path')` | field | The text of a file: beside a loose recipe, inside the repository, or in the recipe's artifact. The field is a `String`. |
| `@param(value = names, doc = '...', default = ..., choices = ..., many = ...)` | field | An input bound from `--name=value` or `--name value`. Required without a default; `default = []` makes it optional with no value. `many` accepts repeats. The field is a `String`, a yes-or-no `boolean`, an `int` or `long`, or with `many` a `String[]` or `List<String>`; an input file is passed as its path. `doc` is required: it is the param's line in `help`. |

A file that another recipe in the same invocation may write, such as a program's own configuration or Ralph's PRD,
is read by the ingredient that models it, through `current(path)`; the recipe passes its path as a `String`.

The body is plain Groovy: it calls ingredient methods and ends. `return` ends it with nothing declared. Everything
the injected instances declare is owned by the recipe. Call order in the body is meaningful. A method that must
follow others says so in its doc comment, as `Agents.connect` does: call it after adding skills.

## Documentation

A unit is read by someone who has its artifact and not its source, so `./cook` is how it is read: a recipe inside a
jar cannot be opened with a pager. Everything the reader needs ships in the artifact: its README, the doc comments on
its models and the annotations on its recipes. `./cook help <address>...` prints help for each address in order and
cooks nothing. An artifact address prints its front page; a recipe address or path prints that recipe's help.

The engine reads a recipe's help from its parsed source, without running the recipe or resolving its imports, so help
works for a recipe whose models are not available.

| Unit | `help` prints, in order | The author writes |
| --- | --- | --- |
| Recipe | `@help`; the params from their annotations, each with its name, type, default or `required`, choices and `doc`; the ingredients it injects; `@confirm` as "Confirm"; `@yield` as "After cooking" | `@help`, and a `doc` on every param |
| Model | The class doc comment; the API from the doc comments of its public members; the writes from its annotations and the doc comments of the annotated members | The class doc comment, and a doc comment on every public member and every annotated member |

A fact that lives in code is rendered from the code and written nowhere else. State a model keeps for itself is
`private`, so the rendered API is the one the author means. Every public method or property and every annotated
member has a doc comment. The engine refuses to load a recipe without `@help` and a unit with an undocumented param,
public member or annotated member, naming it.

A directory README is the artifact's front page: one paragraph on the topic, holding nothing that code holds.

### Recipe texts

A recipe file opens with its texts, annotations on its first import, in this order: `@help`, `@confirm`, `@yield`.
Each holds Markdown in a `'''` string. An annotation value is a constant, so the texts do not change with the
arguments; what the recipe actually wrote is in the diff and in the engine's report.

`@help` describes the recipe by what it provides, consumes and connects to, so a reader composes recipes without its
source. It holds these sections in order, each after a `## <Section>` heading except the Lead. A section that does not
apply is omitted: a recipe that installs no script has no Run-time effects.

| Section | Content |
| --- | --- |
| Lead | One or two sentences: what the destination gets |
| API | The invocations, one per situation, each under a comment naming it; the params are not repeated. Then the stable paths, scripts, commands and naming rules the recipe publishes for people and other recipes to rely on |
| Consumes | The tools that run at run time, with the versions the recipe is written for; what it reads in the draft and what each read triggers; the paths of other artifacts it names; what the person does by hand first |
| Produces | A table of `Target`, `Part`, `When` and `Kind`. `When` is the argument value or draft condition that triggers the row, or `Always`. `Kind` is `owned`, rewritten on every cook and removed by discard; `seed`, written when absent and never owned; or `link`, an owned symbolic link. Then how the written setup behaves, closing with `Never touches:` and the files in its topic it leaves to others |
| Connections | Each contact point with another recipe, tool or person, and who makes it: Cook, automatically; the person, by a parameter; the team, in seeds it owns; the session, at launch. Then the order and the arguments that must agree |
| Run-time effects | What the scripts the recipe installs do when run |
| Re-cooking | The arguments to repeat, as the opening line of its owned sections names them; what cooking a newer version changes; how its conditional parts behave under `--check` |
| Traps | Known faults of the versions written for, each with its workaround |

What to do after cooking is not a section of `@help`: it belongs in `@yield`, which `help` prints as "After cooking".

`@confirm` states a consequence of approving that the diff cannot show, such as access granted outside the
repository. It is displayed above the diff, under the recipe's address, and the one approval question covers both.
`--batch-approve` displays it and proceeds. A recipe whose effects are all in the diff has no `@confirm`.

`@yield` states what to do next, such as a command to run. It is printed under the recipe's address after the writes
succeed, and not when the batch is declined or fails.

### Model class doc comment

A model's class doc comment holds these sections in order.

| Section | Content |
| --- | --- |
| Lead | One or two sentences: the program or content it models |
| Writes for | The programs, versions and formats the model writes for |
| Instances | What one instance covers, and when a recipe places one `at` a directory |
| Use | A recipe excerpt: the import, the injection and the typical calls |
| With other models | The facts it exposes and which models consume them |

### Doc comments

| On | Says |
| --- | --- |
| The class | The lead and model sections above |
| A public method or property | What it means for the destination, what its arguments accept and what is rejected |
| An annotated member | The part written and when, as one row of the rendered writes: `` `engines.node`, a caret range on the major, when a version is set. `` |

Rendered help for an artifact and a recipe, as `./cook help` prints them:

````text
$ ./cook help io.github.lmarqs.cookbook:agents:0.0.0
io.github.lmarqs.cookbook:agents:0.0.0

Shared setup for the coding agents in one repository, so project guidance, permissions and skills stay the same
across every agent program the team uses. The artifact also models the programs it drives, Claude Code and Codex,
and ships the `agents-layout` skill, which explains the layout to the people and agents working in it.
Its `references/design.md` gives the reason behind each decision of the layout, such as committed links, deny-only
roles and no Codex roles. To set the layout up in a repository, cook `#seed`, whose help says how to go on.

Recipes
  io.github.lmarqs.cookbook:agents:0.0.0#connect
  io.github.lmarqs.cookbook:agents:0.0.0#seed

Models
  io.github.lmarqs.cookbook.agents.Agents
  io.github.lmarqs.cookbook.agents.Claude
  io.github.lmarqs.cookbook.agents.Codex
````

The artifact's front page introduces the topic. The explicit recipe address renders the recipe:

````text
$ ./cook help io.github.lmarqs.cookbook:agents:0.0.0#connect
io.github.lmarqs.cookbook:agents:0.0.0#connect

Connects each selected harness to the shared agent layout: renders the shared policy into its settings under
`.agents/adapters/<harness>/`, links its own directory, `.claude/` or `.codex/`, to those files and to the shared
directories, and runs every script in `.agents/hooks/session-start.d/` when a session starts. Each harness reads
`AGENTS.md` natively, and hooks and memory are reached by their paths. The links are committed, so a fresh clone needs
no cooking; everything else in the harness directories is ignored in Git.

## API

```sh
# A team that uses Claude Code only
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
# A team that uses Claude Code and Codex
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
# A new repository: the layout and its connection in one invocation
./cook io.github.lmarqs.cookbook:agents:<version>#seed io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
```

Paths other recipes, launchers and people rely on:

| Path | What it is |
| --- | --- |
| `.agents/adapters/<harness>/` | Each harness's settings; Cook owns only the keys it renders, and the team writes the rest |
| `.agents/adapters/claude/roles/<role>.json` | A role's denies, added to one Claude run with `claude --settings <file>` |
| `.agents/hooks/session-start.d/<name>.sh` | Session context: each script's standard output reaches every connected harness when a session starts, in name order, with the harness name, `claude` or `codex`, as `$1`. A tool recipe owns its own script there, such as `beads.sh`; the team writes its own beside them |
| `.claude/settings.json`, `.claude/skills`, `.claude/commands` | Where Claude Code reads the layout |
| `.codex/config.toml`, `.codex/rules`, `.codex/hooks.json` | Where Codex reads the layout |

## Consumes

- Claude Code and Codex read what it writes; written for Claude Code 2.1.304 and Codex 0.160.1, with the session-start
  hooks tested on Claude Code 2.1.306.
- `.agents/permission/policy.yaml` in the draft, the team's: its `commands` render into each harness's settings, its
  `tools` and `roles` into Claude's, and its `bypass` decides whether Claude's bypass mode is refused. When absent, the
  starter is seeded, which grants nothing.
- `.agents/hooks/session-start.d/*.sh` at run time, from tool recipes and the team: the dispatcher runs each with `sh`.
  `sh`, `awk` and, for Codex, `git` must be on the path.
- A file or directory already at a link's path, such as a committed `.codex/config.toml` or `.claude/skills/`, is
  refused, and nothing is written: move its contents where the message says, delete it, and cook again. A link there
  is replaced.
- By hand first: cook `io.github.lmarqs.cookbook:agents:<version>#seed` to create `AGENTS.md` and the policy, and
  edit the policy.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.agents/permission/policy.yaml` | Starter policy | Always, when absent | seed |
| `.agents/README.md`, `.agents/<directory>/.gitkeep` | The shared directories | Always, when absent | seed |
| `.agents/hooks/session-start` | The session-start dispatcher, mode 0755 | Always | owned |
| `.agents/adapters/claude/settings.json` | `permissions`, with `disableBypassPermissionsMode` while the policy's `bypass` is `refuse` | `--harness=claude` | owned |
| `.agents/adapters/claude/settings.json` | `hooks.SessionStart`, running `"$CLAUDE_PROJECT_DIR"/.agents/hooks/session-start claude` | `--harness=claude` | owned |
| `.claude/settings.json` | A link to `../.agents/adapters/claude/settings.json` | `--harness=claude` | link |
| `.agents/adapters/claude/roles/<role>.json` | The role's `permissions.deny`, added to a run with `claude --settings <file>` | `--harness=claude` and the policy has roles | owned |
| `.claude/skills`, `.claude/commands` | Links to the directories of the same names in `../.agents/`, where Claude finds skills and commands | `--harness=claude` | link |
| `.agents/adapters/codex/config.toml` | `sandbox_mode`, `approval_policy`, `sandbox_workspace_write.network_access` | `--harness=codex` | owned |
| `.agents/adapters/codex/hooks.json` | `hooks.SessionStart`: one entry for `startup\|resume\|clear` running the dispatcher with `codex`, found from the root that `git rev-parse --show-toplevel` prints | `--harness=codex` | owned |
| `.agents/adapters/codex/rules/cook.rules` | The section `commands`: the argument line, a comment on the runs Codex cannot refuse while `bypass` is `refuse`, then one rule per command entry | `--harness=codex` and the policy has command rules | owned |
| `.codex/config.toml`, `.codex/hooks.json`, `.codex/rules` | Links to the files of the same names in `../.agents/adapters/codex/` | `--harness=codex` | link |
| `.gitignore` | The section `harnesses`: the argument line; each selected harness's directory re-included over an earlier rule, then its contents ignored except the links above, all committed; and `CLAUDE.md`, `CLAUDE.local.md` and `**/.claude/CLAUDE.md`, which would make Claude skip `AGENTS.md` | Always | owned |

Cooking with fewer harnesses removes the outputs of the ones left out.

Never touches: `AGENTS.md`; every other key in the adapter files, such as the other events under `hooks`, `env` or
`[mcp_servers]`, which the team writes in the file and cooking keeps; the scripts in `.agents/hooks/session-start.d/`;
`.claude/settings.local.json` and anything else a harness writes in its own directory.

## Connections

| Contact | Made by |
| --- | --- |
| The layout to each harness | The person, by `--harness` |
| The policy to each harness's settings | Cook, rendering the team's policy |
| A tool's session context to every harness | Cook, when the tool's recipe writes its script into `.agents/hooks/session-start.d/`; the team, writing its own there |
| A role to a Claude run | The session, launching `claude --settings .agents/adapters/claude/roles/<role>.json`; the Launching table in `.agents/README.md` lists the commands |
| The team's other hooks to each harness | The team, under `hooks` in `.claude/settings.json` and `.agents/adapters/codex/hooks.json` |
| Shared skills and commands to Claude | Cook, by the `.claude/skills` and `.claude/commands` links |

`io.github.lmarqs.cookbook:agents:<version>#seed` creates the layout this recipe connects: cook it first, or before
this recipe in the same invocation. Every cook names every harness the team uses. Scripts in `session-start.d/` are
read when a session starts, so a recipe that writes one can be cooked before or after this one.

## Run-time effects

`.agents/hooks/session-start <harness>` runs every `.agents/hooks/session-start.d/*.sh` with `sh`, in name order, with
the harness name as `$1`, no standard input, and standard error discarded. It joins their standard output and prints
it once as `{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"<text>"}}`, which both
harnesses add to the session. It prints nothing when the directory is missing or the scripts print nothing, and
always exits 0. Name a script with a number first, such as `20-beads.sh`, to place it in the order.

## Re-cooking

Repeat the arguments on the opening line of the `harnesses` section in `.gitignore`, or of the `commands` section in
`cook.rules`, such as `# io.github.lmarqs.cookbook:agents#connect --harness=claude --harness=codex`. Cook again after
editing the policy, and after deleting `.claude/` or `.codex/` to recreate the links. A newer version rewrites the
keys, links, files and sections it owns; the team's keys, the scripts in `session-start.d/` and the seeded files stay
as they are. Under `--check`, a role file or `cook.rules` shows as a change once the policy gains roles or command
rules, until the recipe is cooked again.

## Traps

- Claude Code applies the project's allow rules only in a workspace someone has trusted, and Codex loads `.codex/rules`
  only in a trusted project: start each once, interactively, in the main checkout.
- Codex trusts hooks per absolute path of `.codex/hooks.json`, recorded in `~/.codex/config.toml`, so a worktree runs
  none of them, the session start included, without `--dangerously-bypass-hook-trust`, and Codex prints no warning
  when it skips them.
- A `CLAUDE.md`, `.claude/CLAUDE.md` or `CLAUDE.local.md` in the directory of an `AGENTS.md` or one above makes Claude
  Code skip that `AGENTS.md`. Claude Code older than 2.1.277 reads no `AGENTS.md`.
- Codex has no roles: a role's denies reach only Claude runs. `codex exec --ignore-rules`, `--approve-for-me` or an
  untrusted project skip or approve the Codex rules, and no setting refuses them.
- `hooks.SessionStart` is Cook's in both harnesses: a session-start hook the team writes there is replaced on the next
  cook. Write it as a script in `.agents/hooks/session-start.d/` instead.
- With `core.symlinks` off, as is common on Windows, Git checks the links out as text files.

The `agents-layout` skill's `references/investigate.md` lists more symptoms, with their checks and fixes.

Parameters
  --harness=<String[]>
      Harnesses to connect to the shared layout; required; choices: claude, codex; repeatable

Ingredients
  io.github.lmarqs.cookbook.agents.Agents
  io.github.lmarqs.cookbook.agents.Claude
  io.github.lmarqs.cookbook.agents.Codex

After cooking
  Review the rendered permissions against each harness's permission model. Edit `.agents/permission/policy.yaml` and
  cook again to update them; after deleting `.claude/` or `.codex/`, cook again to recreate the links.
````

## Layout

```text
<dir>/<Model>.groovy     the model or content, in the package <group>.<dir>
<dir>/<recipe>.groovy    the recipe, opening with its texts
<dir>/README.md          the artifact's front page
<dir>/...               resources beside the models and recipes
cookbook.toml           the cookbook manifest at the cookbook root
build.gradle            applies the io.github.lmarqs.cookbook plugin at the cookbook root
```

Each topic directory is one artifact. Skill content lives under `<dir>/skills/<name>/`, with `SKILL.md` and its
supporting files together. The templates for seeded files live in the `templates/` directory of the skill that
explains them, so a seeded skill can compare a repository's files with the release's templates. Each is named
`<name>.template.<ext>`, as in `agents/skills/agents-layout/templates/AGENTS.template.md`: under the file's own name,
agents working in the cookbook would read it as instructions, since Codex reads every `AGENTS.md` between the
repository root and its working directory.

`cookbook.toml` declares the cookbook's `group` and the engine API version as `api`. The release version is the
Gradle property `releaseVersion`, set by the build that publishes the release, and `0.0.0` without it. This
cookbook's manifest is:

```toml
group = "io.github.lmarqs.cookbook"
api = 1
```

## A cookbook outside this repository

A developer's own recipes live in their own cookbook, packaged by the same plugin and cooked by coordinates from Maven
local. Each release publishes the `io.github.lmarqs.cookbook` plugin to Maven Central. In a checkout of this
repository, `./gradlew -p plugin publishToMavenLocal` publishes it to Maven local as version `0.0.0`.

The cookbook's root holds `cookbook.toml`, as above, one directory per artifact, and these two build files. Plugin
resolution lists Maven local and Maven Central:

```groovy
// settings.gradle
pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
  }
}
rootProject.name = 'my-cookbook'
```

```groovy
// build.gradle
plugins {
  id 'io.github.lmarqs.cookbook' version '<version>'
}
```

`./gradlew publishToMavenLocal -PreleaseVersion=<version>` publishes each artifact's jar and the BOM to
`~/.m2/repository` at that version, or at `0.0.0` without the property. A kitchen that
cooks from there lists it in its `cook.toml`, last so that shared repositories come first:

```toml
repositories = [
  "https://repo.maven.apache.org/maven2/",
  "~/.m2/repository",
]
```

```sh
./cook <group>:<artifact>:<version>#<recipe>
```

While developing a cookbook, a kitchen lists its source directory instead, and cooks each edit with no build. The
directory serves `<group>:<artifact>:<any version>` from the files of `<artifact>/`, leaving out hidden files:

```toml
repositories = ["../../../cookbook"]
```

Coordinates take exact release versions. Publishing a changed cookbook under the same version, or editing it in a
listed source directory, is cooked as it is now: the next cook prints `Changed source <address>` above the diff and records the new sha256 in `cook.lock`.
