---
description: Sets how messages are worded and how documentation pages are shaped and named.
audience: Whoever writes a message, help text or documentation page
---
# Messages and documentation

People and coding agents act on messages and documentation literally. Vague wording, inconsistent names or a missing
next step leads them to guess. Contradictory descriptions of commands and approval make it harder to judge what Cook
will change. A reader understands the requested operation, what approval covers and what to do next. Documentation gives
the intended design and current execution instructions in the places where readers expect them.

## Overview

These rules cover CLI messages, recipe-declared text in native files and repository documentation. They follow the
[architecture](../architecture.md): trusted Groovy recipes produce ordered file operations, reviewed as one terminal
diff with whole-batch approval. The [glossary](../features/glossary.md) defines engine vocabulary.

## Voice

| Rule | Write | Avoid |
| --- | --- | --- |
| Present tense; describe the operation | `cooking proposes additions and updates` | `cooking now replaces the old command` |
| Concise: one fact per sentence | `cook: settings.toml: cannot read file` | `It looks like there may be a problem reading your configuration` |
| Name the thing exactly | `settings.toml:14` | `your settings` |
| Imperative for instructions | `Move --batch-approve before ./recipe.groovy.` | `You may want to consider moving the option.` |
| Neutral, no blame | `cook: settings.toml: invalid value` | `You broke settings.toml.` |
| Plain words and no marketing | `writes native configuration` | `seamlessly configures everything` |
| Consistent terms | the [glossary](../features/glossary.md) term | alternating undo, rollback and discard for one operation |

The same rules apply to guidance read by coding agents, because ambiguity can become an unintended action.

## CLI messages

- Follow the [CLI output rules](cli-standards.md#streams-and-human-output). A diagnostic on stderr starts with
  `cook: `, the name of the command the caller typed, so readers can identify its source. `discard` and `help` add
  their command name, as in `cook: discard: `, and cooking adds none.
- On a suitable TTY, a message for a person may use the documented status icon instead of that prefix. Keep a status
  word, because an icon alone cannot explain the result
  ([terminal presentation](cli-standards.md#color-emoji-and-terminal-control)).
- Put supporting detail on indented lines. Identify the operation and affected file or source, because readers need
  to distinguish a recipe input failure from a target write failure.
- Name destination files relative to the invocation directory, with `:line:column` when known. Identify source files
  by their original location or coordinates, because that is the source to edit.
- Distinguish warnings from failures in words. Leave exit codes out of message guidance, because exit-code meanings
  are undecided ([errors](cli-standards.md#errors)).
- Do not emit ANSI escapes to non-TTY streams, because logs and transcripts must remain readable. Formatting and
  progress messages must not hide or replace the final batch diff.

## Error messages

Every actionable error explains three things, in this order:

1. **What failed:** the operation and object, with its path or name.
2. **Why:** the cause, including found and expected values when useful.
3. **Next:** the command, edit or decision that addresses it.

The following examples illustrate wording for the intended CLI:

```text
cook: unknown option --batch-approve after ./recipe.groovy
  batch options precede the first recipe operand
  run: ./cook --batch-approve ./recipe.groovy
```

```text
cook: target ../shared/settings.toml is outside the destination
  declared writes must stay inside the invocation directory, including symlink resolution
  edit ./recipe.groovy to target a path inside the destination
```

```text
cook: discard: cook.lock lists no recipe io.github.lmarqs.cookbook:agents#connect; nothing was written
  discard removes only what cook.lock records for a recipe
  check the recipe's name in cook.lock
```

Do not suggest discard as a way to restore earlier content: discard removes what a recipe owns, whereas earlier
content comes from the repository's history.

When approval needs a caller's decision, name the choices: run interactively to review and answer the prompt, or
explicitly preapprove with `--batch-approve` before the operands. Neither a noninteractive terminal nor the presence of
existing content authorizes writes. For example:

```text
cook: approval required; nothing was written
  the terminal is noninteractive and --batch-approve was not given
  run interactively to review the diff, or run: ./cook --batch-approve ./recipe.groovy
```

Keep normal diagnostics focused on information the caller can act on, rather than internal type names or paths inside
the package. When a target write fails, name the file that failed, state that the files the batch already wrote were
rolled back. Do not claim recovery of side effects outside the declared batch. For example:

```text
cook: cannot write .codex/config.toml: permission denied
  rolled back the files the batch already wrote
```

When a target cannot be written or withdrawn, the first line names it as `cook.lock` records it, relative to the
invocation directory: `<file> key <path>`, `<file> section <id>`, `<file>` or `<file> link`. Every line of the cause
follows indented two spaces beneath it, so a cause laid out as what, why and next keeps its why and next one level
deeper. For example:

```text
cook: io.github.lmarqs.cookbook:agents:0.0.0#connect: .agents/adapters/claude/settings.json key permissions
  JSON: Expected " at 4
    settings files are strict JSON: remove comments and trailing commas
```

## Output for automation and coding agents

Human prose is for readers; scripts must not treat its wording as a stable data format
([CLI output](cli-standards.md#machine-readable-output)).

Use the same operation names and approval scope in interactive and noninteractive guidance. Display the final diff even
with `--batch-approve`; that flag covers the whole batch. A coding agent reading a message must be able to tell whether
target writes occurred, whether rollback was needed and what information remains for recovery. Do not imply that batch
review sandboxes trusted recipe execution.

Cook writes native configuration that other programs consume. Guidance about a program's prompts, hooks or skill
discovery belongs in the cookbook configuration for that program.

## Generated text in human files

A recipe may declare a section in a shared instruction file. Markers bound the section and name its owning recipe
and region, `cook:begin <recipe>/<region>` and `cook:end <recipe>/<region>`
([generated-content-standards.md](generated-content-standards.md#sections-and-markers)). Keep instructions concise, avoid
duplicating surrounding guidance, and link to supporting detail where useful. State conventions using the values that
apply to the repository, so people and programs can understand them directly.

A section may contain conventions, explanations or Cook commands. Manual edits inside the
declared section appear in the next batch diff; unrelated text outside it remains intact. Use HTML comments for markers
in Markdown.

## Documentation structure

The [map of the docs](../README.md) names the kinds of page under `docs/`, where a fact goes and which page has
authority. The rules below hold for every page.

### Front matter describes the page

Every page under `docs/` opens with front matter that describes it, because a reader, or an agent choosing which files
to load, decides from the top of a file or from one listing over `docs/` whether the page holds what they look for.
The body holds what the page says, and opens with content right after the title.

```markdown
---
description: Explains how discard removes exactly what a recipe owns and keeps everything else in its files.
audience: Whoever stops using a recipe
---
# Discard recipes
```

- `description`, required: one sentence, at most 160 characters, opening with a verb that names the kind, as
  "Records why" for a decision, and unique across pages.
- `audience`, required: who opens the page and when, at most 100 characters.
- `keywords`, optional: a list of search terms the body lacks, such as an error text, a flag or a rejected synonym.

Values are plain text, because GitHub shows front matter as a table: no Markdown, no quotes, and no `: ` or ` #`,
which YAML reads as syntax. The title stays the `# ` heading, so no key repeats it. Status, dates and other fields
that change while the page does not stay out, because a hand-kept field goes stale while it still looks exact. Write
the front matter last, from the body, so a review can judge the body against it; a page whose description cannot be
written is not ready. `./gradlew -p docs check` fails a page whose front matter breaks these rules or whose title does
not follow it. A page's reason sits where its body explains it; ADRs open with their problem, as the
[ADR writing guide](../adrs/README.md#the-problem-comes-first) says. Cookbook units open with the lead of their doc
comment.

ADR facts follow under `## Context`; reference material and examples follow under their own headings, because readers
use the outline to find each kind of information.

### Feature pages

Use these sections where relevant, in this order, so readers find the same kind of information in the same place.
Omit empty sections.

| Section | Content |
| --- | --- |
| Overview | What it is and what it does |
| When to use | Situations it addresses and relevant alternatives |
| Reference | Decided commands, options, inputs, defaults and affected files |
| Examples | Complete invocations with prerequisites, or clearly identified design illustrations |
| Guidelines | Recommended practice and its reason |
| Troubleshooting | Likely symptom, cause and next action |

Do not fill reference tables with undecided exit codes, hook phases, patch formats, metadata schemas or model APIs.
An explanation of the guarantee is sufficient when its concrete representation needs separate design.

### Cookbook units

A recipe documents itself in the `@help`, `@confirm` and `@yield` annotations that open its file; a model in its class
and member doc comments.
A directory README is the artifact's front page. The shape is defined in [cookbook/README.md](../../cookbook/README.md).

A recipe's `@help` describes it by what it provides, consumes and connects to, because a reader composes recipes from
`./cook help` without their source. It holds these sections in order, and omits a section that does not apply:

1. **Lead:** what the destination gets, in one or two sentences.
2. **API:** the invocations, one per situation; the stable paths, scripts, commands and naming rules the recipe
   publishes.
3. **Consumes:** the tools that run at run time, with the versions written for; what it reads in the draft and what each
   read triggers; other artifacts' paths it names; what the person does by hand first.
4. **Produces:** `Target`, `Part`, `When` and `Kind` (`owned`, `seed` or `link`), closing with `Never touches:`.
5. **Connections:** each contact point and who makes it: Cook, between the recipe's own files; the person, by a
   parameter, for another recipe's or the team's files; the team; or the session. Then the order and the arguments
   that must agree.
6. **Run-time effects:** what the scripts it installs do when run.
7. **Re-cooking:** the arguments to repeat, what cooking a newer version changes, and how conditional parts behave under
   `./cook check`.
8. **Traps:** known faults of the versions written for.

What to do after cooking belongs in `@yield`, which `help` prints as "After cooking".
[Recipe texts](../../cookbook/README.md#recipe-texts) gives the content of each section.

### Writing rules for all documentation

- Use present tense for the intended design. In implementation guides, describe what actually runs and identify relevant
  limitations. Keep implementation progress in issues; Git retains earlier text.
- Make each page understandable when reached from search: define a term or link to the
  [glossary](../features/glossary.md) at first use.
- Support claims with accessible evidence. Avoid references to unnamed repositories or private conversations.
  An intended requirement comes from the architecture; implementation claims need implementation evidence.
- Follow an abstract rule with a concrete example when its meaning would otherwise be unclear. Label illustrative
  commands, diffs and output as examples of the intended interaction. Do not present invented output as a real run.
- Use relative links for repository pages, tables when they aid lookup, and language-tagged fenced blocks for commands
  and output. Link where useful in either direction.
- Use Cook or `the-cook` for the engine and `./cook` for the repository entry point. Show the equivalent global entry
  point when useful ([entry points](cli-standards.md#entry-points)). Use one term per engine concept.
- Use generic organizations such as `acme` in made-up examples. Recipe operands must still be exact supported paths or
  coordinates, such as `acme:setup:1.0.0#project`; do not invent a cookbook-name lookup scheme.
- Explain program-specific files and settings when they clarify native configuration. Attribute those choices to
  cookbook ingredients and recipes rather than promising engine contracts for every program. Name the program rather
  than inventing a generic role in its place.
- Keep commands aligned with the [CLI reference](../features/cli.md). Show batch options before operands and recipe
  arguments after their recipe. Document any real prerequisites in current-usage examples.
- Review affected links and index descriptions when changing a page. Retain useful paths; correct contradictory text in
  place.

### Names for engine concepts

The [glossary](../features/glossary.md) is the single list of engine terms; use its term for each concept, because
two names for one concept read as two concepts. A recipe is evaluated; a batch is reviewed, approved and written.

Words to avoid:

- block, managed block or block ID for a section, its markers or its region
- undo, revert or rollback for discard; rollback names only the recovery from a failed write
- apply or applying for cooking
