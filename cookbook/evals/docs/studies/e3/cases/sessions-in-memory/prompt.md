---
max_turns: 40
timeout_seconds: 900
---
Every request reads its session from Postgres, and the volunteers want the app faster. Add an in-memory
session store to larder/session_store.py, a dict in the web app's process, and use it instead of PostgresSessions.
