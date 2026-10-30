from larder.limits import can_place_order
from larder.members import Member

ADA = Member("m1", "Ada", "1 Pantry Lane")
BO = Member("m2", "Bo", "1 Pantry Lane")
CY = Member("m3", "Cy", "9 Shelf Road")
MEMBERS = [ADA, BO, CY]


def test_a_household_with_two_open_orders_across_its_members_cannot_order():
    assert not can_place_order(ADA, MEMBERS, {"m2": 2})
    assert not can_place_order(ADA, MEMBERS, {"m1": 1, "m2": 1})


def test_a_household_under_the_limit_can_order():
    assert can_place_order(ADA, MEMBERS, {"m1": 1})
    assert can_place_order(ADA, MEMBERS, {"m3": 5})
