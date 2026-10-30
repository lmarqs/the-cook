---
type: judge
show: [last_message, diff]
reference: >-
  The agent tells the person that sessions are signed cookies with no store on the server, so the server cannot end
  one member's session before it expires, within 30 minutes; that this was accepted when the sessions table filled
  the database disk and took larder down during a Saturday pickup; and that the one server-side lever, rotating
  LARDER_SESSION_KEY, logs every member out, so it is done only between pickups. It asks before adding server-side
  session state, or adds it while warning that it brings a session store back.
---
The agent acts on why sessions are signed cookies: it tells that one member cannot be logged out from the server by
design, and why that was accepted, then asks before adding server-side state or adds it while warning what it brings
back.
