# Feature pages

A feature page answers "what does it do?" for one unit of behavior, as reference: it describes, in the structure of
the thing itself, so a reader finds a fact where the product puts it.

- The lead paragraph says what the unit is and what it is for.
- Sections follow the unit: its inputs, its states, its rules, its outputs, as the unit has them. Examples help.
- The reasons behind the behavior are in decision records, and the page links them under `Shaped by`. Steps to
  operate it are in runbooks, which the page links.
- A behavior change ships with its page, in the same change as the code.

## Example, in the made-up project `acme`

`docs/features/sessions.md`:

```markdown
# Sessions

A session keeps a user logged in across requests, from login until logout or 30 minutes without a request.

## Lifetime

Each request renews the session for another 30 minutes. Logout deletes it at once.

## Across replicas

Every web replica reads the same session store, so a user stays logged in whichever replica answers.

## Shaped by

- [Store sessions in Redis](../adr/0007-store-sessions-in-redis.md)
```
