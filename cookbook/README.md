# Cookbook

This directory shows the shape of recipes and ingredients. The engine runs the `agents`, `git`, `docs`,
`ralph` and `hello` artifacts.

## What runs today

The `agents`, `git`, `docs` and `ralph` artifacts set a repository up for several coding agents and people at once.
Each recipe's help, read with `./cook help <coordinates>`, says what it writes.

| Need | What runs | Read |
| --- | --- | --- |
| One set of instructions and permissions for every harness | `agents#seed` creates `AGENTS.md` and `.agents/permission/policy.yaml`. `agents#connect` renders the policy into Claude Code's `permissions` and Codex's sandbox and approval settings and `cook.rules`, with roles that narrow it for Claude Code | [agents](src/agents/README.md) |
| A fresh clone, a cloud session or CI that reads the setup without cooking | The `.claude/` and `.codex/` links into `.agents/` are committed, so a fresh checkout already holds each harness's settings, hooks and skills, and each harness reads `AGENTS.md` natively | [agents](src/agents/README.md) |
| Context when a session starts | Both harnesses run `.agents/hooks/session-start`, which adds the output of each `session-start.d/*.sh` to the session | [agents](src/agents/README.md) |
| A worktree for each agent | `git#worktrees` ignores `.worktrees/`, states the rule `.worktrees/<branch>` in `AGENTS.md` and seeds the `setup` and `teardown` scripts the team fills in. `agents#connect --worktree-setup=...` wires Claude Code's worktree hooks: a new worktree runs setup and is removed when setup fails, and removing one runs teardown and deletes its branch only once every commit on it has landed | [worktrees](src/git/worktrees.groovy), [connect](src/agents/connect.groovy) |
| Unattended loop runs | `ralph#ralph-tui` writes `.ralph-tui/bin/run`, which runs ralph-tui in `.worktrees/ralph/<name>` on the branch `ralph/<name>`, and `bin/clean`, which without `--force` refuses to delete commits that have not landed. With `--worker-check`, a task completes only once the team's check passes | [ralph](src/ralph/README.md) |
| Output kept to one clone | With `--share=clone`, `ralph#ralph-tui` keeps ralph-tui's configuration out of commits. `replicate` cooks ralph-tui's configuration again in a fresh clone from the source and arguments `cook.lock` records | [replicate](../docs/features/replicate.md) |
| A variant for one branch | Cooking a recipe with other arguments, or editing a seeded file, on a branch changes committed files and `cook.lock`, so the variant reaches every worktree of that branch and merges with it | [the lock](../docs/architecture.md#ownership-and-the-lock) |
| One place for each fact, read by people and agents | `docs#layout` writes the `docs-layout` skill, which teaches where a fact goes in `docs/`, writes the map at `docs/README.md` with the team, and writes and reviews pages by it | [docs](src/docs/README.md) |
| Drift caught in CI | `./cook check` exits 1 when the kitchen differs from what `cook.lock` records, such as a policy edited and not cooked | [check](../docs/features/cli.md#check) |

Every change in this table goes through the review, the lock and the recovery that [What runs](../README.md#what-runs) lists.

## Units

A directory under the cookbook's `src/` is one artifact holding its models, recipes, resources and one `README.md`. A model describes
how a program reads its files, or supplies content another model consumes; a recipe chooses a setup through models.
Models may depend on other models for their types and on ordinary libraries. Recipes depend on models.

A cookbook releases as jars in a Maven-layout repository, one jar with a POM per directory under `src/`. Each jar
holds the directory's sources, resources and README under its package path, such as `io/github/lmarqs/cookbook/ralph/`:
everything a reader needs without the source checkout. This cookbook's `build.gradle` writes them with plain Gradle. Each release of this repository publishes the `agents`,
`docs`, `git` and `ralph` artifacts to Maven Central as `io.github.lmarqs.cookbook:<artifact>:<version>`, so they cook
with no `cook.toml`; the other directories are not published.

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

A model's package is its cookbook's group plus its directory name: `cookbook/src/ralph/RalphTui.groovy` declares
`io.github.lmarqs.cookbook.ralph.RalphTui` in the artifact `io.github.lmarqs.cookbook:ralph`. A recipe imports every type it injects, so
its imports are its visible dependency list and `@ingredient(RalphTui)` resolves through them.

Recipe names are noun phrases naming the state the destination gets, without repeating the directory's topic:
`git/worktrees.groovy` and `agents/connect.groovy`. A recipe that configures one of
several programs in its directory names the program first: `ralph/ralph-tui.groovy`.

Params are kebab-case and name the value's role in what the recipe sets up, as the recipe sees it: `--worktree-setup`,
`--share`. A param that applies to one party of a run names that party first: `--worker-role` and `--worker-settings`
limit the workers a loop runner starts, not whoever launches it. One concept keeps one name across artifacts:
`--harness` names the coding agents a recipe connects, with the programs' own names, `claude` and `codex`, as values,
`--share` names who shares what a recipe writes, `team` when the repository commits it and `clone` when each clone
keeps its own, with `team` the default, and `--instructions`, repeatable, names the Markdown files where a recipe states
the rules it sets up for people and agents, such as `AGENTS.md` and `README.md`. A param avoids a word the configured program uses for something else, as
ralph-tui 0.12 has a `setup` command, and a word this repository uses for something else, as `hook` names harness and
Git hooks. A param that names another recipe's or the team's file takes its path from the repository root and is
declared with `path`, so the engine refuses it when absolute or holding a `..` segment
([0030](../docs/adrs/0030-recipe-connections.md)).

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
| `@Sections(path, id, comment, missing)` | member returning `Map<String, List<String>>` | The marked section `id` in one file per entry: the key is the file's path under `path`, and the value its lines. With `missing = 'fail'`, a file the draft lacks is refused rather than created holding only the section. |
| `@Key(path, segments)` | member | One key path in a JSON, TOML or YAML document, owned whole. A segment can select a list entry. |
| `@Keys(path, under)` | member returning `Map` | One owned key per entry, under the key path `under`. A `List` map key is a deeper path. |
| `@File(path, mode)` | member returning `String` | The whole file. |
| `@Files(path, mode)` | member returning `Map<String, String>` | One whole file per entry, under `path`. |
| `@Links(path, instead)` | member returning `Map<String, String>` | One symbolic link per entry, under `path`, pointing at the value. A link already at the path is replaced; a file or directory there is refused, and `instead` names where its contents belong, by default the link's target. |
| `@Seed(path, existing, mode)` | member returning `String` | Written only when the file is absent, and never owned. With `existing = 'fail'`, cooking the recipe the first time fails when the file exists, unless Cook runs with `--force`, which replaces the file, or `--skip`, which keeps it. `mode` sets the permissions of the file it writes, such as `0755`, and never of a file it keeps. |
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
| `@recipe(arguments)` | field | The recipe's own name, as its section markers and `cook.lock` carry it: its coordinates without the version, such as `io.github.lmarqs.cookbook:git#worktrees`, or its place in the kitchen, such as `recipes/setup`. With `arguments = true`, the name followed by each param the recipe was given, in declaration order, as bound, such as `io.github.lmarqs.cookbook:git#worktrees --instructions=AGENTS.md`: the argument line its owned sections open with. The field is a `String`. |
| `@param(value = names, doc = '...', default = ..., choices = ..., many = ..., sorted = ..., path = ...)` | field | An input bound from `--name=value` or `--name value`. Required without a default; `default = []` makes it optional with no value. `many` accepts repeats and binds them without duplicates, in the order given; `sorted` sorts them, for a param whose order means nothing. `path` takes each value as a path from the repository root: it is refused when blank, absolute or holding a `..` segment, and reaches the recipe without empty or `.` segments. The field is a `String`, a yes-or-no `boolean`, an `int` or `long`, or with `many` a `String[]` or `List<String>`; an input file is passed as its path. `doc` is required: it is the param's line in `help`. |

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
| Connections | Each contact point with another recipe, tool or person, and who makes it: Cook, automatically, only between the recipe's own files; the person, by a parameter, for every contact with another recipe's or the team's files ([0030](../docs/adrs/0030-recipe-connections.md)); the team, in seeds it owns; the session, at launch. Then the order and the arguments that must agree |
| Run-time effects | What the scripts the recipe installs do when run |
| Re-cooking | The arguments to repeat, as the opening line of its owned sections names them; what cooking a newer version changes; how its conditional parts behave under `./cook check` |
| Traps | Known faults of the versions written for, each with its workaround |

What to do after cooking is not a section of `@help`: it belongs in `@yield`, which `help` prints as "After cooking".

`@confirm` states a consequence of approving that the diff cannot show, such as access granted outside the
repository. It is displayed above the diff, under the recipe's address, and the one approval question covers both.
`--batch-approve` displays it and proceeds. A recipe whose effects are all in the diff has no `@confirm`.

`@yield` states what to do next, such as a command to run. It is printed under the recipe's address after the writes
succeed, and not when the batch is declined or fails. It installs each program the written files need at run time
first, at the version the recipe is written for, with a mise command that also names the runtimes the program needs,
since mise installs from npm, GitHub releases and the aqua registry alike: `mise use -g aqua:steveyegge/beads@1.3.1`.
Without `-g`, mise pins the programs in the project's `mise.toml` instead, which is the team's choice.

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
$ ./cook help io.github.lmarqs.cookbook:ralph:0.0.0
io.github.lmarqs.cookbook:ralph:0.0.0

ralph-tui works through a tracker's tasks, a Beads epic or a JSON PRD, by running coding agents in a loop. This artifact
sets it up so each run works unattended in a worktree of its own, `.worktrees/ralph/<name>` by default, on the branch
`ralph/<name>`, and connects to worker limits and worktree setup only where the person names them. To set it up, cook
`#ralph-tui`, whose help says what it writes and how to launch a run.

## Gating completion

…

Recipes
  io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui

Models
  io.github.lmarqs.cookbook.ralph.RalphTui
````

The artifact's front page introduces the topic. The explicit recipe address renders the recipe, shortened here to its
sections; [the ralph front page](src/ralph/README.md) and [`ralph-tui.groovy`](src/ralph/ralph-tui.groovy) hold the
full texts:

````text
$ ./cook help io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui
io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui

Sets ralph-tui up to work through a tracker's tasks with unattended coding agents, each run in a worktree of its own:
a configuration with one agent entry per harness, the prompts it renders for each task, and a launcher that creates,
sets up and cleans the run's worktree.

## API

…

## Consumes

…

## Produces

…

## Connections

…

## Run-time effects

…

## Re-cooking

…

## Traps

…

Parameters
  …

Ingredients
  …

After cooking
  …
````

## Layout

```text
src/<dir>/<Model>.groovy     the model or content, in the package <group>.<dir>
src/<dir>/<recipe>.groovy    the recipe, opening with its texts
src/<dir>/README.md          the artifact's front page
src/<dir>/...               resources beside the models and recipes
test/<dir>/<Subject>Test.groovy  a test of the artifact's recipes, with its fixtures beside it
cookbook.toml               the cookbook manifest at the cookbook root
build.gradle                packages each src/<dir>/ as a jar and runs the tests under test/
```

Each directory under `src/` is one artifact; nothing under `test/` is part of one. Skill content lives under `<dir>/skills/<name>/`, with `SKILL.md` and its
supporting files together. The templates for seeded files live in the `templates/` directory of the skill that
explains them, so a seeded skill can compare a repository's files with the release's templates. Each is named
`<name>.template.<ext>`, as in `agents/skills/agents-layout/templates/AGENTS.template.md`: under the file's own name,
agents working in the cookbook would read it as instructions, since Codex reads every `AGENTS.md` between the
repository root and its working directory.

`cookbook.toml` declares the cookbook's `group`. The release version is the Gradle property `releaseVersion`, set by
the build that publishes the release, and `0.0.0` without it. This cookbook's manifest is:

```toml
group = "io.github.lmarqs.cookbook"
```

## A cookbook outside this repository

A developer's own recipes live in their own cookbook, packaged by any build that writes, for each artifact, a jar of
its files under its package path, `<group path>/<artifact>/`, with a POM, and cooked by coordinates from Maven local.
The cookbook's root holds `cookbook.toml`, as above, one directory per artifact under `src/`, and, with Gradle, these
two build files:

```groovy
// settings.gradle
rootProject.name = 'my-cookbook'
```

```groovy
// build.gradle
buildscript {
  repositories { mavenCentral() }
  dependencies { classpath 'org.tomlj:tomlj:1.1.1' }
}

plugins {
  id 'base'
  id 'maven-publish'
}

group = org.tomlj.Toml.parse(file('cookbook.toml').toPath()).getString('group')
version = findProperty('releaseVersion') ?: '0.0.0'

file('src').listFiles().findAll { it.isDirectory() && !it.name.startsWith('.') }.each { artifact ->
  def jar = tasks.register("${artifact.name}Jar", Jar) {
    archiveBaseName = artifact.name
    preserveFileTimestamps = false
    reproducibleFileOrder = true
    from(artifact) { into("${project.group.replace('.', '/')}/${artifact.name}") }
  }
  publishing.publications.create(artifact.name, MavenPublication) {
    artifactId = artifact.name
    it.artifact(jar)
  }
}
```

`./gradlew publishToMavenLocal -PreleaseVersion=<version>` publishes each artifact's jar and POM to
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
directory serves `<group>:<artifact>:<any version>` from the files of `src/<artifact>/`, leaving out hidden files:

```toml
repositories = ["../../../cookbook"]
```

Coordinates take exact release versions. Publishing a changed cookbook under the same version, or editing it in a
listed source directory, is cooked as it is now: the next cook prints `Changed source <address>` above the diff and records the new sha256 in `cook.lock`.
