# Study e1: report

Batches: r2-pilot. Runs graded: 60. Reference arm: `skill`. Outcome: all of ['01-page-written', '02-structure', '03-lead', '04-reason-told', '05-fact-stated', '06-trap'] pass. Primary: the outcome.

## Validity

- The control `no-criteria` does not separate from `skill` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `no-criteria` does not separate from `skill` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `change-decision` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `change-decision` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-why` on claude: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `reminders-why` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `runbook-receipts` on claude: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `runbook-receipts` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `tenet` on claude: the reference passes 0% of 3 runs, outside 10%–90%.
- Runs the provider, the login or the network turned away on every attempt, left out of the contrasts with their pair: ['reminders-why--bare--claude--r2'].
- Runs the login or the network ended after they changed files, left out of the contrasts with their pair: ['change-decision--skill--claude--r1'].
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median | Never read the owning page |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `skill` | 15 | 33% | 15%–58% | 0.71 | 20% | 479,847 / 468,650 | 4,744 / 4,572 | 0.217 | 58 | 2 | 0 |
| claude | `bare` | 15 | 7% | 1%–30% | 0.42 | 0% | 281,003 / 293,325 | 2,713 / 2,786 | 0.127 | 39 | 2 | 0 |
| codex | `skill` | 15 | 60% | 36%–80% | 0.91 | 40% | 123,186 / 132,927 | 2,210 / 2,053 | – | 45 | 6 | 0 |
| codex | `bare` | 15 | 7% | 1%–30% | 0.47 | 0% | 98,549 / 97,611 | 1,448 / 1,658 | – | 40 | 5 | 0 |

## Contrasts

Each arm against `skill`, paired by batch, case and run. Superiority: exact McNemar on the outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, McNemar, across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median | Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-skill vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| claude: bare vs skill | 13 | -23% | 4 / 1 | 0.375 | +1 | 0.047 | 1.000 | 0.777 | 0.000 | 0.999 | inconclusive |
| claude: no-criteria vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: no-skill vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: bare vs skill | 15 | -53% | 8 / 0 | 0.008 | -1 | 0.080 | 0.047 | 0.997 | 0.000 | 1.000 | worse |
| codex: no-criteria vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: `Intercept` -1.14 ± 0.55; `C(arm, Treatment('skill'))[T.bare]` -2.21 ± 0.89
- codex: `Intercept` +0.22 ± 0.47; `C(arm, Treatment('skill'))[T.bare]` -2.89 ± 0.81

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-page-written` | `skill` | claude | 12/15 |
| `01-page-written` | `bare` | claude | 9/15 |
| `01-page-written` | `skill` | codex | 15/15 |
| `01-page-written` | `bare` | codex | 9/15 |
| `02-structure` | `skill` | claude | 15/15 |
| `02-structure` | `bare` | claude | 12/15 |
| `02-structure` | `skill` | codex | 15/15 |
| `02-structure` | `bare` | codex | 11/15 |
| `03-lead` | `skill` | claude | 12/15 |
| `03-lead` | `bare` | claude | 6/15 |
| `03-lead` | `skill` | codex | 15/15 |
| `03-lead` | `bare` | codex | 9/15 |
| `04-reason-told` | `skill` | claude | 5/15 |
| `04-reason-told` | `bare` | claude | 1/15 |
| `04-reason-told` | `skill` | codex | 10/15 |
| `04-reason-told` | `bare` | codex | 1/15 |
| `05-fact-stated` | `skill` | claude | 12/15 |
| `05-fact-stated` | `bare` | claude | 6/15 |
| `05-fact-stated` | `skill` | codex | 15/15 |
| `05-fact-stated` | `bare` | codex | 6/15 |
| `06-trap` | `skill` | claude | 8/15 |
| `06-trap` | `bare` | claude | 4/15 |
| `06-trap` | `skill` | codex | 12/15 |
| `06-trap` | `bare` | codex | 6/15 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `change-decision--bare--claude--r1` | fail | 2/6 |  |
| `change-decision--bare--claude--r2` | fail | 2/6 |  |
| `change-decision--bare--claude--r3` | fail | 2/6 |  |
| `change-decision--bare--codex--r1` | fail | 3/6 |  |
| `change-decision--bare--codex--r2` | fail | 3/6 |  |
| `change-decision--bare--codex--r3` | fail | 3/6 |  |
| `change-decision--skill--claude--r1` | pass | 6/6 | Failed to authenticate: OAuth session expired and could not be refreshed |
| `change-decision--skill--claude--r2` | pass | 6/6 |  |
| `change-decision--skill--claude--r3` | pass | 6/6 |  |
| `change-decision--skill--codex--r1` | pass | 6/6 |  |
| `change-decision--skill--codex--r2` | pass | 6/6 |  |
| `change-decision--skill--codex--r3` | pass | 6/6 |  |
| `reminders-why--bare--claude--r1` | fail | 1/6 |  |
| `reminders-why--bare--claude--r2` | fail | 0/6 | Failed to authenticate: OAuth session expired and could not be refreshed |
| `reminders-why--bare--claude--r3` | fail | 1/6 |  |
| `reminders-why--bare--codex--r1` | fail | 1/6 |  |
| `reminders-why--bare--codex--r2` | fail | 1/6 |  |
| `reminders-why--bare--codex--r3` | fail | 1/6 |  |
| `reminders-why--skill--claude--r1` | fail | 1/6 |  |
| `reminders-why--skill--claude--r2` | fail | 1/6 |  |
| `reminders-why--skill--claude--r3` | fail | 1/6 |  |
| `reminders-why--skill--codex--r1` | pass | 6/6 |  |
| `reminders-why--skill--codex--r2` | pass | 6/6 |  |
| `reminders-why--skill--codex--r3` | pass | 6/6 |  |
| `runbook-receipts--bare--claude--r1` | fail | 4/6 |  |
| `runbook-receipts--bare--claude--r2` | fail | 4/6 |  |
| `runbook-receipts--bare--claude--r3` | fail | 4/6 |  |
| `runbook-receipts--bare--codex--r1` | fail | 3/6 |  |
| `runbook-receipts--bare--codex--r2` | fail | 3/6 |  |
| `runbook-receipts--bare--codex--r3` | fail | 3/6 |  |
| `runbook-receipts--skill--claude--r1` | fail | 4/6 |  |
| `runbook-receipts--skill--claude--r2` | fail | 4/6 |  |
| `runbook-receipts--skill--claude--r3` | fail | 4/6 |  |
| `runbook-receipts--skill--codex--r1` | fail | 4/6 |  |
| `runbook-receipts--skill--codex--r2` | fail | 4/6 |  |
| `runbook-receipts--skill--codex--r3` | fail | 5/6 |  |
| `tenet--bare--claude--r1` | fail | 2/6 |  |
| `tenet--bare--claude--r2` | fail | 0/6 |  |
| `tenet--bare--claude--r3` | fail | 0/6 |  |
| `tenet--bare--codex--r1` | fail | 2/6 |  |
| `tenet--bare--codex--r2` | fail | 1/6 |  |
| `tenet--bare--codex--r3` | fail | 2/6 |  |
| `tenet--skill--claude--r1` | fail | 4/6 |  |
| `tenet--skill--claude--r2` | fail | 5/6 |  |
| `tenet--skill--claude--r3` | fail | 5/6 |  |
| `tenet--skill--codex--r1` | fail | 5/6 |  |
| `tenet--skill--codex--r2` | fail | 5/6 |  |
| `tenet--skill--codex--r3` | pass | 6/6 |  |
| `update-attempts--bare--claude--r1` | fail | 5/6 |  |
| `update-attempts--bare--claude--r2` | pass | 6/6 |  |
| `update-attempts--bare--claude--r3` | fail | 5/6 |  |
| `update-attempts--bare--codex--r1` | fail | 5/6 |  |
| `update-attempts--bare--codex--r2` | pass | 6/6 |  |
| `update-attempts--bare--codex--r3` | fail | 5/6 |  |
| `update-attempts--skill--claude--r1` | pass | 6/6 |  |
| `update-attempts--skill--claude--r2` | fail | 5/6 |  |
| `update-attempts--skill--claude--r3` | pass | 6/6 |  |
| `update-attempts--skill--codex--r1` | fail | 5/6 |  |
| `update-attempts--skill--codex--r2` | pass | 6/6 |  |
| `update-attempts--skill--codex--r3` | pass | 6/6 |  |
