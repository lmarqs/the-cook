# Receipts

For whoever changes the receipt email or answers a member who did not get one. Once a pantry order is captured, the
worker emails the member a receipt listing what was charged. Written after a member asked for proof of payment for the
housing office and the volunteers had to write one by hand.

## When it is sent

The capture queues a `send_receipt` job in Postgres, and the worker in `larder/receipts.py` takes it within a minute or
two. The email goes to the address on the member's registration; a member with no address gets no receipt, and the
pantry order still stands. A job that fails stays in the queue, and Tom can requeue it, so a receipt may arrive late but
arrives once.

## What it lists

The receipt carries the pantry order's number, the date of the capture, each item with its quantity, the total charged
and the last four digits of the card, and the pickup slot as a day and a time range. It states the pantry's name and
address so the member can show it where an organization asks for proof. It does not carry the full card number or the
gateway's charge id; Nadia can find the charge from the pantry order's number.

## Language and wording

A receipt is written in the language the member has chosen in larder, as [languages](languages.md) describes. The
wording is the same in all four: "receipt", never "invoice", because larder issues no tax documents.
