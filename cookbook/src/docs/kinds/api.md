## Interfaces, `api/`

Holds what an interface's contract file leaves out: who calls it and why, authentication, versioning, deprecation
policy and limits. An interface is any contract another system depends on: an HTTP API, an event stream, a queue, a
webhook or the analytics events a product team reads. Diátaxis type: reference.

What makes a good interface page:

- Covers one interface, with a lead that says what it is for and who calls or consumes it.
- Names, in `Contract`, the definition file, such as an OpenAPI document, a proto file or an event schema, and how the
  reference is built from it. The definition is the contract; a copy of it in prose drifts from it.
