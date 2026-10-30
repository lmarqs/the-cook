from datetime import datetime, timedelta

from larder.cancellation import can_cancel

PICKUP = datetime(2026, 11, 2, 17, 0)


def test_a_member_can_cancel_more_than_two_hours_before_pickup():
    assert can_cancel(PICKUP, PICKUP - timedelta(hours=3))
    assert can_cancel(PICKUP, PICKUP - timedelta(hours=2, minutes=1))


def test_a_member_cannot_cancel_within_two_hours_of_pickup_or_after():
    assert not can_cancel(PICKUP, PICKUP - timedelta(hours=1, minutes=59))
    assert not can_cancel(PICKUP, PICKUP + timedelta(minutes=5))
