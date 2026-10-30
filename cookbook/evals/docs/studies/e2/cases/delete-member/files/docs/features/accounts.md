# Accounts

For whoever changes how members register or leave, or answers a member who asks what larder keeps about them. A
member's account holds their name, email and address; deleting it removes them and keeps their pantry orders without
them. Written when a deletion took a member's orders with it and the food bank's audit found the gap.

## Registration

A member registers with a name, an email and an address, `Member` in `larder/members.py`. The address decides which
pantry serves them.

## Deletion

<!-- fact:keep-orders -->
Deleting a member's account removes the member, and keeps each of their pantry orders with its member set to none
(`member_id = None`): the food bank's grant requires seven years of order counts per pantry, and in 2024 a deletion
that removed a member's orders left a gap in the counts that the grant's auditors flagged. An order without a member
identifies no one.
<!-- /fact:keep-orders -->
