# Standards

A standard holds the practices every artifact of one sort follows, such as every failure message or every log record.
Each practice carries its reason, so a reader can apply its intent to a case its words leave open, and the page is
named after the artifact, so a person writing one finds it where they look.

Group the practices under headings by subject, as the example does; each practice is one bullet followed by its
reason. The BCP 14 keywords, MUST, SHOULD and MAY in capitals, make plain which practices must not vary; a team that
uses them says so once in its part of the map.

## Example, in the made-up project `acme`

`docs/standards/error-messages.md`:

```markdown
# Error messages

For whoever writes or reviews a failure message. Set in 2024, when half the support tickets quoted a message that
named no next step. Scope: every message the `acme` command line prints on a failure.

## Content

- A message names what failed and the next step. The reader acts on it without opening the docs.
- A message quotes the input that failed, unless the input is a secret. The reader sees which of several values was
  wrong.

## Form

- A message MUST start with `acme:`. Scripts that wrap the tool tell its messages apart from their own, and break
  when it varies.
```

## Where a fact goes

Cases the description of the kind leaves open, each with the reasoning that places it:

- **"Logs never carry a customer's tax id."** The artifact constrained is the log record, so the practice sits in
  `log-records.md`, where a person writing a log call looks. `sensitive-data.md` lists the tax id among the
  sensitive classes and links the practice.
- **"Every error body is a problem details object (RFC 9457)"** covers every endpoint, so it is a standard,
  `http-errors.md`. The error codes one API returns are that interface's contract, on its page in `api/`, which
  links the standard.
- **"Use pino" and "each logger is named after its module."** Choosing the library, if reversing it would touch
  many modules, is a decision record. Naming each logger is a practice in `log-records.md`.
- **A decision that implies practices.** The record of "fronts call processes, processes call domains" keeps the
  choice and its reasons; the import practices it implies sit in `module-dependencies.md`, each linking the record
  as its reason. A reader writing an import gets what to do first and follows the link for why.
- **"The p95 latency stays under 300 ms"** is a target the running system meets, not a practice for something a
  person writes: an objective in `observability/`.
- **"Run the tests before pushing"** is how people work, not a sort of artifact.
- **A practice a check comes to enforce.** The change that adds the lint rule removes the practice from the standard
  and puts its reason in a comment beside the rule, so the practice is stated once, where it is enforced.
