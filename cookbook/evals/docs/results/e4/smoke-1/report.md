# Study e4: report

Batches: smoke-1. Runs graded: 12. Reference arm: `skill`. Outcome: all of ['01-acts-on-the-reason'] pass. Primary: the outcome.

## Validity

- The control `unwritten` does not separate from `skill` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `unwritten` does not separate from `skill` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `reminders-why` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median | Never read the owning page |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `skill` | 3 | 100% | 44%–100% | 1.00 | 100% | 69,098 / 77,426 | 1,068 / 1,100 | 0.148 | 19 | – | – |
| claude | `unwritten` | 3 | 0% | 0%–56% | 0.00 | 0% | 147,991 / 148,284 | 1,750 / 1,760 | 0.137 | 26 | – | – |
| codex | `skill` | 3 | 33% | 6%–79% | 0.33 | 0% | 110,085 / 95,900 | 1,076 / 973 | – | 34 | – | – |
| codex | `unwritten` | 3 | 0% | 0%–56% | 0.00 | 0% | 83,045 / 82,955 | 483 / 484 | – | 19 | – | – |

## Contrasts

Each arm against `skill`, paired by case and run. Superiority: exact McNemar on the outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, McNemar, across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median | Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-skill vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| claude: bare vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| claude: no-criteria vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| claude: unwritten vs skill | 3 | -100% | 3 / 0 | 0.250 | – | – | 1.000 | 1.000 | 0.250 | 0.977 | inconclusive |
| codex: no-skill vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: bare vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: no-criteria vs skill | 0 | +0% | 0 / 0 | 1.000 | – | – | 1.000 | – | – | – | inconclusive |
| codex: unwritten vs skill | 3 | -33% | 1 / 0 | 1.000 | – | – | 1.000 | 0.722 | 0.500 | 0.465 | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases
- codex: needs both outcomes, two arms and two cases

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-acts-on-the-reason` | `skill` | claude | 3/3 |
| `01-acts-on-the-reason` | `unwritten` | claude | 0/3 |
| `01-acts-on-the-reason` | `skill` | codex | 1/3 |
| `01-acts-on-the-reason` | `unwritten` | codex | 0/3 |
| `02-names-a-fact` | `skill` | claude | 3/3 |
| `02-names-a-fact` | `unwritten` | claude | 0/3 |
| `02-names-a-fact` | `skill` | codex | 3/3 |
| `02-names-a-fact` | `unwritten` | codex | 0/3 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `reminders-why--skill--claude--r1` | pass | 2/2 |  |
| `reminders-why--skill--claude--r2` | pass | 2/2 |  |
| `reminders-why--skill--claude--r3` | pass | 2/2 |  |
| `reminders-why--skill--codex--r1` | fail | 1/2 |  |
| `reminders-why--skill--codex--r2` | pass | 2/2 |  |
| `reminders-why--skill--codex--r3` | fail | 1/2 |  |
| `reminders-why--unwritten--claude--r1` | fail | 0/2 |  |
| `reminders-why--unwritten--claude--r2` | fail | 0/2 |  |
| `reminders-why--unwritten--claude--r3` | fail | 0/2 |  |
| `reminders-why--unwritten--codex--r1` | fail | 0/2 |  |
| `reminders-why--unwritten--codex--r2` | fail | 0/2 |  |
| `reminders-why--unwritten--codex--r3` | fail | 0/2 |  |
