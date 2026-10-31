# Philosophy

Holds the tenets that guide decisions: what the team favors when two good options conflict. One page,
`docs/philosophy.md`. Diátaxis type: explanation.

A fact belongs here when it guides many decisions and rules something out.

What makes a good philosophy page:

- Each tenet is one sentence, then what it rules out. "Quality matters" rules out nothing.
- Each tenet links the records that apply it, so a reader can judge whether it still holds.

## Example

This shows one good page, not the page. `docs/philosophy.md` in the made-up `acme`:

```markdown
# Philosophy

For whoever weighs two good options that no record settles. Written after two reviews in a row argued the same
trade-off from the start.

**Fewer features, each finished.** Rules out shipping a feature without its page, its runbook and its tests. Applied
in [a shared session store](adr/0007-keep-sessions-in-a-shared-store.md).
```
