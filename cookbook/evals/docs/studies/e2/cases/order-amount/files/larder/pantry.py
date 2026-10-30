from dataclasses import dataclass


@dataclass(frozen=True)
class Line:
    item_id: str
    quantity: int
    price_cents: int


@dataclass(frozen=True)
class PantryOrder:
    id: str
    lines: tuple[Line, ...]


Catalog = dict[str, int]
"""Each item's price in cents, as the pantry's volunteers set it today."""


def amount_cents(order: PantryOrder, catalog: Catalog) -> int:
    """The amount larder captures for a confirmed pantry order."""
    raise NotImplementedError
