# Study e1: report

Batches: pilot-2. Runs graded: 90. Reference arm: `skill`. Outcome: all of ['01-page-written', '02-structure', '03-lead', '04-reason-told', '05-fact-stated', '06-trap'] pass.

## Validity

- The control `no-criteria` does not separate from `skill` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `no-criteria` does not separate from `skill` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `change-decision` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `change-decision` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-why` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-why` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `runbook-receipts` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `tenet` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `tenet` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `update-attempts` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `update-attempts` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `skill` | 15 | 87% | 62%–96% | 0.98 | 80% | 200,079 / 212,601 | 3,389 / 3,867 | 0.277 | 37 | 3 |
| claude | `no-skill` | 15 | 87% | 62%–96% | 0.98 | 60% | 151,471 / 158,916 | 3,357 / 3,939 | 0.207 | 36 | 2 |
| claude | `no-criteria` | 15 | 73% | 48%–89% | 0.91 | 60% | 215,784 / 215,136 | 3,921 / 4,283 | 0.284 | 43 | 3 |
| codex | `skill` | 15 | 60% | 36%–80% | 0.91 | 60% | 125,037 / 116,823 | 1,546 / 1,669 | – | 55 | 5 |
| codex | `no-skill` | 15 | 60% | 36%–80% | 0.91 | 60% | 107,589 / 116,466 | 1,844 / 1,710 | – | 57 | 5 |
| codex | `no-criteria` | 15 | 47% | 25%–70% | 0.72 | 20% | 136,692 / 129,463 | 2,084 / 2,038 | – | 60 | 6 |

## Contrasts

Each arm against `skill`, paired by case and run. Superiority: exact McNemar, Holm-corrected across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-skill vs skill | 15 | +0% | 1 / 1 | 1.000 | 1.000 | 0.161 | 0.003 | 0.967 | inconclusive |
| claude: no-criteria vs skill | 15 | -13% | 2 / 0 | 0.500 | 1.000 | 0.640 | 0.890 | 0.041 | inconclusive |
| codex: no-skill vs skill | 15 | +0% | 0 / 0 | 1.000 | 1.000 | 0.000 | 0.188 | 0.229 | equivalent |
| codex: no-criteria vs skill | 15 | -13% | 4 / 2 | 0.688 | 1.000 | 0.578 | 0.241 | 0.300 | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: `Intercept` +2.13 ± 0.47; `C(arm, Treatment('skill'))[T.no-criteria]` -0.72 ± 0.71; `C(arm, Treatment('skill'))[T.no-skill]` +0.49 ± 0.83
- codex: `Intercept` +0.46 ± 0.45; `C(arm, Treatment('skill'))[T.no-criteria]` -0.90 ± 0.73; `C(arm, Treatment('skill'))[T.no-skill]` +0.18 ± 0.75

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-page-written` | `skill` | claude | 15/15 |
| `01-page-written` | `no-skill` | claude | 15/15 |
| `01-page-written` | `no-criteria` | claude | 14/15 |
| `01-page-written` | `skill` | codex | 15/15 |
| `01-page-written` | `no-skill` | codex | 15/15 |
| `01-page-written` | `no-criteria` | codex | 11/15 |
| `02-structure` | `skill` | claude | 15/15 |
| `02-structure` | `no-skill` | claude | 15/15 |
| `02-structure` | `no-criteria` | claude | 15/15 |
| `02-structure` | `skill` | codex | 13/15 |
| `02-structure` | `no-skill` | codex | 12/15 |
| `02-structure` | `no-criteria` | codex | 14/15 |
| `03-lead` | `skill` | claude | 15/15 |
| `03-lead` | `no-skill` | claude | 15/15 |
| `03-lead` | `no-criteria` | claude | 14/15 |
| `03-lead` | `skill` | codex | 15/15 |
| `03-lead` | `no-skill` | codex | 15/15 |
| `03-lead` | `no-criteria` | codex | 11/15 |
| `04-reason-told` | `skill` | claude | 14/15 |
| `04-reason-told` | `no-skill` | claude | 14/15 |
| `04-reason-told` | `no-criteria` | claude | 14/15 |
| `04-reason-told` | `skill` | codex | 12/15 |
| `04-reason-told` | `no-skill` | codex | 13/15 |
| `04-reason-told` | `no-criteria` | codex | 10/15 |
| `05-fact-stated` | `skill` | claude | 15/15 |
| `05-fact-stated` | `no-skill` | claude | 15/15 |
| `05-fact-stated` | `no-criteria` | claude | 14/15 |
| `05-fact-stated` | `skill` | codex | 15/15 |
| `05-fact-stated` | `no-skill` | codex | 15/15 |
| `05-fact-stated` | `no-criteria` | codex | 11/15 |
| `06-trap` | `skill` | claude | 14/15 |
| `06-trap` | `no-skill` | claude | 14/15 |
| `06-trap` | `no-criteria` | claude | 11/15 |
| `06-trap` | `skill` | codex | 12/15 |
| `06-trap` | `no-skill` | codex | 12/15 |
| `06-trap` | `no-criteria` | codex | 8/15 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `change-decision--no-criteria--claude--r1` | pass | 6/6 |  |
| `change-decision--no-criteria--claude--r2` | pass | 6/6 |  |
| `change-decision--no-criteria--claude--r3` | pass | 6/6 |  |
| `change-decision--no-criteria--codex--r1` | pass | 6/6 |  |
| `change-decision--no-criteria--codex--r2` | pass | 6/6 |  |
| `change-decision--no-criteria--codex--r3` | fail | 0/6 | {"message": "Selected model is at capacity. Please try a different model."} |
| `change-decision--no-skill--claude--r1` | pass | 6/6 |  |
| `change-decision--no-skill--claude--r2` | pass | 6/6 |  |
| `change-decision--no-skill--claude--r3` | pass | 6/6 |  |
| `change-decision--no-skill--codex--r1` | pass | 6/6 |  |
| `change-decision--no-skill--codex--r2` | pass | 6/6 |  |
| `change-decision--no-skill--codex--r3` | pass | 6/6 |  |
| `change-decision--skill--claude--r1` | pass | 6/6 |  |
| `change-decision--skill--claude--r2` | pass | 6/6 |  |
| `change-decision--skill--claude--r3` | pass | 6/6 |  |
| `change-decision--skill--codex--r1` | pass | 6/6 |  |
| `change-decision--skill--codex--r2` | pass | 6/6 |  |
| `change-decision--skill--codex--r3` | pass | 6/6 | {"message": "Selected model is at capacity. Please try a different model."} |
| `reminders-why--no-criteria--claude--r1` | fail | 1/6 |  |
| `reminders-why--no-criteria--claude--r2` | pass | 6/6 |  |
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
| `runbook-receipts--no-criteria--claude--r1` | fail | 5/6 |  |
| `runbook-receipts--no-criteria--claude--r2` | fail | 5/6 |  |
| `runbook-receipts--no-criteria--claude--r3` | fail | 5/6 |  |
| `runbook-receipts--no-criteria--codex--r1` | fail | 5/6 |  |
| `runbook-receipts--no-criteria--codex--r2` | fail | 5/6 |  |
| `runbook-receipts--no-criteria--codex--r3` | fail | 5/6 |  |
| `runbook-receipts--no-skill--claude--r1` | fail | 5/6 |  |
| `runbook-receipts--no-skill--claude--r2` | pass | 6/6 |  |
| `runbook-receipts--no-skill--claude--r3` | pass | 6/6 |  |
| `runbook-receipts--no-skill--codex--r1` | fail | 5/6 |  |
| `runbook-receipts--no-skill--codex--r2` | fail | 5/6 |  |
| `runbook-receipts--no-skill--codex--r3` | fail | 5/6 |  |
| `runbook-receipts--skill--claude--r1` | fail | 5/6 |  |
| `runbook-receipts--skill--claude--r2` | fail | 5/6 |  |
| `runbook-receipts--skill--claude--r3` | pass | 6/6 |  |
| `runbook-receipts--skill--codex--r1` | fail | 5/6 |  |
| `runbook-receipts--skill--codex--r2` | fail | 5/6 |  |
| `runbook-receipts--skill--codex--r3` | fail | 5/6 |  |
| `tenet--no-criteria--claude--r1` | pass | 6/6 |  |
| `tenet--no-criteria--claude--r2` | pass | 6/6 |  |
| `tenet--no-criteria--claude--r3` | pass | 6/6 |  |
| `tenet--no-criteria--codex--r1` | pass | 6/6 |  |
| `tenet--no-criteria--codex--r2` | fail | 5/6 |  |
| `tenet--no-criteria--codex--r3` | pass | 6/6 |  |
| `tenet--no-skill--claude--r1` | pass | 6/6 |  |
| `tenet--no-skill--claude--r2` | pass | 6/6 |  |
| `tenet--no-skill--claude--r3` | fail | 5/6 |  |
| `tenet--no-skill--codex--r1` | fail | 4/6 |  |
| `tenet--no-skill--codex--r2` | fail | 4/6 |  |
| `tenet--no-skill--codex--r3` | fail | 5/6 |  |
| `tenet--skill--claude--r1` | pass | 6/6 |  |
| `tenet--skill--claude--r2` | pass | 6/6 |  |
| `tenet--skill--claude--r3` | pass | 6/6 |  |
| `tenet--skill--codex--r1` | fail | 4/6 |  |
| `tenet--skill--codex--r2` | fail | 4/6 |  |
| `tenet--skill--codex--r3` | fail | 5/6 |  |
| `update-attempts--no-criteria--claude--r1` | pass | 6/6 |  |
| `update-attempts--no-criteria--claude--r2` | pass | 6/6 |  |
| `update-attempts--no-criteria--claude--r3` | pass | 6/6 |  |
| `update-attempts--no-criteria--codex--r1` | pass | 6/6 |  |
| `update-attempts--no-criteria--codex--r2` | pass | 6/6 |  |
| `update-attempts--no-criteria--codex--r3` | pass | 6/6 |  |
| `update-attempts--no-skill--claude--r1` | pass | 6/6 |  |
| `update-attempts--no-skill--claude--r2` | pass | 6/6 |  |
| `update-attempts--no-skill--claude--r3` | pass | 6/6 |  |
| `update-attempts--no-skill--codex--r1` | pass | 6/6 |  |
| `update-attempts--no-skill--codex--r2` | pass | 6/6 |  |
| `update-attempts--no-skill--codex--r3` | pass | 6/6 |  |
| `update-attempts--skill--claude--r1` | pass | 6/6 |  |
| `update-attempts--skill--claude--r2` | pass | 6/6 |  |
| `update-attempts--skill--claude--r3` | pass | 6/6 |  |
| `update-attempts--skill--codex--r1` | pass | 6/6 |  |
| `update-attempts--skill--codex--r2` | pass | 6/6 |  |
| `update-attempts--skill--codex--r3` | pass | 6/6 |  |
