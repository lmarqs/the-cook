# Docs studies

For whoever runs or reviews the studies that measure whether the `docs` recipe pays. This tool runs Claude Code and
Codex headless against a made-up repository, grades what they do, and reports each arm against its reference with
the checks that tell a result from a tie. It exists because the spike of 2026-10-29 scored every arm 30 of 30 and
could not tell them apart; it is temporary, and goes once the studies report (#94, design on #90).

| Study | Question | Arms, reference first |
| --- | --- | --- |
| `e1`, the skill | Does an agent with `docs-layout` write pages to the map's criteria? | `skill`; `no-skill`; control `no-criteria` |
| `e2`, one owner per fact | Does one owning page make agents apply the deciding fact? | `owned`; control `scattered` |
| `e3`, the lead | Does a page's lead make agents pick the right page and apply its reason? | `leads`; `no-leads`; control `false-leads` |

## Running it

Needs `uv`, `bwrap`, the Claude Code and Codex CLIs logged in, and the built Cook CLI:
`./gradlew --configure-on-demand :cli:installDist` from the repository root.

```sh
./evals selftest                                # parsers, statistics, graders on planted defects, the sandbox
./evals fixture e2 retry-capture scattered /tmp/r   # one run's repository, to inspect an arm
./evals run e2 --batch pilot-1 --runs 3 --max-cost-usd 20
./evals grade e2 pilot-1
./evals label e2 pilot-1                        # then mark the owner column of labels.csv pass or fail
./evals report e2 pilot-1                       # results/e2/pilot-1/report.md
```

## How a run works

1. `fixture/larder`, a pantry-sharing app with its `docs/`, is copied with the case's `files/`, the recipe is cooked
   into it by the real CLI with both skills roots, and the arm's transform in `tool/arms.py` makes the study's one
   difference. `cook.toml` and `cook.lock` are removed, so the run sees no tool path. The result is one commit.
2. The agent runs in `bwrap`: the system read-only, the repository at `/work`, and a HOME that holds only the two
   login files, bound rather than copied so a token refresh writes the real file. Claude runs with no MCP servers;
   jobs are shuffled with a recorded seed so arms interleave in time.
3. `run.json` records the steps in order (reads, searches, edits), tokens, Claude's cost, duration and the files
   changed; `changes.patch` holds the diff.
4. `grading.json` holds each grader's verdict and evidence, and the study's outcome: all of the graders named in
   `study.yaml` pass.

## Cases and graders

A case is `studies/<study>/cases/<id>/`: `prompt.md` with `max_turns` and `timeout_seconds` in its header,
`case.yaml` with the arm data and the `owning_page`, `graders/NN-<name>.md`, and optionally `files/` copied into the
repository and `hidden/` tests the run never sees. A grader's header names its `type`; its body is the expectation.

| Type | Passes when |
| --- | --- |
| `file_exists` | a changed file matches `glob` |
| `regex` | `pattern` matches the `target`: `last_message`, `diff`, or changed files matching a glob |
| `tests_pass` | the repository's tests and the case's `hidden` tests pass, run in the sandbox without network |
| `read_before_edit` | `path` is read before the first edit |
| `docs_structure` | each changed page meets the review's structural rules |
| `lead_present` | each changed page matching `glob` opens with a lead that is not its title restated |
| `judge` | a model of the other family, blind to the arm, finds the expectation met in what `show` lists |

## The report

`report.md` opens with validity flags, each of which voids or qualifies a comparison: the control does not separate
from the reference (Holm-corrected McNemar p ≥ 0.05), a case's reference passes outside 10–90% of its runs, a
grader passes or fails every run, the judge is unvalidated or below kappa 0.61, a run saw MCP servers or plugins.
Then each arm's pass rate with its Wilson interval, pass^k, tokens, cost, duration and steps before the owning page
was read; each contrast's McNemar, Wilcoxon on tokens and TOST against `study.yaml`'s margins, ending as better,
worse, equivalent or inconclusive; and a mixed-effects logistic model with a random intercept per case.

`results/` keeps `batch.json`, `run.json`, `changes.patch`, `grading.json`, `labels.csv` and `report.md`; the event
streams and repositories stay local.
