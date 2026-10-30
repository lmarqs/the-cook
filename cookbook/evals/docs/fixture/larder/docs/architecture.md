# Architecture

For whoever joins larder or changes how its parts connect. Larder is one Python web app over one Postgres database,
with a worker for background jobs and an external card gateway. Written when a new volunteer spent a week finding
where a payment is captured.

## Parts

- **Web app**, in `larder/`: serves the pantry, takes pantry orders and keeps sessions.
- **Worker**, in `larder/receipts.py`: runs background jobs, such as receipt emails, from the queue in Postgres.
- **Card gateway**, outside the repository: charges cards; `larder/gateway.py` is its client.

## How they connect

The web app keeps sessions in Postgres ([keep sessions in the database](adr/0001-keep-sessions-in-the-database.md))
and queues jobs there for the worker. A confirmed pantry order is captured through the card gateway by
`larder/capture.py`.
