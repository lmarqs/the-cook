# Study e1: report

Batches: smoke. Runs graded: 6. Reference arm: `skill`. Outcome: all of ['01-record-written', '02-structure', '03-lead', '04-context-tells-what-happened', '05-decision-names-the-product'] pass.

## Validity

- The control `no-criteria` does not separate from `skill` on claude (Holm p = 1.0): no comparison in this study counts.
- The control `no-criteria` does not separate from `skill` on codex (Holm p = 1.0): no comparison in this study counts.
- Case `write-adr` on claude: the reference passes 100% of 1 runs, outside 10%–90%.
- Case `write-adr` on codex: the reference passes 100% of 1 runs, outside 10%–90%.
- Grader `01-record-written` passes every run of every arm: it discriminates nothing.
- Grader `02-structure` passes every run of every arm: it discriminates nothing.
- Grader `03-lead` passes every run of every arm: it discriminates nothing.
- Grader `04-context-tells-what-happened` passes every run of every arm: it discriminates nothing.
- Grader `05-decision-names-the-product` passes every run of every arm: it discriminates nothing.
- The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).

## Arms

| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean | Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude | `skill` | 1 | 100% | 21%–100% | 1.00 | 100% | 184,188 / 184,188 | 3,057 / 3,057 | 0.242 | 35 | 3 |
| claude | `no-skill` | 1 | 100% | 21%–100% | 1.00 | 100% | 152,376 / 152,376 | 3,405 / 3,405 | 0.228 | 41 | 2 |
| claude | `no-criteria` | 1 | 100% | 21%–100% | 1.00 | 100% | 303,172 / 303,172 | 4,432 / 4,432 | 0.333 | 50 | 3 |
| codex | `skill` | 1 | 100% | 21%–100% | 1.00 | 100% | 127,747 / 127,747 | 1,887 / 1,887 | – | 60 | 6 |
| codex | `no-skill` | 1 | 100% | 21%–100% | 1.00 | 100% | 125,239 / 125,239 | 1,442 / 1,442 | – | 54 | 5 |
| codex | `no-criteria` | 1 | 100% | 21%–100% | 1.00 | 100% | 165,479 / 165,479 | 1,919 / 1,919 | – | 58 | 5 |

## Contrasts

Each arm against `skill`, paired by case and run. Superiority: exact McNemar, Holm-corrected across the study. Equivalence: TOST within ±10% of success and ±15% of tokens.

| Contrast | Pairs | Difference in pass | b / c | McNemar p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| claude: no-skill vs skill | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| claude: no-criteria vs skill | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| codex: no-skill vs skill | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |
| codex: no-criteria vs skill | 1 | +0% | 0 / 0 | 1.000 | 1.000 | – | – | – | inconclusive |

## Mixed-effects logistic model

pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.

- claude: needs both outcomes, two arms and two cases
- codex: needs both outcomes, two arms and two cases

## Graders

| Grader | Arm | Harness | Pass |
| --- | --- | --- | --- |
| `01-record-written` | `skill` | claude | 1/1 |
| `01-record-written` | `no-skill` | claude | 1/1 |
| `01-record-written` | `no-criteria` | claude | 1/1 |
| `01-record-written` | `skill` | codex | 1/1 |
| `01-record-written` | `no-skill` | codex | 1/1 |
| `01-record-written` | `no-criteria` | codex | 1/1 |
| `02-structure` | `skill` | claude | 1/1 |
| `02-structure` | `no-skill` | claude | 1/1 |
| `02-structure` | `no-criteria` | claude | 1/1 |
| `02-structure` | `skill` | codex | 1/1 |
| `02-structure` | `no-skill` | codex | 1/1 |
| `02-structure` | `no-criteria` | codex | 1/1 |
| `03-lead` | `skill` | claude | 1/1 |
| `03-lead` | `no-skill` | claude | 1/1 |
| `03-lead` | `no-criteria` | claude | 1/1 |
| `03-lead` | `skill` | codex | 1/1 |
| `03-lead` | `no-skill` | codex | 1/1 |
| `03-lead` | `no-criteria` | codex | 1/1 |
| `04-context-tells-what-happened` | `skill` | claude | 1/1 |
| `04-context-tells-what-happened` | `no-skill` | claude | 1/1 |
| `04-context-tells-what-happened` | `no-criteria` | claude | 1/1 |
| `04-context-tells-what-happened` | `skill` | codex | 1/1 |
| `04-context-tells-what-happened` | `no-skill` | codex | 1/1 |
| `04-context-tells-what-happened` | `no-criteria` | codex | 1/1 |
| `05-decision-names-the-product` | `skill` | claude | 1/1 |
| `05-decision-names-the-product` | `no-skill` | claude | 1/1 |
| `05-decision-names-the-product` | `no-criteria` | claude | 1/1 |
| `05-decision-names-the-product` | `skill` | codex | 1/1 |
| `05-decision-names-the-product` | `no-skill` | codex | 1/1 |
| `05-decision-names-the-product` | `no-criteria` | codex | 1/1 |

## Runs

| Run | Outcome | Graders passed | Error |
| --- | --- | --- | --- |
| `write-adr--no-criteria--claude--r1` | pass | 5/5 |  |
| `write-adr--no-criteria--codex--r1` | pass | 5/5 |  |
| `write-adr--no-skill--claude--r1` | pass | 5/5 |  |
| `write-adr--no-skill--codex--r1` | pass | 5/5 |  |
| `write-adr--skill--claude--r1` | pass | 5/5 |  |
| `write-adr--skill--codex--r1` | pass | 5/5 |  |
