## Features, `features/`

Holds what the system does, one page per unit of behavior. Diátaxis type: reference.

- The lead paragraph says what the unit is and what it is for.
- Sections follow the unit's own structure, and examples are welcome.
- The page describes. The reasons behind the behavior live in `adrs/` and the steps to operate it in `runbooks/`; the
  page links the records and runbooks that shaped it.
- A `Known limits` section, when the unit has any, lists what it doesn't handle, handles only partly or handles in a
  way that catches a reader out, each with its workaround. A limit is removed in the change that lifts it.
