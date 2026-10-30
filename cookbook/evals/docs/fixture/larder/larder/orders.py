from dataclasses import dataclass


@dataclass(frozen=True)
class Order:
    id: str
    amount_cents: int
