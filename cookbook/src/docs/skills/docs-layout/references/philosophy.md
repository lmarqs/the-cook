# Philosophy

A tenet says what the team favors when two good options conflict, so a decision nobody has recorded yet still goes
the way the team would take it. The Objective says why the project exists; the tenets say how it gets there.

- A tenet rules something out. "Quality matters" rules out nothing; "Fewer features, each finished" rules out
  shipping half a feature.
- Each tenet links the records that apply it, so a reader sees it at work and can judge whether it still holds.

## Example, in the made-up project `acme`

`docs/philosophy.md`:

```markdown
# Philosophy

These tenets serve the [Objective](../README.md#objective).

## Tenets

### Fewer features, each finished

Rules out shipping a feature without its page, its runbook and its tests. Applied in
[sessions in Redis](adrs/0007-store-sessions-in-redis.md).
```
