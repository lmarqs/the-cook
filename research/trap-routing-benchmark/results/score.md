## Accuracy by group, strict (lenient where a second label exists)

| Group | Facts | claude old | claude new | claude none | codex old | codex new | codex none |
| --- | --- | --- | --- | --- | --- | --- | --- |
| control | 9 | 89% | 100% | 69% | 78% | 91% | 89% |
| example | 3 | 70% | 100% | 33% | 33% | 67% | 33% |
| recognition | 6 | 53% (55%) | 80% (82%) | 17% | 17% (33%) | 50% (67%) | 30% |
| contested | 2 | 50% (75%) | 90% | 0% (50%) | 25% (80%) | 50% | 40% (50%) |
| slow | 2 | 0% | 70% | 50% | 20% | 100% | 0% |
| no_row | 2 | 0% | 0% | 0% | 0% | 35% | 0% |
| all | 24 | 60% (62%) | 83% (84%) | 38% (42%) | 41% (50%) | 70% (75%) | 48% (49%) |

## Consistency by group: mean share of runs giving a fact's most common answer

| Group | claude old | claude new | claude none | codex old | codex new | codex none |
| --- | --- | --- | --- | --- | --- | --- |
| control | 100% | 100% | 94% | 100% | 98% | 100% |
| example | 97% | 100% | 100% | 100% | 100% | 100% |
| recognition | 85% | 97% | 97% | 100% | 100% | 97% |
| contested | 50% | 90% | 80% | 75% | 100% | 70% |
| slow | 90% | 80% | 100% | 80% | 100% | 100% |
| no_row | 100% | 100% | 100% | 95% | 85% | 100% |
| all | 91% | 97% | 95% | 96% | 98% | 97% |

## Memory or not: precision and recall of answering memory, against the strict label

| Measure | claude old | claude new | claude none | codex old | codex new | codex none |
| --- | --- | --- | --- | --- | --- | --- |
| precision | 96% | 100% | - | 100% | 100% | 62% |
| recall | 48% | 84% | 0% | 6% | 44% | 18% |

## Per fact: most common answer, its share of runs, and a mark when it matches the label

| Fact | Group | Label | claude old | claude new | claude none | codex old | codex new | codex none |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| secret | control | not_in_repository | not_in_repository 100% ✓ | not_in_repository 100% ✓ | not_in_repository 100% ✓ | not_in_repository 100% ✓ | not_in_repository 100% ✓ | not_in_repository 100% ✓ |
| person | control | not_in_repository | not_in_repository 100% ✓ | not_in_repository 100% ✓ | personal_settings 100% | not_in_repository 100% ✓ | not_in_repository 100% ✓ | not_in_repository 100% ✓ |
| plan | control | tracker_or_commit | tracker_or_commit 100% ✓ | tracker_or_commit 100% ✓ | tracker_or_commit 100% ✓ | tracker_or_commit 100% ✓ | tracker_or_commit 100% ✓ | tracker_or_commit 100% ✓ |
| preference | control | personal_settings | personal_settings 100% ✓ | personal_settings 100% ✓ | personal_settings 100% ✓ | personal_settings 100% ✓ | personal_settings 100% ✓ | personal_settings 100% ✓ |
| shown | control | nowhere_already_shown | nowhere_already_shown 100% ✓ | nowhere_already_shown 100% ✓ | nowhere_already_shown 100% ✓ | nowhere_already_shown 100% ✓ | nowhere_already_shown 100% ✓ | nowhere_already_shown 100% ✓ |
| enforced | control | check_hook_or_policy | nowhere_already_shown 100% | check_hook_or_policy 100% ✓ | nowhere_already_shown 100% | nowhere_already_shown 100% | check_hook_or_policy 100% ✓ | nowhere_already_shown 100% |
| every_task | control | agents_md | agents_md 100% ✓ | agents_md 100% ✓ | agents_md 100% ✓ | check_hook_or_policy 100% | check_hook_or_policy 80% | agents_md 100% ✓ |
| procedure | control | skill | skill 100% ✓ | skill 100% ✓ | skill 100% ✓ | skill 100% ✓ | skill 100% ✓ | skill 100% ✓ |
| product_decision | control | documentation | documentation 100% ✓ | documentation 100% ✓ | agents_md 50% | documentation 100% ✓ | documentation 100% ✓ | documentation 100% ✓ |
| migration_number | example | memory | check_hook_or_policy 90% | memory 100% ✓ | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% |
| db_reset | example | check_hook_or_policy | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ |
| db_migrate_exit | example | memory | memory 100% ✓ | memory 100% ✓ | check_hook_or_policy 100% | check_hook_or_policy 100% | memory 100% ✓ | check_hook_or_policy 100% |
| lint_fixes | recognition | memory | memory 60% ✓ | memory 90% ✓ | agents_md 100% | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% |
| coupled_proto | recognition | memory | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% |
| generated_file | recognition | memory / check_hook_or_policy | memory 90% ✓ | memory 90% ✓ | agents_md 100% | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | agents_md 100% |
| false_done | recognition | memory | memory 70% ✓ | memory 100% ✓ | tracker_or_commit 100% | check_hook_or_policy 100% | memory 100% ✓ | check_hook_or_policy 100% |
| overridden_setting | recognition | memory | documentation 90% | memory 100% ✓ | agents_md 80% | nowhere_already_shown 100% | memory 100% ✓ | memory 80% ✓ |
| narrow_guard | recognition | check_hook_or_policy | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ | check_hook_or_policy 100% ✓ |
| flaky_test | contested | memory / tracker_or_commit | memory 50% ✓ | memory 100% ✓ | tracker_or_commit 100% ✓ | memory 50% ✓ | memory 100% ✓ | memory 80% ✓ |
| needs_flag | contested | memory / nowhere_already_shown | memory 50% ✓ | memory 80% ✓ | check_hook_or_policy 60% | nowhere_already_shown 100% ✓ | check_hook_or_policy 100% | agents_md 60% |
| hard_to_find | slow | agents_md | nowhere_already_shown 100% | agents_md 100% ✓ | agents_md 100% ✓ | nowhere_already_shown 100% | agents_md 100% ✓ | memory 100% |
| costly_lookup | slow | skill | nowhere_already_shown 80% | agents_md 60% | agents_md 100% | nowhere_already_shown 60% | skill 100% ✓ | agents_md 100% |
| external_tracker | no_row | none_of_these | check_hook_or_policy 100% | check_hook_or_policy 100% | check_hook_or_policy 100% | documentation 90% | agents_md 100% | agents_md 100% |
| deadline | no_row | none_of_these | documentation 100% | documentation 100% | documentation 100% | documentation 100% | none_of_these 70% ✓ | documentation 100% |

Runs scored: 1440. Missing cells: 0.
