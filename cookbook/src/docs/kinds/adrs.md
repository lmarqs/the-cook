## Decisions, `adrs/`

Holds why the system is built as it is: each decision, the options weighed and why the choice won. Diátaxis type:
explanation.

- A record holds one decision that shapes structure, qualities, dependencies, interfaces or construction, and that is
  hard to reverse, surprising to a reader new to its context and the result of a real trade-off.
- The file is `NNNN-<slug>.md`, numbered in sequence, and each number belongs to one record for good. The title names
  the decision taken.
- Sections, in order: `Context`, the forces at play; `Options`, each alternative and why it lost; `Decision`, the
  choice and why it won; `Consequences`, what becomes easier and what it costs.
- A record fits on about one page.
- A decision that changes is edited in place, and `Options` keeps the previous choice and why it changed. A decision
  reversed outright is deleted; version history keeps it and its number stays taken.
