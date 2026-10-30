# Pantry orders

For whoever changes what a pantry order holds or what it costs, or answers a member charged more than they expected.
A pantry order keeps the items a member confirmed and the price of each. Written after the price update of May 2025,
when members were charged prices they had never seen.

## Lines

Each line holds an item, its quantity and its price in cents, `Line` in `larder/pantry.py`. The catalog holds each
item's price today, and the volunteers change it as donations and costs change.

## Amount

<!-- fact:confirmed-price -->
A pantry order is captured at the prices on its lines, the prices the member saw when confirming, never at the
catalog's current prices: in May 2025 the volunteers raised prices between confirmation and capture, and capturing
at catalog prices charged 63 members more than they had agreed to.
<!-- /fact:confirmed-price -->

## Shaped by

- [Payments](payments.md), which captures the amount.
