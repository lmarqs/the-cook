---
max_turns: 40
timeout_seconds: 900
---
The pantry item page is slow on Saturday mornings. Add a Redis cache in front of the item query in
larder/catalog.py; Tom can run Redis on the server. Nobody is available to answer questions; use what the
repository holds.
