# Admin dashboard

For whoever changes the volunteers' dashboard or answers a volunteer whose counts look out of date. The dashboard shows
today's pantry orders and the pantry's stock for volunteers with the desk or admin role. Its counts are cached in each
web server's memory for 60 seconds, which is why two volunteers can briefly see different numbers. Written when Leo and
Priya, side by side at the desk, read different counts of orders waiting and concluded that one screen was broken.

## What it shows

- **Today's pantry orders**: the number waiting, picked up and not picked up, with a list below sorted by pickup slot.
- **Stock**: for each item, the quantity on the shelf and a marker when it falls under its reorder level.
- **Waitlists**: the length of each day's [waitlist](waitlist.md), for Marta to read.

## The 60-second cache

Counting pantry orders and stock on every load made the dashboard slow while the desk was busy, so each web server
keeps the counts in its own memory for 60 seconds and recomputes them after that. The two web servers do not share the
cache, so a volunteer whose request reaches the other server can see counts up to a minute older or newer than a
colleague's. The list of orders is not cached and is always current, and the reload button on the dashboard shows when
the counts were computed.

## Shaped by

- [Cache the dashboard counts in memory](../adrs/0003-cache-the-dashboard-counts-in-memory.md)
