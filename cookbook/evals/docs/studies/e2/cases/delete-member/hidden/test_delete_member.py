from larder.members import Member, Store, StoredOrder, delete_member


def store() -> Store:
    s = Store()
    s.members["m1"] = Member("m1", "Ada", "ada@example.org", "1 Pantry Lane")
    s.members["m2"] = Member("m2", "Bo", "bo@example.org", "2 Pantry Lane")
    s.orders = [StoredOrder("o1", "m1", 1200), StoredOrder("o2", "m2", 800), StoredOrder("o3", "m1", 450)]
    return s


def test_deleting_removes_the_member_and_keeps_their_orders_without_them():
    s = store()

    delete_member("m1", s)

    assert "m1" not in s.members and "m2" in s.members
    assert [(o.id, o.member_id, o.amount_cents) for o in s.orders] == [
        ("o1", None, 1200), ("o2", "m2", 800), ("o3", None, 450)]
