# Update recipes

A developer has cooked recipes at exact releases and wants newer ones without finding each release or retyping the
arguments. Recreating output from the lock keeps the old versions. The developer can choose the recipes to update,
understand the release range and review the proposed changes before approving them.

## Overview

Update moves [recipes](glossary.md) recorded in `cook.lock` to newer releases on request, using their recorded
arguments. [Replicate](replicate.md) cooks the recorded versions again; update chooses newer versions first, then
uses the same cooking review. It evaluates trusted recipe code. Recipe updates leave the engine's
[wrapper pin](wrapper.md#upgrading) as it is.

### From 8.0.0 to the newest 8.x, then the newest release

This transcript was captured from the built CLI against a separate kitchen, selected with `-C "$kitchen"`. Its
`cook.toml` lists a local Maven-layout repository:

```toml
repositories = ["repository"]
```

For this example, that repository holds sample releases `8.0.0`, `8.2.0` and `9.0.0` of
`io.github.lmarqs.cookbook:git`, with all three listed in Maven metadata. They package the same worktrees recipe
sources, so updates change only the lock. These are controlled example releases, not a claim about the releases
available from Maven Central. In your kitchen, the configured repositories determine the available versions.

Start by cooking the worktrees recipe at `8.0.0`:

```sh
./cook -C "$kitchen" --batch-approve io.github.lmarqs.cookbook:git:8.0.0#worktrees
```

After the diff, the output includes:

```text
Created .gitignore
Created .worktrees/README.md
Created .worktrees/setup
Created .worktrees/teardown
Created cook.lock
```

Update the recipe by its version-less lock name. The newest 8.x release in this repository is `8.2.0`:

```sh
./cook update -C "$kitchen" --batch-approve io.github.lmarqs.cookbook:git#worktrees
```

Output through the write summary, before the recipe's yield text:

```text
Update io.github.lmarqs.cookbook:git#worktrees 8.0.0 -> 8.2.0
--- a/cook.lock
+++ b/cook.lock
@@ -2,7 +2,7 @@
 version: 1
 recipes:
   - name: "io.github.lmarqs.cookbook:git#worktrees"
-    source: "io.github.lmarqs.cookbook:git:8.0.0#worktrees"
+    source: "io.github.lmarqs.cookbook:git:8.2.0#worktrees"
     sha256: "f7213b67b20747c9597662c99d7a183fe26c65d48816e98d8b76776ac31b2895"
     arguments: {}
     owns:

Updated cook.lock
```

The command exits 0. Here `--batch-approve` displays the diff and writes without prompting. Omit it to review and
answer the approval prompt interactively.

A compatible dry run now holds the recipe at `8.2.0`, even though `9.0.0` is available:

```sh
./cook update -C "$kitchen" --dry-run io.github.lmarqs.cookbook:git#worktrees
```

```text
Holding io.github.lmarqs.cookbook:git#worktrees 8.2.0; 9.0.0 needs --major
Already at the newest releases.
```

This exits 0: there is no newer release in the default range. Preview crossing that range with `--major`:

```sh
./cook update -C "$kitchen" --major --dry-run io.github.lmarqs.cookbook:git#worktrees
```

```text
Update io.github.lmarqs.cookbook:git#worktrees 8.2.0 -> 9.0.0
--- a/cook.lock
+++ b/cook.lock
@@ -2,7 +2,7 @@
 version: 1
 recipes:
   - name: "io.github.lmarqs.cookbook:git#worktrees"
-    source: "io.github.lmarqs.cookbook:git:8.2.0#worktrees"
+    source: "io.github.lmarqs.cookbook:git:9.0.0#worktrees"
     sha256: "f7213b67b20747c9597662c99d7a183fe26c65d48816e98d8b76776ac31b2895"
     arguments: {}
     owns:

Would update cook.lock
```

This exits 1 because the recipe would move; nothing was written. To accept the move to the newest release:

```sh
./cook update -C "$kitchen" --major --batch-approve io.github.lmarqs.cookbook:git#worktrees
```

Output through the write summary, before the recipe's yield text:

```text
Update io.github.lmarqs.cookbook:git#worktrees 8.2.0 -> 9.0.0
--- a/cook.lock
+++ b/cook.lock
@@ -2,7 +2,7 @@
 version: 1
 recipes:
   - name: "io.github.lmarqs.cookbook:git#worktrees"
-    source: "io.github.lmarqs.cookbook:git:8.2.0#worktrees"
+    source: "io.github.lmarqs.cookbook:git:9.0.0#worktrees"
     sha256: "f7213b67b20747c9597662c99d7a183fe26c65d48816e98d8b76776ac31b2895"
     arguments: {}
     owns:

Updated cook.lock
```

The command exits 0 and records `9.0.0`. A dry run without names now checks the whole scope:

```sh
./cook update -C "$kitchen" --dry-run
```

```text
Already at the newest releases.
```

This exits 0. Commit the updated `cook.lock` and any output changes after reviewing them.

## Reference

```text
./cook update [-C <dir>] [--dry-run] [--batch-approve] [--major] [<name>]...
```

Put options after `update` and before the first name. `-C <dir>` or `--kitchen=<dir>` selects the kitchen. Update
takes no recipe arguments and accepts neither `--force` nor `--skip`.

### Scope

Without names, update considers every coordinate recipe in `cook.lock`, in lock order, and skips path recipes.
With names, it considers only those recipes, in the order given. Use [lock names](cli.md#lock-names), such as
`io.github.lmarqs.cookbook:git#worktrees`, without versions or recipe arguments. Naming a path recipe is an error:
it has no version to update.

Both forms require `cook.lock`. A duplicate name or a name absent from the lock refuses the whole request before
writing anything. Recipes with no newer release stay as they are; only recipes moving to newer releases are cooked.
Moves replay normalized supplied arguments from the lock; the new recipe supplies any omitted defaults. A moving
recipe whose arguments were never recorded needs an ordinary cook with its arguments first. `arguments: {}` is a
valid record of no supplied arguments.

### Compatible range

By default, update chooses the numerically highest release newer than the recorded version within the same major.
Below `1.0.0`, it stays within the same nonzero minor; `0.0.x` stays pinned:

| Recorded version | Allowed newer releases by default |
| --- | --- |
| `8.0.0` | Any newer `8.x.y`, such as `8.2.0`, but no `9.x.y` |
| `0.4.2` | Any newer `0.4.x`, but no `0.5.x` or `1.x.y` |
| `0.0.3` | None; `0.0.x` stays pinned by default |

`--major` removes these range limits, including the 0.x limits, and chooses the newest release above the recorded
version. Update never downgrades. If no move is available within the range but a newer release exists outside it,
the CLI prints a `Holding` line naming that release and `--major`.

### Where releases come from

Update combines release listings from all cookbook repositories configured in the kitchen's `cook.toml`, or Maven
Central when there is no `cook.toml`. Maven repositories read the artifact's `maven-metadata.xml`; when that file is
absent they try `maven-metadata-local.xml`. An absent listing contributes no releases. Cook accepts only numeric
`major.minor.patch` releases and compares the three components numerically, ignoring snapshots, prereleases and
other strings. After selection, the first configured repository holding the chosen artifact supplies its sources,
as in ordinary cooking.

### Unknown recipes

A cookbook source directory serves current files at any version and cannot list releases. If any repository cannot
list releases for an artifact it serves, its recipes are unknown to update, even if another repository has a listing
for that artifact. See the [source-directory trap](pitfalls.md#a-cookbook-source-directory-makes-its-recipes-unknown-to-update).

Without names, update warns on stderr, naming each skipped recipe and repository, and continues with the known
recipes. With names, an explicitly named unknown recipe refuses the whole request before writing anything and
exits 2. Use a repository that lists releases, or cook the exact release by its coordinates. A source directory
still supplies its current files regardless of the version in those coordinates.

### Arguments that no longer fit

If recorded arguments no longer bind to a new release, update refuses the whole batch without writing anything.
The diagnostic identifies the new coordinates, the accepted arguments and the recorded argument line. Read the new
release with the `help` command shown in the diagnostic, then cook its exact coordinates with suitable arguments.
Update accepts no replacement recipe arguments of its own.

### Dry runs, approval and recovery

`--dry-run` shows the moves and the cooking diff without writing or asking for approval. It exits 1 if any recipe
would move, 0 if none would, and 2 on an error. A skipped unknown recipe is a warning; it does not by itself make
the exit status nonzero. The status depends on release moves, even if a move changes only `cook.lock`.

A normal update exits 0 after approved writes or when no move is available, 1 for declined or missing approval,
and 2 on an error. Update uses cooking's confirm texts, terminal diff, whole-batch approval, yield texts and
write-failure rollback; see [review and recovery](cook.md#review-and-recovery). `--batch-approve` shows the review
and writes without prompting. Noninteractive execution requires that explicit approval to write.
