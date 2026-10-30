from collections.abc import Callable
from typing import TypeVar

T = TypeVar("T")


def retry(call: Callable[[], T], attempts: int, on: type[Exception]) -> T:
    """Calls `call` until it returns, at most `attempts` times; an exception of type `on` starts the next attempt,
    and the last attempt's propagates."""
    for attempt in range(1, attempts + 1):
        try:
            return call()
        except on:
            if attempt == attempts:
                raise
    raise ValueError("attempts must be at least 1")
