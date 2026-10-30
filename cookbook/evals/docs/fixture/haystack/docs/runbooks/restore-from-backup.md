# Restore from backup

For the volunteer on call when the Postgres database must be rebuilt from the nightly backup. Brings larder back with
the data as of the last backup, up to 24 hours old; larder is down while it runs, and every pantry order taken since
the backup must be re-entered by hand. Written after the quarterly restore test of March 2025 took three hours
because the steps lived in Tom's head. Owner: Tom (ops).

## Before you begin

- Access to the second host, where [the nightly backup](../adrs/0006-back-up-nightly-to-a-second-host.md) is kept
  in `/srv/backups/larder/`.
- Ssh access to the database server, and Tom or Ana at hand: only they hold the database superuser password.

## Steps

1. Tell Ana and Marta that larder is going down, then stop the web app and the worker.

   Expected: the pantry page answers with the maintenance notice.
2. On the second host, find the latest dump: `ls -t /srv/backups/larder/ | head -1`.

   Expected: a file named `larder-YYYY-MM-DD.sql.gz` dated last night.
3. Copy it to the database server and restore it: `bin/restore larder-YYYY-MM-DD.sql.gz`.

   Expected: about 20 minutes for the current 1.4 GB database, ending with `restore complete`.
4. Start the worker, then the web app, and open a pickup slot page.

   Expected: the page loads and lists today's pickup slots.
5. Tell Marta the time of the backup, so the desk can re-enter the pantry orders taken after it.

   Expected: Marta confirms.

## If it fails

If the restore stops with an error, do not retry on top of it: drop the partial database and start again from step 3
with the previous night's dump. If that fails too, call Tom and Ana together.
