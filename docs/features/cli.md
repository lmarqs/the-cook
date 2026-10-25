# CLI reference

This page describes the intended command line contract from the
[architecture](../architecture.md#command-line-and-build-boundary) and its requirements
[FR-17 through FR-20](../architecture.md#requirements): command syntax, operands, arguments and options. The
[glossary](glossary.md) defines terms such as recipe, batch and `cook.lock`. For the options the current CLI accepts,
see the root README's [current implementation](../../README.md#current-implementation).

## Synopsis

```text
./cook [-C <dir>] [batch-options] <recipe> [recipe-arguments] [<recipe> [recipe-arguments]]...
./cook [-C <dir>] <command> ...
./cook check [-C <dir>] [<name>]...
./cook replicate [-C <dir>] [batch-options] [<name>]...
./cook update [-C <dir>] [batch-options] [--major] [<name>]...
./cook discard [-C <dir>] [batch-options] <name>...
./cook help [-C <dir>] [<recipe>]...
./cook --help
./cook --version
```

`./cook` is the release's [wrapper](wrapper.md), committed in the repository; it runs the engine version it pins.
Without a command word, `./cook` cooks the recipes it is given. Running `./cook` with no arguments, `./cook help` or
`./cook --help` shows the usage. `--help` takes no other arguments. `./cook --version` prints `cook <version>`, the
version the build wrote into the engine, and takes no other arguments either.

## Commands

| Operation | Operand | Proposed change | Use it to |
| --- | --- | --- | --- |
| Cook, without a command word | One or more recipes, with arguments for each | Declared additions and updates against current content, and removal of what each recipe's previous version owned and no longer declares | Choose or change a recipe's version or arguments |
| Check | Lock names, without arguments; none is every recipe `cook.lock` lists | None: it reports what replicate would change and exits 1 when anything would | Find a kitchen that differs from its lock, in CI or before a commit |
| Replicate | Lock names, without arguments; none is every recipe `cook.lock` lists | Cook again from the sources and arguments recorded in `cook.lock` | Fix what check reports, or recreate output a clone does not commit |
| Update | Coordinate lock names, without arguments; none is every coordinate recipe `cook.lock` lists | Cook newer compatible releases with recorded arguments; `--major` allows any newer release | Move recipes to newer releases on request ([update](update.md)) |
| Discard | One or more lock names, without arguments | Removal of what `cook.lock` lists for each recipe; no recipe runs | Remove what a recipe owns |
| Help | One or more recipes, without arguments | Show each recipe's help in order; read only, and no recipe runs | Read a recipe |

The [architecture](../architecture.md#cooking-and-discard) defines these operations; [cooking](cook.md),
[check](#check), [replicate](replicate.md), [update](update.md) and [discard](discard.md) explain them with examples.

```sh
# Cook a recipe
./cook ./recipe.groovy

# Report whether the kitchen matches cook.lock
./cook check

# Recreate a recipe's output from the lock
./cook replicate io.github.lmarqs.cookbook:ralph#ralph-tui

# Remove what two recipes own
./cook discard io.github.lmarqs.cookbook:agents#connect recipe
```

## Recipe operands

A recipe operand is a path inside the destination repository or a recipe's coordinates,
`group:artifact:version#recipe`. A word is a recipe operand when it contains `/` or `:` or ends with `.groovy`, so an
ordinary relative path such as `recipe.groovy` is one; a recipe file with none of these marks is given as `./notes`.
Cook refuses an operand outside the repository, and a URL with a message that points to `cook.toml`: a recipe shared
beyond one repository is published in a cookbook and cooked by its coordinates. Coordinates resolve from the
repositories that the destination's `cook.toml` lists, or from Maven Central without one. A recipe compiles against its
artifact's models; a recipe path, against the upper-case `.groovy` files in its directory.

```sh
./cook recipe.groovy
./cook ./recipes/project.groovy
./cook io.github.lmarqs.cookbook:agents:0.0.0#connect --harness=claude
```

`help` prints a recipe's `@help`, `@confirm` and `@yield` texts with its `@param` and `@ingredient` annotations, read
from its source without running it or resolving its imports. It is the one way to read a recipe through Cook:

```sh
./cook help ./recipes/project.groovy
./cook help ./recipes/project.groovy ./recipes/dependency.groovy
```

## Lock names

Check, replicate, update and discard read `cook.lock`, so they take a recipe only by its lock name, the short name
`cook.lock` writes: `group:artifact#recipe`, without a version, or a path's place in the kitchen without `.groovy`, such as
`recipes/setup`. A versioned operand or a `.groovy` path is refused with the lock name to write instead, such as
`acme:notes:1.2.3#notes is not a name cook.lock writes` above `write acme:notes#notes`, because the lock, not the
operand, supplies the recorded version. Update selects a newer release from that version. A lock name that is not
in `cook.lock` is refused with the names it lists. These commands take no recipe arguments. Discard resolves no
coordinates, so a path's lock name matches after its file is gone. Check and replicate load the recorded source and
arguments, so that source must be available.

Without names, check and replicate take every recipe `cook.lock` lists, in the order it lists them, and refuse a
kitchen without `cook.lock`. Update also requires the lock: without names it considers every coordinate recipe in
lock order and skips path recipes; with names it considers only those recipes, in operand order. An explicitly named
path recipe is refused because it has no version to update. See [update scope](update.md#scope) for unknown releases.
Discard always takes names, because it removes output.

A first operand equal to `check`, `discard`, `replicate`, `update` or `help` selects that command. To cook a recipe file
named `check`, `discard`, `replicate`, `update` or `help`, make its path explicit:

```sh
./cook ./discard
```

## Recipe arguments and ordering

Recipe arguments follow their recipe as `--name=value` or `--name value`. Each following recipe operand starts a new
argument scope. Shell-quote values containing spaces so the shell passes the value as part of one argument.

```sh
./cook --batch-approve ./base.groovy --agent codex ./project.groovy --agent=claude
./cook ./recipe.groovy --instructions='Keep existing comments.'
```

In the first invocation, `--agent codex` belongs to `base.groovy`, and `--agent=claude` belongs to `project.groovy`.
Recipes and their operations compose left to right into one batch; the
[architecture](../architecture.md#ordered-composition-and-declared-targets) defines how the last writer of a section,
key or file wins.

After `--name`, the next word is the argument's value unless it is a recipe operand or the param is yes-or-no, which
takes no separate value ([prior art](../ux/cli-standards.md#prior-art)). A value that looks like a recipe
operand is written with `=`:

```sh
./cook ./a.groovy --source=./b.groovy
./cook ./a.groovy --source ./b.groovy
```

The first invocation evaluates only `a.groovy` and gives it `source` with the value `./b.groovy`. In the second,
`./b.groovy` starts a second recipe, and `--source` is left without a value.

| Written | Result |
| --- | --- |
| `--agent=codex` | The value `codex` for `agent` |
| `--agent codex` | The value `codex` for `agent` |
| `--source=./b.groovy` | The value `./b.groovy` for `source` |
| `--source ./b.groovy` | `./b.groovy` starts the next recipe, and `--source` is refused: `--source needs a value`, with `write --source=<value>` below |
| `agent=codex`, after a recipe or a complete argument | Refused: `Not a recipe: agent=codex`, with `recipe arguments start with --` below |
| `--verbose`, for a yes-or-no param | The value `true` for `verbose`; `--verbose=false` turns it off |
| `--verbose false`, for a yes-or-no param | Refused, naming the recipe: `--verbose takes no separate value`, with `write --verbose=false` below |
| `--agent` with no value after it, for any other param | Refused, naming the recipe: `--agent needs a value`, with `write --agent=<value>` below |
| The same recipe twice, with any arguments or versions | Refused: `cook.lock` keeps one entry per recipe |

When a recipe fails to evaluate, nothing is written, and the error names the recipe by its position and operand, such
as `recipe 2 (io.github.lmarqs.cookbook:agents:0.0.0#connect)`.

Before writing, Cook shows each recipe's `@confirm` text under the heading `Confirm <address>`, then a unified diff of
every changed path, `cook.lock` included, and asks `Write these changes? [y/N]`. Any answer other than `y` or `yes`
writes nothing, prints `cook: nothing was written` and exits with status 1. `--batch-approve` shows the same texts and
diff and writes without asking. Without a terminal to ask in, Cook writes only with `--batch-approve`; otherwise it
prints `cook: approval required; nothing was written`, with the reason and the next step below, and exits with status 1.

After the writes, Cook prints one line per changed path, in path order, or `Already cooked.` when nothing changes:

| Line | Path |
| --- | --- |
| `Created <path>` | Absent before the run |
| `Updated <path>` | Present before and after the run |
| `Deleted <path>` | Removed by the run |
| `Replaced <path>` | Present before the run, and written over from a recipe's template with `--force` |
| `Kept <path>` | Present before the run, and left as it is with `--skip` |

With `--dry-run`, Cook shows the texts and the diff without asking, the lines read `Would create`, `Would update`,
`Would delete`, `Would replace` and `Would keep`, and nothing is written. After the writes, Cook prints each recipe's
`@yield` text in order, headed by its address.

## Existing files

Some recipes write a file from a template the first time they are cooked, such as `AGENTS.md` in
`io.github.lmarqs.cookbook:agents:0.0.0#seed`. When that file already exists, cooking the recipe the first time fails,
writes nothing and names the file and the recipe:

```text
AGENTS.md already exists; io.github.lmarqs.cookbook:agents#seed writes it from a template the first time it is cooked. Run with --force to replace it, or --skip to keep it.
```

When several recipes of one command meet such files, one refusal lists every file with its recipe, so `--force` never
replaces a file the refusal did not name. The [prior art](../ux/cli-standards.md#prior-art) shows how other tools treat
an existing file.

| Option | Position | Effect |
| --- | --- | --- |
| `--force` | Before the first recipe | Writes the template over the file, or over a symbolic link at its path, and cooks the rest |
| `--skip` | Before the first recipe | Leaves the file as it is, cooks the rest and records the recipe in `cook.lock` |

The options act only the first time a recipe is cooked, take recipe coordinates and exclude each other. Later runs
leave the file to whoever edits it.

```sh
./cook --force io.github.lmarqs.cookbook:agents:0.0.0#seed
./cook --skip io.github.lmarqs.cookbook:agents:0.0.0#seed
```

A recipe that makes a path a symbolic link refuses a file or directory already there, since its contents would stop
being read, and writes nothing. `--force` does not override the refusal; a link already at the path is replaced and
shown in the diff:

```text
cook: io.github.lmarqs.cookbook:agents:0.0.0#connect: .codex/config.toml link
  .codex/config.toml is a file, and this recipe makes it a link to ../.agents/adapters/codex/config.toml
    its contents would stop being read
    move its contents into .agents/adapters/codex/config.toml, delete it, and cook again
```

## Batch options and approval

| Option | Used with | Position | Effect |
| --- | --- | --- | --- |
| `--batch-approve` | Cooking, `discard`, `replicate` and `update` | Before the first recipe operand | Displays the final diff and proceeds without prompting; approval covers the whole batch |
| `--dry-run` | Cooking, `discard`, `replicate` and `update` | Before the first recipe operand | Displays the final diff and writes nothing |

`--batch-approve` is the batch option and the only preapproval spelling. Without it, Cook asks before changing target
files. Noninteractive execution without this flag does not silently authorize writes.

The review starts with a `Changed source` notice for each release whose sources differ from what `cook.lock` recorded
under the same coordinates, as when a jar was republished under its version, with both sha256 values. The diff that
follows shows what the changed sources write. Replicate also reports edited path sources this way; see
[source drift](replicate.md#source-drift-and-changed-arguments).

```sh
./cook --batch-approve ./recipe.groovy
./cook --batch-approve ./a.groovy ./b.groovy
./cook discard --batch-approve recipe
```

A batch option after a recipe fails with `cook: --batch-approve goes before the first recipe`. This invocation fails:

```sh
./cook ./a.groovy --batch-approve
```

Put the option before the first recipe instead:

```sh
./cook --batch-approve ./a.groovy
```

## Check

`./cook check` answers whether the kitchen matches `cook.lock`: it cooks each named recipe, or every recipe the lock
lists, again from the source and arguments the lock records, as [replicate](replicate.md) does, and writes nothing. A
CI step or a pre-commit hook runs it to fail when someone changed a recipe's input, such as a policy file, and did not
cook and commit the result. The step names no version and no argument, so it stays as it is when a recipe is upgraded
or cooked with other arguments.

```sh
./cook check
./cook check io.github.lmarqs.cookbook:agents#connect
```

When nothing would change, check prints `Already cooked.` and exits 0. Otherwise it prints each `Changed source`
notice, the diff and a `Would create`, `Would update`, `Would delete` or `Would replace` line per path, and exits 1 with
this diagnostic on stderr, naming the `-C` directory and the recipes it was given:

```text
cook: check: 2 files differ from what cook.lock records; nothing was written
  check cooks each recipe again from the source and arguments cook.lock records
  run ./cook replicate, review the diff and commit it
```

Check shows no `Confirm` texts and asks nothing, because it never writes; it takes `-C` and no batch option. Output a
clone does not commit, such as `.ralph-tui/` with `--share=clone`, is absent from a fresh clone, so check reports it
there; a CI step names the recipes whose output is committed.

## Exit status

| Status | Meaning |
| --- | --- |
| 0 | The command did what it was asked: it wrote the changes, found nothing to change or printed what was asked |
| 1 | Nothing was written because approval was declined or missing, check found a difference, or `update --dry-run` found a newer release in scope |
| 2 | An error: a misused command line, a refused operand, a recipe that failed to load or evaluate, or a failed write |

A CI step therefore tells a kitchen that differs from its lock, status 1, from a check that could not run, status 2.
`update --dry-run` exits 1 when a recipe would move, 0 when none would, and 2 on an error. An unknown recipe skipped
in an unnamed update is a warning, not an error; see [unknown recipes](update.md#unknown-recipes).

## Kitchen

The kitchen is the directory Cook cooks in, and it is the destination. Without an option it is the invocation's
current working directory.

| Option | Used with | Position | Effect |
| --- | --- | --- | --- |
| `-C <dir>`, `--kitchen=<dir>` | Every command | Before the first recipe operand, and before or after a command word; once | Cooks in `<dir>` as if Cook had started there: recipe operands and other relative paths resolve against it |

```sh
./cook -C services/api ./recipe.groovy
./cook --kitchen=services/api --batch-approve ./recipe.groovy
```

Like a batch option, the kitchen option after a recipe receives an error, because arguments after a recipe belong to
that recipe.

Approval covers all files and hunks in the batch. Existing or edited content follows this same diff and batch-approval
flow; ownership recorded in `cook.lock` never blocks a change. See
[batch review and recovery](../architecture.md#batch-approval-and-failure-recovery) for the write and rollback contract.
