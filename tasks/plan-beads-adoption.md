# Beads Adoption Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development task by task.

**Goal:** Implement the approved team/personal Beads adoption PRD with optional Viewer and managed ralph ignore advice.
**Architecture:** Independent tracker and Viewer recipes in one artifact. Native tools operate databases after cooking; Cook owns declared configuration.
**Tech Stack:** Groovy 4, Java 17, Gradle, JUnit 4; bd 1.3.1, bv 0.25.2, ralph-tui 0.12.0.
**Spec:** tasks/prd-beads-adoption.md

## Global Constraints

- The engine alone maintains cook.lock; no second ownership ledger.
- Required ignore configuration is recipe-owned .gitignore content.
- Preserve tasks, unrelated configuration and team-owned seeds.
- Cooking works without native binaries and executes no initialization or sync commands.
- Connections are explicit parameters (ADR 0030).
- Quick check for changed modules; Done check: ./gradlew check cliBuild jpackageTest; git diff --check.
- Stop and report a native incompatibility that prevents the approved contract; do not change pins or weaken checks.

## Review Focus

- Inherited sync settings: personal data must not reach team destinations (Task 1).
- Linked worktree discovery: no accidental second store (Tasks 1, 2).
- Export failure: stale Viewer input cannot masquerade as current (Task 2).
- Sharing transitions: owned instructions and ignores remain consistent (Task 4).
- Removal: preserve task data and unrelated hooks (Tasks 3, 5, 6).

### Task 1: Tracker runtime spike (US-001)

**Files:** Create research/beads-adoption-spike.md; refine US-004 in tasks/prd-beads-adoption.md.
**Interfaces:** Consumes research/beads-and-viewer.md and the full US-001 criteria in the spec. Produces exact paths, keys and native setup commands for tracker implementation.
- [ ] Read US-001 and its cited upstream evidence. Discover installed binaries without changing global setup.
- [ ] Execute every US-001 runtime case in disposable repositories with synthetic data. Record actual commands, versions, diffs and results.
- [ ] Write evidence and ownership table; update downstream US-004 criteria with proven interfaces.
- [ ] Run required checks; self-review and commit the evidence. If prerequisites or native behavior block proof, report evidence and stop dependent work.

### Task 2: Viewer and runner runtime spike (US-002)

**Files:** Extend research/beads-adoption-spike.md; refine US-005 in the spec.
**Interfaces:** Consumes Task 1 store/setup contract; produces Viewer paths, parameters, refresh and failure behavior.
- [ ] Execute US-002 exactly, including real trackers and fake worker.
- [ ] Record results and concrete Viewer contributions; update downstream acceptance criteria.
- [ ] Verify evidence, run checks and commit. Escalate an empty Viewer contribution rather than invent configuration.

### Task 3: Ralph ownership cleanup (US-003)

**Files:** cookbook/src/ralph/ralph-tui.groovy, cookbook/src/ralph/bin/run.sh, cookbook/test/ralph/RalphTuiCookTest.groovy, cookbook/README.md.
**Interfaces:** Existing --share=team|clone API remains unchanged; runtime diagnostics refer to recipe-owned setup.
- [ ] Read US-003. Add regression tests for replacement advice, argument preservation, transitions and discard; observe failure.
- [ ] Implement only US-003 and regenerate rendered help using repository conventions.
- [ ] Run focused tests, Quick and Done checks; inspect generated config and lock records; commit.

### Task 4: Tracker recipe (US-004)

**Files:** cookbook/src/beads/tracker.groovy and focused model/native resources; cookbook/test/beads/BeadsTrackerCookTest.groovy.
**Interfaces:** Use exact paths/keys/commands in US-004 as refined by Task 1; share=team|clone default team; repeatable instructions paths.
- [ ] Add behavior tests from refined US-004; observe failure before implementation.
- [ ] Implement declared file operations, ownership and help matching native findings; no runtime tool execution while cooking.
- [ ] Test both modes, preservation, transitions, idempotence and discard with generated lock inspection.
- [ ] Run Quick and Done checks; self-review and commit.

### Task 5: Viewer recipe (US-005)

**Files:** cookbook/src/beads/viewer.groovy and focused model/resources; cookbook/test/beads/BeadsViewerCookTest.groovy.
**Interfaces:** Use concrete US-005 contract from Task 2; independent owned sections from tracker.
- [ ] Add composition/removal/runtime-guidance tests and observe failure.
- [ ] Implement only proven reusable contributions; support native Beads users without requiring Cook tracker setup.
- [ ] Verify independent recook/discard and generated ownership; run Quick/Done checks and commit.

### Task 6: End-to-end acceptance (US-006)

**Files:** Runtime verification procedure/harness and research/beads-adoption-spike.md.
**Interfaces:** Final cooked output from Tasks 3–5.
- [ ] Execute the complete US-006 matrix with pinned tools and synthetic data, including existing state and hooks.
- [ ] Record reproducible commands and results; verify transitions, composition and preservation.
- [ ] Run checks and commit evidence/procedure; report real blockers without weakening acceptance.

### Task 7: Help and distribution (US-007)

**Files:** cookbook/src/beads/README.md, cookbook/README.md, cookbook/gradle/central.gradle and packaging tests; AGENTS.md and relevant docs references.
**Interfaces:** Tested final recipe API and supported native versions.
- [ ] Implement US-007 with existing docs conventions; add packaging regression evidence before changes.
- [ ] Verify all examples against runtime findings, artifact listing and ownership behavior.
- [ ] Run Quick and Done checks; self-review and commit.
