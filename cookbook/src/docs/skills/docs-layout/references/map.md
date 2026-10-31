# The map

`docs/README.md` is the page a reader or an agent opens first: the kinds of page this repository keeps, when a fact
belongs to each, and what a good page of each achieves. It is the team's page, in its language and headings.

## Where a fact goes

Each fact has one owner: the page of the kind whose description the fact fits. Other pages link it, since a copy
drifts. The link text names the fact, as in `wait out [the rate limit](api/orders.md#limits)`, so the sentence reads
without the hop. A reader gets what they need at the first level they open, and follows links for depth.

## What a page holds

- What the repository shows nowhere else: a reason, a rule, a convention and its scope. Code, contracts and history
  hold their own facts, and a page links them.
- What is true now. A tracker holds plans and state that change faster than a page.
- Reasons told as **what happened**: the situation, what it cost, what was tried, then the conclusion. "We need a
  queue" is a conclusion; "the payment call timed out and customers who retried were charged twice" is a reason a
  reader can test.

## The lead

A page opens with its lead: who reads it and when, what it says, why it exists. Written last, read first; a page
whose lead cannot be written is not ready.

## Terms and guides

`docs/glossary.md` gives each concept one name: `**Term**:`, a definition of a sentence or two, then `_Avoid_:` with
the synonyms rejected. A kind of many pages has a guide, `docs/<kind>/README.md`, listing each page with a sentence.
Both appear with their first content: empty structure says something exists that doesn't.

## Misfilings

Most misplaced facts are one of these:

- A feature rule written as a decision. Giveaway: its options had to be invented.
- A screen or output convention written into a feature page. Giveaway: it names a component, a color or a position.
- A feature's behavior written as a standard. Giveaway: it applies to one unit only.
- A procedure written as a feature. Giveaway: numbered steps a responder follows.
- A copy of the tracker's state. Giveaway: a phase or status updated by hand. A team that keeps a plan in `docs/`
  for its reasons declares a kind for it and keeps the state in the tracker.

## A new kind

Ask first: which part of each kind's description do the facts miss? Do they have another owner, as an FAQ's answers
belong to the features they explain? Are they true now, and shown nowhere else? Can a good page of the kind be
described, so a review can compare a page with it?

## The shape of a map

1. A title, and a paragraph saying this page tells which page owns each fact.
2. A section per kind the team keeps, from its reference: what it holds, when a fact belongs there, what a good page
   achieves. No section a page must have. It links the kind's guide or page once one exists.
3. Where a fact goes, what a page holds, and the lead, in the team's words.
4. One worked example: a subject of this repository supplying one sentence to each of three kinds, and why each lands
   where it does.
5. The misfilings this team meets, with their giveaways.
6. Terms, guides, declaring a kind, and a line naming the `docs-layout` skill.
