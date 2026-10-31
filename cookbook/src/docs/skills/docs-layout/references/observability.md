# Observability

Holds what the telemetry definitions leave out: what each signal means and cannot tell apart, why each alert sits
where it does, and where the system is not seen. Diátaxis type: reference.

A fact belongs here when a reader needs it to judge an alert or a quiet dashboard. The monitor definitions hold the
queries; a query copied into prose drifts from the one that runs.

What makes a good observability page:

- Each signal says what a reader can conclude from it: what it measures, its source, what it splits by, what it
  cannot tell apart.
- Each alert carries the reason for its threshold and links the runbook that answers it.
- The blind spots are named, each with how a reader finds out otherwise.

## Example

This shows one good page, not the page. `docs/observability/api.md` in the made-up `acme`:

```markdown
# API

For whoever reads an API alert or judges a deploy. Written after a deploy looked healthy for an hour while every
partner order failed, because partner traffic had no signal of its own.

Request status comes from the load balancer, split by caller key, so partner and mobile traffic read apart. 5xx
above 1% for 5 minutes alerts; below that, one partner's retries make noise; [API errors](../runbooks/api-errors.md)
answers it. Jobs the worker takes leave no request trace.
```
