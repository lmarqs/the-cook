# Replicate recipes

## Objective

**Problem.** A fresh clone keeps `cook.lock` but can lack a recipe's ignored output. A developer wants to recreate
those files without finding and retyping the arguments used to cook them.

**Goal.** Replicating named recipes cooks them again from the sources and arguments recorded in `cook.lock`, with
one diff and approval for the whole batch.

This page follows the intended [architecture](../architecture.md#cooking-and-discard), which owns the requirements. For
current execution, see the [root README](../../README.md).

## Overview

[Replicate](glossary.md) reads each named recipe's source and normalized supplied arguments from the committed
`cook.lock`, then evaluates it against the kitchen's current content. It can recreate missing output or propose
updates to existing output. It runs trusted recipe code, just as [cooking](cook.md) does.

### A fresh clone with `--share=clone`

Cook the Ralph TUI recipe with an exact cookbook version, replacing `<version>` with that version:

```sh
./cook io.github.lmarqs.cookbook:ralph:<version>#ralph-tui --harness=claude --share=clone
```

After approving the diff, commit `cook.lock` and `.gitignore` along with the wrapper and any `cook.toml` needed to
resolve the cookbook. `--share=clone` keeps `.ralph-tui/` out of commits. A fresh clone therefore has the recorded
source and arguments but lacks the configuration, prompts and launcher. In that clone, run:

```sh
./cook replicate io.github.lmarqs.cookbook:ralph#ralph-tui
```

Replicate uses the recorded version and replays `--harness=claude --share=clone`. Review and approve the diff to
create that clone's `.ralph-tui/`. No recipe arguments follow the replicate operand. The recipe's opening comment
still shows its arguments to someone reading a generated file; replicate gets them from the lock even when that file
is absent.

## Reference

```text
./cook replicate [-C <dir>] [batch-options] <recipe>...
```

Each recipe is written as its short name in `cook.lock`, such as `group:artifact#recipe` or `recipes/setup`, versioned
coordinates `group:artifact:version#recipe`, or a `.groovy` path in the kitchen. These operands identify the lock
entry. Even if an operand supplies another version, replicate loads the recorded source and version. A path recipe's
recorded source must still be available; replicate recreates output, not recipe sources.

Replicate takes one or more named recipes and no recipe arguments. It cooks them in the order given, not the original
invocation's order: `cook.lock` does not record invocation grouping or order. When recipes write the same target,
choose the order deliberately; the last writer wins, as in [ordered operations](cook.md#ordered-operations).

The whole request is refused before any writes if a recipe is named twice, is absent from `cook.lock`, or has no
recorded arguments. An older lock entry without an `arguments` field needs one ordinary cook with its arguments
first. `arguments: {}` means no arguments were supplied and can be replicated. Recorded arguments contain supplied
values, including explicitly supplied defaults; unsupplied defaults come from the recipe when it runs.

### Source drift and changed arguments

Replicate loads sources through the kitchen's current cookbook repositories, or from the recorded local path. A lock
hash is a comparison point, not a stored copy of the source. If the source sha256 differs, review starts with a
`Changed source <address>` notice showing the recorded and current sha256 values before the diff. This covers both
a release whose sources changed under the same version and an edited path recipe. The notice does not refuse the
run; approval accepts the proposed output and updates the lock.

If the recorded arguments no longer bind to the recipe, the error shows the recorded argument line and asks you to
cook it with its arguments. Use ordinary cooking to choose new arguments or a new version, then commit the updated
lock and output.

### Approval, boundaries, and recovery

Replicate accepts `--dry-run` and `--batch-approve` before the first recipe, and `-C <dir>` or `--kitchen=<dir>` to
select the kitchen. It does not accept `--check`, `--force` or `--skip`. The [CLI reference](cli.md) defines these
options and their scope.

Replicate uses cooking's confirm texts, terminal diff, whole-batch approval, yield texts and write-failure rollback;
see [review and recovery](cook.md#review-and-recovery). Approval, destination boundaries and rollback cover declared
file operations, not arbitrary effects of trusted recipe code.
