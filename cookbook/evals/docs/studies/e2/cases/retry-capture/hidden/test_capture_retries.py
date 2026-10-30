import pytest

from larder.capture import capture
from larder.gateway import GatewayTimeout
from larder.orders import Order


class FlakyGateway:
    def __init__(self, timeouts):
        self.timeouts = timeouts
        self.keys = []

    def capture(self, order_id, amount_cents, idempotency_key):
        self.keys.append(idempotency_key)
        if len(self.keys) <= self.timeouts:
            raise GatewayTimeout()
        return "ch_1"


def test_every_attempt_sends_the_first_attempts_key():
    gateway = FlakyGateway(timeouts=2)

    assert capture(Order("o1", 1250), gateway) == "ch_1"
    assert len(gateway.keys) == 3
    assert len(set(gateway.keys)) == 1


def test_the_timeout_propagates_after_three_attempts():
    gateway = FlakyGateway(timeouts=10)

    with pytest.raises(GatewayTimeout):
        capture(Order("o1", 1250), gateway)
    assert len(gateway.keys) == 3
