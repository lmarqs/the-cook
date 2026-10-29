# Decision records

A record answers "why is it built this way?" for one decision, so that a reader months later can tell whether the
reasons still hold before changing it. The reasoning is the content: the options weighed and why each lost carry as
much knowledge as the choice.

- Write one when the decision shapes structure, qualities, dependencies, interfaces or construction, is hard to
  reverse, would surprise a reader new to its context, and came from a real trade-off. Smaller choices live in the
  code and its review.
- `Context` states the forces: the problem, the constraints, the goals. `Options` lists each alternative with why it
  lost, and the chosen one with why it won. `Decision` states the choice in a sentence or two. `Consequences` states
  what becomes easier, what becomes harder, and the costs.
- The title is the decision taken, as a sentence: "Store sessions in Redis".
- A record fits on about one page; detail lives in the pages it links.

## Example, in the made-up project `acme`

`docs/adr/0007-store-sessions-in-redis.md`:

```markdown
# Store sessions in Redis

## Context

The web tier runs on four replicas behind a round-robin balancer, and a login on one replica is lost on the next
request. Sessions are read on every request and expire after 30 minutes.

## Options

- **Sticky sessions at the balancer**: lost because a replica restart logs its users out.
- **Sessions in Postgres**: lost because every request would add a write to the busiest table.
- **Sessions in Redis**: chosen; reads take under a millisecond and keys expire on their own.

## Decision

Sessions live in Redis, keyed by session id, with a 30-minute expiry.

## Consequences

Any replica serves any request. Redis joins the services a deploy depends on, and the
[Redis down runbook](../runbooks/redis-down.md) covers its failure.
```

## When the decision changes

Moving from Redis to Valkey keeps the record's number and edits it: `Options` gains "**Redis**: chosen in 2024; left
because of its license change", and `Decision` states Valkey and why.
