# Feature pages

A feature page answers "what does it do?" for one unit of behavior, as reference: it describes, in the structure of
the thing itself, so a reader finds a fact where the product puts it.

The lead says why the unit exists, or why it is as it is, as it happened: a reason below the bar for a decision
record has no other owner. Sections follow the unit: its inputs, its states, its conditions, its outputs, as the
unit has them. `Shaped by` links the decision records behind the behavior.

## Example, in the made-up project `acme`

`docs/features/sessions.md`:

```markdown
# Sessions

For whoever changes login, or answers a user who was logged out. A session keeps a user logged in across requests,
from login until logout or 30 minutes without a request. The limit was 10 minutes until 2023, when shoppers paying
on slow mobile connections lost their carts mid-checkout.

## Lifetime

Each request renews the session for another 30 minutes. Logout deletes it at once.

## Across replicas

Every web replica reads the same session store, so a user stays logged in whichever replica answers.

## Shaped by

- [Keep sessions in a shared store](../adr/0007-keep-sessions-in-a-shared-store.md)
```
