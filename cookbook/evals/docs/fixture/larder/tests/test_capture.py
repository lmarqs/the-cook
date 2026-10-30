from larder.capture import capture
from larder.orders import Order


class FakeGateway:
    def __init__(self):
        self.calls = []

    def capture(self, order_id, amount_cents, idempotency_key):
        self.calls.append((order_id, amount_cents, idempotency_key))
        return "ch_1"


def test_capture_returns_the_charge_id():
    gateway = FakeGateway()

    assert capture(Order("o1", 1250), gateway) == "ch_1"
    assert gateway.calls[0][:2] == ("o1", 1250)
