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
and `--instructions`, repeatable, names the Markdown files where a recipe states the rules it sets up for people and
agents, such as `AGENTS.md` and `README.md`. A param avoids a word the configured program uses for something else, as
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
sets it up so each run works unattended in a worktree of its own, `.worktrees/ralph/<name>` on the branch
`ralph/<name>`, and connects to worker limits and worktree setup only where the person names them. To set it up, cook
`#ralph-tui`, whose help says what it writes and how to launch a run.

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
# Claude Code workers, limited by the project's settings
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude
# Claude Code and Codex workers, Claude by default
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --harness=codex
# Claude workers bound to the agents layout's role worker
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-role=worker
# Claude workers with a settings file the team writes
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worker-settings=ralph-worker.json
# Claude workers in run worktrees the launcher sets up and tears down with the team's scripts
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --worktree-setup=.worktrees/setup --worktree-teardown=.worktrees/teardown
# Also giving agents and people the launcher's commands, in the files they read
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --instructions=AGENTS.md
```

The first `--harness` is the default agent. `--worker-role` and `--worker-settings` reach only the Claude entry, and
only one of them is given. `--worktree-setup` and `--worktree-teardown` each name a committed executable by its path
from the repository root, outside `.ralph-tui/`; without them, `bin/run` runs no setup and `bin/clean` no teardown.
`--instructions` names a Markdown file by its path from the repository root, and repeats for each file; the file must
exist. What it publishes for people, sessions and other recipes:

| Name | What it is |
| --- | --- |
| `.ralph-tui/bin/run <name> (--epic <id> \| --prd <file>) [--base <ref>] [ralph-tui run arguments…]` | Runs ralph-tui in the run's worktree, as Run-time effects says |
| `.ralph-tui/bin/clean <name> [--force]` | Removes the run's worktree and branch, and every `ralph-parallel/*` worktree and branch |
| `.worktrees/ralph/<name>` on branch `ralph/<name>` | The rule for a run's worktree, following `.worktrees/<branch>` |
| `.ralph-tui/templates/beads.hbs`, `json.hbs` | The prompts ralph-tui renders for a run with `--epic` and with `--prd` |

## Consumes

- ralph-tui 0.12.0 and Git at run time; Claude Code 2.1.306 and codex-cli 0.160.1 as workers. `bd` 1.3.1 only for a
  run with `--epic`, which reads the Beads database; a run with `--prd` needs no `bd`.
- `.agents/adapters/claude/roles/<name>.json` in the draft, with `--worker-role=<name>`: written by
  `io.github.lmarqs.cookbook:agents#connect --harness=claude` from `roles.<name>` in `.agents/permission/policy.yaml`.
  Without it, cooking is refused and names both.
- The `--worker-settings` path as given: written into the configuration, never read or written at cook time. Workers
  resolve it in the run worktree, so it names a committed file by its path from the repository root, outside
  `.ralph-tui/`, where Git ignores all but the configuration, prompts and launcher.
- The `--worktree-setup` file, at run time, only with `--worktree-setup`: `bin/run` runs it in each run worktree it
  creates and fails the run when it is missing there, not executable or fails. Never read or written at cook time.
- The `--worktree-teardown` file, at run time, only with `--worktree-teardown`: `bin/clean` runs it in the run worktree
  before removing anything, and stops when it is missing there, not executable or fails. Never read or written at cook
  time.
- The files `--instructions` names, which must exist, written by the team or by a recipe cooked earlier in the same
  invocation.
- By hand first: nothing. By hand after: installing ralph-tui and the runtimes it needs, and for `--epic`, a Beads
  database with the epic, as After cooking says.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.ralph-tui/config.toml` | Config version 2.1, `tracker = "beads"`, the first harness as `defaultAgent`, `maxIterations = 10`, `autoCommit = true`, `[errorHandling] strategy = "skip"`, `[parallel] directMerge = true`, and one `[[agents]]` entry per harness | Always, when absent | seed |
| `.ralph-tui/templates/beads.hbs`, `json.hbs` | The prompts: stay in this worktree, leave committing to ralph-tui, never push, print `<promise>COMPLETE</promise>` only after the checks pass and `BLOCKED:` otherwise; never close or update the bead, or never edit the PRD file | Always, when absent | seed |
| `.ralph-tui/bin/run`, `.ralph-tui/bin/clean` | The launcher scripts, executable | Always | owned |
| `.gitignore` | The section `ralph-tui`: the argument line, `/.ralph-tui/*` but the configuration, prompts and launcher, and `/.worktrees/ralph/` | Always | owned |

The Claude entry runs `claude --permission-prompts none --permission-mode acceptEdits --print`: edits are accepted, and
any command the settings do not allow is refused rather than asked, with `skipPermissions = false`. `--worker-role` or
`--worker-settings` adds `--settings <file>` to it. The Codex entry runs `codex -c approval_policy=never -c
sandbox_workspace_write.network_access=false exec --sandbox workspace-write`: no approvals, no network, writes only in
the worktree. Neither sets a model or an effort.

Never touches: `.agents/permission/policy.yaml`, any settings or role file, version pins, remotes, pull requests, the
tracker's state, and the rest of each `--instructions` file.

## Connections

| Contact | Made by |
| --- | --- |
| What a Claude worker may do | The person, by `--worker-role` or `--worker-settings`; the team, by adding entries to the configuration; the session, by `ralph-tui run --agent <entry>` |
| What a Codex worker may do | The team, in the seeded entry, which sets Codex's `workspace-write` sandbox |
| The tracker a run takes tasks from | The session: `--epic <id>` runs the Beads epic, `--prd <file>` the JSON PRD |
| The run worktree's setup | The person, by `--worktree-setup`; the team writes the file |
| The run worktree's teardown | The person, by `--worktree-teardown`; the team writes the file |
| The branch a run starts from | The session, by `--base`, `HEAD` by default |
| Model and effort | The session, by `ralph-tui run --model` and `CLAUDE_CODE_EFFORT_LEVEL`; the team, per entry |
| The launcher's commands in the files agents and people read | The person, by `--instructions`; the section is appended at the file's end, and stays where the team moves it |
| The agents layout's Launching table | The team, from After cooking |
| Delivering the run branch | The session: the launcher never pushes, merges or opens a pull request |

`io.github.lmarqs.cookbook:git#worktrees` ignores and documents `.worktrees/`; cook the two in either order.
`io.github.lmarqs.cookbook:beads#tracker` gives the Beads side; cook the two in either order. With `--worker-role`,
cook `io.github.lmarqs.cookbook:agents#connect --harness=claude` first, or before this recipe in the same invocation,
with the role defined in the policy.

## Run-time effects

`bin/run <name>` refuses while `git status --porcelain .ralph-tui` in the main checkout lists anything, because a
worktree holds only what is committed. It creates `.worktrees/ralph/<name>` on a new branch `ralph/<name>` from
`--base`, `HEAD` by default, or reuses the worktree or the branch when they exist. With `--worktree-setup`, it runs that
file from the root of each worktree it creates, never one it reuses, and stops the run when the file is missing, not
executable or fails, leaving the worktree for `bin/clean`; without `--worktree-setup`, it runs no setup. It prints the
worktree's path and branch, then runs `ralph-tui run --epic <id>`, or `--prd` with the file's absolute path, in the
worktree, with the remaining arguments, such as `--headless`, `--agent codex` or `--model opus`. It sets and strips no
environment variable, so `CLAUDE_CODE_EFFORT_LEVEL` reaches Claude workers.

`bin/clean <name>` refuses while `ralph/<name>` has commits `HEAD` does not, which includes a branch delivered by a
squash merge, or while its worktree has changes; `--force` removes them anyway. With `--worktree-teardown`, it then
runs that file from the root of the run worktree, when one exists, and stops when the file is missing, not executable
or fails, removing nothing. It removes the worktree, every worktree on a `ralph-parallel/*` branch with that branch,
then the branch `ralph/<name>`.

## Re-cooking

Repeat the arguments on the opening line of the `ralph-tui` section in `.gitignore`, such as `#
io.github.lmarqs.cookbook:ralph#ralph-tui --harness=claude --harness=codex --worktree-setup=.worktrees/setup`. Cooking
with another `--worktree-setup` or `--worktree-teardown`, or without one, rewrites the launcher, and cooking without a
file's `--instructions` removes its section. The seeds are never rewritten: to take other harnesses, a worker role or a
newer version's configuration or prompts, edit them or delete them and cook again. A newer version rewrites the
launcher and the sections.

## Traps

- Claude Code ignores the project's `permissions.allow` in a workspace nobody has trusted, and a fresh clone or a new
  worktree may be one: an unattended Claude worker's allows come through `--worker-role` or `--worker-settings`.
- Deny rules are prefix rules: `git -C . push` gets past `git push`. They are a guardrail; the structural limit is a
  worker without push credentials.
- A Codex `workspace-write` worker can neither read nor write the Beads database, nor write `.git`: ralph-tui commits
  and closes beads for it, and the prompts ask for neither.
- Codex runs no project hooks in a worktree it has not trusted, so a Codex worker gets no session-start context unless
  started with `--dangerously-bypass-hook-trust`.
- ralph-tui 0.12's Claude plugin accepts only `sonnet`, `opus` and `haiku` as `--model`.
- ralph-tui 0.12's parallel mode starts children that are still blocked, and refuses fewer than three tasks.
- `bin/clean` removes every `ralph-parallel/*` worktree, so cleaning one run while another runs in parallel mode stops
  the other's workers.

Parameters
  --harness=<String[]>
      Harnesses ralph-tui drives, one agent entry each; the first is the default; required; choices: claude, codex; repeatable
  --worker-role=<String>
      A role of the agents layout whose rendered Claude settings the claude entry passes with --settings; optional
  --worker-settings=<String>
      A Claude settings file the claude entry passes with --settings; optional
  --worktree-setup=<String>
      An executable, by its path from the repository root, that bin/run runs in each run worktree it creates; optional
  --worktree-teardown=<String>
      An executable, by its path from the repository root, that bin/clean runs in a run worktree before removal; optional
  --instructions=<String[]>
      A Markdown file, from the repository root, where the recipe gives the launcher's commands; optional; repeatable

Ingredients
  io.github.lmarqs.cookbook.ralph.RalphTui

After cooking
  Install ralph-tui and the runtimes it needs with `mise use -g node@22 bun@1.3 npm:ralph-tui@0.12.0`: npm needs node to
  install it, and its launcher runs on bun. Without `-g`, mise pins them in the project's `mise.toml` instead, which is
  the team's choice. Review `.ralph-tui/config.toml` and the prompts, and commit `.gitignore` and `.ralph-tui/`:
  `bin/run` refuses while `.ralph-tui` has uncommitted changes. Then run `.ralph-tui/bin/run <name> --epic <id>`, with a
  Beads database holding the epic, or `.ralph-tui/bin/run <name> --prd <file>`, and `.ralph-tui/bin/clean <name>` once
  the branch `ralph/<name>` is delivered. Cooked without `--instructions`, cook again with it, such as
  `--instructions=AGENTS.md`, to give agents these commands, and with the agents layout, add them to the Launching
  table in `.agents/README.md`.
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
