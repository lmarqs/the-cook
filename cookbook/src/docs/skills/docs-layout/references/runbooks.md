# Runbooks

A runbook says what to do when one trigger fires: an alert, a request or a symptom. It is written for someone under
pressure, so each step is one action with the output that shows it worked.

A runbook for a symptom names the symptom, its likely causes and the remedy for each, as a troubleshooting topic does
in DITA. When the cause isn't obvious, the first steps are checks that tell the causes apart, as for "users cannot
log in":

```markdown
1. Check the API's error rate: `make api-errors`.

   Expected: below 1%. Above it, the API is failing; go to step 4. Below it, the login page is at fault; go to step 2.
```

Each time the trigger fires, the responder adds what they learned: a step that was missing, an output that differed.

## Example, in the made-up project `acme`

`docs/runbooks/redis-down.md`:

```markdown
# Redis down

For whoever answers the Redis alert. Brings the session store back; while it is down, every user is
logged out. Written after the March outage, when finding the restart took 40 minutes.

## Before you begin

- `kubectl` access to the `acme-prod` cluster.

## Steps

1. Check the pod: `kubectl -n acme get pod -l app=redis`.

   Expected: one pod, `Running`, `1/1`.
2. Restart it if it is in `CrashLoopBackOff`: `kubectl -n acme rollout restart statefulset/redis`.

   Expected: the pod is `Running` again within two minutes.

## If it fails

Restore the store from the last snapshot with `make redis-restore`. [Sessions](../features/sessions.md) explains
what depends on Redis.
```
