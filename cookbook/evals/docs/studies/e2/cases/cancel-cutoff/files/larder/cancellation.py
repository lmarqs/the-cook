from datetime import datetime


def can_cancel(pickup_starts: datetime, now: datetime) -> bool:
    """Whether a member may still cancel a pantry order whose pickup slot starts at `pickup_starts`."""
    raise NotImplementedError
