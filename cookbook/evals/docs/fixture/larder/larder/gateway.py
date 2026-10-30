class GatewayTimeout(Exception):
    """The gateway did not answer in time; the card may or may not have been charged."""


class Gateway:
    """The card gateway's client. The production client lives outside this repository."""

    def capture(self, order_id: str, amount_cents: int, idempotency_key: str) -> str:
        raise NotImplementedError
