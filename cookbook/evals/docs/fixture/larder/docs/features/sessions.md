# Sessions

For whoever changes login or the session expiry, or answers a member who was logged out. A session keeps a member
logged in until 30 minutes pass without a request. The limit was 10 minutes until 2024, when members on slow mobile
connections were logged out mid-checkout and lost their pantry orders.

## Lifetime

Each request renews the session for 30 minutes, `EXPIRY_MINUTES` in `larder/sessions.py`. Until March 2024 the limit
was 10 minutes: members packing a pantry order on a slow mobile connection were logged out before they confirmed it
and lost what they had picked, and support logged 120 tickets about it that month. The limit was raised to 30
minutes, the shortest that ended those tickets.

## Storage

Sessions live in Postgres ([keep sessions in the database](../adr/0001-keep-sessions-in-the-database.md)).

## Shaped by

- [Keep sessions in the database](../adr/0001-keep-sessions-in-the-database.md)
