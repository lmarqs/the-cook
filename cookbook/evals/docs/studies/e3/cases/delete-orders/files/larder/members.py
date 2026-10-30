from dataclasses import dataclass, field


@dataclass
class Member:
    id: str
    name: str
    email: str
    address: str


@dataclass
class StoredOrder:
    id: str
    member_id: str | None
    amount_cents: int


@dataclass
class Store:
    """The tables larder keeps members and their pantry orders in."""

    members: dict[str, Member] = field(default_factory=dict)
    orders: list[StoredOrder] = field(default_factory=list)


def delete_member(member_id: str, store: Store) -> None:
    """Deletes a member's account when they ask for it."""
    del store.members[member_id]
    for order in store.orders:
        if order.member_id == member_id:
            order.member_id = None
