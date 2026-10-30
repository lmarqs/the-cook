## Decisions, `adrs/`

Holds why the system is built as it is: each decision, the options weighed and why the choice won. Diátaxis type:
explanation.

- A record holds one foundation decision: other work is built on it, so reversing it changes more than one feature or
  module. It shapes structure, qualities, dependencies or interfaces, is hard to reverse, surprising to a reader new
  to its context and the result of a real trade-off.
- Not a record: how one feature is implemented, which its feature page or the code holds; a choice one change can
  reverse; a convention that static checking, such as linting, enforces, since its configuration already states it.
- The file is `NNNN-<slug>.md`, numbered in sequence; a deleted record's number is best left unused, since an old
  reference may still name it. The title names the decision taken.
- The lead says whom the decision concerns, the decision, and in a clause what moved it.
- Sections, in order: `Context`, what happened: the situation that raised the decision, what it cost and what was
  tried, then the forces the options are judged by; `Options`, each alternative and why it lost; `Decision`, the
  choice and why it won, with the product that implements it, if any, and why that one; `Consequences`, what becomes
  easier and what it costs.
- A record fits on about one page.
- A decision that changes is edited in place: it keeps its number, its title and slug follow the new choice, and
  `Options` keeps the previous choice and why it changed. A record is deleted when its decision is reversed outright,
  or when it fails the bar above, its facts moved to the page that owns them; version history keeps it.
