---
type: regex
target: docs/philosophy.md
pattern: "adrs/0001-.*adrs/0002-|adrs/0002-.*adrs/0001-"
flags: s
---
The tenet links the two records that apply it: sessions in the database and background jobs on Postgres.
