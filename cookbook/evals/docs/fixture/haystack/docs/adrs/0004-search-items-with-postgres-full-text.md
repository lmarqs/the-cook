# Search items with Postgres full-text

For whoever changes how members search the pantry's items. Item search uses Postgres full-text search on the item
name, because the earlier search took four seconds and a separate search service was more than larder needs.

## Context

Item search first matched names with `LIKE '%word%'`, which read every row of the 2,300 items on each search. In
spring 2025 a member searching for "chickpeas" waited four seconds, and Priya found that the pantry page was
slowest where members typed. Two forces judge the options: a search must answer in well under a second, and the
volunteers who run larder keep the number of services they operate down.

## Options

- **`LIKE` queries**: no change; lost because they read every row and cannot use an index.
- **Elasticsearch**: ranking and typo tolerance; lost on the second force, a service to run, secure and keep in
  step with the stock for 2,300 items.
- **Postgres full-text search**: chosen.

## Decision

Item search uses a Postgres full-text index on the item name, queried with `websearch_to_tsquery` in the `simple`
configuration, so the one database serves search too.

## Consequences

Search answers in about 20 milliseconds. Words match by whole word, not by part of a word, so "chick" does not find
"chickpeas"; members search for the whole word. Typos find nothing, and the volunteers accept that.
