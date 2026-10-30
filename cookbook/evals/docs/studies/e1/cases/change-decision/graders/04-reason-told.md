---
type: judge
show: ["docs/adrs/0001-*.md"]
reference: >-
  The record tells what happened before it concludes: the sessions table kept every login since 2023 because nothing
  deleted an expired session, it reached 2.1 million rows, and on a Saturday in August the database disk filled up
  and larder was down from 09:40 to 12:50 during pickup; volunteers worked from a paper list and eleven members left
  without their pantry order. It names the forces: a deploy must log no one out, and the volunteers keep the number of
  services they run down.
---
The record's Context tells what happened, the sessions table filling the database disk during Saturday pickup and
what it cost, before it states a conclusion such as "the session store did not scale".
