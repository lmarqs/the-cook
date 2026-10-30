---
type: regex
target: diff
pattern: "^--- /dev/null\\n\\+\\+\\+ b/docs/adr/\\d{4}-"
flags: m
expect: no_match
---
No decision record is added: the map gives a rule that guides many decisions to the philosophy page, and one decision
applying it to a record.
