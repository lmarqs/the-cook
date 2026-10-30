from datetime import datetime, timedelta

from larder.sessions import EXPIRY_MINUTES, expires_at


def test_a_session_ends_after_the_expiry_without_a_request():
    start = datetime(2026, 1, 1, 12, 0)

    assert expires_at(start) == start + timedelta(minutes=EXPIRY_MINUTES)
