## Runbooks, `runbooks/`

Holds what to do when a trigger fires: an alert, a request or a symptom. Diátaxis type: how-to.

- One runbook per trigger, named after it.
- The lead states the outcome, one line on the impact, and the owner.
- Sections, in order: `Before you begin`, the tools and permissions; `Steps`, one action each with the output that
  shows it worked; `If it fails`, what to try and whom to call.
- A runbook links the pages that explain; its steps act. Steps that are a fixed list of commands become a script, and
  the runbook names it.
- A runbook is updated each time its trigger fires.
