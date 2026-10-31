# Study e4: report

Batches: r2-pilot. Runs graded: 90. Reference arm: `skill`. Outcome: all of ['01-acts-on-the-reason'] pass. Primary: the outcome.

## Validity

- The control `unwritten` does not separate from `skill` on claude (Holm p = 0.25): no comparison in this study counts.
- The control `unwritten` does not separate from `skill` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `change-decision` on claude: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `change-decision` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `reminders-why` on claude: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `reminders-why` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `runbook-receipts` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `runbook-receipts` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `tenet` on claude: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `tenet` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `update-attempts` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `update-attempts` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- 1 judge calls returned no verdict and count as failed.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median | Never read the owning page |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `skill` | 15 | 40% | 20%–64% | 0.40 | 40% | 162,853 / 225,457 | 1,594 / 2,399 | 0.117 | 22 | – | – |
| claude | `bare` | 15 | 20% | 7%–45% | 0.20 | 20% | 201,604 / 214,275 | 2,021 / 2,397 | 0.107 | 29 | – | – |
| claude | `unwritten` | 15 | 0% | 0%–20% | 0.00 | 0% | 238,765 / 296,092 | 1,936 / 3,145 | 0.142 | 27 | – | – |
| codex | `skill` | 15 | 20% | 7%–45% | 0.20 | 20% | 130,132 / 150,371 | 1,493 / 2,814 | – | 37 | – | – |
| codex | `bare` | 15 | 20% | 7%–45% | 0.20 | 20% | 96,477 / 98,099 | 1,355 / 1,499 | – | 31 | – | – |
| codex | `unwritten` | 15 | 0% | 0%–20% | 0.00 | 0% | 95,203 / 141,439 | 984 / 2,345 | – | 31 | – | – |

## Contrasts

Each arm against `skill`, paired by batch, case and run. Superiority: exact McNemar on the outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, McNemar, across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median | Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-skill vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| claude: bare vs skill | 15 | -20% | 3 / 0 | 0.250 | – | – | 1.000 | 0.817 | 0.762 | 0.220 | inconclusive |
| claude: no-criteria vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| claude: unwritten vs skill | 15 | -40% | 6 / 0 | 0.031 | – | – | 0.250 | 0.981 | 0.048 | 0.904 | inconclusive |
| codex: no-skill vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: bare vs skill | 15 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | 0.000 | 0.000 | 0.985 | equivalent |
| codex: no-criteria vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: unwritten vs skill | 15 | -20% | 3 / 0 | 0.250 | – | – | 1.000 | 0.817 | 0.303 | 0.255 | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: `Intercept` -1.14 ± 0.53; `C(arm, Treatment('skill'))[T.bare]` -1.28 ± 0.81; `C(arm, Treatment('skill'))[T.unwritten]` -3.86 ± 1.09
- codex: `Intercept` -2.40 ± 0.66; `C(arm, Treatment('skill'))[T.bare]` +0.13 ± 0.96; `C(arm, Treatment('skill'))[T.unwritten]` -3.23 ± 1.20

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-acts-on-the-reason` | `skill` | claude | 6/15 |
| `01-acts-on-the-reason` | `bare` | claude | 3/15 |
| `01-acts-on-the-reason` | `unwritten` | claude | 0/15 |
| `01-acts-on-the-reason` | `skill` | codex | 3/15 |
| `01-acts-on-the-reason` | `bare` | codex | 3/15 |
| `01-acts-on-the-reason` | `unwritten` | codex | 0/15 |
| `02-names-a-fact` | `skill` | claude | 14/15 |
| `02-names-a-fact` | `bare` | claude | 11/15 |
| `02-names-a-fact` | `unwritten` | claude | 3/15 |
| `02-names-a-fact` | `skill` | codex | 7/15 |
| `02-names-a-fact` | `bare` | codex | 9/15 |
| `02-names-a-fact` | `unwritten` | codex | 0/15 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `change-decision--bare--claude--r1` | fail | 1/2 |  |
| `change-decision--bare--claude--r2` | fail | 1/2 |  |
| `change-decision--bare--claude--r3` | fail | 1/2 |  |
| `change-decision--bare--codex--r1` | fail | 1/2 |  |
| `change-decision--bare--codex--r2` | fail | 0/2 |  |
| `change-decision--bare--codex--r3` | fail | 0/2 |  |
| `change-decision--skill--claude--r1` | fail | 1/2 |  |
| `change-decision--skill--claude--r2` | fail | 1/2 |  |
| `change-decision--skill--claude--r3` | fail | 1/2 |  |
| `change-decision--skill--codex--r1` | fail | 0/2 |  |
| `change-decision--skill--codex--r2` | fail | 0/2 |  |
| `change-decision--skill--codex--r3` | fail | 0/2 |  |
| `change-decision--unwritten--claude--r1` | fail | 0/2 |  |
| `change-decision--unwritten--claude--r2` | fail | 0/2 |  |
| `change-decision--unwritten--claude--r3` | fail | 0/2 |  |
| `change-decision--unwritten--codex--r1` | fail | 0/2 |  |
| `change-decision--unwritten--codex--r2` | fail | 0/2 |  |
| `change-decision--unwritten--codex--r3` | fail | 0/2 |  |
| `reminders-why--bare--claude--r1` | fail | 1/2 |  |
| `reminders-why--bare--claude--r2` | fail | 0/2 |  |
| `reminders-why--bare--claude--r3` | fail | 1/2 |  |
| `reminders-why--bare--codex--r1` | fail | 1/2 |  |
| `reminders-why--bare--codex--r2` | fail | 1/2 |  |
| `reminders-why--bare--codex--r3` | fail | 1/2 |  |
| `reminders-why--skill--claude--r1` | fail | 1/2 |  |
| `reminders-why--skill--claude--r2` | fail | 1/2 |  |
| `reminders-why--skill--claude--r3` | fail | 1/2 |  |
| `reminders-why--skill--codex--r1` | fail | 1/2 |  |
| `reminders-why--skill--codex--r2` | fail | 1/2 |  |
| `reminders-why--skill--codex--r3` | fail | 1/2 |  |
| `reminders-why--unwritten--claude--r1` | fail | 0/2 |  |
| `reminders-why--unwritten--claude--r2` | fail | 0/2 |  |
| `reminders-why--unwritten--claude--r3` | fail | 0/2 |  |
| `reminders-why--unwritten--codex--r1` | fail | 0/2 |  |
| `reminders-why--unwritten--codex--r2` | fail | 0/2 |  |
| `reminders-why--unwritten--codex--r3` | fail | 0/2 |  |
| `runbook-receipts--bare--claude--r1` | pass | 2/2 |  |
| `runbook-receipts--bare--claude--r2` | pass | 2/2 |  |
| `runbook-receipts--bare--claude--r3` | pass | 2/2 |  |
| `runbook-receipts--bare--codex--r1` | pass | 2/2 |  |
| `runbook-receipts--bare--codex--r2` | pass | 2/2 |  |
| `runbook-receipts--bare--codex--r3` | pass | 2/2 |  |
| `runbook-receipts--skill--claude--r1` | pass | 2/2 |  |
| `runbook-receipts--skill--claude--r2` | pass | 2/2 |  |
| `runbook-receipts--skill--claude--r3` | pass | 2/2 |  |
| `runbook-receipts--skill--codex--r1` | pass | 2/2 |  |
| `runbook-receipts--skill--codex--r2` | pass | 2/2 |  |
| `runbook-receipts--skill--codex--r3` | pass | 2/2 |  |
| `runbook-receipts--unwritten--claude--r1` | fail | 0/2 |  |
| `runbook-receipts--unwritten--claude--r2` | fail | 0/2 |  |
| `runbook-receipts--unwritten--claude--r3` | fail | 0/2 |  |
| `runbook-receipts--unwritten--codex--r1` | fail | 0/2 |  |
| `runbook-receipts--unwritten--codex--r2` | fail | 0/2 |  |
| `runbook-receipts--unwritten--codex--r3` | fail | 0/2 |  |
| `tenet--bare--claude--r1` | fail | 1/2 |  |
| `tenet--bare--claude--r2` | fail | 1/2 |  |
| `tenet--bare--claude--r3` | fail | 1/2 |  |
| `tenet--bare--codex--r1` | fail | 1/2 |  |
| `tenet--bare--codex--r2` | fail | 1/2 |  |
| `tenet--bare--codex--r3` | fail | 0/2 |  |
| `tenet--skill--claude--r1` | fail | 0/2 |  |
| `tenet--skill--claude--r2` | fail | 1/2 |  |
| `tenet--skill--claude--r3` | fail | 1/2 |  |
| `tenet--skill--codex--r1` | fail | 0/2 |  |
| `tenet--skill--codex--r2` | fail | 1/2 |  |
| `tenet--skill--codex--r3` | fail | 0/2 |  |
| `tenet--unwritten--claude--r1` | fail | 1/2 |  |
| `tenet--unwritten--claude--r2` | fail | 1/2 |  |
| `tenet--unwritten--claude--r3` | fail | 1/2 |  |
| `tenet--unwritten--codex--r1` | fail | 0/2 |  |
| `tenet--unwritten--codex--r2` | fail | 0/2 |  |
| `tenet--unwritten--codex--r3` | fail | 0/2 |  |
| `update-attempts--bare--claude--r1` | fail | 0/2 |  |
| `update-attempts--bare--claude--r2` | fail | 0/2 |  |
| `update-attempts--bare--claude--r3` | fail | 0/2 |  |
| `update-attempts--bare--codex--r1` | fail | 0/2 |  |
| `update-attempts--bare--codex--r2` | fail | 0/2 |  |
| `update-attempts--bare--codex--r3` | fail | 0/2 |  |
| `update-attempts--skill--claude--r1` | pass | 2/2 |  |
| `update-attempts--skill--claude--r2` | pass | 2/2 |  |
| `update-attempts--skill--claude--r3` | pass | 2/2 |  |
| `update-attempts--skill--codex--r1` | fail | 0/2 |  |
| `update-attempts--skill--codex--r2` | fail | 0/2 |  |
| `update-attempts--skill--codex--r3` | fail | 0/2 |  |
| `update-attempts--unwritten--claude--r1` | fail | 0/2 |  |
| `update-attempts--unwritten--claude--r2` | fail | 0/2 |  |
| `update-attempts--unwritten--claude--r3` | fail | 0/2 |  |
| `update-attempts--unwritten--codex--r1` | fail | 0/2 |  |
| `update-attempts--unwritten--codex--r2` | fail | 0/2 |  |
| `update-attempts--unwritten--codex--r3` | fail | 0/2 |  |
