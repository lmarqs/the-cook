# Interface pages

An interface's contract lives in its definition file: an OpenAPI document, a proto file or a schema, from which the
reference is built. An event stream, a queue or the analytics events a product team reads is an interface too, and
its schema is its definition file. The interface page holds what that file leaves out, and links the file for
everything else, so the two stay in agreement.

Sections after `Contract` hold what the interface has of authentication, versioning, deprecation policy and limits.

## Example, in the made-up project `acme`

`docs/api/orders.md`:

```markdown
# Orders API

For whoever integrates a partner or changes the API. The Orders API lets the mobile app and partner shops place and
track orders. Partners asked for it in 2024, when they were keying 200 emailed orders a day into acme by hand.

## Contract

[openapi/orders.yaml](../../openapi/orders.yaml) is the contract; `make api-docs` builds the reference from it.

## Authentication

Partners send an API key in `X-Acme-Key`; the mobile app sends the user's session token.

## Limits

A partner key allows 600 requests a minute; a 429 carries `Retry-After`.
```
