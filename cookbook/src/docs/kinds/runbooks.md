## Runbooks, `runbooks/`

Holds what to do when a trigger fires: an alert, a request or a symptom. Diátaxis type: how-to.

What makes a good runbook:

- Covers one trigger and is named after it.
- Opens with a lead that states the outcome and, in one line, the impact.
- Gives, in `Before you begin`, the tools and permissions; in `Steps`, one action each with the output that shows it
  worked; in `If it fails`, what to try next.
- For a symptom, names the symptom, its likely causes and the remedy for each. When the cause isn't obvious, it
  starts with checks that tell the causes apart, each saying what its output means.
- Acts, and leaves explaining to the pages that explain. Steps that are a fixed list of commands read better as a
  script the runbook names.
- Gains what the responder learned each time the trigger fires: a step that was missing, an output that differed.
