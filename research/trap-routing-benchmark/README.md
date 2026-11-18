# Trap routing benchmark

Does the `agents-layout` skill's memory text after PR #134 send learned facts to the right place more reliably than
the text before it? Each run gives one agent one version of
`cookbook/src/agents/skills/agents-layout/references/memory.md` and one learned fact, and asks where the team should
record the fact. It measures routing with the text in hand, not whether a run notices a trap or invokes the skill on
its own.

## Method

**Arms.** `old` is the text at `6e34b991`, before the PR; `new` the text at `c7b3d7c3`, the PR's commit; `none` gives
no text, only the list of destinations, as a floor. `run.py` reads each text with `git show`, and records the full
commit in each result.

**Facts.** 24 in `facts.json`, each written as what a run saw, in words neither text uses for its categories. One
shared description of the repository precedes every fact. Groups:

| Group | Facts | What it tests |
| --- | --- | --- |
| `control` | 9 | One fact per row of the table that sends a fact away from memory; the rewrite should not move them |
| `example` | 3 | The example entries of both texts |
| `recognition` | 6 | One fact per sign in the new text's "How to recognize a trap" |
| `contested` | 2 | Facts the research note could not decide from the old text |
| `slow` | 2 | A run that was slow, not wrong; only the new text addresses these |
| `no_row` | 2 | Facts no row fits |

**Labels.** Each fact has the destination the new text gives it, with a reason. The author of the new text wrote
them, so they favour it. Three contested facts also accept a second label, and accuracy is shown strict and lenient.
Consistency, the headline measure, needs no labels.

**Answer.** One JSON object, `destination` from ten fixed values and a one-sentence `reason`, enforced by each
harness's schema option (`schema.json`). The destination list and its wording are the same in every arm.

**Runs.** 24 facts, 3 arms, 2 harnesses, 10 runs each: 1,440 runs. Each is a fresh headless call with no tools, in
an empty directory outside the repository.

| Harness | Version | Model | Isolation |
| --- | --- | --- | --- |
| Claude Code | 2.1.327 | `claude-opus-5-5` | `--setting-sources ''`, `--tools ''`, `--strict-mcp-config`, a one-line `--system-prompt` |
| Codex CLI | 0.169.0 | `gpt-6-astra`, medium effort | `--ignore-user-config`, `--ignore-rules`, `--ephemeral`, read-only sandbox |

A probe run in each harness listed the context it was given: no `CLAUDE.md`, `AGENTS.md`, rules or skills reached
either. Claude Code still injects the account's email and an environment block; `--bare`, which drops them, accepts
only an API key. The results were checked for personal data before commit.

**Measures, fixed before the run.**

1. Consistency: for each fact, the share of its 10 runs that give its most common answer.
2. Accuracy against the label, strict and lenient, per group.
3. Precision and recall of answering `memory`, against the strict label.

**What would support the PR.** The old arm sends the old text's own examples away from memory and the new arm keeps
the new ones in it; controls score the same in both arms; consistency on `recognition`, `contested` and `no_row` is
higher in `new`. A difference counts only where it holds in both harnesses. `slow` is reported but left out of the
comparison, because the old text says nothing about it by construction.

## Rerun

```sh
python3 run.py                # all cells; skips those already in results/raw.jsonl, so a stopped run resumes
python3 score.py > results/score.md
```

`run.py --facts <id>... --arms <arm>... --harness <harness>... --runs <n>` runs a subset. `score.py` exits non-zero on
a missing or duplicate cell; `--partial` scores an incomplete file. Where two answers tie for most common, the one
seen first is shown.

## Results

Run on 2026-11-18: 1,440 runs, none failed, none missing. Full tables in [results/score.md](results/score.md); every
answer and its reason in `results/raw.jsonl`. The Claude runs cost $7.29 at list price; the Codex runs read 14.9M
input tokens, mostly its own cached base prompt.

**The new text routes better than the old in both harnesses.** Accuracy against the labels, all 24 facts:

| | Claude old | Claude new | Codex old | Codex new |
| --- | --- | --- | --- | --- |
| Strict | 60% | 83% | 41% | 70% |
| Recall of `memory` | 48% | 84% | 6% | 44% |
| Precision of `memory` | 96% | 100% | 100% | 100% |

Per fact, `slow` left out, the new text scored higher on 6 facts and lower on none in Claude (one-sided sign test
p = 0.016), and higher on 7 and lower on 1 in Codex (p = 0.035). The labels are the new text's author's, so these
numbers say the new text is followed, not that its routing is right.

**Against the criteria fixed before the run:**

| Criterion | Claude | Codex |
| --- | --- | --- |
| Old text sends its own examples away from memory | Yes: `migration_number` and `db_reset` go to the policy | Yes, both |
| New text keeps its own examples in memory | Yes, both | Half: `db_migrate_exit` yes, `migration_number` goes to the policy 10 of 10 |
| Controls no worse | Yes: 89% to 100% | Yes: 78% to 91% |
| Consistency higher on `recognition`, `contested`, `no_row` | `recognition` 85% to 97%, `contested` 50% to 90%, `no_row` unchanged at 100% | `recognition` unchanged at 100%, `contested` 75% to 100%, `no_row` 95% to 85% |

Consistency is at or near its ceiling almost everywhere: both models give the same answer to most facts in all three
arms, right or wrong. It separates the arms only on the contested facts, where it rose in both harnesses.

**Where the new text does not work:**

- *A check bigger than the change.* `coupled_proto` goes to the check or policy in all 60 of its runs, every arm
  and harness. The sentence that keeps such a trap in memory until the check exists does not move either model,
  and Codex reads `migration_number` the same way.
- *No row fits.* `external_tracker` and `deadline` should go nowhere, with the team asked. Claude never answers
  that, in any arm; Codex does for `deadline` in 7 of 10 new-text runs, and never for `external_tracker`. The
  sentence is not followed.
- *Slow, not wrong.* `hard_to_find` reaches `AGENTS.md` in both harnesses with the new text, where the old text
  sent it nowhere. `costly_lookup` reaches a skill in Codex, but `AGENTS.md` in Claude 6 of 10 times.
- *A command that needs a flag.* `needs_flag` goes to the policy in all 10 Codex runs with the new text, where the
  old text sent it nowhere, which its second label accepts; Claude moved the other way, to memory.
- *Every task.* Codex sends `every_task` to the check or policy with both texts, not to `AGENTS.md`.

**What this does not show.**

- Whether a run notices a trap or invokes the skill when nobody asks; every run here had the text and the question.
- Anything beyond one wording per fact, one model per harness and 24 facts written by the new text's author.
- That the labels are right. Where both models agree against a label in every arm, as on `coupled_proto` and
  `external_tracker`, the label may be what is wrong.
