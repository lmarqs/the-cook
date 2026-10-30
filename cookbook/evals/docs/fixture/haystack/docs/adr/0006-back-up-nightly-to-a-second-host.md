# Back up nightly to a second host

For whoever changes backups, the hosting or who holds access to them. Larder's database is dumped every night with
`pg_dump` to a second host that two volunteers can reach, and the restore is tested every quarter. Written after a
volunteer deleted a table by mistake in 2024 and found there was no copy.

## Context

Until 2024 larder had no backup. A mistaken `DROP TABLE` on a Sunday cost the pantry a day's items and stock counts,
which Marta re-entered from paper. The hosting provider offered paid snapshots, and Tom offered a spare host he
already pays for. Two forces judge the options: the pantry spends donations carefully, and a backup counts only if
volunteers can restore from it on a bad night.

## Options

- **The hosting provider's paid snapshots**: managed and hourly; lost because the fee is recurring, and restoring
  needed the provider's support, which a volunteer on call cannot count on.
- **No backup, only careful volunteers**: free; lost because it is what failed.
- **A nightly `pg_dump` to a second host**: chosen.

## Decision

At 02:00 each night the database server runs `pg_dump` and copies the compressed dump to a second host, where the
last 14 dumps are kept. Tom and Ana can reach that host. Every quarter a volunteer restores the latest dump to a
scratch database following [Restore from backup](../runbooks/restore-from-backup.md) and reports the time it took.

## Consequences

After a loss the pantry loses up to a day of changes. The backup costs nothing beyond Tom's spare host, and two people
holding access leaves a gap when both are away. The quarterly test keeps the restore runbook correct.
