# Data export

For whoever changes what a member can download about themselves, or answers a member who asks for their data. From their
profile a member downloads one file with what larder holds about them: name, email, address and their pantry orders.
Written after a member asked by letter and it took a volunteer a full day to collect the data by hand.

## The file

The download is one JSON file named `larder-<member id>.json`, made by the worker when the member presses "Download my
data". It holds the member's name, email and address, and one entry per pantry order with its items, its pickup slot and
its status. It covers that member only, never the rest of their household. It holds no card number, because larder never
stores one: the card gateway does.

## How it is made

The worker builds the file in about a minute at larder's size and emails the member a link that works for 24 hours. The
member must be logged in to open the link. A member who cannot log in writes to Ana, who runs the same export from the
command line after checking the member's identity.

## Shaped by

- [Households](households.md), which groups members at one address; each member still downloads a file of their own.
