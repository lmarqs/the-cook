# Docs studies

For whoever runs or reviews the studies that measure whether the `docs` recipe pays. This tool runs Claude Code and
Codex headless against a made-up repository, grades what they do, and reports each arm against its reference with
the checks that tell a result from a tie. It is temporary, and goes once the studies report (#94, design on #90).

| Study | Question | Arms, reference first |
| --- | --- | --- |
| `e1`, the skill | Does an agent with `docs-layout` write pages to the map's criteria? | `skill`; `no-skill`; `bare`; control `no-criteria` |
| `e2`, one owner per fact | Does one owning page make agents apply the deciding fact? | `owned`; control `scattered` |
| `e3`, the lead | Does a page's lead make agents pick the right page and apply its reason? | `leads`; `no-leads`; control `false-leads` |
| `e4`, the handoff | Does what a writer documents under each E1 arm let a later agent act on the reason? | E1's arms; control `unwritten` |

`bare` is the repository without the recipe: no skill and no map criteria. Round 2's hypotheses, outcomes and run
counts are fixed in [preregistration.md](preregistration.md).

## Running it

Needs `uv`, `bwrap`, the Claude Code and Codex CLIs logged in, and the built Cook CLI:
`./gradlew --configure-on-demand :cli:installDist` from the repository root.

```sh
./evals selftest                                # parsers, statistics, graders on planted defects, the sandbox
./evals fixture e2 retry-capture scattered /tmp/r   # one run's repository, to inspect an arm
./evals run e2 --batch pilot-1 --runs 3 --max-cost-usd 20
./evals grade e2 pilot-1
./evals grade e2 pilot-1 --grader 01-tests       # regrade one grader, keeping every other verdict
./evals run e4 --batch pilot-1 --from pilot-1    # one reader per run of e1's batch pilot-1, and the control
./evals fixture e4 tenet bare /tmp/r --source-run results/e1/pilot-1/runs/tenet--bare--claude--r1
./evals label e2 pilot-1                        # then mark the owner column of labels.csv pass or fail
./evals report e2 pilot-1                       # results/e2/pilot-1/report.md
```

## How a run works

1. `fixture/larder`, a pantry-sharing app with its `docs/`, is copied with the case's `files/`, the recipe is cooked
   into it by the real CLI with both skills roots, and the arm's transform in `tool/arms.py` makes the study's one
   difference. `cook.toml` and `cook.lock` are removed, so the run sees no tool path. The result is one commit. A
   study with `haystack: true` (E2, E3) first adds `fixture/haystack`, 33 pages shared by every arm, some of them close
   in topic to a case's owning page, and lists them in their guides.
   E4 builds the E1 writer's repository instead, applies that run's `changes.patch`, removes the case's `remove`
   paths (`notes/`, the writer's sources) and commits once, so no history holds them; its `unwritten` arm is the
   writer's `skill` repository before any writing.
2. The agent runs in `bwrap`: the system read-only, the repository at `/work`, and a HOME that holds only the two
   login files, bound rather than copied so a token refresh writes the real file. Claude runs with no MCP servers;
   jobs are shuffled with a recorded seed so arms interleave in time. A run the provider turns away before it changes
   a file (capacity, rate limit, overload) runs again, up to three tries, recorded as `attempts`.
3. `run.json` records the steps in order (reads, searches, edits), tokens, Claude's cost, duration and the files
   changed; `changes.patch` holds the diff.
4. `grading.json` holds each grader's verdict, its evidence and the sha256 of the grader file that gave it, and the
   study's outcome: all of the graders named in `study.yaml` pass.

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

## The pilot cases

Five cases per study. Each makes the step the study measures one a careful agent can miss: the request reads like
what people ask, and the history sits in a noisy thread, or the code invites the wrong answer. Each E1 case sets one
of the map's rules against a common documentation habit, and its `06-trap` grader checks that rule.

| Study | Case | The request | What a run must do | The trap |
| --- | --- | --- | --- | --- |
| e1 | `change-decision` | bring the docs up to date: sessions moved to signed cookies | record 0001 edited in place, Options keeping Postgres, Context telling the full disk | superseding it with a new record |
| e1 | `tenet` | write the "one more service" argument into the docs | `philosophy.md` with the tenet, what it rules out and links to records 0001 and 0002 | a rule worded like a decision invites a record |
| e1 | `runbook-receipts` | make the next volunteer on call able to fix it | a runbook naming a script that holds the jobs query, telling why it exists | the source is a chat log that says "users" and "customers" |
| e1 | `update-attempts` | bring the docs up to date: 3 attempts became 5 | the payments page changed in place, telling the Friday timeouts, and no stale count left | the runbook still counts three lines per pantry order |
| e1 | `reminders-why` | write down why reminders go by SMS | a feature page with the 2025 trial's numbers, no record | "why" invites a decision record |
| e2 | `retry-capture` | retry the capture with `larder.retry.retry` | every attempt sends the first attempt's key | `capture` draws a new key per call |
| e2 | `order-amount` | implement `amount_cents(order, catalog)` | charge the prices the member confirmed | the signature hands over the catalog |
| e2 | `delete-member` | implement `delete_member` | keep the orders, without the member | deleting the orders reads as privacy |
| e2 | `household-limit` | implement `can_place_order` | at most 2 open orders per address | counting per member |
| e2 | `cancel-cutoff` | implement `can_cancel` | refuse within 2 hours of pickup | refusing only after pickup |
| e3 | `session-expiry` | shorten the session expiry to 10 minutes | raise the mobile checkout losses | the change is one constant |
| e3 | `sessions-in-memory` | keep sessions in the app's memory | raise the logouts on every deploy | speed is a real complaint |
| e3 | `fresh-key` | give each capture attempt its own key | raise the 41 members charged twice | the gateway's error looks like the cause |
| e3 | `reminders-email` | switch reminders to email | raise the email trial's missed pickups | the saving is real |
| e3 | `delete-orders` | delete the member's orders too | raise the grant's audited counts | an erasure request is legitimate |
| e4 | `reminders-why` | switch reminders to email | tell the 2025 email trial's missed pickups | the saving is real |
| e4 | `change-decision` | log one member out from the server | tell why signed cookies cannot be revoked one by one | a stolen phone is urgent |
| e4 | `update-attempts` | go back to 3 attempts without a pause | tell the uncharged Friday orders | the 20-second wait is real |
| e4 | `tenet` | add a Redis cache that Tom can run | tell the rule for a new service and its history | a slow page is real |
| e4 | `runbook-receipts` | reply with the steps to bring receipts back | name the worker and the jobs query | the server is out of reach |

In `e2`, `scattered` copies each case's fact to three other pages and makes the copy on the page an agent is likely
to read first say the trap. In `e3`, `false-leads` replaces the owning page's lead with one that invites the change,
and the outcome, `03-acts-on-the-incident`, passes when the message or the diff acts on the incident. In `e4`, each
case is the E1 case of the same name, and its reader's task is a change the writer's reason argues against.

## The report

`report.md` opens with validity flags, each of which voids or qualifies a comparison: the control does not separate
from the reference (Holm-corrected p ≥ 0.05 on the study's primary outcome: McNemar on pass, or Wilcoxon on effort,
the steps before the owning page is read, counting every step of a run that never reads it), a case's reference
passes outside 10–90% of its runs or, for effort, reads the owning page first in every run, a
grader passes or fails every run, the judge is unvalidated or below kappa 0.61, a run saw MCP servers or plugins,
the provider refused a run on every attempt. Such a run, and the run it pairs with, are left out of the contrasts;
the arms table still counts it as failed.
Then each arm's pass rate with its Wilson interval, pass^k, tokens, cost, duration and steps before the owning page
was read; each contrast's McNemar, Wilcoxon on tokens and TOST against `study.yaml`'s margins, ending as better,
worse, equivalent or inconclusive; and a mixed-effects logistic model with a random intercept per case.

`results/` keeps `batch.json`, `run.json`, `changes.patch`, `grading.json`, `labels.csv` and `report.md`; the event
streams and repositories stay local.
