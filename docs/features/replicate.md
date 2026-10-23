---
objective:
  problem: >-
    A fresh clone keeps `cook.lock` but can lack a recipe's ignored output. A developer wants to recreate
    those files without finding and retyping the arguments used to cook them.
  goal: >-
    Replicating named recipes cooks them again from the sources and arguments recorded in `cook.lock`, with
    one diff and approval for the whole batch.
---
# Replicate recipes

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

Replicate loads the recorded version and replays `--harness=claude --share=clone`; no arguments follow the operand.
Approve the diff to create that clone's `.ralph-tui/`. A generated file's opening comment also shows the arguments to
a reader, but replicate reads them from the lock, so it works when the file is absent.

## Reference

```text
./cook replicate [-C <dir>] [batch-options] <recipe>...
```

Each recipe is written as its short name in `cook.lock`, such as `group:artifact#recipe` or `recipes/setup`, versioned
coordinates `group:artifact:version#recipe`, or a `.groovy` path in the kitchen. The operand only picks the lock
entry: replicate loads the recorded source and version even when the operand names another version. A path recipe's
source file must still exist, because replicate recreates output, not recipe sources.

Replicate takes one or more named recipes and no recipe arguments. It cooks them in the order given, since
`cook.lock` records neither the order nor the grouping of the original invocation. When recipes write the same
target, choose the order deliberately; the last writer wins, as in [ordered operations](cook.md#ordered-operations).

Replicate refuses the whole request before writing anything if a recipe is named twice, is absent from `cook.lock`,
has no recorded arguments, or records them in a form Cook cannot read. An entry without an `arguments` field needs one
ordinary cook with its arguments; an unreadable field goes away when the recipe is cooked again or discarded.
`arguments: {}` means no arguments were supplied, and replicate accepts it. The lock records supplied values,
including defaults given explicitly; the recipe fills in the other defaults when it runs.

### Source drift and changed arguments

Replicate loads sources through the kitchen's current cookbook repositories, or from the recorded local path. A lock
hash is only for comparison; Cook keeps no copy of the source. If the source sha256 differs, review starts with a
`Changed source <address>` notice showing the recorded and current sha256 values before the diff. This covers both
a release whose sources changed under the same version and an edited path recipe. The run continues after the notice,
and approving the diff accepts the output and updates the lock.

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
