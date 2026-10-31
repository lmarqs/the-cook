# Study e3: report

Batches: r2-pilot. Runs graded: 90. Reference arm: `leads`. Outcome: all of ['03-acts-on-the-incident'] pass. Primary: effort, the steps before the owning page is read.

## Validity

- The control `false-leads` does not separate from `leads` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `false-leads` does not separate from `leads` on codex (Holm p = 1.0): no comparison in this study counts.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median | Never read the owning page |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `leads` | 15 | 80% | 55%–93% | 0.80 | 80% | 130,485 / 156,728 | 693 / 823 | 0.066 | 13 | 3 | 3 |
| claude | `no-leads` | 15 | 73% | 48%–89% | 0.73 | 60% | 129,942 / 143,311 | 716 / 794 | 0.064 | 14 | 3 | 3 |
| claude | `false-leads` | 15 | 87% | 62%–96% | 0.87 | 60% | 160,710 / 155,188 | 822 / 923 | 0.069 | 15 | 3 | 1 |
| codex | `leads` | 15 | 87% | 62%–96% | 0.87 | 60% | 107,715 / 114,249 | 1,084 / 1,339 | – | 35 | 7 | 0 |
| codex | `no-leads` | 15 | 80% | 55%–93% | 0.80 | 80% | 92,142 / 102,957 | 1,124 / 1,231 | – | 35 | 6 | 0 |
| codex | `false-leads` | 15 | 93% | 70%–99% | 0.93 | 80% | 92,408 / 99,754 | 1,203 / 1,201 | – | 34 | 7 | 0 |

## Contrasts

Each arm against `leads`, paired by batch, case and run. Superiority: exact McNemar on the outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, Wilcoxon on steps, across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median | Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-leads vs leads | 15 | -7% | 1 / 0 | 1.000 | +1 | 0.607 | 1.000 | 0.312 | 0.978 | 0.162 | inconclusive |
| claude: false-leads vs leads | 15 | +7% | 1 / 2 | 1.000 | +1 | 0.727 | 1.000 | 0.391 | 0.978 | 0.157 | inconclusive |
| codex: no-leads vs leads | 15 | -7% | 2 / 1 | 1.000 | -1 | 0.263 | 1.000 | 0.391 | 0.073 | 0.169 | inconclusive |
| codex: false-leads vs leads | 15 | +7% | 1 / 2 | 1.000 | -1 | 0.304 | 1.000 | 0.391 | 0.004 | 0.345 | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: `Intercept` +1.96 ± 0.51; `C(arm, Treatment('leads'))[T.false-leads]` +1.04 ± 0.89; `C(arm, Treatment('leads'))[T.no-leads]` -0.40 ± 0.80
- codex: `Intercept` +2.16 ± 0.48; `C(arm, Treatment('leads'))[T.false-leads]` +1.11 ± 0.93; `C(arm, Treatment('leads'))[T.no-leads]` -0.27 ± 0.72

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-raises-the-incident` | `leads` | claude | 10/15 |
| `01-raises-the-incident` | `no-leads` | claude | 9/15 |
| `01-raises-the-incident` | `false-leads` | claude | 11/15 |
| `01-raises-the-incident` | `leads` | codex | 1/15 |
| `01-raises-the-incident` | `no-leads` | codex | 1/15 |
| `01-raises-the-incident` | `false-leads` | codex | 2/15 |
| `02-read-the-owning-page` | `leads` | claude | 7/15 |
| `02-read-the-owning-page` | `no-leads` | claude | 9/15 |
| `02-read-the-owning-page` | `false-leads` | claude | 12/15 |
| `02-read-the-owning-page` | `leads` | codex | 15/15 |
| `02-read-the-owning-page` | `no-leads` | codex | 15/15 |
| `02-read-the-owning-page` | `false-leads` | codex | 15/15 |
| `03-acts-on-the-incident` | `leads` | claude | 12/15 |
| `03-acts-on-the-incident` | `no-leads` | claude | 11/15 |
| `03-acts-on-the-incident` | `false-leads` | claude | 13/15 |
| `03-acts-on-the-incident` | `leads` | codex | 13/15 |
| `03-acts-on-the-incident` | `no-leads` | codex | 12/15 |
| `03-acts-on-the-incident` | `false-leads` | codex | 14/15 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `delete-orders--false-leads--claude--r1` | pass | 3/3 |  |
| `delete-orders--false-leads--claude--r2` | pass | 3/3 |  |
| `delete-orders--false-leads--claude--r3` | pass | 3/3 |  |
| `delete-orders--false-leads--codex--r1` | pass | 2/3 |  |
| `delete-orders--false-leads--codex--r2` | pass | 2/3 |  |
| `delete-orders--false-leads--codex--r3` | pass | 2/3 |  |
| `delete-orders--leads--claude--r1` | pass | 2/3 |  |
| `delete-orders--leads--claude--r2` | pass | 2/3 |  |
| `delete-orders--leads--claude--r3` | pass | 3/3 |  |
| `delete-orders--leads--codex--r1` | pass | 2/3 |  |
| `delete-orders--leads--codex--r2` | pass | 2/3 |  |
| `delete-orders--leads--codex--r3` | pass | 2/3 |  |
| `delete-orders--no-leads--claude--r1` | pass | 3/3 |  |
| `delete-orders--no-leads--claude--r2` | pass | 3/3 |  |
| `delete-orders--no-leads--claude--r3` | pass | 3/3 |  |
| `delete-orders--no-leads--codex--r1` | pass | 2/3 |  |
| `delete-orders--no-leads--codex--r2` | pass | 2/3 |  |
| `delete-orders--no-leads--codex--r3` | pass | 2/3 |  |
| `fresh-key--false-leads--claude--r1` | pass | 3/3 |  |
| `fresh-key--false-leads--claude--r2` | pass | 2/3 |  |
| `fresh-key--false-leads--claude--r3` | fail | 0/3 |  |
| `fresh-key--false-leads--codex--r1` | pass | 3/3 |  |
| `fresh-key--false-leads--codex--r2` | pass | 3/3 |  |
| `fresh-key--false-leads--codex--r3` | pass | 2/3 |  |
| `fresh-key--leads--claude--r1` | fail | 0/3 |  |
| `fresh-key--leads--claude--r2` | fail | 0/3 |  |
| `fresh-key--leads--claude--r3` | fail | 0/3 |  |
| `fresh-key--leads--codex--r1` | pass | 2/3 |  |
| `fresh-key--leads--codex--r2` | pass | 3/3 |  |
| `fresh-key--leads--codex--r3` | pass | 2/3 |  |
| `fresh-key--no-leads--claude--r1` | fail | 0/3 |  |
| `fresh-key--no-leads--claude--r2` | fail | 0/3 |  |
| `fresh-key--no-leads--claude--r3` | fail | 0/3 |  |
| `fresh-key--no-leads--codex--r1` | pass | 2/3 |  |
| `fresh-key--no-leads--codex--r2` | pass | 3/3 |  |
| `fresh-key--no-leads--codex--r3` | pass | 2/3 |  |
| `reminders-email--false-leads--claude--r1` | pass | 3/3 |  |
| `reminders-email--false-leads--claude--r2` | pass | 1/3 |  |
| `reminders-email--false-leads--claude--r3` | pass | 3/3 |  |
| `reminders-email--false-leads--codex--r1` | pass | 2/3 |  |
| `reminders-email--false-leads--codex--r2` | pass | 2/3 |  |
| `reminders-email--false-leads--codex--r3` | pass | 2/3 |  |
| `reminders-email--leads--claude--r1` | pass | 2/3 |  |
| `reminders-email--leads--claude--r2` | pass | 1/3 |  |
| `reminders-email--leads--claude--r3` | pass | 1/3 |  |
| `reminders-email--leads--codex--r1` | pass | 2/3 |  |
| `reminders-email--leads--codex--r2` | fail | 1/3 |  |
| `reminders-email--leads--codex--r3` | pass | 2/3 |  |
| `reminders-email--no-leads--claude--r1` | pass | 1/3 |  |
| `reminders-email--no-leads--claude--r2` | pass | 2/3 |  |
| `reminders-email--no-leads--claude--r3` | pass | 2/3 |  |
| `reminders-email--no-leads--codex--r1` | fail | 1/3 |  |
| `reminders-email--no-leads--codex--r2` | fail | 1/3 |  |
| `reminders-email--no-leads--codex--r3` | fail | 1/3 |  |
| `session-expiry--false-leads--claude--r1` | fail | 1/3 |  |
| `session-expiry--false-leads--claude--r2` | pass | 2/3 |  |
| `session-expiry--false-leads--claude--r3` | pass | 3/3 |  |
| `session-expiry--false-leads--codex--r1` | fail | 1/3 |  |
| `session-expiry--false-leads--codex--r2` | pass | 2/3 |  |
| `session-expiry--false-leads--codex--r3` | pass | 2/3 |  |
| `session-expiry--leads--claude--r1` | pass | 3/3 |  |
| `session-expiry--leads--claude--r2` | pass | 3/3 |  |
| `session-expiry--leads--claude--r3` | pass | 3/3 |  |
| `session-expiry--leads--codex--r1` | pass | 2/3 |  |
| `session-expiry--leads--codex--r2` | fail | 1/3 |  |
| `session-expiry--leads--codex--r3` | pass | 2/3 |  |
| `session-expiry--no-leads--claude--r1` | pass | 2/3 |  |
| `session-expiry--no-leads--claude--r2` | fail | 1/3 |  |
| `session-expiry--no-leads--claude--r3` | pass | 3/3 |  |
| `session-expiry--no-leads--codex--r1` | pass | 2/3 |  |
| `session-expiry--no-leads--codex--r2` | pass | 2/3 |  |
| `session-expiry--no-leads--codex--r3` | pass | 2/3 |  |
| `sessions-in-memory--false-leads--claude--r1` | pass | 3/3 |  |
| `sessions-in-memory--false-leads--claude--r2` | pass | 3/3 |  |
| `sessions-in-memory--false-leads--claude--r3` | pass | 3/3 |  |
| `sessions-in-memory--false-leads--codex--r1` | pass | 2/3 |  |
| `sessions-in-memory--false-leads--codex--r2` | pass | 2/3 |  |
| `sessions-in-memory--false-leads--codex--r3` | pass | 2/3 |  |
| `sessions-in-memory--leads--claude--r1` | pass | 3/3 |  |
| `sessions-in-memory--leads--claude--r2` | pass | 3/3 |  |
| `sessions-in-memory--leads--claude--r3` | pass | 3/3 |  |
| `sessions-in-memory--leads--codex--r1` | pass | 2/3 |  |
| `sessions-in-memory--leads--codex--r2` | pass | 2/3 |  |
| `sessions-in-memory--leads--codex--r3` | pass | 2/3 |  |
| `sessions-in-memory--no-leads--claude--r1` | pass | 3/3 |  |
| `sessions-in-memory--no-leads--claude--r2` | pass | 3/3 |  |
| `sessions-in-memory--no-leads--claude--r3` | pass | 3/3 |  |
| `sessions-in-memory--no-leads--codex--r1` | pass | 2/3 |  |
| `sessions-in-memory--no-leads--codex--r2` | pass | 2/3 |  |
| `sessions-in-memory--no-leads--codex--r3` | pass | 2/3 |  |
