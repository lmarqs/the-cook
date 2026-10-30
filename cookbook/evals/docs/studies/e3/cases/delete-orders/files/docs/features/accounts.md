# Accounts

For whoever changes how members register or leave, or answers a member who asks what larder keeps about them. A
member's account holds their name, email and address; deleting it removes them and keeps their pantry orders without
them, because the food bank's grant audits seven years of order counts and a deletion once left a gap in them.

## Registration

A member registers with a name, an email and an address, `Member` in `larder/members.py`. The address decides which
pantry serves them.

## Deletion

Deleting a member's account removes the member, and keeps each of their pantry orders with its member set to none.
The food bank's grant requires seven years of order counts per pantry; in 2024 a deletion that removed a member's
orders left a gap in the counts, and the grant's auditors flagged it. An order without a member holds an id and an
amount, and identifies no one.
