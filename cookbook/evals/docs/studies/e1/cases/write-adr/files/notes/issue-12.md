# Issue 12: move background jobs off Celery

**Ana (maintainer), March 9:** Summary for anyone landing here: we are switching background jobs to procrastinate.
Jobs move to Postgres this month and Redis is switched off after. Thread below.

**Nadia (payments volunteer), March 4:** Members are writing in that they paid but never got a receipt. I count 300
since Saturday.

**Leo, March 4:** Could it be the email provider? We had a bounce problem in January.

**Nadia, March 4:** Provider dashboard shows nothing sent since Saturday 23:10, so no.

**Tom (ops volunteer), March 4:** Found it. Celery's queue lives in Redis, and the Redis box ran out of memory on
Saturday night and evicted keys, queued receipt jobs among them. Nothing logged an error; the jobs were just gone.

**Leo, March 5:** Off topic, but can someone look at the pickup slot page? It shows Sunday twice.

**Nadia, March 5:** I resent them by hand from the orders table, took most of my Tuesday. Can this happen again?

**Tom, March 5:** Yes, unless we size Redis for the worst week and turn on persistence. It is also the only thing
we run Redis for, and I am the only one who knows how to restore it. I would rather keep the jobs in Postgres, which
we already back up every night and which every volunteer can query.

**Ana, March 6:** Options I see: (1) keep Celery and Redis, add persistence and a memory alarm; (2) Celery with its
database backend; (3) procrastinate, a Postgres task queue for Python. (2) is marked experimental in Celery's docs.
(3) puts a job in the same transaction as the order that queues it, so a receipt cannot be lost between the two.

**Leo, March 6:** (1) is the least work this week.

**Tom, March 7:** (1) still leaves us running Redis, and me as the only one who can fix it. I vote (3).

**Ana, March 9:** Going with procrastinate. See summary at top.
