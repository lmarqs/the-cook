# Standards

A standard holds rules every interface in its scope follows, such as command names, messages or output formats. Each
rule carries its reason, so a reader can apply its intent to a case the rule's words leave open.

- The line after the title states the scope.
- Rules use MUST, SHOULD and MAY in capitals, as BCP 14 defines them, and sparingly; the guide states the BCP 14
  sentence once for every standard.
- Each rule carries its reason in one sentence, or links the decision record that holds it.
- A standard is edited in place as its rules change.

## Example, in the made-up project `acme`

`docs/ux/error-messages.md`:

```markdown
# Error messages

Scope: every message the `acme` command line prints on a failure.

## Content

- A message MUST name what failed and the next step. The reader acts on it without opening the docs.
- A message SHOULD quote the input that failed. The reader sees which of several values was wrong.

## Form

- A message MUST start with `acme:`. Scripts that wrap the tool tell its messages apart from their own.
```
