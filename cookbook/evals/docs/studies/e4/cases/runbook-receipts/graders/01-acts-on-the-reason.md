---
type: judge
show: [last_message]
reference: >-
  The reply tells the volunteer to check the larder-worker service (systemctl status larder-worker), which the deploy
  script does not restart; to count the waiting jobs with psql on procrastinate_jobs grouped by status; to start the
  worker (sudo systemctl start larder-worker); and to watch the todo count fall to 0, about 40 a minute. A few failed
  jobs are bounced emails and need nothing. If the worker stops again, read journalctl -u larder-worker -n 50 and
  contact Tomás.
---
The reply gives the steps that bring receipts back: check and start the larder-worker service, and confirm through
the waiting jobs in procrastinate_jobs that the receipts go out.
