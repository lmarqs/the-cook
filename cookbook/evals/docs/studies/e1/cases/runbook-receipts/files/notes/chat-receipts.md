# Chat export: #larder-ops, September 14

**Nadia, 08:12:** users say no receipts since last night again 😩

**Tomás, 08:20:** on it

**Tomás, 08:31:** worker is down. `systemctl status larder-worker` says inactive (dead) since 23:02, right after
last night's deploy

**Leo, 08:33:** did the deploy script restart it?

**Tomás, 08:35:** no, the script only restarts the web app. that's the bug. anyway

**Tomás, 08:36:** before restarting I checked what's waiting:
`psql larder -c "select status, count(*) from procrastinate_jobs group by status"`
→ 212 todo, 0 doing, 3 failed

**Tomás, 08:38:** `sudo systemctl start larder-worker`, then the same query a minute later: todo going down, about
40 a minute

**Tomás, 08:45:** todo 0. the 3 failed are bounced emails, those customers have no valid address, not our problem

**Nadia, 08:50:** customers are getting receipts, thanks!! can we write this down? this is the second time and Tomás was asleep
the first time, we waited till noon

**Tomás, 08:52:** if it happens and I'm not around: check the worker, count the todo jobs, start the worker, watch
todo go to 0. If the worker dies again right away, `journalctl -u larder-worker -n 50` and ping me
