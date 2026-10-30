---
name: docs-layout
description: Writes and reviews the pages of docs/, the source of truth people and agents share, by the criteria in its map, docs/README.md. Use it to record a decision; describe a feature, an interface, a UX standard, a runbook, the architecture or the tenets; add a glossary term; declare a kind of the team's own; review or fix docs/; or adopt the layout in an existing repository. Code comments and READMEs outside docs/ follow the repository's own guidance.
argument-hint: "[adopt | review [--fix] | <kind> [title] | term <term> | kind <name>]"
---

# docs-layout

Every fact has one **owner**: the page whose kind's criteria it meets. The owner states the fact; every other page
links it. `docs/README.md`, the **map**, holds the kinds the repository offers and their criteria. Read the map at the
start of every task here: this skill says how to work, the map says what goes where.

## Invoked with arguments

| Arguments | Do |
| --- | --- |
| `<kind> [title]` | [Write a page](#write-a-page) of that kind, such as `adr`, `features` or `architecture` |
| `term <term>` | [Add a term](#add-a-term) to the glossary |
| `kind <name>` | [Declare a kind](#declare-a-kind) the map does not offer |
| `review` | [Review](#review) the tree and report; files stay as they are |
| `review --fix` | Review, then [fix what it finds](#review---fix) |
| `adopt` | Follow [references/adopt.md](references/adopt.md) from its first step |
| None | Pick the procedure the request fits |

## Write a page

1. Match the subject to the map's criteria. When another kind fits better, say so and write that kind. Test a
   decision against the map's bar for a record first; when it fails a test, say which, and write the fact on the page
   that owns it instead.
2. Search `docs/` for the subject, by its glossary term and its synonyms. When the subject already has an owner, edit
   that page.
3. Gather the facts from their sources: the code, the contract file, the version history, the issue and its review,
   the person. The page states what is true now and what the repository shows nowhere else, and links the source for
   the rest. What happened, as an issue thread or a review tells it, goes on the page: a tracker can move or close.
4. Ask the person for what only they know: first what was going wrong or could not be done, what it cost and what was
   tried; then what the kind needs, such as the options weighed for a decision or the owner of a runbook.
5. For a kind of many pages, when `docs/<kind>/README.md` is absent, create it with the kind's name in the map as its
   title, such as `# Decisions`.
6. Write the page from the kind's template, filling every `[[fill: ...]]` blank:

   | Kind | Template | File name |
   | --- | --- | --- |
   | `adr` | `adr.template.md` | `NNNN-<slug>.md`, one past the highest number ever used, four digits |
   | `features` | `feature.template.md` | `<slug>.md` |
   | `api` | `api.template.md` | `<slug>.md`, after the interface |
   | `ux` | `standard.template.md` | `<slug>.md`, after the subject |
   | `runbooks` | `runbook.template.md` | `<slug>.md`, after the trigger |
   | `architecture` | `architecture.template.md` | `docs/architecture.md`, the kind's one page |
   | `philosophy` | `philosophy.template.md` | `docs/philosophy.md`, the kind's one page |
   | A kind of the team's own | None | As its declaration in the map says |

   Write the lead last, from the body: it says what the body says. The slug is the title in lowercase ASCII words
   joined by `-`. Each kind's reference holds its finer points and a worked example: [adr](references/adr.md),
   [features](references/features.md), [api](references/api.md), [ux](references/ux.md),
   [runbooks](references/runbooks.md), [architecture](references/architecture.md),
   [philosophy](references/philosophy.md).
7. For a kind of many pages, add the page to its guide's list: `- [Title](file.md): one sentence on it.`
8. Connect it: where another page states this page's fact, replace the statement with a link. A feature, interface
   or UX standard links the decision records that shaped it. Use the glossary's terms.
9. Report the files written and each blank the person still has to supply.

## Change a decision

A changed decision is edited in place, keeping its number: the title and the file name's slug follow the new choice,
`Options` gains the previous choice and why it changed, and `Decision` states the new choice and why it won. Update
the links that point at it.

A record whose decision is reversed outright, or that fails the map's bar, is deleted: move its facts that still hold
to the page that owns them, remove the file and its guide line, and point its links at that page.

## Add a term

1. When `docs/glossary.md` is absent, create it from `templates/glossary.template.md`.
2. Add the entry in the map's format; [references/glossary.md](references/glossary.md) has an example.
3. Replace the rejected synonyms in the pages with the term.

## Declare a kind

1. Apply the map's tests for a new kind to the facts that need one. When one fails, say which and stop: the facts
   belong to a kind the map offers, or outside `docs/`.
2. Choose its shape: a directory when several pages share it, one page when there is one.
3. Write its declaration in the map, outside the section the recipe writes: the heading in the map's shape, what the
   kind holds, its Diátaxis type and what each page must contain.
4. Write its first page by those criteria, with its guide for a directory.

## Review

Apply every rule below to every page under `docs/`:

- Each page sits in a kind the map declares, or is the map or the glossary.
- Each kind of many pages has its guide, listing every page of the kind.
- Each link resolves, to a file and a heading that exist.
- Each page has its kind's sections, in order; each decision record is named `NNNN-<slug>.md`, its number used once.
- No page uses a synonym the glossary rejects.
- Each fact is stated on its owner; other pages link it.
- Each page opens with its lead: who reads it and when, what it says, why it exists. A lead that restates the title
  is a finding.
- Each body says what its lead says, no more and no less.
- Each reason tells what happened before its conclusion; a `Context` or a lead made of conclusions alone is a finding.
- Each page states what is true now: spot-check its claims against the code and the contract.
- A feature page describes; its lead says why the unit exists, and it links the decision records behind it.
- Each decision record passes the map's bar; one that fails it is a candidate for deletion.
- `architecture.md` links the record behind each structural choice and states no reasons for them; each tenet in
  `philosophy.md` rules something out.
- Each decision record gives every option with why it lost, and why the choice won; a product named in it carries
  why that one.
- Each rule in a UX standard carries its reason or links the record that holds it.
- Each runbook step shows the output that tells the operator it worked.
- Front matter holds only keys a program reads.

Report each finding: what, where, and the step that fixes it.

## Review --fix

1. Run the review and list every finding before changing a file.
2. Map each finding to a fix, or to a question for the person when it needs their decision, such as which page owns
   a fact, what happened when no source tells it, whether a decision was reversed outright, or whether a record that
   fails the bar is deleted.
3. Make the fixes.
4. Report what changed, and each finding that remains with its reason.

[references/design.md](references/design.md) gives the reason and the source behind each rule.
