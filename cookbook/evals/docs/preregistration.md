# Preregistration, round 2

For whoever runs or reviews round 2 of the docs studies. It fixes, before any round-2 data exists, what each study
tests, which runs count, and how the result is read, so the outcome cannot be fitted to the data. Written after the
round-1 pilots: 330 runs in which no control separated, because on a 12-page repository every agent read the owning
page within a median of 5–6 steps whatever the arm.

## Question

Does a repository laid out by the `docs` recipe let agents act on its reasons better, or at lower cost, than the same
repository without the recipe? E4 answers it for the reasons a writer records; E2 and E3 answer it for pages already
written; E1 measures what the recipe costs the writer.

## Studies and hypotheses

| Study | Arms run, reference first | Primary outcome | Hypothesis |
| --- | --- | --- | --- |
| E4, the handoff | `skill`, `bare`, control `unwritten` | `01-acts-on-the-reason` passes | A reader of `skill` docs acts on the reason more often than a reader of `bare` docs |
| E1, the writer | `skill`, `bare` | input and output tokens | The skill costs the writer more tokens than no recipe |
| E3, the lead | `leads`, `no-leads`, control `false-leads` | effort: steps before the owning page is read | Without leads, or with false ones, agents take more steps to reach the owning page |
| E2, one owner | `owned`, control `scattered` | `01-tests` passes | Scattered copies, one stale, make agents apply the deciding fact less often |

E1's rule graders and E3's `03-acts-on-the-incident` are secondary outcomes, reported but not tested for the
question. E4 reads the E1 batch of the same round: each E1 run is a writer, and its E4 run the reader of what it wrote.

## Design

- Harnesses: Claude Code (`claude-sonnet-5`, medium effort) and Codex (`gpt-6.1-sol`, medium effort), analysed separately. A
  harness's result does not stand in for the other's.
- Cases: the five in each study's `cases/`, as committed when the round-2 pilot starts. E2 and E3 run with the
  haystack, 33 extra pages shared by every arm.
- Pairing: by case and run number within a harness. Superiority: exact McNemar for pass, Wilcoxon signed-rank for
  effort and tokens, Holm-corrected across each study's contrasts on its primary outcome. Equivalence: TOST within
  ±10 points of success and ±15% of tokens.
- Exclusion: a run the provider, the login or the network turns away on all 3 attempts, before it changes a file,
  and a run the login or the network ends after it changed files, measure the infrastructure and not the arm. Each is
  left out of every contrast together with the run it pairs with, and `report.md` names it.
- Validity, as `report.md` flags it: the control separates from the reference (Holm p < 0.05, in the expected
  direction); the reference passes 10–90% of runs on each case (pass outcomes) or reads the owning page after at
  least one other step (effort); no grader passes or fails every run; the judge's kappa against the owner's labels is
  at least 0.61. A study whose control does not separate reports no comparison.

## Runs

The round starts with a pilot of 3 runs per case, arm and harness. Its only uses are the validity flags and the effort
distribution for E3's power; its runs are not pooled into the confirmatory batch, and a case whose reference shows no
headroom in the pilot is replaced before the confirmatory batch, never after.

The confirmatory batch then runs 12 runs per case, arm and harness for E1 and E4 (60 pairs per contrast and harness),
and 6 for E2 and E3 (30 pairs). From a simulation of the exact McNemar test with independent pairs (estimates, 4,000
draws each, script below), 60 pairs detect a 30-point difference (80% against 50%) with 81% power at α 0.017 and
91% at α 0.05, Holm's thresholds bracketing a study's contrasts, and a 20-point difference (70% against 50%) with 35%
and 55%. Smaller effects
than 20 points are out of reach at this budget, and a tie is then reported as inconclusive, not as no effect.

| Pass rates, reference / other | α | n = 15 | n = 30 | n = 45 | n = 60 | n = 90 |
| --- | --- | --- | --- | --- | --- | --- |
| 70% / 50% | 0.050 | 0.09 | 0.26 | 0.40 | 0.55 | 0.75 |
| 80% / 50% | 0.050 | 0.23 | 0.58 | 0.79 | 0.91 | 0.99 |
| 70% / 30% | 0.050 | 0.43 | 0.82 | 0.95 | 0.99 | 1.00 |
| 80% / 50% | 0.017 | 0.11 | 0.40 | 0.64 | 0.81 | 0.97 |
| 70% / 50% | 0.017 | 0.04 | 0.13 | 0.24 | 0.35 | 0.57 |

Claude's cost per run is measured by the pilot, capped at $30 for its 150 Claude runs; round 1 measured it on
`claude-opus-5-5` (writers $0.28, readers $0.13–0.20), at which the confirmatory batch's 450 runs cost $70–130.
Codex reports none.

## What may change, and what may not

- After the pilot and before the confirmatory batch: cases without headroom, graders with a defect, each change
  committed and named on #94 before the batch starts.
- After the confirmatory batch starts: nothing. A grader defect found later is fixed, regraded with
  `evals grade --grader`, and both results are reported; each verdict's `grader_sha` shows which grader gave it.
- The judge is validated on the pilot's labels (`evals label`, then `evals kappa`) before the confirmatory batch is
  graded.

## Power script

```python
import numpy as np
from scipy import stats

rng = np.random.default_rng(7)


def power(p_ref, p_other, n, alpha, sims=4000):
    hits = 0
    for _ in range(sims):
        r, o = rng.random(n) < p_ref, rng.random(n) < p_other
        b, c = int(np.sum(r & ~o)), int(np.sum(o & ~r))
        hits += (1.0 if b + c == 0 else stats.binomtest(b, b + c, 0.5).pvalue) < alpha
    return hits / sims
```
