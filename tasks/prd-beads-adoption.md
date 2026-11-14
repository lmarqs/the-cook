[PRD]
# PRD: Beads adoption for teams and personal use

## Overview

Provide a Beads cookbook for a team adopting Beads as its repository tracker and a contributor keeping task
data local. Offer Beads Viewer as an optional addition. Both modes use recipe-owned configuration that can
be reviewed, updated and discarded. Correct ralph's advice to add unmanaged local ignore rules.

The user approved a runtime spike followed by implementation, optional Viewer integration, and the repository's
quality gates. The proposed recipe split is `beads#tracker` and `beads#viewer`: two recipes under
`cookbook/src/beads/`, with independent ownership and removal. Sharing is a parameter of tracker setup,
`--share=team|clone`, following ralph. Viewer is selected by cooking its recipe, rather than a tracker flag.

The [research](../research/beads-and-viewer.md) inventories current code, the removed recipe and primary upstream
sources. It is source evidence; the spike below supplies runtime evidence before native configuration is encoded.

## Goals

- A team can reproduce tracker configuration and obtain shared tasks in a second clone through documented native steps.
- A personal setup keeps task data and local tracker files out of commits through owned `.gitignore` rules.
- People can add or discard Viewer independently of tracker setup and task data.
- Recooking sharing choices and discarding recipes remove obsolete owned configuration without losing tasks,
  unrelated hooks, instruction text or team-owned seeds.
- Ralph's supported setup instructions lead to owned ignore rules, including when its launcher refuses untracked setup.

## Quality Gates

For every story that changes the repository:

- Run `./gradlew -p <module> check` for each changed module before moving on.
- Run `./gradlew check cliBuild jpackageTest`.
- Run `git diff --check`.
- Report each result, including any check that did not run.

Behavior changes include tests; bug fixes include a regression test observed failing before the fix. Recipe changes
include review of their generated configuration and engine-generated ownership records. Runtime acceptance scenarios
use real pinned Beads and Viewer binaries; fake binaries may test failure paths but cannot establish compatibility.
Experiments use disposable repositories and synthetic tasks, with no production remotes or credentials.

## User Stories

### US-001: Prove team and personal tracker setup

**Description:** As the cookbook maintainer, I want native initialization and sharing behavior measured so that the
recipe describes a working setup with known effects.

**Acceptance Criteria:**

- [ ] Exercise the repository's documented `bd` 1.3.1 baseline; record the actual binary version and platform.
- [ ] Use an empty repository and an existing repository with unrelated ignore rules, instructions and Git hooks.
- [ ] Record each command and its file diff, Git configuration changes, hook effects, commits and effective task store.
- [ ] Establish a team store, create an epic with dependent tasks, and obtain the same task IDs and dependencies in a
  second clone using an isolated test remote. Distinguish code checkout from task-history bootstrap.
- [ ] Establish personal mode with recipe-style root `.gitignore` rules. Demonstrate that normal staging excludes
  local tracker data and that the selected native commands do not add required unmanaged local-exclude rules.
- [ ] Demonstrate the personal sync policy: tasks do not reach the team remote; record inherited configuration effects.
- [ ] Create a linked worktree for each mode and prove it resolves the intended store without creating another database.
- [ ] Establish an initialization sequence that preserves existing hooks/instructions and avoids unexpected staging or
  commits. Where a native action is required after cooking, name it and its effects explicitly.
- [ ] Save reproducible commands, observations and the chosen native configuration in `research/beads-adoption-spike.md`.
  Include a table of proposed targets, owner, owned/seed/runtime kind, and update/discard behavior.
- [ ] Before US-004 begins, update its criteria with the proven target paths, native keys and after-cooking command
  sequences, referencing the experiments that established them.
- [ ] If native behavior prevents the agreed ownership or sharing contract, report the blocker and stop dependent
  implementation. Do not silently change the engine, pins or product scope.

### US-002: Prove optional Viewer and ralph compatibility

**Description:** As a Beads user, I want Viewer and ralph to read the intended current tasks from my worktree so that
their recommendations and execution reflect the tracker I chose.

**Depends on:** US-001.

**Acceptance Criteria:**

- [ ] Exercise `bv` 0.25.2 and ralph-tui 0.12.0, recording actual versions; keep newer-main observations distinct.
- [ ] From the main checkout and a linked worktree, compare tracker task IDs with Viewer input and robot output in both
  sharing modes. Include a missing export, a changed task, a failed refresh and an externally selected workspace.
- [ ] Record the command that refreshes Viewer input, how stale data is reported, and what failure the integration
  must expose. A stale recommendation must not be presented as a verified current claim.
- [ ] Use a small dependency graph to show what Viewer adds to `bd ready`; verify recommendations respect the selected
  task source. Record that selection does not reserve or claim a task.
- [ ] Run one serial epic iteration through ralph with a fake worker and the real tracker, separately with `beads` and
  `beads-bv`. Confirm expected task selection and runner-owned completion. No AI credentials are required.
- [ ] Record viewer-specific native configuration or reusable guidance that `#viewer` should own, and its exact paths,
  parameters and ownership boundaries. Use a distinct instruction section and ignore section where needed.
- [ ] Add findings to the spike report. If Viewer needs no useful reusable contribution, raise that finding before
  building an empty recipe. Do not invent settings to justify the proposed split.
- [ ] Before US-005 begins, update its criteria with the proven Viewer paths, parameters, refresh commands and failure
  behavior, so the implementer has a concrete interface rather than a choice deferred to implementation.

### US-003: Keep ralph ignore guidance within recipe ownership

**Description:** As a ralph recipe user, I want its guidance to keep ignore configuration managed by Cook so that
changing or discarding the recipe removes the configuration it introduced.

**Acceptance Criteria:**

- [ ] Replace the manual `.git/info/exclude` append advice in `cookbook/src/ralph/ralph-tui.groovy` and the refusal
  diagnostic in `cookbook/src/ralph/bin/run.sh` with the appropriate team or `--share=clone` recipe path.
- [ ] Recook examples preserve the user's other recipe arguments; they do not accidentally reset tracker or harness choices.
- [ ] Verify team-to-clone and clone-to-team transitions update only owned ignore content and the engine's record;
  verify discard removes the owned section and preserves unrelated ignore rules.
- [ ] Explain existing tracked-file handling and stale manual exclusions. Previously added local exclusions remain
  user-owned: give inspection and review guidance, rather than silently deleting them.
- [ ] The launcher may continue recognizing legitimate user-managed exclusions, but does not write them or recommend
  them as the recipe's normal setup path. Preserve its checks before copying uncommitted configuration into a worktree.
- [ ] Update affected tests in `cookbook/test/ralph/RalphTuiCookTest.groovy` and rendered help in `cookbook/README.md`.

### US-004: Add the Beads tracker recipe

**Description:** As a team or individual, I want repeatable tracker setup so that adopting Beads has a reviewable
configuration and a documented path to a working task store.

**Depends on:** US-001.

**Acceptance Criteria:**

- [ ] Add `cookbook/src/beads/tracker.groovy` and its program model, with `--share=team|clone` defaulting to `team`.
  Support repeatable explicit `--instructions=<path>` targets for workflow guidance.
- [ ] Implement the [US-001 spike contract](../research/beads-adoption-spike.md#ownership-inputs-for-us-004)
  using declared file operations. Own `.beads/config.yaml` keys `dolt.local-only` (`false` for team, `true` for
  clone), `dolt.auto-push=false`, `no-git-ops=true`, `export.auto=false` and `export.git-add=false`; preserve
  native `sync.remote` and unrelated keys. Cooking works without `bd` installed and does not initialize
  databases, execute native setup, install tools or publish tasks.
- [ ] Own a distinct root `.gitignore` tracker section. Team rules are `/.beads/*` and `!/.beads/config.yaml`;
  clone uses `/.beads/`. Both modes include `.dolt/`, `*.db`, `.beads-credential-key`, `.beads/proxieddb/`
  and `*.gate.lock*`, including the root `.beads.gate.lock` observed in E3. Native `.beads/.gitignore` is
  redundant runtime/setup output, not the required ignore configuration.
- [ ] After-cooking setup uses `bd bootstrap --yes` and `bd dolt status` (E3–E5), preserving hooks, instructions
  and `.git/info/exclude`, including when both origin and upstream exist. Do not substitute `bd init` or
  `--stealth`: the measured init paths add unmanaged excludes or stage/commit source files (E1).
- [ ] First team publication names its destination and requires review of a clean index/worktree before
  `bd dolt push --yes`. Disclose that it publishes the complete task history, persists `sync.remote` and
  creates a source commit under the user's Git identity; source configuration then needs `git push`.
  A new code clone uses `bd bootstrap --yes`; code checkout alone has no task store (E2–E3).
- [ ] Clone instructions require the effective disabled-sync indication in `bd dolt status` before personal
  writes. Explain that bootstrap can read team history and adopt its prefix despite local-only, that
  `BD_DOLT_LOCAL_ONLY` and `config.local.yaml` override repository policy, and that push/pull skip with
  exit 0 while remote addition is refused (E4). Disclose that discarding the owned local-only key can re-enable
  a preserved native remote. No extra `BEADS_DIR` is needed for repository-linked worktrees.
- [ ] Disclose measured bootstrap warnings for absent native metadata, unset `beads.role`, and clone directory
  permissions. Do not invent a metadata seed or hide native diagnostics to make setup appear warning-free.
- [ ] Team mode shares the proven configuration needed for onboarding. Clone mode owns the root `.gitignore` rules
  that keep personal tracker files and exports local. Required setup introduces no unmanaged ignore rules.
- [ ] The engine alone maintains `cook.lock`; the recipe neither edits it nor introduces a second ownership ledger.
- [ ] Installation and native initialization/bootstrap instructions are complete for each mode and name their effects.
  Personal mode's effective sync policy is explicit; ignoring files is not used as proof that tasks cannot sync.
- [ ] Existing configuration follows Cook's existing-file behavior. Preserve unrelated keys and instruction text.
- [ ] Sharing-dependent instructions are owned or otherwise reconciled explicitly; changing mode cannot silently retain
  contradictory instructions in a seed. No mode change deletes task data or silently rewrites remote history.
- [ ] Ordinary users may create, claim and close their tasks. Runner-specific worker restrictions stay with ralph's
  prompts rather than becoming universal Beads policy.
- [ ] Add recipe tests in `cookbook/test/beads/`, covering both modes, recorded ownership, idempotence, existing content,
  argument validation, transitions, instructions and discard.

### US-005: Add independently owned Viewer setup

**Description:** As a tracker user, I want to opt into graph inspection and recommendations without coupling the
tracker's lifecycle to Viewer.

**Depends on:** US-002, US-004.

**Acceptance Criteria:**

- [ ] Add `cookbook/src/beads/viewer.groovy` with only the reusable contributions established by the spike.
- [ ] Support explicit instruction targets; any additional parameter names a demonstrated native choice or connection.
  The recipe neither automatically cooks `#tracker` nor rewrites tracker-owned settings or sections.
- [ ] Cooking Viewer works without its binary or a live database; its runtime prerequisites and install steps are stated.
- [ ] Instructions distinguish human TUI use from agent robot commands, explain export freshness, and leave task
  mutation and claiming with `bd`. Runtime failure behavior matches the pinned version's observed behavior.
- [ ] Existing Beads users can cook Viewer without first cooking Cook's tracker recipe, by supplying any required
  explicit connection. Both recipes together produce compatible output without taking ownership from each other.
- [ ] Tests prove cooking either recipe again leaves the other's contributions unchanged, and discarding Viewer removes
  its owned contributions while retaining tracker configuration, tasks and unrelated content.

### US-006: Verify adoption and removal end to end

**Description:** As an adopter, I want the documented setup and lifecycle exercised so that fresh clones, personal
worktrees and recipe changes behave as promised.

**Depends on:** US-003, US-004, US-005.

**Acceptance Criteria:**

- [ ] Repeat the successful spike scenarios with cooked output and the final after-cooking instructions, using an
  empty repository and a repository with existing Beads state and unrelated hooks/instructions.
- [ ] Exercise tracker alone and tracker plus Viewer in team and clone modes, including a linked worktree and a
  second team clone. Verify repeat cooking has no unexpected changes.
- [ ] Exercise both sharing transitions. Verify any required manual native changes are clearly reported, and that
  existing task IDs, dependencies and content survive. Previously tracked files are not assumed hidden by ignore rules.
- [ ] Discard Viewer, then tracker; inspect the generated ownership records and confirm all owned contributions are
  removed while runtime task data, team-owned seeds and unrelated content remain.
- [ ] Verify ralph uses the selected store in both sharing modes and still works with JSON without Beads installed.
- [ ] Commit a repeatable runtime verification procedure or harness with explicit binary prerequisites; normal unit
  checks do not install external tools or contact remotes. Record observed results in the spike report.

### US-007: Publish accurate recipe help and artifact references

**Description:** As a reader, I want complete help for each adoption case so that I can choose and compose recipes
without reading their implementation.

**Depends on:** US-006.

**Acceptance Criteria:**

- [ ] Both recipes follow the cookbook help standard, including exact supported tool versions, produces/consumes,
  connections, ownership, recooking, removal limits and executable after-cooking steps.
- [ ] Add the Beads artifact README and update cookbook examples, the `released` list in
  `cookbook/gradle/central.gradle`, and packaging assertions for the new artifact.
- [ ] Reconcile the current `:beads` reference in `docs/architecture.md` and relevant artifact lists, using the
  repository's docs map. Update `AGENTS.md` where its artifact description or named rules change.
- [ ] Clearly distinguish local task data from shared setup metadata, native task-history sync from code commits,
  and Viewer exports from authoritative tracker state.

## Functional Requirements

- FR-1: Tracker sharing is selected explicitly by `--share`, with `team` as the default and `clone` for personal data.
- FR-2: Required ignore configuration is declared as recipe-owned `.gitignore` content.
- FR-3: Ownership recording, update and discard use the existing Cook engine contract.
- FR-4: Viewer is optional and independently removable; tracker setup works without Viewer or ralph.
- FR-5: Recipe connections use named parameters and preserve unrelated content, following ADR 0030.
- FR-6: Cooking declares file changes; external tools perform initialization, sync and runtime task operations.
- FR-7: Supported after-cooking steps work in fresh clones and linked worktrees under the tested versions.
- FR-8: Sharing changes leave configuration and instructions consistent or give an actionable refusal.
- FR-9: Discard removes owned configuration and preserves task data and team-owned seeds.

## Non-Goals

- A new lock location, personal ownership ledger, engine command or alternate discard behavior.
- A task tracker, sync controller, package installer or task-data migration implemented inside Cook.
- Automatic deletion of historical manual exclusions, existing task data, remotes or user-managed hooks.
- Requiring Viewer, ralph, a particular agent harness or Beads Rust.
- Automated parallel-run guarantees, remote hosting provisioning or upgrades to upstream tools.
- Implementing or launching this PRD as part of writing it.

## Technical Considerations

Follow `docs/ux/when-cli-when-skill.md`, `docs/adrs/0030-recipe-connections.md`, and the existing-file and ownership
contracts. Model Beads and Viewer separately even when their recipes ship in one artifact. Their ignore and
instruction sections must have distinct ownership. Introduce viewer parameters only when the spike demonstrates
a connection or native configuration choice; do not expose every upstream option.

US-003 can proceed independently of the spike. US-004 depends on the tracker findings, US-005 on Viewer findings,
and US-006 verifies the resulting composition. If a finding changes an agreed contract, update the PRD with the
maintainer before dependent implementation; a failed experiment is evidence, not permission to weaken acceptance.

## Success Metrics

- Both adoption modes pass the runtime matrix with and without Viewer.
- A second team clone obtains the expected task graph; personal tasks stay outside the team sharing destination.
- Main checkout, linked worktree, Viewer and ralph resolve the intended task source.
- Both sharing transitions and independent recipe removal pass ownership and data-preservation checks.
- Required setup has no unmanaged ignore edits, and recipe help matches executed native steps.

## Open Questions

The spike must resolve the exact native initialization flags, config keys, sync settings, Viewer contributions
and freshness behavior under the pinned versions. These are explicit prerequisites to their implementation
stories. The two-recipe split is the proposed answer to optional Viewer selection; an empty or redundant Viewer
recipe is grounds to revisit that split with the maintainer.
[/PRD]
