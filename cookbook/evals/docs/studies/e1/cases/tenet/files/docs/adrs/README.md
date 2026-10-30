# Decisions

- [Keep sessions in the database](0001-keep-sessions-in-the-database.md): sessions live in Postgres, not in memory.
- [Run background jobs on Postgres](0002-run-background-jobs-on-postgres.md): jobs run on procrastinate, not Redis.
