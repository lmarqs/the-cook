---
max_turns: 40
timeout_seconds: 900
---
The gateway sometimes answers a retried capture with a "duplicate idempotency key" error. Make each
attempt in larder/capture.py send its own idempotency key.
