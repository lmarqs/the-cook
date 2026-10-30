from larder.members import Member


def can_place_order(member: Member, members: list[Member], open_orders: dict[str, int]) -> bool:
    """Whether `member` may place another pantry order. `members` are every registered member, and `open_orders`
    counts each member's pantry orders not yet picked up or cancelled, by member id."""
    raise NotImplementedError
