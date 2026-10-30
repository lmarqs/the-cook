# Waitlist

For whoever changes pickup slots or answers a member who asks why a full day still gave them a slot. When every pickup
slot of a day is full, a member can join that day's waitlist and receives the next freed slot. Written in December
2024, when a busy Saturday left 14 families phoning the desk each hour to ask whether anyone had dropped out.

## Joining

The slot picker shows a full day greyed out with a "Join the waitlist" button. A member may be on the waitlist of one
day at a time, so one family does not hold places on several days; joining a second day moves the member there. The
waitlist is a table in Postgres ordered by the moment of joining, and a member sees their place in it.

## How a freed slot is offered

When a slot is freed, the worker reads the front of that day's waitlist and holds the slot for that member for two
hours. The offer shows at the top of the member's pantry page, with an "Accept" button. If the member accepts, the slot
becomes theirs and they leave the waitlist. If two hours pass, or the member declines, the worker moves to the next
member and the slot is held for them in the same way. A slot nobody accepts goes back to the open slots when the
waitlist is empty.

## What volunteers see

The admin dashboard lists each day's waitlist length, so Tomás or Leo can raise a pickup slot's limit on a day where
more than 10 members wait. A place added that way is offered to the waitlist before it is shown as open.

## Shaped by

- [Pickup slots](pickup-slots.md), the slots whose freed places the waitlist offers.
- [Opening hours](opening-hours.md), which decides the slots a day has.
