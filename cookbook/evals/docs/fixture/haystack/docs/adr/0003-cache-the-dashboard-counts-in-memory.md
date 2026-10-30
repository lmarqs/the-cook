# Cache the dashboard counts in memory

For whoever changes the admin dashboard or the queries behind its counts. The dashboard's counts of pantry orders,
members and items in stock are cached in each web server's memory for 60 seconds, because the counting query took 3
seconds when the pantry opened and the volunteers refresh the page often.

## Context

In early 2025 Marta reported that the dashboard took 3 seconds to open on Saturday mornings, when a dozen volunteers
watched it at once and refreshed it every few seconds. The counting query scanned the pantry order and stock tables
on every load, and it slowed the pickup slot page that shared the database. Two forces judge the options: the
dashboard must open at once at opening time, and the counts need only be right to the minute.

## Options

- **No cache**: the simplest; lost because every refresh ran the 3-second query and slowed other pages.
- **A materialised view in Postgres**: shared by both web servers; lost because it needs a scheduled refresh job and
  a migration for counts that matter little.
- **An in-memory cache in each web server**: chosen.

## Decision

Each web server computes the counts at most once every 60 seconds and keeps them in its own memory, which adds no
service and no migration.

## Consequences

The dashboard opens in under 100 milliseconds after the first load of each minute. Counts can be up to a minute
stale, and the two web servers can show counts a few seconds apart; the volunteers have said a minute is harmless.
A deploy empties the cache, and the first load afterwards takes the 3 seconds.
