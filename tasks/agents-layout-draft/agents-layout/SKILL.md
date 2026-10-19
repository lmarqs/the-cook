---
name: agents-layout
description: Explains the shared .agents/ layout that lets several people run Claude Code and Codex in one repository, each in their own way. Use it to adopt the layout, change agent instructions, permissions, roles, skills, commands, hooks or memory, investigate an agent that ignores the setup or does something the team did not expect, or understand why the layout is shaped as it is. Not for the repository's own code.
---

# Agents layout

One repository, many ways of working: one person runs Claude at the keyboard, another leaves Codex working alone,
a third has Claude coordinate Codex workers through an orchestrator. The layout gives all of them the same knowledge
and the same agreed permissions, and leaves each run free to pick its harness, model, mode and role.

```text
AGENTS.md                         instructions for every agent; Claude and Codex read it directly
.agents/
  README.md                       what lives where, and the command line for each kind of run
  permission/policy.yaml          what agents may run; the one file to edit for permissions
  adapters/<harness>/             the policy in each harness's own format; generated
  skills/ commands/ hooks/ memory/
.claude/  .codex/                 committed links into .agents/, plus each person's local files
```

Cook's `io.github.lmarqs.cookbook:agents` recipes write it: `#seed` creates it and `#connect` renders the policy and makes the
links. Commands here write the recipe version as `<version>`; use the one in `cook.lock`.

The layout cannot decide how people work; anyone can override it on their own machine. It guides: shared changes
reach every agent when they go where every agent reads them, and personal ones stay out of the team's files.

## Pick the task

| Task | Read |
| --- | --- |
| Set the layout up, or move an existing `CLAUDE.md`, `.claude/` or `.codex/` setup into it | [references/adopt.md](references/adopt.md) |
| Write or review `policy.yaml` | [references/policy.md](references/policy.md) |
| An agent ignores the instructions, asks when it should not, or runs what it should not | [references/investigate.md](references/investigate.md) |
| Understand why the layout is shaped this way, or argue for changing it | [references/design.md](references/design.md) |

## Where a change goes

| To | Edit | Then |
| --- | --- | --- |
| Tell every agent something | `AGENTS.md` | Commit |
| Tell only Claude something | A file in `.claude/rules/` | Commit |
| Allow, ask before or deny a command in every session | `commands` in `.agents/permission/policy.yaml` | Cook `#connect`, review, commit |
| Deny a command in one kind of run, such as unattended workers | `roles.<role>.deny` in the same file | Cook `#connect`, review, commit |
| Share a skill, slash command, hook script or durable fact | `.agents/skills/`, `commands/`, `hooks/` or `memory/` | Commit; the links expose it |
| Change something for yourself | `~/.claude/`, `.claude/settings.local.json` or `~/.codex/` | Nothing to commit |
| Choose the harness, model, mode or role of one run | The launch command, from Launching in `.agents/README.md` | Nothing to commit |

For example, to let every agent run the test suite:

```yaml
# .agents/permission/policy.yaml
commands:
  allow:
    - [./gradlew, test]
```

```sh
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
git add .agents cook.lock && git commit
```

The diff shows `Bash(./gradlew test *)` added to `.agents/adapters/claude/settings.json` and a `prefix_rule` added to
`.agents/adapters/codex/rules/cook.rules`, and nothing else.

## Guidance

- **Edit the policy, not what is rendered from it.** `.agents/adapters/` and the links come from `policy.yaml`. A hand
  edit reaches one harness only, and the next cook proposes undoing it.
- **Keep `CLAUDE.md`, `.claude/CLAUDE.md` and `CLAUDE.local.md` out of the repository.** While any of them exists here
  or in a directory above, Claude stops reading `AGENTS.md`. `.gitignore` keeps them out of commits; a local one still
  affects whoever has it. Claude-only notes fit `.claude/rules/`, personal notes `~/.claude/CLAUDE.md`.
- **Widen in `commands`, narrow in roles.** A role only adds denies, so a worker never gets more than a person at the
  keyboard. When a role needs something `commands` lacks, that is a change for everyone; agree it with the team.
- **Use `ask` sparingly.** Nobody answers in an unattended run, so an `ask` refuses there, and every unattended run that
  reaches it stops.
- **Leave models out of shared files.** A model is a person's or an orchestrator's choice; a model in the shared
  settings overrides everyone's.
- **Cook every harness the team uses.** `#connect` removes what it wrote for harnesses left out, and those files are
  committed, so cooking only your own harness removes a teammate's.
- **Keep secrets out of `.agents/`.** Everything there is committed and read by every agent.
- **Treat the policy as an agreement, not a lock.** `claude --dangerously-skip-permissions` or `codex exec
  --ignore-rules` skips it on one machine. A rule every machine must obey belongs in the organization's managed
  settings.
