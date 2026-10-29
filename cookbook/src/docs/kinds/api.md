## Interfaces, `api/`

Holds what an interface's contract file leaves out: who calls it and why, authentication, versioning, deprecation
policy and limits. Diátaxis type: reference.

- One page per interface. The lead says what the interface is for and who calls it.
- A `Contract` section names the definition file, an OpenAPI document, a proto file or a schema, and how the reference
  is built from it. The definition is the contract; the page links it.
- Design reasons live in `adrs/`, and the page links them.
