# Study e3: report

Batches: smoke-2. Runs graded: 3. Reference arm: `leads`. Outcome: all of ['03-acts-on-the-incident'] pass. Primary: effort, the steps before the owning page is read.

## Validity

- The control `false-leads` does not separate from `leads` on claude (Holm p = None): no comparison in this study counts.
- Grader `01-raises-the-incident` passes every run of every arm: it discriminates nothing.
- Grader `02-read-the-owning-page` passes every run of every arm: it discriminates nothing.
- Grader `03-acts-on-the-incident` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median | Never read the owning page |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `leads` | 1 | 100% | 21%–100% | 1.00 | 100% | 103,137 / 103,137 | 1,007 / 1,007 | 0.181 | 15 | 5 | 0 |
| claude | `no-leads` | 1 | 100% | 21%–100% | 1.00 | 100% | 72,619 / 72,619 | 999 / 999 | 0.143 | 14 | 5 | 0 |
| claude | `false-leads` | 1 | 100% | 21%–100% | 1.00 | 100% | 67,527 / 67,527 | 1,122 / 1,122 | 0.148 | 17 | 4 | 0 |

## Contrasts

Each arm against `leads`, paired by case and run. Superiority: exact McNemar on the outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, Wilcoxon on steps, across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median | Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-leads vs leads | 1 | +0% | 0 / 0 | 1.000 | +0 | – | – | – | – | – | inconclusive |
| claude: false-leads vs leads | 1 | +0% | 0 / 0 | 1.000 | -1 | – | – | – | – | – | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-raises-the-incident` | `leads` | claude | 1/1 |
| `01-raises-the-incident` | `no-leads` | claude | 1/1 |
| `01-raises-the-incident` | `false-leads` | claude | 1/1 |
| `02-read-the-owning-page` | `leads` | claude | 1/1 |
| `02-read-the-owning-page` | `no-leads` | claude | 1/1 |
| `02-read-the-owning-page` | `false-leads` | claude | 1/1 |
| `03-acts-on-the-incident` | `leads` | claude | 1/1 |
| `03-acts-on-the-incident` | `no-leads` | claude | 1/1 |
| `03-acts-on-the-incident` | `false-leads` | claude | 1/1 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `session-expiry--false-leads--claude--r1` | pass | 3/3 |  |
| `session-expiry--leads--claude--r1` | pass | 3/3 |  |
| `session-expiry--no-leads--claude--r1` | pass | 3/3 |  |
