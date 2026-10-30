---
type: regex
target: docs/philosophy.md
pattern: "adr/0001-.*adr/0002-|adr/0002-.*adr/0001-"
flags: s
---
The tenet links the two records that apply it: sessions in the database and background jobs on Postgres.
