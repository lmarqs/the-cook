# Decision records

A record answers "why is it built this way?" for one decision, so that a reader months later can tell whether the
reasons still hold before changing it. The reasoning is the content: the options weighed and why each lost carry as
much knowledge as the choice.

- A choice below the map's bar lives in the code and its review. Test the decision, not the product that implements
  it: "keep sessions in a store every replica shares" is a foundation; "use Redis for it" is one change to reverse.
- The title is the decision taken, as a sentence: "Keep sessions in a shared store".
- `Context` names the forces the options are judged by; each option that loses, loses on one of them.
- Each option is stated at its best: what it offers, then the force it fails.
- `Decision` states the choice and why it won, in a sentence or two; detail lives in the pages the record links.

## Example, in the made-up project `acme`

`docs/adrs/0007-keep-sessions-in-a-shared-store.md`:

```markdown
# Keep sessions in a shared store

## Context

The web tier runs on four replicas behind a round-robin balancer, and a login held in one replica's memory is lost
when the next request reaches another. Three forces judge the options: a deploy restarts every replica several times
a day and must log no one out; logout and an admin's block must end a session at once; a session holds the user's
cart, up to 20 KB.

## Options

- **Sticky sessions at the balancer**: needs no new service and no code; lost because a deploy's restart logs out
  every user of the restarted replica.
- **Signed cookies**: keep no state on the server, so there is nothing to run; lost because a cookie cannot be ended
  before it expires, and a 20 KB cart exceeds the 4 KB a browser keeps per cookie.
- **A store every replica reads**: chosen.

## Decision

Sessions live in one store that every replica reads, keyed by session id, with a 30-minute expiry each request
renews. It is the one option that survives a deploy and ends a session by deleting its key. The store in use is
named in the [architecture](../architecture.md#how-they-connect).

## Consequences

Any replica serves any request, and a deploy logs no one out. The store joins the services a deploy depends on, and
the [Redis down runbook](../runbooks/redis-down.md) covers its failure.
```

## When the decision changes

When the carts move to the database and a session fits in a cookie, the team may choose signed cookies and accept
that a blocked user stays logged in until a short expiry. The record keeps its number and becomes
`0007-keep-sessions-in-signed-cookies.md`, titled after the new choice; the check lists the links to update.
`Options` gains "**A store every replica reads**: chosen first; left once sessions fit in a cookie", and `Decision`
states signed cookies and why.
