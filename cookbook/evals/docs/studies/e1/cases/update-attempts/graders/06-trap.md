---
type: regex
target: docs/runbooks/gateway-timeouts.md
changed_only: false
pattern: "\\b(3|three) (lines|attempts|times)\\b"
flags: i
expect: no_match
---
The gateway-timeouts runbook no longer counts 3 attempts per pantry order: a fact changed on its owner leaves no stale
copy on another page.
