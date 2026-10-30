---
type: regex
target: docs/features/payments.md
pattern: "\\A(?!.*\\bup to (3|three) attempts)(?=.*(\\b(5|five)\\b[^.\\n]{0,40}attempts|attempts[^.\\n]{0,40}\\b(5|five)\\b|\\]\\([^)]*capture\\.py[^)]*\\)))"
flags: si
---
The payments page no longer says up to 3 attempts, and states the new limit, 5 attempts, or links capture.py, which
holds it.
