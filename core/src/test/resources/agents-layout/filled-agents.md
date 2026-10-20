# Kitchen

A sample repository that cooks the agents layout. See the README for what it does.

Stack: POSIX sh, pinned by nothing.

## Quick reference

- Install or add a dependency: none
- Run: `sh run.sh`
- Build: none
- Test one file: `sh test.sh <file>`
- Lint or format one file: `shellcheck <file>`
- Generate code: none
- Local services: none
- Worktrees: none
- Quick check, seconds on what changed: `sh test.sh`
- Done check, all that must pass before a change is done: `sh test.sh`, in seconds, with no services

## Boundaries

**Always**

- Run the Quick check before moving on. A change is done when the Done check passes.
- Report each check's result, or that it did not run.
- Update this file in the change that alters a command, path or rule it names.
- Keep this file under 800 words; past 1,200, move a section into a skill or memory file before adding. All
  `AGENTS.md` files together stay under 16 KiB.

**Ask first:** pushes to `origin/main` and writes outside this machine. Wait for `.work/services.lock` to clear
before stopping the services.

**Never**

- Commit secrets or credentials, or put personal data in logs, fixtures or commits.
- Make a check pass by weakening it: skipping a test, suppressing a warning, loosening an assertion.

**Stop and ask** when a check fails again after a fix, a command is refused, or a decision is left open.

Enforced by `.agents/permission/policy.yaml`. A refusal is the rule working.

## Where things go

- A procedure an agent repeats: a skill in `.agents/skills/`.
- A trap in one area: `.agents/memory/`, listed under Deep dive.

## Gotchas

- none

## Deep dive (read on demand)

- The layout: `.agents/skills/agents-layout/SKILL.md`
