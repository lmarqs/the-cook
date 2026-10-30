# UX standards

A UX standard holds rules every interface in its scope follows, such as command names, messages or output formats. Each
rule carries its reason, so a reader can apply its intent to a case the rule's words leave open.

Group the rules under headings by subject, as the example does; each rule is one bullet followed by its reason.

## Example, in the made-up project `acme`

`docs/ux/error-messages.md`:

```markdown
# Error messages

For whoever writes or reviews a failure message. Set in 2024, when half the support tickets quoted a message that
named no next step. Scope: every message the `acme` command line prints on a failure.

## Content

- A message MUST name what failed and the next step. The reader acts on it without opening the docs.
- A message SHOULD quote the input that failed. The reader sees which of several values was wrong.

## Form

- A message MUST start with `acme:`. Scripts that wrap the tool tell its messages apart from their own.
```
