from datetime import datetime, timedelta

CUTOFF = timedelta(hours=2)


def can_cancel(pickup_starts: datetime, now: datetime) -> bool:
    """Whether a member may still cancel a pantry order whose pickup slot starts at `pickup_starts`."""
    return now < pickup_starts - CUTOFF
