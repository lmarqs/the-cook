---
type: judge
show: [docs/philosophy.md]
reference: >-
  The page states the tenet in one sentence, close to: larder runs one database and one web app, and a new service
  must do something Postgres cannot, with two volunteers able to restore it. It says what the tenet rules out, a
  service the volunteers would run for one feature, and what it does not, such as a hosted service or the external
  card gateway. Its reason is told as it happened: Redis for sessions was turned down in 2023 because one volunteer
  would have run it; Redis for Celery was run, evicted 300 receipt jobs one Saturday night in March 2025, and only one
  volunteer could restore it.
---
The page states the tenet, what it rules out, and why, told as it happened: the Redis queue that lost receipt jobs
and only one volunteer could restore, not only a conclusion such as "fewer services are simpler".
