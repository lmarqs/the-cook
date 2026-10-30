# SMS provider down

For the volunteer on call when the SMS provider's delivery-failure alert fires. Gets text messages flowing again once
the provider recovers; until then the texts the worker sends stay undelivered, which hits the volunteers' shift
notices first. Written after the October 2024 outage, when two shifts went unfilled because nobody knew the texts
had failed. Owner: Tom (ops).

## Before you begin

- Access to the provider's status page and to its dashboard, where failed messages are listed.
- Access to the worker's log.

## Steps

1. Check the provider's status page.

   Expected: an incident is listed, or the status is green.
2. Find the failures in the worker's log: lines with `SmsDeliveryFailed`.

   Expected: one line per message that failed, with the recipient's phone number masked and the provider's error code.
3. Wait until the status page is green; the provider has recovered every outage so far within a few hours.

   Expected: the alert resolves by itself once new messages are delivered.
4. In the provider's dashboard, open Messages, filter on Failed, and choose Resend all.

   Expected: the Failed list empties and each message shows Delivered.

## If it fails

If the status is green but messages still fail, or Resend all errors, ask Tom in the pantry's chat. Tell Marta if
volunteers missed a shift notice, so she can call them.
