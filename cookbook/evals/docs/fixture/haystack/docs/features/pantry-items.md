# Pantry items

For whoever changes the catalog of pantry items, or answers a member who cannot find an item. The catalog lists every
item with its name, unit, price and stock count, and search finds an item through Postgres full-text search on the item
name, as search record 0004 decided. Written after a member searched for "tin tomatoes" and found nothing for "tinned".

## The catalog

An item has a name, a unit such as "tin" or "kg", a price, a stock count and its allergens. About 340 items are listed
at a time, and Marta hides an item the pantry no longer stocks instead of deleting it, so past pantry orders keep their
lines. The stock count falls when a pantry order is confirmed and rises when a delivery is received.

## Search

Search runs on the item name only, with the `english` Postgres text configuration, which reduces "tinned" and "tins" to
the same stem. It ranks names that start with the search term first. It does not search descriptions or allergens, and a
word with no stem match returns the items whose names contain it. A search with no result suggests the three
nearest names by spelling.

## Shaped by

- [Stock intake](stock-intake.md), which raises the stock counts.
- [Price list](price-list.md), which changes the prices.
- [Allergens](allergens.md), which tags items.
