---
objective:
  problem: >-
    People and coding agents read these pages to change Cook, and each needs to know which page owns a fact before
    reading or writing it. A fact stated on two pages drifts, and a reader who finds the stale copy acts on it.
  goal: >-
    A reader can tell from this page which page answers a question, and a writer can tell where a new fact goes, so
    each fact is stated once and linked from the rest.
---
# Cook documentation

This is the map of `docs/`: the kinds of page this repository keeps, when a fact belongs to each, and what a good page
of each achieves. Read it before opening or writing a page. The pages here describe the intended design; how to build
and run the code today is in the [root README](../README.md), the [core guide](../core/README.md) and the
[infra guide](../infra/README.md), and the cookbook documents itself, as [cookbook units](ux/messages-and-docs.md#cookbook-units)
says.

## Authority

[Architecture](architecture.md) states the intended design, and every other page follows it. When a decision, a
standard or a feature page conflicts with the architecture, the supporting page is corrected: an earlier decision does
not override the agreed design. A page may describe the design before the code implements it; the gap is tracked in an
issue, never on the page, and a description of current behavior in a README stays true.

## Kinds

| Kind | A fact belongs here when | A good page |
| --- | --- | --- |
| [Architecture](architecture.md) | It is about the whole engine: its responsibilities, operations, boundaries and distribution, and the numbered requirements | Names each part and how it depends on the others, so the other pages can cite it |
| [Philosophy](philosophy.md) | It is a goal or tenet that rules out proposals across the design | Lets a reader judge a new responsibility for Cook against its purpose |
| [Decisions](adrs/README.md) | Reversing it changes more than one feature or module, or is hard to reverse, and it came from a real trade-off | States the decision, every reason behind it and each alternative at its best; the [guide](adrs/README.md) gives the bar and the template |
| [Features](features/README.md) | It states what a command or operation does for its user: cooking, check, discard, replicate, update, the wrapper | Lets a reader predict what a command proposes, records and refuses without reading the code |
| [Standards](ux/README.md) | It constrains every artifact of one sort that a person writes: a command, a message, a configuration file, generated content, a skill, a page | Carries each rule with its reason, so it applies to a case its words leave open |
| [Pitfalls](features/pitfalls.md) | It is a trap: a setup that looks right and is not, or a guarantee a reader assumes Cook gives and it does not | Gives the symptom, why it happens, what Cook is responsible for and what the reader reviews |
| [Glossary](features/glossary.md) | It is the meaning of one of Cook's terms | Gives each concept one name, which every page uses |

Every page opens with its objective in front matter, as [the objective rule](ux/messages-and-docs.md#every-document-opens-with-its-objective)
says; `./gradlew -p docs check` enforces it and resolves every relative link and anchor.

## Where a fact goes

The kind is the question a reader brings, never the subject: one subject feeds several kinds. A section of a shared
file that a recipe owns is an example:

- That a recipe owns its output, recorded in `cook.lock` and reviewed against current content, and why, is a
  [decision](adrs/0007-ownership.md).
- What cooking does with a section, and what it keeps around it, is a [feature](features/cook.md#sections-and-markers).
- How a marker line reads in every file format is a [standard](ux/generated-content-standards.md#sections-and-markers).
- That the next run replaces a hand edit inside a section is a
  [pitfall](features/pitfalls.md#a-manual-edit-is-changed-by-the-next-run).
- What "owner" and "marker" mean is the [glossary](features/glossary.md)'s.

Each page links the others rather than restating them.

## Misfilings

- A command, flag or file field written as a decision. Giveaway: its alternatives had to be invented; it belongs on a
  feature page or in a standard.
- Implementation progress written into a page. Giveaway: "not yet", a phase or a status; it belongs in an issue.
- Build and run instructions for today's code written here. Giveaway: steps a user follows to build or run the current
  code; they belong in the root or module README.
- A rule restated from the page that owns it. Giveaway: the same sentence on two pages; one of them becomes a link.
