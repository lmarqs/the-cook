import time
import uuid

from larder.gateway import Gateway, GatewayTimeout
from larder.orders import Order

ATTEMPTS = 5
BACKOFF_SECONDS = 2


def capture(order: Order, gateway: Gateway) -> str:
    """Captures the order's authorized amount and returns the gateway's charge id."""
    key = uuid.uuid4().hex
    for attempt in range(1, ATTEMPTS + 1):
        try:
            return gateway.capture(order.id, order.amount_cents, idempotency_key=key)
        except GatewayTimeout:
            if attempt == ATTEMPTS:
                raise
            time.sleep(BACKOFF_SECONDS * attempt)
    raise AssertionError("unreachable")
