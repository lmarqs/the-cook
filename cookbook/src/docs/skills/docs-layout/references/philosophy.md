# Philosophy

A tenet says what the team favors when two good options conflict, so a decision nobody has recorded yet still goes
the way the team would take it.

- A tenet rules something out. "Quality matters" rules out nothing; "Fewer features, each finished" rules out
  shipping half a feature.
- Each tenet links the records that apply it, so a reader sees it at work and can judge whether it still holds.

## Example, in the made-up project `acme`

`docs/philosophy.md`:

```markdown
# Philosophy

For whoever weighs two good options that no record settles. It holds what the team favors. Written after two reviews
in a row argued the same trade-off from the start.

## Tenets

### Fewer features, each finished

Rules out shipping a feature without its page, its runbook and its tests. Applied in
[a shared session store](adr/0007-keep-sessions-in-a-shared-store.md).
```
