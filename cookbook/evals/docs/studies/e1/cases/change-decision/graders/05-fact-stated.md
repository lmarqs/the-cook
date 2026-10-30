---
type: regex
target: docs/adr/0001-*.md
pattern: "## Options\\s.*Postgres.*## Decision\\s.*cookie"
flags: si
---
The record keeps the previous choice, Postgres, under Options, and its Decision names signed cookies.
