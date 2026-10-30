# Reminders

For whoever changes pickup reminders or what they cost, or answers a member who missed a pickup. Larder reminds a
member of their pickup slot by SMS the day before. It does not use email: in the 2025 email trial a third of members
never opened the reminders, and missed pickups doubled.

## Channel

A reminder goes by SMS to the member's phone, `CHANNEL` in `larder/reminders.py`. From April to June 2025 the pantry
tried email to save the SMS cost: 34% of reminders were never opened, missed pickups rose from 6% to 13% of pantry
orders, and the volunteers threw away the fresh food in the bags nobody collected. SMS came back in July.

## Timing

A reminder is sent at 10:00 the day before the pickup slot.
