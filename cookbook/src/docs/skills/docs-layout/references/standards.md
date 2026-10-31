# Standards

Holds the practices every artifact of one sort follows, such as every log record or every failure message, where no
check enforces them. Diátaxis type: reference.

A fact belongs here when it constrains something a person writes, everywhere it is written. One interface's
contract, one feature's behavior and a latency target are other kinds. A practice a check comes to enforce leaves the
page, its reason moving to a comment beside the check.

What makes a good standard:

- It is named after the sort of artifact, as `log-records.md`, where the person writing one looks.
- Each practice carries its reason, so a reader applies its intent to a case its words leave open.
- What must not vary is unmistakable, as the BCP 14 keywords MUST, SHOULD and MAY make it.

## Example

This shows one good page, not the page. `docs/standards/error-messages.md` in the made-up `acme`:

```markdown
# Error messages

For whoever writes or reviews a failure message of the `acme` command line. Set in 2024, when half the support
tickets quoted a message that named no next step.

- A message names what failed and the next step, so the reader acts without opening the docs.
- A message MUST start with `acme:`. Scripts that wrap the tool tell its messages from their own, and break when it
  varies.
```
