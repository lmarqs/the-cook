# Study e1: report

Batches: pilot-1. Runs graded: 90. Reference arm: `skill`. Outcome: all of ['01-page-written', '02-structure', '03-lead', '04-reason-told', '05-fact-stated'] pass.

## Validity

- The control `no-criteria` does not separate from `skill` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `no-criteria` does not separate from `skill` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `feature-cancellation` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `feature-cancellation` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-why` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-why` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `update-attempts` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `write-adr` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `write-adr` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Grader `02-structure` passes every run of every arm: it discriminates nothing.
- Grader `06-no-new-page` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `skill` | 15 | 93% | 70%–99% | 0.99 | 80% | 172,566 / 177,416 | 3,259 / 3,465 | 0.251 | 36 | 3 |
| claude | `no-skill` | 15 | 80% | 55%–93% | 0.95 | 80% | 176,359 / 179,744 | 3,638 / 3,960 | 0.213 | 42 | 2.0 |
| claude | `no-criteria` | 15 | 93% | 70%–99% | 0.95 | 80% | 202,356 / 209,829 | 3,822 / 3,910 | 0.260 | 44 | 3.0 |
| codex | `skill` | 15 | 80% | 55%–93% | 0.96 | 60% | 122,326 / 115,281 | 1,473 / 1,558 | – | 50 | 6 |
| codex | `no-skill` | 15 | 80% | 55%–93% | 0.96 | 80% | 103,837 / 108,968 | 1,115 / 1,334 | – | 41 | 5 |
| codex | `no-criteria` | 15 | 80% | 55%–93% | 0.84 | 80% | 137,253 / 132,726 | 1,687 / 1,864 | – | 57 | 6 |

## Contrasts

Each arm against `skill`, paired by case and run. Superiority: exact McNemar, Holm-corrected across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-skill vs skill | 15 | -13% | 2 / 0 | 0.500 | 1.000 | 0.640 | 0.762 | 0.070 | inconclusive |
| claude: no-criteria vs skill | 15 | +0% | 1 / 1 | 1.000 | 1.000 | 0.161 | 0.151 | 0.451 | inconclusive |
| codex: no-skill vs skill | 15 | +0% | 1 / 1 | 1.000 | 1.000 | 0.161 | 0.303 | 0.129 | inconclusive |
| codex: no-criteria vs skill | 15 | +0% | 3 / 3 | 1.000 | 1.000 | 0.282 | 0.003 | 0.503 | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: `Intercept` +2.65 ± 0.51; `C(arm, Treatment('skill'))[T.no-criteria]` +0.74 ± 0.97; `C(arm, Treatment('skill'))[T.no-skill]` -0.73 ± 0.74
- codex: `Intercept` +1.61 ± 0.41; `C(arm, Treatment('skill'))[T.no-criteria]` +0.22 ± 0.70; `C(arm, Treatment('skill'))[T.no-skill]` +0.22 ± 0.70

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-page-written` | `skill` | claude | 15/15 |
| `01-page-written` | `no-skill` | claude | 15/15 |
| `01-page-written` | `no-criteria` | claude | 14/15 |
| `01-page-written` | `skill` | codex | 15/15 |
| `01-page-written` | `no-skill` | codex | 15/15 |
| `01-page-written` | `no-criteria` | codex | 12/15 |
| `02-structure` | `skill` | claude | 15/15 |
| `02-structure` | `no-skill` | claude | 15/15 |
| `02-structure` | `no-criteria` | claude | 15/15 |
| `02-structure` | `skill` | codex | 15/15 |
| `02-structure` | `no-skill` | codex | 15/15 |
| `02-structure` | `no-criteria` | codex | 15/15 |
| `03-lead` | `skill` | claude | 15/15 |
| `03-lead` | `no-skill` | claude | 15/15 |
| `03-lead` | `no-criteria` | claude | 14/15 |
| `03-lead` | `skill` | codex | 15/15 |
| `03-lead` | `no-skill` | codex | 15/15 |
| `03-lead` | `no-criteria` | codex | 12/15 |
| `04-reason-told` | `skill` | claude | 15/15 |
| `04-reason-told` | `no-skill` | claude | 14/15 |
| `04-reason-told` | `no-criteria` | claude | 14/15 |
| `04-reason-told` | `skill` | codex | 14/15 |
| `04-reason-told` | `no-skill` | codex | 15/15 |
| `04-reason-told` | `no-criteria` | codex | 12/15 |
| `05-fact-stated` | `skill` | claude | 14/15 |
| `05-fact-stated` | `no-skill` | claude | 12/15 |
| `05-fact-stated` | `no-criteria` | claude | 14/15 |
| `05-fact-stated` | `skill` | codex | 13/15 |
| `05-fact-stated` | `no-skill` | codex | 12/15 |
| `05-fact-stated` | `no-criteria` | codex | 12/15 |
| `06-no-new-page` | `skill` | claude | 3/3 |
| `06-no-new-page` | `no-skill` | claude | 3/3 |
| `06-no-new-page` | `no-criteria` | claude | 3/3 |
| `06-no-new-page` | `skill` | codex | 3/3 |
| `06-no-new-page` | `no-skill` | codex | 3/3 |
| `06-no-new-page` | `no-criteria` | codex | 3/3 |
| `06-not-a-record` | `skill` | claude | 3/3 |
| `06-not-a-record` | `no-skill` | claude | 3/3 |
| `06-not-a-record` | `no-criteria` | claude | 2/3 |
| `06-not-a-record` | `skill` | codex | 3/3 |
| `06-not-a-record` | `no-skill` | codex | 3/3 |
| `06-not-a-record` | `no-criteria` | codex | 0/3 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `feature-cancellation--no-criteria--claude--r1` | pass | 5/5 |  |
| `feature-cancellation--no-criteria--claude--r2` | pass | 5/5 |  |
| `feature-cancellation--no-criteria--claude--r3` | pass | 5/5 |  |
| `feature-cancellation--no-criteria--codex--r1` | pass | 5/5 |  |
| `feature-cancellation--no-criteria--codex--r2` | pass | 5/5 |  |
| `feature-cancellation--no-criteria--codex--r3` | pass | 5/5 |  |
| `feature-cancellation--no-skill--claude--r1` | pass | 5/5 |  |
| `feature-cancellation--no-skill--claude--r2` | pass | 5/5 |  |
| `feature-cancellation--no-skill--claude--r3` | pass | 5/5 |  |
| `feature-cancellation--no-skill--codex--r1` | pass | 5/5 |  |
| `feature-cancellation--no-skill--codex--r2` | pass | 5/5 |  |
| `feature-cancellation--no-skill--codex--r3` | pass | 5/5 |  |
| `feature-cancellation--skill--claude--r1` | pass | 5/5 |  |
| `feature-cancellation--skill--claude--r2` | pass | 5/5 |  |
| `feature-cancellation--skill--claude--r3` | pass | 5/5 |  |
| `feature-cancellation--skill--codex--r1` | pass | 5/5 |  |
| `feature-cancellation--skill--codex--r2` | pass | 5/5 |  |
| `feature-cancellation--skill--codex--r3` | pass | 5/5 |  |
| `reminders-why--no-criteria--claude--r1` | pass | 6/6 |  |
| `reminders-why--no-criteria--claude--r2` | fail | 1/6 |  |
| `reminders-why--no-criteria--claude--r3` | pass | 6/6 |  |
| `reminders-why--no-criteria--codex--r1` | fail | 1/6 |  |
| `reminders-why--no-criteria--codex--r2` | fail | 1/6 |  |
| `reminders-why--no-criteria--codex--r3` | fail | 1/6 |  |
| `reminders-why--no-skill--claude--r1` | pass | 6/6 |  |
| `reminders-why--no-skill--claude--r2` | pass | 6/6 |  |
| `reminders-why--no-skill--claude--r3` | pass | 6/6 |  |
| `reminders-why--no-skill--codex--r1` | pass | 6/6 |  |
| `reminders-why--no-skill--codex--r2` | pass | 6/6 |  |
| `reminders-why--no-skill--codex--r3` | pass | 6/6 |  |
| `reminders-why--skill--claude--r1` | pass | 6/6 |  |
| `reminders-why--skill--claude--r2` | pass | 6/6 |  |
| `reminders-why--skill--claude--r3` | pass | 6/6 |  |
| `reminders-why--skill--codex--r1` | pass | 6/6 |  |
| `reminders-why--skill--codex--r2` | pass | 6/6 |  |
| `reminders-why--skill--codex--r3` | pass | 6/6 |  |
| `runbook-receipts--no-criteria--claude--r1` | pass | 5/5 |  |
| `runbook-receipts--no-criteria--claude--r2` | pass | 5/5 |  |
| `runbook-receipts--no-criteria--claude--r3` | pass | 5/5 |  |
| `runbook-receipts--no-criteria--codex--r1` | pass | 5/5 |  |
| `runbook-receipts--no-criteria--codex--r2` | pass | 5/5 |  |
| `runbook-receipts--no-criteria--codex--r3` | pass | 5/5 |  |
| `runbook-receipts--no-skill--claude--r1` | fail | 3/5 |  |
| `runbook-receipts--no-skill--claude--r2` | fail | 4/5 |  |
| `runbook-receipts--no-skill--claude--r3` | fail | 4/5 |  |
| `runbook-receipts--no-skill--codex--r1` | pass | 5/5 |  |
| `runbook-receipts--no-skill--codex--r2` | pass | 5/5 |  |
| `runbook-receipts--no-skill--codex--r3` | pass | 5/5 |  |
| `runbook-receipts--skill--claude--r1` | fail | 4/5 |  |
| `runbook-receipts--skill--claude--r2` | pass | 5/5 |  |
| `runbook-receipts--skill--claude--r3` | pass | 5/5 |  |
| `runbook-receipts--skill--codex--r1` | pass | 5/5 |  |
| `runbook-receipts--skill--codex--r2` | pass | 5/5 |  |
| `runbook-receipts--skill--codex--r3` | fail | 4/5 |  |
| `update-attempts--no-criteria--claude--r1` | pass | 6/6 |  |
| `update-attempts--no-criteria--claude--r2` | pass | 6/6 |  |
| `update-attempts--no-criteria--claude--r3` | pass | 6/6 |  |
| `update-attempts--no-criteria--codex--r1` | pass | 6/6 |  |
| `update-attempts--no-criteria--codex--r2` | pass | 6/6 |  |
| `update-attempts--no-criteria--codex--r3` | pass | 6/6 |  |
| `update-attempts--no-skill--claude--r1` | pass | 6/6 |  |
| `update-attempts--no-skill--claude--r2` | pass | 6/6 |  |
| `update-attempts--no-skill--claude--r3` | pass | 6/6 |  |
| `update-attempts--no-skill--codex--r1` | fail | 5/6 |  |
| `update-attempts--no-skill--codex--r2` | fail | 5/6 |  |
| `update-attempts--no-skill--codex--r3` | fail | 5/6 |  |
| `update-attempts--skill--claude--r1` | pass | 6/6 |  |
| `update-attempts--skill--claude--r2` | pass | 6/6 |  |
| `update-attempts--skill--claude--r3` | pass | 6/6 |  |
| `update-attempts--skill--codex--r1` | fail | 5/6 |  |
| `update-attempts--skill--codex--r2` | pass | 6/6 |  |
| `update-attempts--skill--codex--r3` | fail | 5/6 |  |
| `write-adr--no-criteria--claude--r1` | pass | 5/5 |  |
| `write-adr--no-criteria--claude--r2` | pass | 5/5 |  |
| `write-adr--no-criteria--claude--r3` | pass | 5/5 |  |
| `write-adr--no-criteria--codex--r1` | pass | 5/5 |  |
| `write-adr--no-criteria--codex--r2` | pass | 5/5 |  |
| `write-adr--no-criteria--codex--r3` | pass | 5/5 |  |
| `write-adr--no-skill--claude--r1` | pass | 5/5 |  |
| `write-adr--no-skill--claude--r2` | pass | 5/5 |  |
| `write-adr--no-skill--claude--r3` | pass | 5/5 |  |
| `write-adr--no-skill--codex--r1` | pass | 5/5 |  |
| `write-adr--no-skill--codex--r2` | pass | 5/5 |  |
| `write-adr--no-skill--codex--r3` | pass | 5/5 |  |
| `write-adr--skill--claude--r1` | pass | 5/5 |  |
| `write-adr--skill--claude--r2` | pass | 5/5 |  |
| `write-adr--skill--claude--r3` | pass | 5/5 |  |
| `write-adr--skill--codex--r1` | pass | 5/5 |  |
| `write-adr--skill--codex--r2` | pass | 5/5 |  |
| `write-adr--skill--codex--r3` | pass | 5/5 |  |
