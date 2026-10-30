# Gateway timeouts

For the volunteer on call when the gateway-timeout alert fires. Brings captures back once the card gateway answers
again; while it times out, confirmed pantry orders stay uncharged. Written after the 2024 outage, when nobody knew
whether a timed-out capture had charged the card. Owner: the payments volunteers.

## Before you begin

- Access to the gateway's status page and to the worker's logs.

## Steps

1. Check the gateway's status page.

   Expected: an incident is listed, or the status is green.
2. Once it is green, find the timed-out captures in the worker's log: lines with `GatewayTimeout`.

   Expected: three lines per pantry order that gave up, one for each attempt, with its order id.

## If it fails

Ask the payments volunteers in the pantry's chat.
