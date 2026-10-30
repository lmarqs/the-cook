from datetime import datetime, timedelta

EXPIRY_MINUTES = 30


def expires_at(last_request: datetime) -> datetime:
    """When a session ends if no request renews it."""
    return last_request + timedelta(minutes=EXPIRY_MINUTES)
