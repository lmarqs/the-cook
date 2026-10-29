## Decisions, `adrs/`

Holds why the system is built as it is: each decision, the options weighed and why the choice won. Diátaxis type:
explanation.

- A record holds one foundation decision: other work is built on it, so reversing it changes more than one feature or
  module. It shapes structure, qualities, dependencies or interfaces, is hard to reverse, surprising to a reader new
  to its context and the result of a real trade-off.
- How one feature is implemented goes to its feature page or the code, a choice one change can reverse to the code,
  and a convention a linter or formatter enforces to its configuration.
- The file is `NNNN-<slug>.md`, numbered in sequence; a deleted record's number is best left unused, since an old
  reference may still name it. The title names the decision taken.
- Sections, in order: `Context`, the forces at play; `Options`, each alternative and why it lost; `Decision`, the
  choice and why it won; `Consequences`, what becomes easier and what it costs.
- A record fits on about one page.
- A decision that changes is edited in place, and `Options` keeps the previous choice and why it changed. A record is
  deleted when its decision is reversed outright, or when it fails the bar above, its facts moved to the page that
  owns them; version history keeps it.
