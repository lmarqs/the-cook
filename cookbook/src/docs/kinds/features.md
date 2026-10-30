## Features, `features/`

Holds what the system does, one page per unit of behavior. Diátaxis type: reference.

- The lead says what the unit is and why it exists, or why it is as it is: what went wrong, or could not be done,
  without it.
- Sections follow the unit's own structure, and examples are welcome.
- The page describes. The options weighed for a foundation decision live in `adr/` and the steps to operate the unit
  in `runbooks/`; the page links the records and runbooks that shaped it.
- A `Known limits` section, when the unit has any, lists what it doesn't handle, handles only partly or handles in a
  way that catches a reader out, each with its workaround. A limit is removed in the change that lifts it.
