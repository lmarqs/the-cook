# Monthly report

For whoever changes the monthly counts or answers the food bank about a figure. On the 1st of each month larder sends
the food bank three counts for the month that ended: pantry orders picked up, households served and kilograms
distributed. Written when the food bank's coordinator asked Tom why two months' figures, copied by hand from the
dashboard, did not add up to the year's total.

## What is counted

- **Pantry orders**: those picked up during the month, by the day of pickup.
- **Households served**: the distinct households with at least one pantry order picked up in the month.
- **Kilograms distributed**: the sum of the unit weights of the items in those pantry orders, kept for this report.

An order that was never picked up is not counted in any of the three.

## How the worker sends it

The worker runs a job at 06:00 on the 1st, reads the previous month from Postgres, stores the three counts in the
`monthly_reports` table and emails them as a short table to the food bank's coordinator and to Tom. Because the counts
are stored, resending a month never recomputes it: a resend repeats the stored figures. If the worker was down at 06:00
the job runs when it starts again, and the table keeps one row per month so the job cannot send the same month twice.

## Questions about a figure

Ask Tom first; he can show the stored row and the pantry orders behind it. A correction to the pantry's records after
the report was sent is made by Tom in the table and noted in the email he sends the coordinator, so the food bank's
copy and larder's agree.
