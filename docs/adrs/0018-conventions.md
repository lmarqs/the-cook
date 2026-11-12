---
description: Records why instruction files state conventions that read true without Cook.
audience: Whoever writes instruction text a recipe generates
---
# 0018. Native instruction text states conventions independently of Cook

Teammates and coding agents need to understand repository conventions even when they do not use the tool that configured
them. Instructions can become misleading when they assume that tool is always present, and long instruction files add
reading cost to every session. Repository conventions remain readable without Cook, with concise instructions that
explain the actual setup readers encounter.

## Context

- The [architecture](../architecture.md#ingredients-recipes-and-cookbooks) uses native files that configured programs
  consume directly, without invoking Cook to read them.
- Worked example: `acme/api` uses a recipe to write a marked section in an agent instruction file describing its test
  command, and a section in its ignore file. A teammate without Cook can read the instructions and use the command.
- People and coding agents learn a repository's rules from instruction files the team keeps for them, such as a
  README for people and an agent instruction file for agents. A rule a recipe sets up, such as where worktrees go,
  reaches them only through those files.
- A path remains ignored while an ignore entry covers it. Deleting the last covering entry can expose existing files
  as untracked; deleting an ignore entry does not delete those files.

## Decision

1. Recipe-declared instruction text states the repository's conventions as they hold for a reader without Cook: it
   describes the configured program or the repository's practice directly, and names Cook only where a reader needs
   to invoke it. The engine enforces no length bound on that text; its wording and length follow the
   [generated-content standard](../ux/generated-content-standards.md#instruction-files-and-conventions).
2. Instruction text and ignore entries in shared files are [sections](../features/glossary.md) bounded by markers
   that name the owning recipe and region ([0007](0007-ownership.md)). Text outside a section is preserved
   ([0008](0008-native-configuration.md)).
3. Instruction sections and ignore entries follow the ordered composition and ownership of
   [0007](0007-ownership.md): cooking another recipe leaves them in place, and a recipe's new version removes the
   ones its previous version owned and no longer declares, even when their content was edited. Removing an ignore
   entry leaves the paths it describes in place.
4. Stopping use of Cook needs no operation that strips Cook-specific text: native files, their markers and
   `cook.lock` can stay as they are. Removing a recipe's sections and entries is a [discard](../features/discard.md).
5. A recipe states the rules it sets up, such as where its files are and how to use them, as a section in the
   instruction files the person names ([0030](0030-recipe-connections.md)).

## Reasons

- **1.** Instructions about repository practice and instructions for invoking Cook have different readers and
  dependencies. The example's teammate needs the test command, not an account of how it was generated. Direct, concise
  wording serves readers without requiring Cook. Recipe authors remain responsible for the relevance and length of their
  text.
- **2.** Boundaries let a recipe address one section without rewriting the rest of a shared file, and manual edits
  remain part of current-content review. [0007](0007-ownership.md) gives the reasons for markers and what they name.
- **3.** Explicit operations keep omission from becoming implicit deletion. Ignore entries affect visibility of paths,
  so their deletion must be reviewed even though it leaves those paths in place. Overlapping declared targets still
  follow order: the last recipe to write a target wins and becomes its owner.
- **4.** Native files already outlive the configuring invocation. Sorting text into what to keep and what to strip on
  exit would add cleanup semantics that the agreed cooking and discard contract does not need. A reviewer decides
  whether a particular deletion leaves instructions that accurately describe the remaining setup.
- **5.** Otherwise every repository that cooks a recipe writes the same few lines by hand on where its files are and
  how to use them, and those lines drift when the recipe changes. Owned, they change with the recipe's arguments and
  version. The trade is a section the team does not edit by hand, since the recipe owns it
  ([0007](0007-ownership.md)).

## Rejected alternatives

### State conventions only in recipe sources

At its best, each choice is described once. It loses because it leaves people and agents reading native instructions
without the information they need; recipe sources are not the consuming program's instruction file.

### Classify sections for automatic retention or removal

At its best, cleanup can keep conventions while removing instructions tied to the configuring tool. It loses because it
requires a stored class for every section and a separate reconciliation step. Removal by a recipe's new version and
reversal of a batch instead expose exactly what will change for approval.

### Enforce a fixed instruction-size budget in the engine

At its best, every session gets a bounded amount of generated text. It loses because it adds an unrequested enforcement
system and cannot decide which repository instructions are useful. Concision remains authoring guidance.

## Consequences

Costs:

- Recipe authors must keep instruction text concise and accurate for its readers.
- Deleting instructions or ignore entries can leave retained files unexplained or visible to version control; review
  must consider the remaining setup.
- Keeping native files after stopping Cook leaves future edits to people or another configuring invocation.

Must be maintained:

- Clear native instruction text and format-compatible section boundaries.
- Reviewable instruction and ignore-file changes that preserve unrelated content.

## Confirmation

- Read the example's resulting native files without Cook: the repository convention remains understandable.
- Review a Markdown section update: HTML comment markers bound the change and unrelated text remains intact.
- Review a later unrelated run: the earlier instruction and ignore sections remain in place.
- Review a recipe that sets up a rule: each instruction file the person named holds a section stating it.
- Review a recipe's new version that stops declaring an edited section and an ignore entry: the actual deletions
  appear for approval, with no automatic deletion of the described path or restoration of earlier text.
