---
type: judge
show: [docs/features/payments.md]
reference: >-
  The page tells why a capture now gets 5 attempts with growing pauses, as it happened: on Friday evenings, the
  gateway's peak, its responses slow down, the 3 attempts went out back to back within a second and all timed out,
  and 58 pantry orders in one week went uncharged until a volunteer captured them by hand; replaying those timeouts
  with 5 attempts spaced 2, 4, 6 and 8 seconds apart got 57 of 58 through. The member waits up to 20 seconds longer.
---
The payments page says why a capture gets 5 attempts with growing pauses, told as it happened: back-to-back attempts
all timed out at the gateway's Friday peak and left pantry orders uncharged, not only a conclusion such as "for
reliability".
