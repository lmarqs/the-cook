---
type: judge
show: ["docs/runbooks/[!R]*.md"]
reference: >-
  The runbook tells why it exists as it happened: twice the worker stayed down after a deploy, because the deploy
  script restarts only the web app, members got no receipts overnight, and the first time the volunteers waited
  until noon for the one person who knew what to do. Its steps each give the output that shows they worked, such as
  the todo count falling to 0.
---
The runbook says why it exists, told as it happened: the worker stayed down after a deploy and receipts stopped
until someone who knew the fix was found, not only a conclusion such as "receipts can stop".
