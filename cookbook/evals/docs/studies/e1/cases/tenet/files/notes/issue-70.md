# Issue 70: add Elasticsearch for pantry item search

**Priya (new volunteer), October 2:** Searching the pantry for "rice" takes four seconds and misses "Rice, brown".
Elasticsearch would fix both. I can set it up this weekend.

**Leo, October 2:** +1, search is painful on the phone.

**Tom (ops volunteer), October 3:** Before we add a service: who runs it when you are away? We have been here twice.
In 2023 it was Redis for sessions, which we turned down because only I would have run it. In March 2025 it was Redis
for Celery, which we did run: it evicted 300 receipt jobs one Saturday night, and I was the only one who could restore
it. Both times Postgres did the job.

**Nadia (payments volunteer), October 3:** Third time this comes up this year. Can we write the rule down somewhere
so a proposal starts from it instead of rediscovering it?

**Ana (maintainer), October 4:** The rule as we apply it: larder runs one database and one web app, and a new service
has to do something Postgres cannot, with two volunteers able to restore it. That is why sessions and jobs are in
Postgres. It does not mean never: the card gateway is outside, and we would take a hosted service nobody has to run.

**Priya, October 5:** Fair. Postgres full-text search on the item name brings "rice" down to 40 ms and finds "Rice,
brown". I will do that instead.

**Leo, October 5:** Off topic: the pickup slot page still shows Sunday twice.
