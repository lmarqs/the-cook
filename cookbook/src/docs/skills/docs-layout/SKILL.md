---
name: docs-layout
description: Teaches where a fact goes in docs/, the source of truth people and agents share, and why. Writes the map, docs/README.md, with the team; writes and reviews pages by it; adds glossary terms; adopts an existing docs tree. Use it to record a decision, describe a feature, an interface, a standard, what telemetry shows, a runbook, the architecture or the tenets, to place a fact you are unsure about, or to review docs/. Code comments and READMEs outside docs/ follow the repository's own guidance.
argument-hint: "[map | adopt | review [--fix] | <kind> [title] | term <term>]"
---

# docs-layout

Each fact reads best on one page, its **owner**; every other page links it. The **map**, `docs/README.md`, says which
kinds of page this repository keeps and what a good page of each achieves. This skill teaches how to place a fact and
write a page that serves its reader. The map says what is true here.

## Three rules

**The map is this repository's, and where it and this skill differ, the map wins.** The pages that already exist of a
kind show its shape here: match them before anything in this skill. The examples in the references show one good
page each, in a made-up project, not the page to copy.

**A page never holds a placeholder.** Ask the person for what the sources do not give before writing. What stays
unknown is left out of the page and reported with it as an open question. A blank in a committed page reads as a fact
nobody knows, and it stays.

**An existing page is edited for its facts.** Change what is wrong, missing or stated twice. Change its structure only
when its shape fails a reader, and say how; never to make it look like an example or another page.

## Invoked with arguments

| Arguments | Do |
| --- | --- |
| `map` | [Write the map](#map) with the person, or add or remove a kind on it |
| `<kind> [title]` | [Write a page](#write-a-page) of that kind, such as `adr`, `features` or `standards` |
| `term <term>` | [Add a term](#add-a-term) to the glossary |
| `review` | [Review](#review) the tree and report; files stay as they are |
| `review --fix` | Review, then [fix what the team agrees to](#review---fix) |
| `adopt` | Follow [references/adopt.md](references/adopt.md) |
| None | Pick the procedure the request fits; with no map, start with `map` |

## Map

When `docs/README.md` is absent or names no kind, write it with the person.

1. Ask what the repository holds that its code, contracts and history show nowhere else, and what went wrong for want
   of it: decisions argued again, behavior nobody could state, a procedure found by trial. Look at what exists:
   `docs/`, the root README, wiki exports, long comments.
2. Pick the kinds those facts need, from the references: [adr](references/adr.md),
   [features](references/features.md), [api](references/api.md), [standards](references/standards.md),
   [observability](references/observability.md), [runbooks](references/runbooks.md),
   [architecture](references/architecture.md), [philosophy](references/philosophy.md). Fewer kinds read better than
   many; a kind with no facts yet waits.
3. Write the map by [references/map.md](references/map.md), in the team's language and words, with a worked example
   taken from this repository's own subjects.

When a map exists, add or remove a kind on it in the map's own words, after the questions in
[references/map.md](references/map.md#a-new-kind).

## Write a page

1. Find the owner. Search `docs/` for the subject, by its glossary term and its synonyms; when a page owns it, edit
   that page.
2. Place a new page by the map's questions. When **no kind fits**, say so, and offer the choices: outside `docs/`,
   such as the root README or a code comment; a kind the team declares; or nowhere. A forced kind misleads every
   reader who trusts the map.
3. Gather the facts from the code, the contract, the version history, the issue and its review, and the person. Ask
   first what went wrong or could not be done, what it cost and what was tried: that story is the reason a reader
   tests later, and only the person may know it.
4. Write the body, then the lead from it. Take the shape from the kind's existing pages; when there are none, from
   the map and the kind's reference.
5. For a kind of many pages, add a line to its guide, `docs/<kind>/README.md`, creating the guide with the kind's
   first page; link a new guide or one-page kind from the map.
6. Where another page states this page's fact, replace the statement with a link.
7. Report the files written and the open questions.

## Change a decision

A changed decision is edited in place and keeps its number; its title and slug follow the new choice, and the
options it records keep the previous choice and why it changed. A record reversed outright, or below the map's bar,
leaves: its facts that still hold move to the page they fit, and its links point there.

## Add a term

Add the entry to `docs/glossary.md`, creating it with its first term and linking it from the map;
[references/glossary.md](references/glossary.md) shows the format. Replace the rejected synonyms in the pages.

## Review

Compare each page under `docs/` with the map and with these signs of a page that serves its readers:

- It sits in a kind the map declares, or is the map or the glossary; it is reached from the map.
- Each link resolves, to a file and a heading that exist.
- It achieves what the map describes for its kind.
- Each fact is stated on its owner; other pages link it. It uses the glossary's terms.
- It opens with a lead: who reads it and when, what it says, why it exists. Its body says what the lead says.
- Each reason tells what happened before its conclusion.
- It states what is true now: spot-check its claims against the code.
- It holds no placeholder.
- Its kind was not forced: no section had to be invented to fit it.
- Its shape did not change in a commit that changed no fact.

Report each difference: where, the description it differs from, and the step that would close it. A page that
serves its readers in a shape of its own is not a difference. The team decides what to close.

## Review --fix

Review, list every difference, and ask the person about each that needs their decision, such as which page owns a
fact or what happened when no source tells it. Make the fixes they agree to, then report what changed and what
remains.

[references/design.md](references/design.md) gives the reason and the source behind each choice.
