## Where a fact goes

Each fact reads best on one page: the page of the kind whose description above the fact fits. That page states the
fact, and every other page links to it, since a copy drifts from its original. The link text names the fact, as in
`wait out [the rate limit](api/orders.md#limits)`, so the sentence reads without following the link. A summary
inside a page may repeat what the same page's body says.

A reader gets what they need to act at the first level they open, and follows links for depth: this map, then a
kind's guide, a page's lead, its body, and the pages it links. A page states what its reader acts on and links the
rest.

## What a page holds

- What the repository shows nowhere else: the reason behind a choice, a convention and its scope. The code, contract
  files and version history hold their own facts, and a page links to them.
- What is true now, and why it is so. Plans and backlogs change faster than pages, and the issue tracker keeps their
  state.
- Its reasons, told as they happened: the situation as it was met, what it cost and what was tried, then the
  conclusion. "We need a queue" is a conclusion; "the payment call timed out inside the request, and customers who
  retried were charged twice" is a reason a reader can test.
- Front matter only with keys a program reads, such as a site generator's; a field kept by hand goes stale.

## The lead

Every page opens, under its title, with its lead: what a reader needs to know about the page before reading it, in
this order:

1. Who reads it, and when they reach for it.
2. What it says.
3. Why it exists: what moved the team to write it, told as it happened.

A kind may name more for its lead. The lead is written last and read first: a page whose lead cannot be written is
not ready, and a body that says more or less than its lead has drifted from it.

## Terms

Terms live in `glossary.md`: each entry is `**Term**:` with a definition of one or two sentences saying what the thing
is, then `_Avoid_:` with the synonyms the team rejected. It holds the terms specific to this project, one preferred
term per concept.

## Guides

A kind's directory appears with its first page, together with its guide, `README.md`: the kind's name as its title,
such as `# Architecture decisions`, then a list linking each page with one sentence on it. A kind of one page appears
with its content. Empty structure tells a reader something exists that doesn't.

## A new kind

A team declares a kind of its own on this page, outside the section the recipe writes, with a heading in the shape of
the ones above, ``## <Name>, `<directory>/` `` or ``## <Name>, `<page>.md` ``, then what it holds and what a good page
of it contains. Questions worth asking first:

1. Does a kind above fit? If not, which part of its description do the facts miss?
2. Do the facts have another owner? An FAQ usually does: each answer belongs to the feature or decision it explains.
3. Does it state what is true now, changing with its subject? A plan, a roadmap or a postmortem records what is
   intended or what happened, and the tracker or version history keeps it.
4. Does the repository show it nowhere else: not the code, a contract file, version history or another tool's own
   file?
5. Can what a good page of it contains be named, so a review can compare a page with it?

## Skill

The `docs-layout` skill writes pages of each kind, adds terms and reviews the tree.
