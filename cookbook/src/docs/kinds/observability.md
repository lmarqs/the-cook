## Observability, `observability/`

Holds what the telemetry definitions leave out: what each signal means, what it can and cannot tell apart, why each
objective and alert exists, and where the system is not seen. Diátaxis type: reference.

What makes a good observability page:

- Covers one system or surface, such as a service or a fleet of workers, and is named after it.
- Describes, in `Signals`, each signal a reader relies on: what it measures, where it comes from, what it can be split
  by, and what it cannot tell apart.
- Gives, in `Objectives and alerts`, each objective and alert with its threshold, the reason for the threshold, and
  the runbook that answers it. The monitor definitions hold the queries; a query copied into prose drifts from the one
  that runs.
- Lists, in `Blind spots`, what the telemetry does not see, each with how a reader finds out otherwise.
