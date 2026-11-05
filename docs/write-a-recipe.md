---
description: Writes a model and a recipe in a repository, cooks them by path, gives the recipe a section of a shared file, then moves both into a cookbook that cook.toml serves without a build.
audience: Whoever has cooked a shipped recipe and wants one of their own, with a terminal and fifteen minutes
---
# Write a recipe

The shipped cookbooks cover agents, worktrees, loops and docs. The setup your team is missing is a recipe you write:
two Groovy files in the repository, cooked like any other, with the same diff, the same lock and the same `check`. In
fifteen minutes you write one that keeps a notes file and a line of `.gitignore`, then move it into a cookbook other
repositories can cook. The output shown is from a real run, shortened.

You need the repository from [Get started](get-started.md), or any repository with the `cook` wrapper committed. The
examples start from a `.gitignore` that holds one line of your own, `node_modules/`.

## 1. Write a model

A model is a Groovy class marked `@Ingredient`. Its annotated members say which files, sections or keys it writes,
and the rest of the class is the API a recipe calls. Create `recipes/Notes.groovy`:

```groovy
package acme.notes

/** The team's notes, kept in NOTES.md. */
@Ingredient
class Notes {
  private final List<String> lines = []

  /** Adds a line to the notes. */
  void add(String line) {
    lines << line
  }

  /** NOTES.md, one bullet per line added. */
  @File(path = 'NOTES.md')
  String file() {
    lines ? lines.collect { "- ${it}\n" }.join() : null
  }
}
```

`file()` returns `null` until a line is added. A model declares only what a recipe asks for, and Cook refuses one
that writes something on a new instance.

## 2. Write a recipe and cook it

A recipe is a lower-case `.groovy` file beside its models. It describes itself in `@help`, asks for a model with
`@ingredient` and calls it. Create `recipes/notes.groovy`:

```groovy
@help('Adds a line to NOTES.md.')
import acme.notes.Notes

@ingredient(Notes) def notes

notes.add('Run the tests before committing.')
```

Cook it by its path:

```sh
./cook recipes/notes.groovy
```

```text
--- /dev/null
+++ b/NOTES.md
@@ -0,0 +1,1 @@
+- Run the tests before committing.
--- /dev/null
+++ b/cook.lock
...
+  - name: "recipes/notes"
+    source: "recipes/notes.groovy"
...
+    owns:
+      files:
+        - "NOTES.md"

Write these changes? [y/N] y
Created NOTES.md
Created cook.lock
```

The lock records the recipe by its path without `.groovy`, and that it owns `NOTES.md`. Cook it again and nothing
changes, so it prints `Already cooked.` `./cook help recipes/notes.groovy` prints the `@help` text and the model it
uses, without running anything.

## 3. Own a section, not a file

`.gitignore` belongs to the team, so the recipe takes one marked section of it and leaves the rest alone. Give the
model a list and a member that returns it as a section:

```groovy
  private final List<String> ignored = []

  /** Keeps a path out of Git. */
  void ignore(String path) {
    ignored << path
  }

  /** The paths added with ignore, in a section of .gitignore. */
  @Section(path = '.gitignore', id = 'scratch')
  List<String> ignoredPaths() {
    ignored ?: null
  }
```

Add `notes.ignore('scratch/')` to the end of the recipe and cook it again:

```text
--- a/.gitignore
+++ b/.gitignore
@@ -1,1 +1,4 @@
 node_modules/
+# cook:begin recipes/notes/scratch
+scratch/
+# cook:end recipes/notes/scratch
```

The markers name the recipe that owns the lines between them. `node_modules/` stays yours: no recipe owns it, so
cooking never touches it. [Sections and markers](features/cook.md#sections-and-markers) says how Cook finds and
updates a section.

## 4. Take an argument

A `@param` field turns a fixed value into an option. Replace the recipe's body below the `@ingredient` line:

```groovy
@param(value = 'line', doc = 'The line to add', default = 'Run the tests before committing.')
String line

notes.add(line)
notes.ignore('scratch/')
```

```sh
./cook recipes/notes.groovy --line='Ask before force-pushing.'
```

```text
--- a/NOTES.md
+++ b/NOTES.md
@@ -1,1 +1,1 @@
-- Run the tests before committing.
+- Ask before force-pushing.
```

The lock records the argument, so `./cook replicate` and `./cook check` use the same value. `./cook help` now lists
`--line` with its `doc` and default.

## 5. Move it into a cookbook

A recipe path works in its own repository only. To cook the recipe from another, move it into a cookbook: a
directory with a `cookbook.toml` that names its group, and one directory per artifact under `src/`.

The recipe's name changes with the move, from `recipes/notes` to `acme:notes#notes`, so take the old one out first.
`discard` removes exactly what the lock says the recipe wrote, without running it:

```sh
./cook discard recipes/notes
```

Then move the files and tell the repository where the cookbook is. The model's package, `acme.notes`, is the group
followed by the artifact's directory:

```sh
mkdir -p ../cookbook/src/notes
mv recipes/Notes.groovy recipes/notes.groovy ../cookbook/src/notes/
echo 'group = "acme"' > ../cookbook/cookbook.toml
echo 'repositories = ["../cookbook"]' > cook.toml
./cook acme:notes:0.1.0#notes --line='Ask before force-pushing.'
```

```text
--- a/.gitignore
+++ b/.gitignore
@@ -1,1 +1,4 @@
 node_modules/
+# cook:begin acme:notes#notes/scratch
+scratch/
+# cook:end acme:notes#notes/scratch
...
+  - name: "acme:notes#notes"
+    source: "acme:notes:0.1.0#notes"
```

A cookbook's source directory serves any version as its files are now, with no build, so each edit cooks
straight away. Commit `cook.toml` and `cook.lock` with the rest. Any repository that lists the same cookbook in its
`cook.toml` cooks the recipe by the same coordinates.

## Where to go next

- [A cookbook outside this repository](../cookbook/README.md#a-cookbook-outside-this-repository) packages a cookbook
  as jars, for Maven local or a Maven repository your team shares.
- [The author's API](../cookbook/README.md#the-authors-api) lists every annotation a model and a recipe can use: keys
  in JSON, TOML and YAML, links, seeded files.
- The [docs cookbook](../cookbook/src/docs/README.md) is a short shipped artifact to read next to your own.
- [Cooking](features/cook.md) says what a recipe owns, what the lock records and what cooking refuses.
