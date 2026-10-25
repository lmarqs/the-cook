---
objective:
  problem: >-
    A team commits the files its recipes write together with the record of how they were written. Someone edits an
    input, such as a policy, and commits without cooking, so the committed files no longer follow from the record. A
    continuous integration step that repeats each recipe's version and arguments must be edited on every upgrade or
    new argument, and fails or passes for the wrong reason when nobody edits it.
  goal: >-
    One unchanging step tells a team whether its committed files follow from the record, and a script can tell a
    difference from a failure.
---
# 0031. Check compares the kitchen with what cook.lock records

## Context

- `cook.lock` records each recipe's source, sha256 and normalized supplied arguments, including defaults given
  explicitly, and replicate cooks a recipe again from them ([0007](0007-ownership.md)).
- A step that cooks with the version and arguments written in it must repeat every value the lock holds, such as
  `io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex`. The lock is part of the diff,
  so a step that omits an explicit default, or names the version before an upgrade, fails on a kitchen that matches
  its lock.
- An operand with a version names a choice the lock overrides, since replicate cooks the version the lock records.
- A CI step acts on a kitchen that differs from its lock by cooking, and on a cookbook that fails to resolve by
  investigating, so it needs to tell the two apart.
- The [CLI standard](../ux/cli-standards.md#command-grammar) gives each argument one spelling, and makes an action other
  than cooking a command.

## Decision

1. `./cook check [<name>]...` cooks each named recipe, or every recipe `cook.lock` lists in its order, again from the
   source and arguments the lock records, writes nothing, and exits 1 when anything would change. It shows changed
   sources and the diff, takes no batch option, and refuses a kitchen without `cook.lock`.
2. Cooking has no check option, and replicate has none: `check` is the only way to ask the question.
3. Check, replicate and discard take a recipe only by its lock name, and refuse versioned coordinates or a `.groovy`
   path with the lock name to write. Replicate without names cooks every recipe the lock lists; discard always takes
   names.
4. Cook exits 0 when it did what was asked, 1 when nothing was written because approval was declined or missing or
   check found a difference, and 2 on any error.

## Reasons

- **1.** The lock is the record the committed files must follow from, so the check reads its inputs from the lock and
  the step names no version or argument that a later cook could leave behind. Checking every recipe by default covers
  a recipe cooked after the step was written. Check runs replicate's evaluation, so its report is what
  `./cook replicate` proposes, and its diagnostic names that command as the fix. The trade is that output a clone does
  not commit is absent from a fresh clone, so a CI step names the recipes whose output is committed.
- **2.** A check of supplied inputs answers what a cook would change, which `--dry-run` already shows; a status for it
  has no caller. A second spelling of check on replicate would be one meaning with two forms to document and test.
- **3.** One lock entry has one name. A versioned operand that the lock overrides reads as a choice of version that
  is not made, and a stale version in a script passes without notice.
- **4.** A caller acts differently on a difference, which a cook fixes, and on a failure, which it must investigate.

## Rejected alternatives

### A check option on cooking and on replicate

At its best, the option reads as a mode of a command the person already knows, as in formatters' check options. It
loses because cooking's form makes every step repeat the lock's inputs, and keeping it beside a replicate option gives
one question two spellings.

### Skip output a clone does not commit

At its best, a lock-wide check passes in a fresh clone without naming recipes. It loses for now because Cook would
have to ask the version control system what it ignores, which differs between clones, or recipes would have to
declare such output and the lock record it. Naming the recipes in the step costs one line.

## Consequences

- A CI step is `./cook check`, or `./cook check <name>...` when some recipe's output is not committed, and it does
  not change when a recipe is upgraded or cooked with other arguments.
- Scripts that discarded or replicated by versioned coordinates or `.groovy` paths name the lock name instead.
- Scripts that read status 1 as any failure read status 2 for errors.

## Confirmation

- In a kitchen that matches its lock, `./cook check` prints `Already cooked.` and exits 0; after a hand edit to an
  owned section it prints the diff, exits 1 and names `./cook replicate`, and the file stays as it was.
- `./cook check` in a directory without `cook.lock` exits 2.
- `./cook replicate acme:notes:1.2.3#notes` exits 2 with `write acme:notes#notes`.
- `./cook --check` exits 2 as an unknown option.
