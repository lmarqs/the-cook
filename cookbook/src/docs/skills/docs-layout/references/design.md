# Why the layout is shaped as it is

Each rule, its reason and its source. The design and its full decision register are on issue #90 of
`lmarqs/the-cook`.

| Rule | Reason | Source |
| --- | --- | --- |
| One map, `docs/README.md`, holds the criteria | Readers and agents open one page first; criteria written once stay consistent | *Software Engineering at Google*, landing pages; Google, "Duplication is Evil" |
| Each fact has one owning page; other pages link it | A copy drifts from its original | Google documentation guide; Write the Docs; outdated references in 28.9% of 991 top projects (Tan et al. 2024) |
| Kinds by subject, each with a Diátaxis type | One purpose per page; each type has its own rules | Diátaxis; GitHub's `internal-documentation-example` |
| The recipe writes the text known when it is cooked; the skill writes what comes with a page | Cooked text is owned, tested and updated by each release; text an agent copies varies and keeps the wording of the release it came from | The repository owner's decision |
| Directories and guides appear with their first page | Empty structure misleads | Diátaxis: "Don't do that. It's horrible."; Pocock: create files lazily |
| A guide per kind lists its pages, and the check enforces the list | A landing page lists what its directory holds; a list kept by hand stays complete when checked | Diátaxis; GitLab `_index.md` and `docs-lint` |
| A page holds what the repository shows nowhere else | The code, contracts and version history hold their own facts without drift | Claude Code memory guidance; HumanLayer, "Prefer pointers to copies" |
| Front matter holds keys a program reads | Hand-kept fields go stale and mislead | MADR ADR-0013: front matter "pretends to be more accurate than it can be"; Good Docs Project: adopt only metadata "automatically maintained" |
| Reasoning is content: options, reasons, rules with reasons | The reasons let a reader judge whether a decision still holds | arc42 tips 9-3 and 9-6; MADR; PEP "Rejected Ideas"; Tang et al. 2005; Falessi et al. 2006 |
| A decision record holds a foundation decision: reversing it changes more than one feature or module | Every record present reads as binding, so a record of an implementation choice freezes a detail that should stay easy to change | The repository owner's decision, stricter than Nygard's list, which also admits construction techniques |
| A changed decision is edited in place; a record reversed outright or below the bar is deleted | Every page present is in force, and `Options` keeps the previous choice | The repository owner's decision; Google Cloud ADR guidance |
| Decision records are `NNNN-<slug>.md` under `adrs/` | Numbers give each record a stable name | Nygard for the numbering; the repository owner's decision for `adrs/` |
| Standards use BCP 14 keywords, each rule with its reason | Readers apply the intent to cases the words leave open | RFC 2119, RFC 8174; W3C "Why care?"; Google style guides |
| An interface page links its contract file | The definition file is the contract, and the reference is built from it | OpenAPI Initiative; Google AIP-192 |
| One runbook per trigger, steps with expected output | The responder verifies each step under pressure | Google SRE workbook; AWS OPS07-BP03 |
| A glossary with rejected synonyms | One word per concept makes search and review find every use | Pocock; Good Docs terminology system; ASD-STE100 |
| A fact learned while working goes to its page | Shared pages reach people and every agent; private memory reaches one | Codex: "Keep required team guidance in AGENTS.md or checked-in documentation"; Claude Code: a repeated correction is a CLAUDE.md edit |
| The check runs before a change is done | Links and structure are checked by a program; review covers the rest | GitLab `docs-lint`; Rao et al. 2022 on review |
