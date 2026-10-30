# Study e2: report

Batches: smoke. Runs graded: 4. Reference arm: `owned`. Outcome: all of ['01-tests'] pass.

## Validity

- The control `scattered` does not separate from `owned` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `scattered` does not separate from `owned` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `retry-capture` on claude: the reference passes 100% of 1 runs, outside 10%–90%.
- Case `retry-capture` on codex: the reference passes 100% of 1 runs, outside 10%–90%.
- Grader `01-tests` passes every run of every arm: it discriminates nothing.
- Grader `02-read-the-owning-page` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `owned` | 1 | 100% | 21%–100% | 1.00 | 100% | 121,754 / 121,754 | 2,069 / 2,069 | 0.176 | 25 | 5 |
| claude | `scattered` | 1 | 100% | 21%–100% | 1.00 | 100% | 119,778 / 119,778 | 2,576 / 2,576 | 0.180 | 29 | 5 |
| codex | `owned` | 1 | 100% | 21%–100% | 1.00 | 100% | 103,028 / 103,028 | 1,008 / 1,008 | – | 31 | 11 |
| codex | `scattered` | 1 | 100% | 21%–100% | 1.00 | 100% | 104,908 / 104,908 | 1,085 / 1,085 | – | 23 | 11 |

## Contrasts

Each arm against `owned`, paired by case and run. Superiority: exact McNemar, Holm-corrected across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: scattered vs owned | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| codex: scattered vs owned | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases
- codex: needs both outcomes, two arms and two cases

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-tests` | `owned` | claude | 1/1 |
| `01-tests` | `scattered` | claude | 1/1 |
| `01-tests` | `owned` | codex | 1/1 |
| `01-tests` | `scattered` | codex | 1/1 |
| `02-read-the-owning-page` | `owned` | claude | 1/1 |
| `02-read-the-owning-page` | `scattered` | claude | 1/1 |
| `02-read-the-owning-page` | `owned` | codex | 1/1 |
| `02-read-the-owning-page` | `scattered` | codex | 1/1 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `retry-capture--owned--claude--r1` | pass | 2/2 |  |
| `retry-capture--owned--codex--r1` | pass | 2/2 |  |
| `retry-capture--scattered--claude--r1` | pass | 2/2 |  |
| `retry-capture--scattered--codex--r1` | pass | 2/2 |  |
