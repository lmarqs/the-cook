# Architecture

Holds how the parts of the system fit together: the shape across its directories that no single file shows. One
page, `docs/architecture.md`. Diátaxis type: explanation.

A fact belongs here when it is about the whole: the parts, what each does, how they depend on each other. Why a
part is shaped so is a decision, which this page links.

What makes a good architecture page:

- It names each part as the repository names it, with the directory it lives in, so a reader goes from the page to
  the code.
- It shows the dependencies and main flows as they are, with a diagram where one helps.

## Example

This shows one good page, not the page. `docs/architecture.md` in the made-up `acme`:

```markdown
# Architecture

For whoever joins the team or changes how the parts connect. Written when newcomers spent their first week finding
where an order is settled. `acme` is a web shop: **web**, in `web/`, renders the shop and calls the **API**, in
`api/`, which writes orders to Postgres and queues slow work on Redis for the **worker**, in `worker/`, which sends
mail and settles payments. Sessions sit in a [shared store](adr/0007-keep-sessions-in-a-shared-store.md).
```
