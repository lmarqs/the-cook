---
type: regex
target: diff
pattern: "^diff --git a/docs/\\S+ b/docs/\\S+\\nnew file mode"
flags: m
expect: no_match
---
No page is added under docs/: the change belongs to the page that already owns the retry rules.
