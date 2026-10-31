# Principles of the docs recipe

What the recipe stands for, each principle with its reason and the sources behind it, then the register of the
choices made from them. The `docs-layout` skill is built from this page: the skill carries the rules, the questions
and the giveaways an agent applies, and this page carries why. A change to the skill that breaks a principle here is
a change to this page first. The design is on issues #90 and #101 of `lmarqs/the-cook`, and its full decision
register on #90.

## A fact carries its story

A reason is told as what happened: the situation, what it cost, what was tried, then the conclusion. "We need a
queue" is a conclusion; "the payment call timed out and customers who retried were charged twice" is a reason a
reader months later can test to see whether it still holds. This is the trait the recipe exists to spread, and the
one an abstract page loses first. Sources: PEP 1, Motivation; Rust RFC template, Motivation; arc42 tips 9-3 and 9-6.

## One owner per fact

Each fact reads best on one page; other pages link it, with link text that names the fact. A copy drifts from its
original, and a restated value is what goes stale: outdated references were found in 28.9% of 991 top projects
(Tan et al. 2024). The code, contracts and version history own their own facts, so a page holds only what the
repository shows nowhere else.

## The kind is the question a reader brings, never the subject

A subject feeds several kinds: the session timeout is a feature rule, the shared session store is a decision,
clearing sessions is a runbook. An agent that sorts by subject or by wording files the store decision under the
sessions feature and the timeout rule under the decisions. Diátaxis names the same symptom in human authors: kinds "bleed into each
other" when sorted by form rather than by the reader's need, and its fix is a question put to each fact. The skill
therefore describes each kind by when a fact belongs there and by the misfilings with their giveaways, never by a
sorting rule.

## Teach by criteria and one example, never by a form

Models learn surface cues from the examples and labels in a prompt and apply them where the cue and the meaning part:
shortcut learning (Geirhos et al. 2020; McCoy et al. 2019; Niven and Kao 2019), spurious concept-label links in
in-context examples (Zhou et al. 2024), and label frequency, order and formatting moving classification by tens of
points (Zhao et al. 2021; Lu et al. 2022; Sclar et al. 2023). A template is a form to fill and gets filled: its
blanks survived into committed pages and pages were forced into its sections. Each kind therefore has criteria, what
a good page achieves, and one example marked as one good page, not the page.

## The team owns its map

The map the owner judged best was written by hand, in the team's words, with one worked example and the misfilings
that team meets. A cooked map was fought where it was adopted. The skill writes the map with the person, and where
the map and the skill differ, the map wins.

## Shape follows the readers

A rigid output format lowers the quality of what is written into it (Tam et al. 2024), and a page reshaped to an
example loses the structure and wording that served its readers. A kind's existing pages set its shape; an existing
page is edited for its facts, and its structure changes only when it fails a reader.

## A page never holds a placeholder

A blank in a committed page reads as a fact nobody knows, and stays. What the sources do not give is asked of the
person; what stays unknown is left out and reported.

## Structure appears with its first content

A directory, a guide or a glossary with nothing in it says something exists that does not. Diátaxis: "Don't do that.
It's horrible."

## Choices

Each choice, its reason and its source.

| Choice | Reason | Source |
| --- | --- | --- |
| The skill describes what a good page of each kind achieves; the team chooses | Semantics and heuristics help a team organize its docs, and a team that knows why a page is shaped can adapt it | The repository owner's decision, issues #98 and #101 |
| One map, `docs/README.md`, holds the descriptions | Readers and agents open one page first; descriptions written once stay consistent | *Software Engineering at Google*, landing pages; Google, "Duplication is Evil" |
| The recipe writes only the skill; nothing under `docs/` | A cooked section of the map was fought where it was adopted, translated by a table and argued with by a declared kind; a map the team writes in its words is the one it keeps | The repository owner's decision, issue #101 |
| The skill writes the map with the person, in the team's language, with an example from the repository | The map the owner judged best was written by hand: three kinds, each as a question, one worked example and the common misfilings | A hand-written map in a private repository; the repository owner's decision, issue #101 |
| The map wins over the skill, and a kind's existing pages set its shape | Agents put the recipe above the project and reshaped good pages to match it | An adoption in a private repository; issue #101 |
| No templates; each kind has one example page marked as one good page, not the page | Templates were copied, their blanks survived into committed pages, and pages were forced into their sections; models learn surface cues from examples and labels in the prompt and apply them where the cue and the meaning part | An adoption in a private repository; issue #101; Geirhos et al. 2020, *Shortcut learning in deep neural networks*; Zhou et al. 2024 on spurious concept-label links in in-context examples; Anthropic, *Prompting best practices* |
| The map names no section a page must have | A rigid output format lowers the quality of what is written into it, and a page reshaped to a form loses the facts its old shape carried | Tam et al. 2024, *Let Me Speak Freely?*; an adoption in a private repository |
| A page never holds a placeholder; what stays unknown is reported, not marked | A blank in a committed page reads as a fact nobody knows, and stays | An adoption in a private repository; issue #101 |
| An existing page is edited for its facts; its shape changes only when it fails a reader | Pages reshaped to an example lost structure and wording that served their readers | An adoption in a private repository; the repository owner's decision, issue #101 |
| A kind is described by when a fact belongs there, with the common misfilings and their giveaways | A question placed against a fact teaches; a sorting rule is applied literally | A hand-written map in a private repository; the `domain-modeling` skill's ADR test |
| Each kind says when a fact belongs there, naming in a clause where a near fact goes instead; a subject that spans kinds is the map's worked example | A boundary stated from one side only leaves the reader guessing at the other; a full routing table reads as orders | The repository owner's decision, issues #98 and #101 |
| The map links each guide, one-page kind and the glossary once it exists | A page with no incoming link is an orphan to readers, agents and link checkers; a link to a page not yet written is dead | The repository owner's decision, issue #100 |
| A reader gets what they need to act at the first level they open, and follows links for depth | Map, guide, lead, body and linked pages each answer whether to read on | Nielsen, "Progressive Disclosure", Nielsen Norman Group, 2006 |
| Each fact has one owning page; other pages link it | A copy drifts from its original | Google documentation guide; Write the Docs; outdated references in 28.9% of 991 top projects (Tan et al. 2024) |
| Kinds by the question a reader brings, each with a Diátaxis type; a subject feeds several kinds | Sorting by subject or wording files a decision under the feature it touches; kinds "bleed into each other" when sorted by form rather than the reader's need | Diátaxis, *The map* and *The compass*; GitHub's `internal-documentation-example`; an adoption in a private repository |
| Directories and guides appear with their first page | Empty structure misleads | Diátaxis: "Don't do that. It's horrible."; Pocock: create files lazily |
| A guide per kind lists its pages | A landing page lists what its directory holds | Diátaxis; GitLab `_index.md` |
| A page holds what the repository shows nowhere else | The code, contracts and version history hold their own facts without drift | Claude Code memory guidance; HumanLayer, "Prefer pointers to copies" |
| Front matter holds keys a program reads | Hand-kept fields go stale and mislead | MADR ADR-0013: front matter "pretends to be more accurate than it can be"; Good Docs Project: adopt only metadata "automatically maintained" |
| Reasoning is content: options, reasons, practices with reasons | The reasons let a reader judge whether a decision still holds | arc42 tips 9-3 and 9-6; MADR; PEP "Rejected Ideas"; Tang et al. 2005; Falessi et al. 2006 |
| A reason tells what happened before its conclusion | A conclusion alone hides the situation a reader tests to see whether it still holds | PEP 1, Motivation; Rust RFC template, Motivation; the repository owner's decision |
| Every page opens with its lead: who reads it and when, what it says, why it exists | A reader decides from the top whether the page concerns them, and a review judges the body against the lead | PEP 1, Abstract; Rust RFC template, Summary; the repository owner's decision |
| A decision record names the product chosen and why that one | The bar keeps the product out of a record of its own, so its reason has no other owner | The repository owner's decision |
| A decision record holds a foundation decision: reversing it changes more than one feature or module | Every record present reads as binding, so a record of an implementation choice freezes a detail that should stay easy to change | The repository owner's decision, stricter than Nygard's list, which also admits construction techniques |
| A changed decision is edited in place; a record reversed outright or below the bar is deleted | Every page present is in force, and the record keeps the previous choice among its options | The repository owner's decision; Google Cloud ADR guidance |
| Decision records are `NNNN-<slug>.md` under `adr/` | Numbers give each record a stable name | Nygard for the numbering; the repository owner's decision for `adr/` |
| One kind of standards, a page per sort of artifact, each practice with its reason | Standards split by category overlap, as a log record is both output and code; a practice sits where the person writing the artifact looks, and its reason lets a reader apply the intent to cases its words leave open | W3C "Why care?"; Google style guides; the repository owner's decision |
| A good standard makes what must not vary unmistakable, as BCP 14 keywords do | A reader tells a practice that admits exceptions from one that breaks something when it varies | RFC 2119, RFC 8174 |
| A standard holds what no check enforces | A check states its practice where it is enforced; a page restating it drifts | One owner per fact; the repository owner's decision |
| Architecture is one page: its parts and how they connect | It holds the shape no single file shows, and leaves practices, behavior and reasons to their kinds | arc42 building block and runtime views; the repository owner's decision |
| Philosophy is one page of tenets, each ruling something out and linking the records that apply it | A tenet that rules nothing out cannot guide a decision; the links show it applied | The repository owner's decision |
| A team declares a kind of its own in its map, after the questions | A kind that overlaps another gives a fact two owners; questions apply to any candidate, where a list of exclusions never ends | The map's own descriptions applied to a kind; the repository owner's decision |
| A copy of the tracker's state is a misfiling; a team that keeps a plan in `docs/` for its reasons declares a kind for it | State changes faster than pages and a copy drifts, while a program's phase order and reasons can outlive an issue | Issue #98; a roadmaps kind declared in an adoption; issue #101 |
| Link text names the fact; the value stays on its owner | The sentence reads without the hop, and a restated value is what goes stale | One owner per fact; Tan et al. 2024 |
| A feature page holds its known limits, each with its workaround | A limit is a fact about the behavior it qualifies, read with it and removed in the change that lifts it | One owner per fact; the repository owner's decision |
| An interface page links its contract file | The definition file is the contract, and the reference is built from it | OpenAPI Initiative; Google AIP-192 |
| One runbook per trigger, steps with expected output | The responder verifies each step under pressure | Google SRE workbook; AWS OPS07-BP03 |
| A runbook for a symptom names its causes and a remedy for each, and tells the causes apart first | A responder who checks before acting fixes the cause the symptom has, not the first one guessed | OASIS DITA 1.3 troubleshooting topic; Elastic troubleshooting content type |
| Observability pages hold what telemetry definitions leave out | The monitor definitions hold the queries; what a signal cannot tell apart and why a threshold sits where it does are written nowhere else | One owner per fact; the repository owner's decision |
| A glossary with rejected synonyms | One word per concept makes search and review find every use | Pocock; Good Docs terminology system; ASD-STE100 |
