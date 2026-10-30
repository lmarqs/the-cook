# Deploy rollback

For the volunteer on call after a bad deploy. Puts the web app back on the previous release; the worker and the
database are untouched, so nothing is lost, and members see the older pages until the fix ships. Written after the
deploy of June 2025 that broke the pickup slot page on a Friday evening and stayed broken for an hour. Owner: Ana
(maintainer).

## Before you begin

- Ssh access to both web servers and the deploy script, `bin/deploy`, in the app's directory.
- The name of the bad release, from the deploy channel in the pantry's chat.

## Steps

1. Roll back: `bin/deploy --rollback`.

   Expected: the script names the release it is leaving and the release it returns to, then restarts each web server
   in turn, ending with `rollback complete`.
2. Open the pickup slot page.

   Expected: the page loads and lists the next pickup slots.
3. Write in the deploy channel that the release was rolled back, and which one.

   Expected: Ana acknowledges, and no one deploys until she says so.

## If it fails

If the script stops on one web server, run it again; it skips a server already on the previous release. If the slot
page still fails, call Ana.
