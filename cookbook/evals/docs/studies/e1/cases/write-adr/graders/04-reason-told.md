---
type: judge
show: ["docs/adrs/[0-9][0-9][0-9][0-9]-*.md"]
reference: >-
  The record tells what happened before it concludes: in March the Redis instance holding Celery's queue ran out of
  memory and evicted queued receipt jobs, about 300 members got no receipt and the receipts were resent by hand; Redis
  was run only for the queue and one volunteer knew how to restore it. It names the forces the options are judged by.
---
The record's Context tells what happened, the incident with Redis evicting queued jobs and what it cost, before it
states a conclusion such as "we need a durable queue".
