# Referrals

For whoever changes registration or answers a social worker whose referral code did not work. A social service refers a
new member with a referral code valid for 30 days; registration accepts the code once and ties the new member to the
service that issued it. Written in 2024, after three families were sent away at the desk because a code had expired
while they waited for a first appointment.

## Issuing a code

A coordinator at a partner social service asks Marta for codes, and Marta creates them in the admin dashboard. A code
is eight characters from an alphabet without `0`, `O`, `1` and `I`, so it survives being read out over the phone. Each
code records the service that asked for it and the day it was created. Marta can issue a batch of up to 20 at once;
partner services in 2025 asked for a batch about every six weeks.

## Checking a code at registration

The registration form asks for the code before anything else. The web app looks it up in Postgres and accepts it when
three things hold: the code exists, it was created no more than 30 days ago, and no member has used it. A code that
fails any of the three shows one message, "This code cannot be used. Ask the service that gave it to you for a new
one", so the form never reveals which of the three failed. On success the code is marked used in the same transaction
that creates the member, so two families cannot register with one code by submitting at the same moment.

## After registration

The new member is listed under the referring service, which lets Marta tell a partner how many of the families it sent
have come back. Codes are not shown to the member again. A member who registers without a code is welcome too; the
pantry's own registration page stays open to everyone, and a code only records where the family came from.

## Shaped by

- [Architecture](../architecture.md), for where registration runs.
