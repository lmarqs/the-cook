# Study e3: report

Batches: smoke. Runs graded: 6. Reference arm: `leads`. Outcome: all of ['01-raises-the-incident'] pass.

## Validity

- The control `false-leads` does not separate from `leads` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `false-leads` does not separate from `leads` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `session-expiry` on claude: the reference passes 100% of 1 runs, outside 10%–90%.
- Case `session-expiry` on codex: the reference passes 0% of 1 runs, outside 10%–90%.
- Grader `02-read-the-owning-page` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `leads` | 1 | 100% | 21%–100% | 1.00 | 100% | 65,371 / 65,371 | 957 / 957 | 0.131 | 15 | 5 |
| claude | `no-leads` | 1 | 100% | 21%–100% | 1.00 | 100% | 96,589 / 96,589 | 1,187 / 1,187 | 0.159 | 18 | 5 |
| claude | `false-leads` | 1 | 100% | 21%–100% | 1.00 | 100% | 89,967 / 89,967 | 1,053 / 1,053 | 0.141 | 16 | 7 |
| codex | `leads` | 1 | 0% | 0%–79% | 0.00 | 0% | 89,004 / 89,004 | 771 / 771 | – | 27 | 6 |
| codex | `no-leads` | 1 | 0% | 0%–79% | 0.00 | 0% | 70,360 / 70,360 | 761 / 761 | – | 27 | 6 |
| codex | `false-leads` | 1 | 0% | 0%–79% | 0.00 | 0% | 87,488 / 87,488 | 885 / 885 | – | 29 | 4 |

## Contrasts

Each arm against `leads`, paired by case and run. Superiority: exact McNemar, Holm-corrected across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-leads vs leads | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| claude: false-leads vs leads | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| codex: no-leads vs leads | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| codex: false-leads vs leads | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases
- codex: needs both outcomes, two arms and two cases

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-raises-the-incident` | `leads` | claude | 1/1 |
| `01-raises-the-incident` | `no-leads` | claude | 1/1 |
| `01-raises-the-incident` | `false-leads` | claude | 1/1 |
| `01-raises-the-incident` | `leads` | codex | 0/1 |
| `01-raises-the-incident` | `no-leads` | codex | 0/1 |
| `01-raises-the-incident` | `false-leads` | codex | 0/1 |
| `02-read-the-owning-page` | `leads` | claude | 1/1 |
| `02-read-the-owning-page` | `no-leads` | claude | 1/1 |
| `02-read-the-owning-page` | `false-leads` | claude | 1/1 |
| `02-read-the-owning-page` | `leads` | codex | 1/1 |
| `02-read-the-owning-page` | `no-leads` | codex | 1/1 |
| `02-read-the-owning-page` | `false-leads` | codex | 1/1 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `session-expiry--false-leads--claude--r1` | pass | 2/2 |  |
| `session-expiry--false-leads--codex--r1` | fail | 1/2 |  |
| `session-expiry--leads--claude--r1` | pass | 2/2 |  |
| `session-expiry--leads--codex--r1` | fail | 1/2 |  |
| `session-expiry--no-leads--claude--r1` | pass | 2/2 |  |
| `session-expiry--no-leads--codex--r1` | fail | 1/2 |  |
