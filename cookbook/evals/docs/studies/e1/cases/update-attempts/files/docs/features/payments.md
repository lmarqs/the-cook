# Payments

For whoever changes how larder charges a card, or answers a member charged twice. Larder captures the authorized
amount once a pantry order is confirmed. Its retry rules exist because in 2024 a gateway timeout led to 41 members
being charged twice.

## Capture

A confirmed pantry order is captured for its authorized amount: `larder/capture.py` calls the card gateway and
returns its charge id.

## Retries

A capture that times out is tried again, up to 3 attempts in all, one after the other.

Every attempt to capture one pantry order sends the same idempotency key, the one its first attempt used. The gateway
can report a timeout after it has already charged the card, and it charges again for a new key: in 2024 retries that
drew a fresh key for each attempt charged 41 members twice.

## Shaped by

- [Gateway timeouts](../runbooks/gateway-timeouts.md), the runbook for the alert.
