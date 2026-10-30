---
type: regex
target: diff
pattern: "^--- /dev/null\\n\\+\\+\\+ b/docs/adrs/(?!0001-)\\d{4}-"
flags: m
expect: no_match
---
No second decision record is added: the map edits a changed decision in place instead of superseding it with a new
record.
