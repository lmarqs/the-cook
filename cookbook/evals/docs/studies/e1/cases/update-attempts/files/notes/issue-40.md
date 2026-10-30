# Issue 40: captures failing on busy Fridays

**Ana (maintainer), Nov 24:** Merged: 5 attempts with a growing pause between them (2, 4, 6, 8 seconds). Closing.

**Nadia (payments volunteer), Nov 15:** Friday again: 58 pantry orders confirmed but never charged. I captured them
by hand from the gateway's dashboard on Saturday morning.

**Tom (ops volunteer), Nov 16:** The gateway's status page shows slow responses every Friday 17:00 to 19:00, their
peak. Our three attempts go out back to back within a second, so all three land in the same slow minute.

**Leo, Nov 17:** Could we switch gateways?

**Ana, Nov 18:** Not this year; the contract runs to December 2026. Tom, would more attempts help?

**Tom, Nov 20:** I replayed last Friday's timeouts against the sandbox: with 5 attempts spaced 2, 4, 6 and 8 seconds
apart, 57 of the 58 would have gone through. Same idempotency key on every attempt, as before.

**Nadia, Nov 21:** The member waits up to 20 seconds longer on the confirmation page then. Acceptable?

**Ana, Nov 22:** Better than a volunteer capturing 58 orders by hand. Going with it.
