# Cookbook

This directory shows the shape of recipes and ingredients. The engine runs the `agents`, `git`, `beads` and
`ralph` artifacts; the other directories are a design sketch in Groovy that has not been compiled.

## Units

A cookbook directory is one artifact holding its models, recipes, resources and one `README.md`. A model describes
how a program reads its files, or supplies content another model consumes; a recipe chooses a setup through models.
Models may depend on other models for their types and on ordinary libraries. Recipes depend on models.

A cookbook releases as jars in a Maven-layout repository. The `io.github.lmarqs.cookbook` Gradle plugin packages one
jar per directory into a Maven-layout repository, with a POM for each jar and a bill of materials (BOM),
`<group>:cookbook:<version>`, listing the artifacts at that release, so no directory is named `cookbook`. Each jar
holds the directory's sources, resources and README under its package path, such as `io/github/lmarqs/cookbook/ralph/`:
everything a reader needs without the source checkout. Each release of this repository publishes the `agents`,
`git`, `beads` and `ralph` artifacts to Maven Central as `io.github.lmarqs.cookbook:<artifact>:<version>`, so they cook
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

A model's package is its cookbook's group plus its directory name: `cookbook/docker/Compose.groovy` declares
`io.github.lmarqs.cookbook.docker.Compose` in the artifact `io.github.lmarqs.cookbook:docker`. A recipe imports every type it injects, so
its imports are its visible dependency list and `@ingredient(Compose)` resolves through them.

Recipe names are noun phrases naming the state the destination gets, without repeating the directory's topic:
`node/project.groovy`, `docker/base-image.groovy` and `agents/connect.groovy`. A recipe that configures one of
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
| `@Lines(path)` | member returning `Map<String, String>` | One line per entry, owned on its own. The key is a regular expression: the value replaces the one line it matches, or is appended when none does. More than one match is an error. |
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
| Re-cooking | The arguments to repeat, as the opening line of its owned sections names them; what cooking a newer version changes; how its conditional parts behave under `--check` |
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

ralph-tui 0.12 completes and commits a task when the agent's output holds `<promise>COMPLETE</promise>` anywhere, in any
case and with any whitespace inside the tags, at any exit code, even in a sentence saying it is not done; it never runs
the project's checks itself. `#ralph-tui` always writes `.ralph-tui/bin/worker`, a wrapper that follows the pseudo code
below. Cooking it with `--worker-check=<path>` names the committed check the worker runs and seeds each agent entry to
run the worker; without that flag, the worker refuses every task. The check gets the commit the agent started from as
its argument, and exits 0 when the task's work passes.

A team that wants another gate points an agent entry's `command` at a wrapper of its own, such as
`scripts/worker.py`, and leaves `.ralph-tui/bin/` as written: every cook rewrites it. What ralph-tui does to that
command:

- It runs `<command> --version` first, to detect the agent.
- The arguments are the entry's `defaultFlags`, then the plugin's own; Codex's end in `-`, with the prompt on stdin, as
  Claude's prompt is.
- Output to stderr with `rate limit`, `429`, `too many requests`, `quota exceeded` or `overloaded` marks the iteration
  rate-limited, at any exit code; with exit 1, 2 or 429, so do `throttl`, `capacity` and `backoff`.
- A non-zero exit fails the iteration; `[errorHandling] strategy = "retry"`, `maxRetries` and
  `continueOnNonZeroExit = false` retry it, then skip the task.
- The environment passes through, except variables matching `*_API_KEY`, `*_SECRET_KEY` and `*_SECRET`.
- `command` cannot contain `;&|`$()`; put arguments in `defaultFlags`. A relative path is found in each worker's
  worktree.
- ralph-tui stops the agent by signalling the command's own process, which then stops what it started.
- Parallel workers get only `config.toml` copied into their worktree, and no setup runs there.

A wrapper, in the project's own language:

```
if the arguments are just --version: run the agent with --version and exit with its code
if this is the main checkout: print one line to stderr; exit 1
base = git rev-parse HEAD                    # ralph-tui commits each task, so each starts from a new base
on a stop signal: stop the agent's or the checks' processes and their children, then exit
run the agent with the arguments, stdin passed on, stdout held in an ignored file
if the agent exits non-zero: exit with its code
if the held stdout holds <promise>COMPLETE</promise> in any case, with any whitespace inside the tags:
  run the project's checks against base, output to an ignored log file, never stderr
  if the checks fail: print one line to stderr, free of the words above; exit 1
print the held stdout; exit 0
```

Wired in `.ralph-tui/config.toml`, with the wrapper committed so every run worktree has it:

```toml
[[agents]]
name = "claude"
plugin = "claude"
command = "scripts/ralph-worker"

[errorHandling]
strategy = "retry"
maxRetries = 2
continueOnNonZeroExit = false
```

Recipes
  io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui

Models
  io.github.lmarqs.cookbook.ralph.RalphTui
````

The artifact's front page introduces the topic. The explicit recipe address renders the recipe:

````text
$ ./cook help io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui
io.github.lmarqs.cookbook:ralph:0.0.0#ralph-tui

Sets ralph-tui up to work through a tracker's tasks with unattended coding agents, each run in a worktree of its own:
a configuration with one agent entry per harness, the prompts it renders for each task, and a launcher that creates,
sets up and cleans the run's worktree.

## API

```sh
# Claude Code workers, limited by the project's denies, and its allows where the checkout is trusted
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude
# Claude Code and Codex workers, each run picking one with --agent
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --harness=codex
# Claude workers bound to the agents layout's role worker
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-role=claude:worker
# Claude and Codex workers with a settings file each, which the team writes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --harness=codex --worker-settings=claude:ralph-claude.json --worker-settings=codex:ralph-codex.toml
# Claude workers in run worktrees under worktrees/ralph/ instead of .worktrees/ralph/
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worktree-root=worktrees
# Claude workers in run worktrees the launcher sets up and tears down with the team's scripts
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worktree-setup=.worktrees/setup --worktree-teardown=.worktrees/teardown
# Claude workers whose tasks complete only once the team's check passes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-check=scripts/check
# Beads epics and JSON PRDs: seeds both prompts, and the launcher offers --epic and --prd
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --tracker=beads --tracker=json
# Beads epics whose next bead bv picks, ranking the epic's dependency graph
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --tracker=beads-bv
# Keeping all of .ralph-tui/ out of commits: each clone replicates its own, and bin/run copies it into each run
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --share=clone
# In a new clone, replaying the source and arguments from the committed cook.lock
./cook replicate io.github.lmarqs.cookbook:ralph#ralph-tui
# Also giving agents and people the launcher's commands, in the files they read
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --instructions=AGENTS.md
```

`--worker-role` and `--worker-settings` each take `<harness>:<x>`, once per harness, and the harness is one `--harness`
gives: `claude:<path>` makes the Claude entry pass `--settings <path>`, and `codex:<path>` makes the Codex entry pass
one `-c <key>=<value>` for each key of the TOML file at `<path>`, which is read when cooking. A role binds `claude`
only, as Codex has no roles. A role and a settings file do not bind the same harness; to combine them, write a settings
file that holds both. `--worktree-root` names the directory, by its path from the repository root, under whose `ralph/`
run worktrees go, as `io.github.lmarqs.cookbook:agents#connect --worktree-root` does for Claude Code's; `.worktrees` by
default. `--worktree-setup` and `--worktree-teardown` each name a committed executable by its path from the repository
root, outside `.ralph-tui/`; without them, `bin/run` runs no setup and `bin/clean` no teardown. `--worker-check` names
one too, the check a task must pass to complete; without it, the worker refuses every task. `--tracker` is `beads` or
`beads-bv`, run with `--epic`, or `json`, run with `--prd`, and repeats, with one of `beads` and `beads-bv` at most;
`json` by default. It chooses the seeded prompts, the configuration's `tracker` line and the run flag the launcher
offers. `beads-bv` is ralph-tui's beads tracker with `bv` picking each next bead of the epic. `--instructions` names a
Markdown file by its path from the repository root, and repeats for each file; the file must exist. What it publishes
for people, sessions and other recipes:

| Name | What it is |
| --- | --- |
| `.ralph-tui/bin/run <name> --agent <entry> (--epic <id> \| --prd <file>) [--base <ref>] [ralph-tui run arguments…]` | Runs ralph-tui in the run's worktree, as Run-time effects says |
| `.ralph-tui/bin/clean <name> [--force]` | Removes the run's worktree and branch, and its parallel workers' worktrees and branches |
| `.ralph-tui/bin/worker <agent> [agent arguments…]` | Runs the agent for ralph-tui, as Run-time effects says |
| `<worktree root>/ralph/<name>` on branch `ralph/<name>` | The rule for a run's worktree, following `<worktree root>/<branch>` |
| `.ralph-tui/templates/beads.hbs`, `beads-bv.hbs`, `json.hbs` | The prompts ralph-tui renders for a run with `--epic` under each beads tracker, and with `--prd` |

## Consumes

- ralph-tui 0.12.0 and Git at run time; Claude Code 2.1.306 and codex-cli 0.160.1 as workers. `bd` 1.3.1 only for a
  run with `--epic`, which reads the Beads database; a run with `--prd` needs no `bd`. `bv` 0.25.2 only for a run with
  `--epic` under `--tracker=beads-bv`; without it, ralph-tui takes the plain beads order.
- `.agents/adapters/claude/roles/<name>.json` in the draft, with `--worker-role=claude:<name>`: written by
  `io.github.lmarqs.cookbook:agents#connect --harness=claude` from `roles.<name>` in `.agents/permission/policy.yaml`.
  Without it, cooking is refused and names both.
- The `--worker-settings=claude:<path>` path as given: written into the configuration, never read or written at cook
  time. Workers resolve it in the run worktree, so it names a committed file by its path from the repository root,
  without `..` segments and outside `.ralph-tui/`, where Git ignores all but the configuration, prompts and launcher.
- The `--worker-settings=codex:<path>` file in the draft: read at cook time, and its keys
  written into the configuration as `-c` flags. Without the file, or when it is not TOML, or holds a key or value `-c`
  cannot carry, cooking is refused.
- The `--worktree-setup` file, at run time, only with `--worktree-setup`: `bin/run` runs it in each run worktree it
  creates and fails the run when it is missing there, not executable or fails. Never read or written at cook time.
- The `--worktree-teardown` file, at run time, only with `--worktree-teardown`: `bin/clean` runs it in the run worktree
  before removing anything, and stops when it is missing there, not executable or fails. Never read or written at cook
  time.
- The `--worker-check` file, at run time, only with `--worker-check`: the worker runs it in the worker's worktree,
  with the commit the agent started from as its argument, and a task completes only when it exits 0. Never read or
  written at cook time.
- The files `--instructions` names, which must exist, written by the team or by a recipe cooked earlier in the same
  invocation.
- By hand first: nothing. By hand after: installing ralph-tui and the runtimes it needs, and for `--epic`, a Beads
  database with the epic, as After cooking says.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.ralph-tui/config.toml` | Config version 2.1, `tracker = "beads"` or `"beads-bv"` with that tracker, `maxIterations = 10`, `autoCommit = true`, `[errorHandling] strategy = "skip"`, or with `--worker-check` `strategy = "retry"`, `maxRetries = 2` and `continueOnNonZeroExit = false`, `[parallel] directMerge = true`, and one `[[agents]]` entry per harness, with `--worker-check` running `.ralph-tui/bin/worker` with the harness as its first argument | Always, when absent | seed |
| `.ralph-tui/templates/beads.hbs`, `beads-bv.hbs`, `json.hbs` | The prompts of the chosen trackers: stay in this worktree, leave committing to ralph-tui, never push, print `<promise>COMPLETE</promise>` only after the checks pass and `BLOCKED:` otherwise; never close or update the bead, or never edit the PRD file | Always, when absent | seed |
| `.ralph-tui/bin/run`, `.ralph-tui/bin/clean` | The launcher scripts, executable | Always | owned |
| `.ralph-tui/bin/worker` | The worker wrapper, executable | Always | owned |
| `.gitignore` | The section `ralph-tui`: the argument line, `/.ralph-tui/*` but the configuration, prompts and launcher, and `/<worktree root>/ralph/` | `--share=team`, the default | owned |
| `.gitignore` | The section `ralph-tui`: the argument line, `/.ralph-tui/` and `/<worktree root>/ralph/` | `--share=clone` | owned |

The Claude entry runs `claude --permission-prompts none --permission-mode acceptEdits --print`: edits are accepted, and
any command the settings do not allow is refused rather than asked, with `skipPermissions = false`.
`--worker-role=claude:<role>` or `--worker-settings=claude:<path>` adds `--settings <file>` to it. The Codex entry runs
`codex --dangerously-bypass-hook-trust -c approval_policy=never -c sandbox_workspace_write.network_access=false exec
--sandbox workspace-write`: no approvals, no network, writes only in the worktree, and the project's hooks run whether
or not anyone trusted them, since Codex skips an untrusted or changed hook without a message and the session start
would lose its context; the hooks are the main checkout's committed `.codex/hooks.json`.
`--worker-settings=codex:<path>` adds one `-c <key>=<value>` for each key of the file, after the entry's own, so they
win. Neither sets a model or an effort.

Never touches: `.agents/permission/policy.yaml`, any settings or role file, version pins, remotes, pull requests, the
tracker's state, and the rest of each `--instructions` file.

## Connections

| Contact | Made by |
| --- | --- |
| What a Claude worker may do | The person, by `--worker-role=claude:<role>` or `--worker-settings=claude:<path>`; the team, by adding entries to the configuration; the session, by `bin/run --agent <entry>` |
| What a Codex worker may do | The person, by `--worker-settings=codex:<path>`; the team, in the seeded entry, which sets Codex's `workspace-write` sandbox; `--agent codex` runs with only what `--worker-settings=codex:` gave the Codex entry |
| The tracker a run takes tasks from | The session: `--epic <id>` runs the Beads epic, `--prd <file>` the JSON PRD; the person, by `--tracker=beads-bv`, has `bv` order the epic's beads |
| Where a run's configuration and prompts come from | The team, by committing `.ralph-tui/` with `--share=team`; the person, by `--share=clone` or by ignoring it in their clone, and `bin/run` copies it into each run |
| Whether a task completes only once the checks pass | The person, by `--worker-check`; the team writes the file, or points an entry's `command` at a wrapper of its own |
| Where run worktrees go | The person, by `--worktree-root` |
| The run worktree's setup | The person, by `--worktree-setup`; the team writes the file |
| The run worktree's teardown | The person, by `--worktree-teardown`; the team writes the file |
| The branch a run starts from | The session, by `--base`, `HEAD` by default |
| Model and effort | The session, by `ralph-tui run --model` and `CLAUDE_CODE_EFFORT_LEVEL`; the team, per entry |
| The launcher's commands in the files agents and people read | The person, by `--instructions`; the section is appended at the file's end, and stays where the team moves it |
| The agents layout's Launching table | The team, from After cooking |
| Delivering the run branch | The session: the launcher never pushes, merges or opens a pull request |

`io.github.lmarqs.cookbook:git#worktrees` ignores and documents `.worktrees/`; cook the two in either order.
`io.github.lmarqs.cookbook:beads#tracker` gives the Beads side; cook the two in either order. With
`--worker-role=<harness>:<role>`, cook `io.github.lmarqs.cookbook:agents#connect --harness=<harness>` first, or before
this recipe in the same invocation, with the role defined in the policy.

## Run-time effects

`bin/run <name>` refuses while `git status --porcelain .ralph-tui` in the main checkout lists anything, because a
worktree holds only what is committed. It creates `<worktree root>/ralph/<name>` on a new branch `ralph/<name>` from
`--base`, `HEAD` by default, or reuses the worktree or the branch when they exist; it refuses a `--base` when the branch
exists, and a `--agent`, `--epic`, `--prd` or `--base` given twice. It requires `--agent <entry>`, and after the
worktree is ready, and any copy of the configuration is made, refuses an `--agent` that no `[[agents]]` entry's `name`
or `plugin` in the worktree's `.ralph-tui/config.toml` equals, listing the entries' names. With `--worktree-setup`, it
runs that file from the root of each worktree it creates, never one it reuses, and stops the run when the file is
missing, not executable or fails, leaving the worktree for `bin/clean`; without `--worktree-setup`, it runs no setup.
When `ralph/<name>` has no committed `.ralph-tui/config.toml`, it copies the main checkout's `config.toml` and
`templates/` into the worktree on every run, because ralph-tui reads them only from there. It first checks that Git
ignores them and `session.json` in the worktree, because ralph-tui commits each task with `git add -A`; otherwise it
refuses and prints the line that ignores `.ralph-tui/` in the clone. With a copy, it passes `--prompt` with the copied
prompt of the run's tracker, unless the run gives `--prompt`, because ralph-tui copies only `config.toml` into a
parallel worker. It prints the worktree's path and branch, then runs `ralph-tui run --epic <id> --agent <entry>`, or
`--prd` with the file's absolute path, in the worktree, with the remaining arguments, such as `--headless` or
`--model opus`. Cooked with `--tracker=beads-bv`, a run with `--epic` exports `BEADS_DIR` as the main checkout's
`.beads`, unless it is set, because bv reads the issues `bd export` writes, and finds no database from a run worktree.
It sets and strips no other environment variable, so `CLAUDE_CODE_EFFORT_LEVEL` reaches Claude workers.

An agent entry whose `command` is `.ralph-tui/bin/worker`, as `--worker-check` seeds, has ralph-tui run
`.ralph-tui/bin/worker <agent> [arguments…]` for each task, from the run worktree or a parallel worker's. The worker
refuses every task without `--worker-check`, and in the main checkout, and passes `--version` alone to every seeded
harness. It runs the agent with ralph-tui's stdin and holds the agent's stdout in `.ralph-tui/worker-output.log`; an
agent that exits non-zero ends the worker with its exit code, its output withheld. When that output holds the promise
in any form ralph-tui accepts, it runs the check with the commit the agent started from, writing the check's output to
`.ralph-tui/worker-check.log`, never to stderr. A failed check prints one line to stderr and exits 1, so the task stays
open and ralph-tui retries it, then skips it. Otherwise it writes the held output for ralph-tui. Stopping the worker
stops the agent's or the check's processes. `bin/run` copies the worker with a copied configuration.

`bin/clean <name>` refuses while `ralph/<name>` or a parallel worker's branch has a commit whose change neither `HEAD`
nor `origin/HEAD` has. A merge, rebase or cherry-pick counts as delivered, except where it loses a change: a squash
merge, a commit changed while delivering it, and a rebase or cherry-pick that drops a merge commit whose result differs
from Git's own merge, such as a resolved conflict. It also refuses while its worktree or a worker's has changes.
`--force` removes them anyway. With `--worktree-teardown`, it then runs that file from the root of the run worktree,
when one exists, and stops when the file is missing, not executable or fails, removing nothing. It removes the worktrees
ralph-tui made for the run's parallel workers, in `.ralph-worktrees/<last part of the name>/` beside the run worktree,
such as `<worktree root>/ralph/.ralph-worktrees/<name>/`, with their branches, then the worktree and the branch
`ralph/<name>`.

## Re-cooking

Repeat the arguments on the opening line of the `ralph-tui` section in `.gitignore`, such as `#
io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worktree-setup=.worktrees/setup`. Cooking
with another `--worktree-setup` or `--worktree-teardown`, or without one, rewrites the launcher, cooking with another
`--worker-check`, or without one, rewrites the worker, and cooking without a file's `--instructions` removes its
section. Cooking with another `--worktree-root` rewrites the launcher and the `.gitignore` section, which then look only
under the new root, so clean every run before. Cooking with another `--share` rewrites the `.gitignore` section
only: Git keeps tracking a `.ralph-tui/` committed before `--share=clone` until `git rm -r --cached .ralph-tui` removes
it from the index. The seeds are never rewritten or removed, so a narrower `--tracker`
leaves a prompt seeded earlier in place, and a cook without `--tracker` takes `json`: re-cooking for `--epic` repeats
`--tracker=beads` or `--tracker=beads-bv`, or the launcher's usage line and the instructions section offer only `--prd`.
A cook whose arguments the existing `.ralph-tui/config.toml` lacks is refused and writes nothing: an agent entry for
each `--harness`, the flags of each `--worker-role` or `--worker-settings`, the worker as the `command` of each entry
for `--worker-check`, a beads `tracker` for `--tracker=beads`, and `tracker = "beads-bv"` for `--tracker=beads-bv`. Edit
the file or delete it and cook again; a Codex settings file is copied into the configuration when seeding, so editing
one means deleting `config.toml` and cooking again. A newer version's prompts are taken by deleting them and cooking
again, and a newer version's entry flags, such as the Codex entry's `--dangerously-bypass-hook-trust`, by adding them to
the entry by hand. A newer version rewrites the launcher and the sections. Every cook rewrites `.ralph-tui/bin/`, so a
team never edits it: to use scripts of its own, it runs its own launcher instead of `bin/run` and `bin/clean`, or points
an agent entry's `command` at its own worker, such as `scripts/worker.py`, in the configuration, which is never
rewritten.

## Traps

- Claude Code ignores the project's `permissions.allow` in a workspace nobody has trusted, and a fresh clone or a new
  worktree may be one: an unattended Claude worker's allows come through `--worker-role=claude:<role>` or
  `--worker-settings=claude:<path>`.
- ralph-tui 0.12 completes and commits a task when the agent's output holds `<promise>COMPLETE</promise>` anywhere, in
  any case and with any whitespace inside the tags, even in a sentence saying it is not done, whatever the checks say.
  `--worker-check` gates it on the team's check; the artifact's README, under Gating completion, says how a team writes
  a wrapper of its own.
- ralph-tui 0.12 runs an installed plugin with no options for an `--agent` that matches no entry, and for Claude that
  means no permission checks. `bin/run` refuses it; `ralph-tui run` started by hand does not.
- ralph-tui commits with `git add -A`, so whatever a worker changes in the run worktree, `.beads/` included, rides in
  the run branch.
- ralph-tui 0.12 marks a task done before committing it, and reports a failed commit nowhere, headless or in the TUI.
  A commit refused by signing with no key or by a hook leaves the task done and its changes staged in the run
  worktree, where the next task's commit takes them in under its own message. When `ralph/<name>` has fewer commits
  than the run completed tasks, run `git status` in the run worktree and check the commit settings; `bin/clean`
  refuses while the changes remain.
- ralph-tui 0.12 copies only `config.toml` into a parallel worker's worktree and runs no setup there: with
  `.ralph-tui/` not committed, as with `--share=clone`, parallel workers find no `.ralph-tui/bin/worker`, so with
  `--worker-check` run serially, or commit `.ralph-tui/`.
- ralph-tui 0.12's Claude plugin adds `--dangerously-skip-permissions` unless the entry has `skipPermissions = false`
  under `[agents.options]`; at the top of the entry, as `ralph-tui setup` would place it, the key is dropped silently.
- ralph-tui 0.12's Claude plugin accepts only `sonnet`, `opus` and `haiku` as `--model`.
- ralph-tui 0.12's parallel mode starts children that are still blocked, and refuses fewer than three tasks.
- ralph-tui 0.12 reads the prompt `.ralph-tui/templates/<tracker>.hbs`: a configuration whose `tracker` is changed to
  `beads-bv` by hand leaves `beads.hbs` unread and takes ralph-tui's own prompt, which has the agent close the bead
  with `bd close`. `--tracker=beads-bv` seeds `beads-bv.hbs`.
- ralph-tui 0.12's `beads-bv` takes the plain beads order, and says so nowhere, when `bv` is missing or fails, or picks
  a bead outside the epic; its parallel mode never asks `bv`, so `bv` orders serial runs only.
- Codex has no roles: it loads no rules for one run, so a role's denies reach Claude workers only, and
  `--worker-role=codex:<role>` is refused. A Codex worker's limits come from its sandbox and the project's rules, and
  `--worker-settings=codex:<path>` sets its sandbox.
- A Codex `workspace-write` worker has no network, localhost included, so tests that reach local services fail, and
  writes only in the worktree and `/tmp`, so a tool that caches under the home directory fails: every `mise run`
  stops with `Read-only file system (os error 30)`. A `--worker-settings=codex:<path>` file with
  `sandbox_workspace_write.network_access = true` and `shell_environment_policy.set.MISE_CACHE_DIR = "/tmp/<name>"`
  opens both; its flags follow the entry's `network_access=false`, and Codex takes the last.
- Codex runs the hooks of the main checkout's `.codex/hooks.json`, not the run worktree's, so a hook changed on the
  run branch takes effect only once the main checkout has the change.
- A Codex `workspace-write` worker can neither read nor write the Beads database, nor write `.git`: ralph-tui commits
  and closes beads for it, and the prompts ask for neither.
- Deny rules are prefix rules: `git -C . push` gets past `git push`. They are a guardrail; the structural limit is a
  worker without push credentials.
- ralph-tui 0.12 treats stderr naming a rate limit, `429` or `overloaded` as rate limiting, and retries or switches
  agent: a worker's checks that print such words to stderr look rate-limited.
- A `[parallel] worktreeDir` in the configuration moves the parallel workers where `bin/clean` does not look: it then
  removes the run and leaves its workers' worktrees and branches.
- A tracked PRD run with `--parallel` leaves its completion marks uncommitted: ralph-tui 0.12 writes them after merging
  the workers' commits, and commits nothing after that.
- A run branch with a committed `.ralph-tui/config.toml` takes the configuration and prompts committed there, so a
  `--base` from before a change to them runs the earlier ones.

Parameters
  --harness=<String[]>
      Harnesses ralph-tui drives, one agent entry each; each run picks one with --agent; required; choices: claude, codex; repeatable
  --worker-role=<String[]>
      claude:<role>, a role of the agents layout whose rendered settings the Claude entry passes; optional; repeatable
  --worker-settings=<String[]>
      <harness>:<path>, a settings file of the team's that harness's entry passes; optional; repeatable
  --worktree-root=<String>
      The directory, from the repository root, under whose ralph/ bin/run puts each run worktree; default: .worktrees
  --worktree-setup=<String>
      An executable, by its path from the repository root, that bin/run runs in each run worktree it creates; optional
  --worktree-teardown=<String>
      An executable, by its path from the repository root, that bin/clean runs in a run worktree before removal; optional
  --worker-check=<String>
      An executable, by its path from the repository root, that must pass before a worker completes a task; optional
  --tracker=<String[]>
      Trackers whose prompts are seeded and run flag offered: beads or beads-bv (--epic), json (--prd); default: json; choices: beads, beads-bv, json; repeatable
  --share=<String>
      Who shares .ralph-tui/: team commits its configuration, prompts and launcher; clone commits none of it; default: team; choices: team, clone
  --instructions=<String[]>
      A Markdown file, from the repository root, where the recipe gives the launcher's commands; optional; repeatable

Ingredients
  io.github.lmarqs.cookbook.ralph.RalphTui

After cooking
  Install ralph-tui and the runtimes it needs with `mise use -g node@22 bun@1.3 npm:ralph-tui@0.12.0`: npm needs node to
  install it, and its launcher runs on bun. Without `-g`, mise pins them in the project's `mise.toml` instead, which is
  the team's choice. With `--tracker=beads-bv`, install `bv` too, such as with `brew install dicklesworthstone/tap/bv`.
  Review `.ralph-tui/config.toml` and the prompts. Commit `.gitignore` and, with `--share=team`, `.ralph-tui/` to share
  them with the team; with `--share=clone`, `.gitignore` keeps `.ralph-tui/` out of commits. In a new clone, run
  `./cook replicate io.github.lmarqs.cookbook:ralph#ralph-tui` to recreate it from the committed `cook.lock`. To keep
  `.ralph-tui/` to one clone of a team that shares it, ignore it with
  `echo /.ralph-tui/ >> "$(git rev-parse --git-path info/exclude)"`. `bin/run` copies an uncommitted `.ralph-tui/` into
  each run. `bin/run` refuses while `.ralph-tui` has changes Git neither commits nor ignores. Then run
  `.ralph-tui/bin/run <name> --agent <entry> --epic <id>`, with a Beads database holding the epic, or
  `.ralph-tui/bin/run <name> --agent <entry> --prd <file>`, and `.ralph-tui/bin/clean <name>` once the branch
  `ralph/<name>` is delivered. Cooked without `--instructions`, cook again with it, such as `--instructions=AGENTS.md`,
  to give agents these commands, and with the agents layout, add them to the Launching table in `.agents/README.md`.
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
