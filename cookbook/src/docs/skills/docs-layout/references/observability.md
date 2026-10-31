# Observability

An observability page answers "what can the telemetry tell me, and what not?" for one system. The monitor and
dashboard definitions hold the queries; the page holds what they leave out: what each signal means, why each
threshold is where it is, and where the system is not seen, so a reader judges an alert or a quiet dashboard
correctly.

- A signal is described by what a reader can conclude from it: what it measures, its source, what it can be split by,
  and what it cannot tell apart.
- An alert's threshold carries its reason, and the alert names the runbook that answers it.

## Example, in the made-up project `acme`

`docs/observability/api.md`:

```markdown
# API

For whoever reads an API alert or judges a deploy. It says what the API's telemetry shows and misses. Written after
a deploy looked healthy for an hour while every partner order failed, because partner traffic had no signal of its
own.

## Signals

- **Request rate and status**, from the load balancer's logs: every request, split by route and by caller key, so
  partner and mobile traffic read apart.
- **Latency**, from the tracer: sampled at 10%, so a rare slow route can go unseen for a day.

## Objectives and alerts

- **5xx above 1% for 5 minutes**: below that, a single partner's retries make noise. Answered by
  [API errors](../runbooks/api-errors.md).
- **Session store unreachable**: every user is logged out while it lasts. Answered by
  [Redis down](../runbooks/redis-down.md).

## Blind spots

- Jobs the worker takes from the queue leave no request trace; their failures show only in the worker's logs.
```
