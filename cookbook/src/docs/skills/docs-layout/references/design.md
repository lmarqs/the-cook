# Why the layout is shaped as it is

Each choice, its reason and its source. The design and its full decision register are on issue #90 of
`lmarqs/the-cook`.

| Choice | Reason | Source |
| --- | --- | --- |
| The recipe describes what a good page of each kind achieves; the team chooses | Semantics and heuristics help a team organize its docs, and a team that knows why a page is shaped can adapt it; the cooked text stays precise in what each kind is | The repository owner's decision, issue #98 |
| One map, `docs/README.md`, holds the descriptions | Readers and agents open one page first; descriptions written once stay consistent | *Software Engineering at Google*, landing pages; Google, "Duplication is Evil" |
| Each kind describes only its own pages; cases that span kinds are worked examples in the skill | A kind that routes facts to another says the same thing twice, and the map reads as orders instead of descriptions | The repository owner's decision, issue #98 |
| A reader gets what they need to act at the first level they open, and follows links for depth | Map, guide, lead, body and linked pages each answer whether to read on | Nielsen, "Progressive Disclosure", Nielsen Norman Group, 2006 |
| Each fact has one owning page; other pages link it | A copy drifts from its original | Google documentation guide; Write the Docs; outdated references in 28.9% of 991 top projects (Tan et al. 2024) |
| Kinds by subject, each with a Diátaxis type | One purpose per page; each type has its own shape | Diátaxis; GitHub's `internal-documentation-example` |
| The recipe writes the text known when it is cooked; the skill writes what comes with a page | Cooked text is owned, tested and updated by each release; text an agent copies varies and keeps the wording of the release it came from | The repository owner's decision |
| Directories and guides appear with their first page | Empty structure misleads | Diátaxis: "Don't do that. It's horrible."; Pocock: create files lazily |
| A guide per kind lists its pages | A landing page lists what its directory holds | Diátaxis; GitLab `_index.md` |
| A page holds what the repository shows nowhere else | The code, contracts and version history hold their own facts without drift | Claude Code memory guidance; HumanLayer, "Prefer pointers to copies" |
| Front matter holds keys a program reads | Hand-kept fields go stale and mislead | MADR ADR-0013: front matter "pretends to be more accurate than it can be"; Good Docs Project: adopt only metadata "automatically maintained" |
| Reasoning is content: options, reasons, practices with reasons | The reasons let a reader judge whether a decision still holds | arc42 tips 9-3 and 9-6; MADR; PEP "Rejected Ideas"; Tang et al. 2005; Falessi et al. 2006 |
| A reason tells what happened before its conclusion | A conclusion alone hides the situation a reader tests to see whether it still holds | PEP 1, Motivation; Rust RFC template, Motivation; the repository owner's decision |
| Every page opens with its lead: who reads it and when, what it says, why it exists | A reader decides from the top whether the page concerns them, and a review judges the body against the lead | PEP 1, Abstract; Rust RFC template, Summary; the repository owner's decision |
| A decision record's `Decision` names the product chosen and why that one | The bar keeps the product out of a record of its own, so its reason has no other owner | The repository owner's decision |
| A decision record holds a foundation decision: reversing it changes more than one feature or module | Every record present reads as binding, so a record of an implementation choice freezes a detail that should stay easy to change | The repository owner's decision, stricter than Nygard's list, which also admits construction techniques |
| A changed decision is edited in place; a record reversed outright or below the bar is deleted | Every page present is in force, and `Options` keeps the previous choice | The repository owner's decision; Google Cloud ADR guidance |
| Decision records are `NNNN-<slug>.md` under `adr/` | Numbers give each record a stable name | Nygard for the numbering; the repository owner's decision for `adr/` |
| One kind of standards, a page per sort of artifact, each practice with its reason | Standards split by category overlap, as a log record is both output and code; a practice sits where the person writing the artifact looks, and its reason lets a reader apply the intent to cases its words leave open | W3C "Why care?"; Google style guides; the repository owner's decision |
| Good standards make what must not vary unmistakable, as BCP 14 keywords do | A reader tells a practice that admits exceptions from one that breaks something when it varies | RFC 2119, RFC 8174 |
| A standard holds what no check enforces | A check states its practice where it is enforced; a page restating it drifts | One owner per fact; the repository owner's decision |
| Architecture is one page: its parts and how they connect | It holds the shape no single file shows, and leaves practices, behavior and reasons to their kinds | arc42 building block and runtime views; the repository owner's decision |
| Philosophy is one page of tenets, each ruling something out and linking the records that apply it | A tenet that rules nothing out cannot guide a decision; the links show it applied | The repository owner's decision |
| A team declares a kind of its own in the map, after five questions | A kind that overlaps another gives a fact two owners; questions apply to any candidate, where a list of exclusions never ends | The map's own descriptions applied to a kind; the repository owner's decision |
| Plans and backlogs stay in the tracker | Their state changes faster than pages, and a page that copies it drifts | The repository owner's decision, issue #98 |
| Link text names the fact; the value stays on its owner | The sentence reads without the hop, and a restated value is what goes stale | One owner per fact; Tan et al. 2024 |
| A feature page holds its known limits, each with its workaround | A limit is a fact about the behavior it qualifies, read with it and removed in the change that lifts it | One owner per fact; the repository owner's decision |
| An interface page links its contract file | The definition file is the contract, and the reference is built from it | OpenAPI Initiative; Google AIP-192 |
| One runbook per trigger, steps with expected output | The responder verifies each step under pressure | Google SRE workbook; AWS OPS07-BP03 |
| A runbook for a symptom names its causes and a remedy for each, and tells the causes apart first | A responder who checks before acting fixes the cause the symptom has, not the first one guessed | OASIS DITA 1.3 troubleshooting topic; Elastic troubleshooting content type |
| Observability pages hold what telemetry definitions leave out | The monitor definitions hold the queries; what a signal cannot tell apart and why a threshold sits where it does are written nowhere else | One owner per fact; the repository owner's decision |
| A glossary with rejected synonyms | One word per concept makes search and review find every use | Pocock; Good Docs terminology system; ASD-STE100 |
