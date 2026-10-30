## Where a fact goes

Each fact has one owning page: the page of the kind whose criteria above the fact meets. That page states the fact,
and every other page links to it. The link text names the fact, as in
`wait out [the rate limit](api/orders.md#limits)`, so the sentence reads without following the link. A summary
inside a page may repeat what the same page's body says.

## What a page holds

- What the repository shows nowhere else: the reason behind a choice, a convention, a rule and its scope. The code,
  contract files and version history hold their own facts, and a page links to them.
- What is true now.
- Its reasoning: a decision record gives the options and the reasons, a rule gives its reason, and a reference page
  links the decisions that shaped it.
- Front matter holds keys a program reads, such as a site generator's.

## Terms

Terms live in `glossary.md`: each entry is `**Term**:` with a definition of one or two sentences saying what the thing
is, then `_Avoid_:` with the synonyms the team rejected. It holds the terms specific to this project, one preferred
term per concept.

## Guides

A kind's directory appears with its first page, together with its guide, `README.md`: the kind's name as its title,
such as `# Decisions`, then a list linking each page with one sentence on it. A kind of one page appears with its
content.

## A new kind

A team declares a kind of its own on this page, outside the section the recipe writes, with a heading in the shape of
the ones above, ``## <Name>, `<directory>/` `` or ``## <Name>, `<page>.md` ``, then what it holds and what each page
must contain. A candidate passes every test first:

1. No kind above fits: for each, its facts fail a criterion that can be named.
2. Its facts have no other owner. An FAQ fails: each answer belongs to the feature or decision it explains.
3. It states what is true now, changing with its subject. A plan, a roadmap or a postmortem fails.
4. The repository shows it nowhere else: not the code, a contract file, version history or another tool's own file.
5. What each page must contain can be named, so a review can judge a page.

Facts that fail test 1 or 2 go to the kind above that owns them; facts that fail test 3 or 4 stay outside `docs/`.

## Skill

The `docs-layout` skill, in `{{skills}}/docs-layout/`, writes pages of each kind, adds terms and reviews the tree.
