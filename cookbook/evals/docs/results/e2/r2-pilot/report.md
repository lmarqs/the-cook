# Study e2: report

Batches: r2-pilot. Runs graded: 60. Reference arm: `owned`. Outcome: all of ['01-tests'] pass. Primary: the outcome.

## Validity

- The control `scattered` does not separate from `owned` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `scattered` does not separate from `owned` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `cancel-cutoff` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `cancel-cutoff` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `delete-member` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `delete-member` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `household-limit` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `household-limit` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `order-amount` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `order-amount` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `retry-capture` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `retry-capture` on codex: the reference passes 100% of 3 runs, outside 10%–90%.
- Grader `01-tests` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median | Never read the owning page |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `owned` | 15 | 100% | 80%–100% | 1.00 | 100% | 237,551 / 234,075 | 918 / 1,046 | 0.086 | 17 | 5 | 3 |
| claude | `scattered` | 15 | 100% | 80%–100% | 1.00 | 100% | 234,825 / 235,364 | 997 / 1,065 | 0.087 | 17 | 5 | 3 |
| codex | `owned` | 15 | 100% | 80%–100% | 1.00 | 100% | 85,923 / 87,859 | 914 / 837 | – | 26 | 6 | 0 |
| codex | `scattered` | 15 | 100% | 80%–100% | 1.00 | 100% | 85,089 / 83,948 | 685 / 752 | – | 23 | 6 | 0 |

## Contrasts

Each arm against `owned`, paired by batch, case and run. Superiority: exact McNemar on the outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, McNemar, across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median | Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: scattered vs owned | 15 | +0% | 0 / 0 | 1.000 | +0 | 0.979 | 1.000 | 0.000 | 0.847 | 0.003 | equivalent |
| codex: scattered vs owned | 15 | +0% | 0 / 0 | 1.000 | +0 | 0.424 | 1.000 | 0.000 | 0.561 | 0.034 | equivalent |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases
- codex: needs both outcomes, two arms and two cases

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-tests` | `owned` | claude | 15/15 |
| `01-tests` | `scattered` | claude | 15/15 |
| `01-tests` | `owned` | codex | 15/15 |
| `01-tests` | `scattered` | codex | 15/15 |
| `02-read-the-owning-page` | `owned` | claude | 12/15 |
| `02-read-the-owning-page` | `scattered` | claude | 12/15 |
| `02-read-the-owning-page` | `owned` | codex | 15/15 |
| `02-read-the-owning-page` | `scattered` | codex | 15/15 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `cancel-cutoff--owned--claude--r1` | pass | 2/2 |  |
| `cancel-cutoff--owned--claude--r2` | pass | 2/2 |  |
| `cancel-cutoff--owned--claude--r3` | pass | 2/2 |  |
| `cancel-cutoff--owned--codex--r1` | pass | 2/2 |  |
| `cancel-cutoff--owned--codex--r2` | pass | 2/2 |  |
| `cancel-cutoff--owned--codex--r3` | pass | 2/2 |  |
| `cancel-cutoff--scattered--claude--r1` | pass | 2/2 |  |
| `cancel-cutoff--scattered--claude--r2` | pass | 2/2 |  |
| `cancel-cutoff--scattered--claude--r3` | pass | 2/2 |  |
| `cancel-cutoff--scattered--codex--r1` | pass | 2/2 |  |
| `cancel-cutoff--scattered--codex--r2` | pass | 2/2 |  |
| `cancel-cutoff--scattered--codex--r3` | pass | 2/2 |  |
| `delete-member--owned--claude--r1` | pass | 2/2 |  |
| `delete-member--owned--claude--r2` | pass | 2/2 |  |
| `delete-member--owned--claude--r3` | pass | 2/2 |  |
| `delete-member--owned--codex--r1` | pass | 2/2 |  |
| `delete-member--owned--codex--r2` | pass | 2/2 |  |
| `delete-member--owned--codex--r3` | pass | 2/2 |  |
| `delete-member--scattered--claude--r1` | pass | 2/2 |  |
| `delete-member--scattered--claude--r2` | pass | 2/2 |  |
| `delete-member--scattered--claude--r3` | pass | 2/2 |  |
| `delete-member--scattered--codex--r1` | pass | 2/2 |  |
| `delete-member--scattered--codex--r2` | pass | 2/2 |  |
| `delete-member--scattered--codex--r3` | pass | 2/2 |  |
| `household-limit--owned--claude--r1` | pass | 2/2 |  |
| `household-limit--owned--claude--r2` | pass | 2/2 |  |
| `household-limit--owned--claude--r3` | pass | 2/2 |  |
| `household-limit--owned--codex--r1` | pass | 2/2 |  |
| `household-limit--owned--codex--r2` | pass | 2/2 |  |
| `household-limit--owned--codex--r3` | pass | 2/2 |  |
| `household-limit--scattered--claude--r1` | pass | 2/2 |  |
| `household-limit--scattered--claude--r2` | pass | 2/2 |  |
| `household-limit--scattered--claude--r3` | pass | 2/2 |  |
| `household-limit--scattered--codex--r1` | pass | 2/2 |  |
| `household-limit--scattered--codex--r2` | pass | 2/2 |  |
| `household-limit--scattered--codex--r3` | pass | 2/2 |  |
| `order-amount--owned--claude--r1` | pass | 2/2 |  |
| `order-amount--owned--claude--r2` | pass | 2/2 |  |
| `order-amount--owned--claude--r3` | pass | 2/2 |  |
| `order-amount--owned--codex--r1` | pass | 2/2 |  |
| `order-amount--owned--codex--r2` | pass | 2/2 |  |
| `order-amount--owned--codex--r3` | pass | 2/2 |  |
| `order-amount--scattered--claude--r1` | pass | 2/2 |  |
| `order-amount--scattered--claude--r2` | pass | 2/2 |  |
| `order-amount--scattered--claude--r3` | pass | 2/2 |  |
| `order-amount--scattered--codex--r1` | pass | 2/2 |  |
| `order-amount--scattered--codex--r2` | pass | 2/2 |  |
| `order-amount--scattered--codex--r3` | pass | 2/2 |  |
| `retry-capture--owned--claude--r1` | pass | 1/2 |  |
| `retry-capture--owned--claude--r2` | pass | 1/2 |  |
| `retry-capture--owned--claude--r3` | pass | 1/2 |  |
| `retry-capture--owned--codex--r1` | pass | 2/2 |  |
| `retry-capture--owned--codex--r2` | pass | 2/2 |  |
| `retry-capture--owned--codex--r3` | pass | 2/2 |  |
| `retry-capture--scattered--claude--r1` | pass | 1/2 |  |
| `retry-capture--scattered--claude--r2` | pass | 1/2 |  |
| `retry-capture--scattered--claude--r3` | pass | 1/2 |  |
| `retry-capture--scattered--codex--r1` | pass | 2/2 |  |
| `retry-capture--scattered--codex--r2` | pass | 2/2 |  |
| `retry-capture--scattered--codex--r3` | pass | 2/2 |  |
