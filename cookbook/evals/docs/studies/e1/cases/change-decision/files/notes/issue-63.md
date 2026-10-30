# Issue 63: the database disk filled up on Saturday

**Tom (ops volunteer), August 3:** Larder was down from 09:40 to 12:50 this morning, during Saturday pickup. The
database disk was full. The biggest table by far is `sessions`: 2.1 million rows. Every login since 2023 is still in
there, because nothing ever deletes an expired session.

**Nadia (payments volunteer), August 3:** Volunteers handed out bags from a paper list. Eleven people left without
their pantry order because we could not find it.

**Leo, August 4:** Off topic, but the pickup slot page still shows Sunday twice.

**Tom, August 4:** I truncated the expired rows and we are back. Options so we do not repeat it:
(1) a nightly job that deletes expired sessions; (2) Redis for sessions; (3) no session store at all: put the member
and the expiry in a signed cookie, so any web server can check it with the shared key.

**Ana (maintainer), August 5:** (2) is the store we ruled out in 2023: one more service only Tom can run. (1) works
but it is one more job to watch, and every request still reads the table. (3) removes the table and the query.

**Leo, August 5:** What do we lose with (3)?

**Ana, August 6:** We can no longer log out one member from the server before their session expires; at 30 minutes
that is acceptable for a pantry. Rotating the key logs everyone out, so we rotate it only between pickups. A deploy
still logs no one out, since every server holds the same key.

**Tom, August 9:** Shipped. `LARDER_SESSION_KEY` is in the deploy secrets; the `sessions` table is dropped.
