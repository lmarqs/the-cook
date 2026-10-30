# Run background jobs on Postgres

For whoever adds a background job or changes the database. Jobs live in Postgres through procrastinate, because the
Redis instance holding Celery's queue evicted 300 receipt jobs in March 2025.

## Context

In March 2025 the Redis instance holding Celery's queue ran out of memory on a Saturday night and evicted queued
receipt jobs without logging an error; 300 members got no receipt, and a volunteer spent a day resending them by hand.
Redis ran only for the queue, and one volunteer knew how to restore it. Two forces judge the options: a queued job
must not be lost, and the volunteers keep the number of services they operate down.

## Options

- **Celery with Redis, persistence and a memory alarm**: the least work; lost on the second force, Redis kept for one
  queue and one volunteer.
- **Celery with its database backend**: marked experimental in Celery's documentation.
- **procrastinate on Postgres**: chosen.

## Decision

Jobs run on procrastinate, a Postgres task queue for Python, because it keeps a job in the same transaction as the
pantry order that queues it, and Postgres is the database larder already runs and backs up every night.

## Consequences

Redis is switched off. A job is saved with the pantry order that queues it, or not at all.
