# Study e3: report

Batches: pilot-1. Runs graded: 90. Reference arm: `leads`. Outcome: all of ['01-raises-the-incident'] pass.

## Validity

- The control `false-leads` does not separate from `leads` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `false-leads` does not separate from `leads` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `delete-orders` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `delete-orders` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `fresh-key` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-email` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `reminders-email` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `session-expiry` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Case `session-expiry` on codex: the reference passes 0% of 3 runs, outside 10%–90%.
- Case `sessions-in-memory` on claude: the reference passes 100% of 3 runs, outside 10%–90%.
- Grader `02-read-the-owning-page` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `leads` | 15 | 100% | 80%–100% | 1.00 | 100% | 67,769 / 73,733 | 1,047 / 1,053 | 0.127 | 15 | 4 |
| claude | `no-leads` | 15 | 100% | 80%–100% | 1.00 | 100% | 67,547 / 79,184 | 1,036 / 1,027 | 0.124 | 16 | 4 |
| claude | `false-leads` | 15 | 100% | 80%–100% | 1.00 | 100% | 67,476 / 79,496 | 1,138 / 1,144 | 0.119 | 17 | 5 |
| codex | `leads` | 15 | 20% | 7%–45% | 0.20 | 0% | 92,311 / 104,817 | 851 / 1,172 | – | 35 | 7 |
| codex | `no-leads` | 15 | 7% | 1%–30% | 0.07 | 0% | 90,779 / 101,637 | 976 / 1,165 | – | 30 | 8 |
| codex | `false-leads` | 15 | 27% | 11%–52% | 0.27 | 0% | 102,698 / 102,363 | 813 / 1,181 | – | 32 | 7 |

## Contrasts

Each arm against `leads`, paired by case and run. Superiority: exact McNemar, Holm-corrected across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-leads vs leads | 15 | +0% | 0 / 0 | 1.000 | 1.000 | 0.000 | 0.421 | 0.045 | equivalent |
| claude: false-leads vs leads | 15 | +0% | 0 / 0 | 1.000 | 1.000 | 0.000 | 0.389 | 0.179 | equivalent |
| codex: no-leads vs leads | 15 | -13% | 2 / 0 | 0.500 | 1.000 | 0.640 | 0.847 | 0.004 | inconclusive |
| codex: false-leads vs leads | 15 | +7% | 2 / 3 | 1.000 | 1.000 | 0.415 | 0.720 | 0.023 | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases
- codex: `Intercept` -1.56 ± 0.42; `C(arm, Treatment('leads'))[T.false-leads]` +0.29 ± 0.62; `C(arm, Treatment('leads'))[T.no-leads]` -1.37 ± 0.88

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-raises-the-incident` | `leads` | claude | 15/15 |
| `01-raises-the-incident` | `no-leads` | claude | 15/15 |
| `01-raises-the-incident` | `false-leads` | claude | 15/15 |
| `01-raises-the-incident` | `leads` | codex | 3/15 |
| `01-raises-the-incident` | `no-leads` | codex | 1/15 |
| `01-raises-the-incident` | `false-leads` | codex | 4/15 |
| `02-read-the-owning-page` | `leads` | claude | 15/15 |
| `02-read-the-owning-page` | `no-leads` | claude | 15/15 |
| `02-read-the-owning-page` | `false-leads` | claude | 15/15 |
| `02-read-the-owning-page` | `leads` | codex | 15/15 |
| `02-read-the-owning-page` | `no-leads` | codex | 15/15 |
| `02-read-the-owning-page` | `false-leads` | codex | 15/15 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `delete-orders--false-leads--claude--r1` | pass | 2/2 |  |
| `delete-orders--false-leads--claude--r2` | pass | 2/2 |  |
| `delete-orders--false-leads--claude--r3` | pass | 2/2 |  |
| `delete-orders--false-leads--codex--r1` | pass | 2/2 |  |
| `delete-orders--false-leads--codex--r2` | pass | 2/2 |  |
| `delete-orders--false-leads--codex--r3` | fail | 1/2 |  |
| `delete-orders--leads--claude--r1` | pass | 2/2 |  |
| `delete-orders--leads--claude--r2` | pass | 2/2 |  |
| `delete-orders--leads--claude--r3` | pass | 2/2 |  |
| `delete-orders--leads--codex--r1` | fail | 1/2 |  |
| `delete-orders--leads--codex--r2` | fail | 1/2 |  |
| `delete-orders--leads--codex--r3` | fail | 1/2 |  |
| `delete-orders--no-leads--claude--r1` | pass | 2/2 |  |
| `delete-orders--no-leads--claude--r2` | pass | 2/2 |  |
| `delete-orders--no-leads--claude--r3` | pass | 2/2 |  |
| `delete-orders--no-leads--codex--r1` | fail | 1/2 |  |
| `delete-orders--no-leads--codex--r2` | fail | 1/2 |  |
| `delete-orders--no-leads--codex--r3` | fail | 1/2 |  |
| `fresh-key--false-leads--claude--r1` | pass | 2/2 |  |
| `fresh-key--false-leads--claude--r2` | pass | 2/2 |  |
| `fresh-key--false-leads--claude--r3` | pass | 2/2 |  |
| `fresh-key--false-leads--codex--r1` | pass | 2/2 |  |
| `fresh-key--false-leads--codex--r2` | pass | 2/2 |  |
| `fresh-key--false-leads--codex--r3` | fail | 1/2 |  |
| `fresh-key--leads--claude--r1` | pass | 2/2 |  |
| `fresh-key--leads--claude--r2` | pass | 2/2 |  |
| `fresh-key--leads--claude--r3` | pass | 2/2 |  |
| `fresh-key--leads--codex--r1` | fail | 1/2 |  |
| `fresh-key--leads--codex--r2` | pass | 2/2 |  |
| `fresh-key--leads--codex--r3` | pass | 2/2 |  |
| `fresh-key--no-leads--claude--r1` | pass | 2/2 |  |
| `fresh-key--no-leads--claude--r2` | pass | 2/2 |  |
| `fresh-key--no-leads--claude--r3` | pass | 2/2 |  |
| `fresh-key--no-leads--codex--r1` | fail | 1/2 |  |
| `fresh-key--no-leads--codex--r2` | fail | 1/2 |  |
| `fresh-key--no-leads--codex--r3` | fail | 1/2 |  |
| `reminders-email--false-leads--claude--r1` | pass | 2/2 |  |
| `reminders-email--false-leads--claude--r2` | pass | 2/2 |  |
| `reminders-email--false-leads--claude--r3` | pass | 2/2 |  |
| `reminders-email--false-leads--codex--r1` | fail | 1/2 |  |
| `reminders-email--false-leads--codex--r2` | fail | 1/2 |  |
| `reminders-email--false-leads--codex--r3` | fail | 1/2 |  |
| `reminders-email--leads--claude--r1` | pass | 2/2 |  |
| `reminders-email--leads--claude--r2` | pass | 2/2 |  |
| `reminders-email--leads--claude--r3` | pass | 2/2 |  |
| `reminders-email--leads--codex--r1` | fail | 1/2 |  |
| `reminders-email--leads--codex--r2` | fail | 1/2 |  |
| `reminders-email--leads--codex--r3` | fail | 1/2 |  |
| `reminders-email--no-leads--claude--r1` | pass | 2/2 |  |
| `reminders-email--no-leads--claude--r2` | pass | 2/2 |  |
| `reminders-email--no-leads--claude--r3` | pass | 2/2 |  |
| `reminders-email--no-leads--codex--r1` | fail | 1/2 |  |
| `reminders-email--no-leads--codex--r2` | fail | 1/2 |  |
| `reminders-email--no-leads--codex--r3` | fail | 1/2 |  |
| `session-expiry--false-leads--claude--r1` | pass | 2/2 |  |
| `session-expiry--false-leads--claude--r2` | pass | 2/2 |  |
| `session-expiry--false-leads--claude--r3` | pass | 2/2 |  |
| `session-expiry--false-leads--codex--r1` | fail | 1/2 |  |
| `session-expiry--false-leads--codex--r2` | fail | 1/2 |  |
| `session-expiry--false-leads--codex--r3` | fail | 1/2 |  |
| `session-expiry--leads--claude--r1` | pass | 2/2 |  |
| `session-expiry--leads--claude--r2` | pass | 2/2 |  |
| `session-expiry--leads--claude--r3` | pass | 2/2 |  |
| `session-expiry--leads--codex--r1` | fail | 1/2 |  |
| `session-expiry--leads--codex--r2` | fail | 1/2 |  |
| `session-expiry--leads--codex--r3` | fail | 1/2 |  |
| `session-expiry--no-leads--claude--r1` | pass | 2/2 |  |
| `session-expiry--no-leads--claude--r2` | pass | 2/2 |  |
| `session-expiry--no-leads--claude--r3` | pass | 2/2 |  |
| `session-expiry--no-leads--codex--r1` | fail | 1/2 |  |
| `session-expiry--no-leads--codex--r2` | fail | 1/2 |  |
| `session-expiry--no-leads--codex--r3` | fail | 1/2 |  |
| `sessions-in-memory--false-leads--claude--r1` | pass | 2/2 |  |
| `sessions-in-memory--false-leads--claude--r2` | pass | 2/2 |  |
| `sessions-in-memory--false-leads--claude--r3` | pass | 2/2 |  |
| `sessions-in-memory--false-leads--codex--r1` | fail | 1/2 |  |
| `sessions-in-memory--false-leads--codex--r2` | fail | 1/2 |  |
| `sessions-in-memory--false-leads--codex--r3` | fail | 1/2 |  |
| `sessions-in-memory--leads--claude--r1` | pass | 2/2 |  |
| `sessions-in-memory--leads--claude--r2` | pass | 2/2 |  |
| `sessions-in-memory--leads--claude--r3` | pass | 2/2 |  |
| `sessions-in-memory--leads--codex--r1` | fail | 1/2 |  |
| `sessions-in-memory--leads--codex--r2` | fail | 1/2 |  |
| `sessions-in-memory--leads--codex--r3` | pass | 2/2 |  |
| `sessions-in-memory--no-leads--claude--r1` | pass | 2/2 |  |
| `sessions-in-memory--no-leads--claude--r2` | pass | 2/2 |  |
| `sessions-in-memory--no-leads--claude--r3` | pass | 2/2 |  |
| `sessions-in-memory--no-leads--codex--r1` | fail | 1/2 |  |
| `sessions-in-memory--no-leads--codex--r2` | fail | 1/2 |  |
| `sessions-in-memory--no-leads--codex--r3` | pass | 2/2 |  |
