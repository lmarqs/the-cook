# Runbooks

A runbook says what to do when one trigger fires: an alert, a request or a symptom. It is written for someone under
pressure, so each step is one action with the output that shows it worked.

Each time the trigger fires, the responder adds what they learned: a step that was missing, an output that differed.

## Example, in the made-up project `acme`

`docs/runbooks/redis-down.md`:

```markdown
# Redis down

For the on-call engineer when the Redis alert fires. Brings the session store back; while it is down, every user is
logged out. Written after the March outage, when finding the restart took 40 minutes. Owner: platform team.

## Before you begin

- `kubectl` access to the `acme-prod` cluster.

## Steps

1. Check the pod: `kubectl -n acme get pod -l app=redis`.

   Expected: one pod, `Running`, `1/1`.
2. Restart it if it is in `CrashLoopBackOff`: `kubectl -n acme rollout restart statefulset/redis`.

   Expected: the pod is `Running` again within two minutes.

## If it fails

Page the platform on-call. [Sessions](../features/sessions.md) explains what depends on Redis.
```
