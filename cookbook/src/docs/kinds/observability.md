## Observability, `observability/`

Holds what the telemetry definitions leave out: what each signal means, what it can and cannot tell apart, why each
objective and alert exists, and where the system is not seen. Diátaxis type: reference.

Good observability pages:

- Cover one system or surface each, such as a service or a fleet of workers, and are named after it.
- Describe, in `Signals`, each signal a reader relies on: what it measures, where it comes from, what it can be split
  by, and what it cannot tell apart.
- Give, in `Objectives and alerts`, each objective and alert with its threshold, the reason for the threshold, and
  the runbook that answers it. The monitor definitions hold the queries; a query copied into prose drifts from the one
  that runs.
- List, in `Blind spots`, what the telemetry does not see, each with how a reader finds out otherwise.
