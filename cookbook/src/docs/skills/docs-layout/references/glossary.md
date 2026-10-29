# Glossary

The glossary gives each concept one name, so people, agents and the code use the same word, and a search for it finds
every page about it.

- Each entry is `**Term**:`, then a definition of one or two sentences saying what the thing is, then `_Avoid_:` with
  the synonyms the team rejected.
- It holds the terms specific to this project, one preferred term per concept.
- The check warns where a page uses a rejected synonym.

## Example, in the made-up project `acme`

```markdown
**Order**:
A customer's request for items, from checkout until delivery or cancellation.
_Avoid_: purchase, transaction

**Shop**:
A partner business that sells through acme with its own API key.
_Avoid_: merchant, vendor
```
