# Architecture

The page shows the shape no single file shows: which parts the system has, what each is responsible for, and how they
depend on each other. It takes arc42's building block and runtime views; arc42's decisions and glossary already live
in `adrs/` and `glossary.md`.

- A part is named as the repository names it, with the directory it lives in, so a reader goes from the page to the
  code.
- A structural choice links its record; the reasons stay there. Requirements, interface rules and behavior belong to
  their own kinds, and the page links them.

## Example, in the made-up project `acme`

`docs/architecture.md`:

```markdown
# Architecture

For whoever joins the team or changes how the parts connect. `acme` is a web shop: a browser app, an API and a
worker, over one Postgres database. Payment and mail are external services. Written when newcomers spent their first
week finding where an order is settled.

## Parts

- **Web**, in `web/`: renders the shop and calls the API.
- **API**, in `api/`: serves orders and accounts, and queues slow work.
- **Worker**, in `worker/`: sends mail and settles payments from the queue.

## How they connect

The web calls the API over HTTP and keeps sessions in Redis, which every web replica reads
([a shared session store](adrs/0007-keep-sessions-in-a-shared-store.md)). The API writes orders to Postgres and puts
jobs on a Redis queue, which the worker reads.
```
