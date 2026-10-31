---
name: docs-layout
description: Writes and reviews the pages of docs/, the source of truth people and agents share, by the descriptions in its map, docs/README.md. Use it to record a decision; describe a feature, an interface, a standard, the observability of a system, a runbook, the architecture or the tenets; add a glossary term; declare a kind of the team's own; review or fix docs/; or adopt the layout in an existing repository. Code comments and READMEs outside docs/ follow the repository's own guidance.
argument-hint: "[adopt | review [--fix] | <kind> [title] | term <term> | kind <name>]"
---

# docs-layout

Each fact reads best on one page, its **owner**: the page whose kind's description it fits. The owner states the fact;
every other page links it. `docs/README.md`, the **map**, describes the kinds the repository offers and what a good
page of each achieves. Read the map at the start of every task here: this skill says how to work, the map says what
goes where.

## Invoked with arguments

| Arguments | Do |
| --- | --- |
| `<kind> [title]` | [Write a page](#write-a-page) of that kind, such as `adr`, `features` or `standards` |
| `term <term>` | [Add a term](#add-a-term) to the glossary |
| `kind <name>` | [Declare a kind](#declare-a-kind) the map does not offer |
| `review` | [Review](#review) the tree and report; files stay as they are |
| `review --fix` | Review, then [fix what it finds](#review---fix) |
| `adopt` | Follow [references/adopt.md](references/adopt.md) from its first step |
| None | Pick the procedure the request fits |

## Write a page

1. Match the subject to the map's descriptions. When another kind fits better, say so and write that kind. For a
   decision, check it against what the map says a good record holds; when it falls short, say where, and write the
   fact on the page it fits instead.
2. Search `docs/` for the subject, by its glossary term and its synonyms. When the subject already has an owner, edit
   that page.
3. Gather the facts from their sources: the code, the contract file, the version history, the issue and its review,
   the person. The page states what is true now and what the repository shows nowhere else, and links the source for
   the rest. What happened, as an issue thread or a review tells it, goes on the page: a tracker can move or close.
4. Ask the person for what only they know: first what was going wrong or could not be done, what it cost and what was
   tried; then what the kind needs, such as the options weighed for a decision.
5. For a kind of many pages, when `docs/<kind>/README.md` is absent, create it with the kind's name in the map as its
   title, such as `# Architecture decisions`.
6. Write the page from the kind's template, filling the blanks that apply and dropping a section the page has nothing
   for:

   | Kind | Template | File name |
   | --- | --- | --- |
   | `adr` | `adr.template.md` | `NNNN-<slug>.md`, one past the highest number ever used, four digits |
   | `features` | `feature.template.md` | `<slug>.md` |
   | `api` | `api.template.md` | `<slug>.md`, after the interface |
   | `standards` | `standard.template.md` | `<slug>.md`, after the sort of artifact |
   | `observability` | `observability.template.md` | `<slug>.md`, after the system or surface |
   | `runbooks` | `runbook.template.md` | `<slug>.md`, after the trigger |
   | `architecture` | `architecture.template.md` | `docs/architecture.md`, the kind's one page |
   | `philosophy` | `philosophy.template.md` | `docs/philosophy.md`, the kind's one page |
   | A kind of the team's own | None | As its declaration in the map says |

   Write the lead last, from the body: it says what the body says. The slug is the title in lowercase ASCII words
   joined by `-`. Each kind's reference holds its finer points and a worked example: [adr](references/adr.md),
   [features](references/features.md), [api](references/api.md), [standards](references/standards.md),
   [observability](references/observability.md), [runbooks](references/runbooks.md),
   [architecture](references/architecture.md), [philosophy](references/philosophy.md).
7. For a kind of many pages, add the page to its guide's list: `- [Title](file.md): one sentence on it.`
8. Connect it: where another page states this page's fact, replace the statement with a link. Use the glossary's
   terms.
9. Report the files written and each blank the person still has to supply.

## Change a decision

A changed decision is edited in place, keeping its number: the title and the file name's slug follow the new choice,
`Options` gains the previous choice and why it changed, and `Decision` states the new choice and why it won. Update
the links that point at it.

A record whose decision is reversed outright, or that holds less than a foundation decision, can be deleted: move its
facts that still hold to the page they fit, remove the file and its guide line, and point its links at that page.

## Add a term

1. When `docs/glossary.md` is absent, create it from `templates/glossary.template.md`.
2. Add the entry in the map's format; [references/glossary.md](references/glossary.md) has an example.
3. Replace the rejected synonyms in the pages with the term.

## Declare a kind

1. Ask the map's questions for a new kind about the facts that need one. When an answer points to a kind the map
   offers, or outside `docs/`, say so and stop.
2. Choose its shape: a directory when several pages share it, one page when there is one.
3. Write its declaration in the map, outside the section the recipe writes: the heading in the map's shape, what the
   kind holds, its Diátaxis type and what a good page of it contains.
4. Write its first page by that description, with its guide for a directory.

## Review

Compare every page under `docs/` with the map and with these signs of a page that serves its readers:

- It sits in a kind the map declares, or is the map or the glossary.
- Each kind of many pages has its guide, listing every page of the kind.
- Each link resolves, to a file and a heading that exist.
- It has what the map describes for its kind; each decision record is named `NNNN-<slug>.md`, its number used once.
- It uses the glossary's terms, not their rejected synonyms.
- Each fact is stated on its owner; other pages link it.
- It opens with its lead: who reads it and when, what it says, why it exists. A lead that restates the title says
  nothing.
- Its body says what its lead says, no more and no less.
- Each reason tells what happened before its conclusion.
- It states what is true now: spot-check its claims against the code and the contract.
- Front matter holds only keys a program reads.

Report each difference: what, where, the description it differs from, and the step that would close it. The team
decides which to close.

## Review --fix

1. Run the review and list every difference before changing a file.
2. Map each difference to a fix, or to a question for the person when it needs their decision, such as which page
   owns a fact, what happened when no source tells it, or whether a record is deleted.
3. Make the fixes the person agrees to.
4. Report what changed, and each difference that remains with its reason.

[references/design.md](references/design.md) gives the reason and the source behind each choice of the layout.
