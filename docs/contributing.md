---
description: States the tenets a proposal for Cook is judged against.
audience: Whoever proposes a new responsibility for Cook
---
# Contributing

A proposal for Cook is judged against its purpose: turn reusable recipes into visible, approved changes to native
program files, while keeping program-specific setup separate from the engine. A contributor who knows which part of
the problem Cook solves can tell whether a new responsibility serves it. [Philosophy](philosophy.md) states the
problem, who has it and what they get; the tenets below rule proposals in or out, and the [ADRs](adrs/README.md)
record the reasoning behind the decisions they shape. The [architecture](architecture.md) states the full design, and
the [current implementation](../README.md#current-implementation) describes what runs today.

## Tenets

### Start from the objective

**Problem.** A proposal without a stated purpose is judged against whatever purpose its reader assumes, and can
outlive the reason it was made.

**Tenet.** State the problem and desired outcome before the mechanism. Keep the reasons visible so a contributor can
judge whether a decision still serves its goal. See the [ADR structure](adrs/README.md#the-problem-comes-first).

### Keep the engine focused on file operations

**Problem.** A core that owns a team's development lifecycle must change whenever that way of working changes.

**Tenet.** Cook evaluates recipes and reviews and performs their declared file operations. The agents, loop
runners and CI systems it configures run and control their own workflows. See the
[role of the tool](adrs/0001-role-of-the-tool.md) ADR.

### Trust recipes explicitly

**Problem.** Approval of a diff can be mistaken for a guarantee about everything executable recipe code might do.

**Tenet.** Recipes are trusted Groovy code. Approval, destination boundaries and rollback govern declared batch
operations; they do not sandbox recipe evaluation or guarantee that arbitrary recipe code has no side effects. Choose
recipe sources accordingly. See the [cookbook trust](adrs/0026-cookbook-trust.md) ADR.

### Keep program models in cookbooks

**Problem.** Program-specific setup tied to the engine makes the engine follow each program's configuration changes.

**Tenet.** Ingredients model native program files and configuration, and cookbooks distribute ingredients and recipes
separately from the engine. See the [organization content](adrs/0013-organization-content.md) ADR.

### Review the actual change

**Problem.** Hidden reconciliation or refusals based on manual edits prevent readers from judging the change they
requested against the files they have now.

**Tenet.** Evaluate supplied recipes against current content on every invocation that cooks them and show their combined
effect as one diff, approved as a whole. Ownership, recorded in the committed `cook.lock`, never blocks a change, and
cooking one recipe never removes another recipe's output. The cost is that when a later recipe takes over a target,
the diff is the only notice. See [ordered composition](architecture.md#ordered-composition-and-declared-targets)
(FR-7 to FR-9) and the [ownership](adrs/0007-ownership.md) ADR.

### Make recovery part of writing

**Problem.** A failed write can leave only part of the approved change, and removing a recipe's output can be
confused with restoring the past.

**Tenet.** Roll back a failed batch so the files stay as they were. Discard removes exactly what a recipe owns and
restores nothing; earlier content belongs to the repository's history. See
[batch approval and failure recovery](architecture.md#batch-approval-and-failure-recovery) (FR-13, FR-15, FR-25),
[cooking](features/cook.md) and [discard](features/discard.md).

### Let native configuration stand on its own

**Problem.** A setup tool can become an unnecessary dependency of every program it configures.

**Tenet.** Programs read the native files Cook writes directly, and teammates keep their own editors, agents and ways
of working. See the [dependence on the binary](adrs/0010-dependence-on-the-binary.md) and
[place of the tool](adrs/0028-place-of-the-tool.md) ADRs.
