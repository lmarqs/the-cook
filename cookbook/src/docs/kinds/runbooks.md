## Runbooks, `runbooks/`

Holds what to do when a trigger fires: an alert, a request or a symptom. Diátaxis type: how-to.

Good runbooks:

- Cover one trigger each and are named after it.
- Open with a lead that states the outcome and, in one line, the impact.
- Give, in `Before you begin`, the tools and permissions; in `Steps`, one action each with the output that shows it
  worked; in `If it fails`, what to try next.
- For a symptom, name the symptom, its likely causes and the remedy for each. When the cause isn't obvious, they
  start with checks that tell the causes apart, each saying what its output means.
- Act, and leave explaining to the pages that explain. Steps that are a fixed list of commands read better as a
  script the runbook names.
- Gain what the responder learned each time the trigger fires: a step that was missing, an output that differed.
