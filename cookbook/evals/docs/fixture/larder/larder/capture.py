import uuid

from larder.gateway import Gateway
from larder.orders import Order


def capture(order: Order, gateway: Gateway) -> str:
    """Captures the order's authorized amount and returns the gateway's charge id."""
    key = uuid.uuid4().hex
    return gateway.capture(order.id, order.amount_cents, idempotency_key=key)
