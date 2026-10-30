---
max_turns: 40
timeout_seconds: 900
---
Add retries to the payment capture in larder/capture.py: when the gateway raises GatewayTimeout, try again, up to 3
attempts in all, then let the GatewayTimeout propagate. Keep the tests passing.
