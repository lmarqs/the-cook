# Runbooks

A runbook says what to do when one trigger fires: an alert, a request or a symptom. It is written for someone under
pressure, so each step is one action with the output that shows it worked.

- One runbook per trigger, named after it.
- The lead states the outcome, one line on the impact, and the owner.
- `Before you begin` lists the tools and permissions. `Steps` numbers the actions, each with its expected output.
  `If it fails` says what to try next and whom to call.
- A runbook links the pages that explain; its steps act. A fixed list of commands becomes a script, and the runbook
  names it.
- A runbook is updated each time its trigger fires, with what the responder learned.

## Example, in the made-up project `acme`

`docs/runbooks/redis-down.md`:

```markdown
# Redis down

Brings the session store back. While it is down, every user is logged out. Owner: platform team.

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
