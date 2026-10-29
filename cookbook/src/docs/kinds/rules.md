## Where a fact goes

Each fact has one owning page: the page of the kind whose criteria above the fact meets. That page states the fact,
and every other page links to it. A summary inside a page may repeat what the same page's body says.

## What a page holds

- What the repository shows nowhere else: the reason behind a choice, a convention, a rule and its scope. The code,
  contract files and version history hold their own facts, and a page links to them.
- What is true now. A page changes in the same change as its subject, and a fact learned while working goes to the
  page that owns it.
- Its reasoning: a decision record gives the options and the reasons, a rule gives its reason, and a reference page
  links the decisions that shaped it.
- Front matter holds keys a program reads, such as a site generator's.

## Terms

Terms live in `glossary.md`: each entry is `**Term**:` with a definition of one or two sentences saying what the thing
is, then `_Avoid_:` with the synonyms the team rejected. It holds the terms specific to this project, one preferred
term per concept.

## Guides

A kind's directory appears with its first page, together with its guide, `README.md`: one sentence on what the kind
holds, then a list linking each page with one sentence on it.

## Check

`sh {{skills}}/docs-layout/scripts/check.sh` passes before a change is done. The `docs-layout` skill writes pages
of each kind, adds terms and reviews the tree.
