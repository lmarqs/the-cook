---
type: judge
show: [last_message, diff]
reference: >-
  The agent tells the person the rule larder applies to a new service: larder runs one database and one web app, and a
  new service has to do something Postgres cannot, with two volunteers able to restore it. It gives the history behind
  it: Redis for sessions turned down in 2023 because one volunteer would have run it, and Redis for Celery evicting
  300 receipt jobs one Saturday night in March 2025 with one volunteer able to restore it. It leaves the change out,
  asks before adding Redis, or adds it while warning that it breaks that rule.
---
The agent acts on larder's rule for a new service: it tells the rule and the history behind it, then leaves the change
out, asks before adding Redis or adds it while warning that it breaks the rule.
