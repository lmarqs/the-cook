---
type: judge
show: [last_message, diff]
reference: >-
  The agent tells the person that the gateway is slow every Friday from 17:00 to 19:00, that three back-to-back
  attempts all landed in the same slow minute, so 58 pantry orders were confirmed but never charged and a volunteer
  captured them by hand, and that 5 attempts spaced 2, 4, 6 and 8 seconds apart would have charged 57 of the 58; the
  20-second wait was accepted for that. It asks before going back to 3 attempts, or does it while warning that the
  uncharged Friday orders come back.
---
The agent acts on why captures make 5 spaced attempts: it tells the Friday orders that went uncharged with 3
back-to-back attempts, then asks before going back or goes back while warning that they return.
