---
name: agents-layout
description: Explains the shared .agents/ layout that lets several people run AI Agents in one repository, each in their own way. Use it to adopt the layout, change agent instructions, permissions, roles, skills or commands, investigate an agent that ignores the setup or runs what the team did not expect, or understand why the layout is shaped as it is. Not for the repository's own code, and not for a person's own harness settings outside this repository.
---

# Agents layout

One repository, many ways of working: one person runs Claude at the keyboard, another leaves Codex working alone,
a third has Claude coordinate Codex workers through an orchestrator. The layout gives all of them the same knowledge
and the same agreed permissions, and leaves each run free to pick its harness, model, mode and role.

## Three owners

Every file here has one owner. Most surprises come from editing a file someone else owns.

| Owner | Files | Changed by |
| --- | --- | --- |
| The team | `AGENTS.md`, `.agents/permission/policy.yaml`, `.agents/skills/`, `commands/`, `hooks/`, `memory/`, `.agents/README.md` | Editing and committing, through review |
| Cook | `.agents/skills/agents-layout/` | Cooking `io.github.lmarqs.cookbook:agents#seed` with a new release |
| Cook | `.agents/adapters/`, the links in `.claude/` and `.codex/`, the `harnesses` section of `.gitignore` | Cooking `io.github.lmarqs.cookbook:agents#connect` after a team file changes |
| Each person | `~/.claude/`, `.claude/settings.local.json`, `~/.codex/` | Nobody else; Cook never writes them and git ignores them |

```text
AGENTS.md                         instructions for every agent; Claude and Codex read it directly
.agents/
  README.md                       what lives where, and the command line for each kind of run
  permission/policy.yaml          what agents may run; the one file to edit for permissions
  adapters/<harness>/             the policy in each harness's own format; generated
  skills/ commands/ hooks/ memory/
.claude/  .codex/                 committed links into .agents/, plus each person's local files
```

Cook's `io.github.lmarqs.cookbook:agents` recipes write it: `#seed` creates it, this skill included, and `#connect`
renders the policy and makes the links. Commands here write the recipe version as `<version>`; use the one in `cook.lock`.

The layout cannot decide how people work; anyone can override it on their own machine. It guides: shared changes
reach every agent when they go where every agent reads them, and personal ones stay out of the team's files.

## Shared directories

Everything the team shares with agents, apart from `AGENTS.md` and the policy, goes in one of four directories under
`.agents/`. Each harness reaches them in its own way; nothing needs copying per harness.

| Directory | Holds | Claude reaches it | Codex reaches it |
| --- | --- | --- | --- |
| `skills/` | One directory per skill: `SKILL.md`, whose frontmatter `name` and `description` say when to use it, and the files it refers to | Through the `.claude/skills` link | Natively |
| `commands/` | One Markdown file per slash command; the file name is the command | Through the `.claude/commands` link | Not read; turn a command both need into a skill |
| `hooks/` | Scripts that hook settings run, by their path from the repository root | Its `hooks` settings, cooked with `#claude-settings` | Its `hooks.json`, cooked with `#codex-settings` |
| `memory/` | Durable facts about the repository that the code and `README.md` do not show; never secrets or personal data | When `AGENTS.md` points to a file | When `AGENTS.md` points to a file |

- **A skill** is for knowledge an agent needs only for some tasks: it loads when its description matches, so it costs
  nothing otherwise. A rule every task needs belongs in `AGENTS.md`.
- **A command** is a prompt a person starts by name, such as `/release`. Keep it short and let it name the skill
  that holds the detail.
- **A hook** runs on a harness event, such as before a command or after an edit, with no agent deciding. The script
  is shared; when it runs is set per harness, because the events and their input differ.
- **Memory** holds what a past session learned and every later one needs, such as why a dependency is pinned. List
  each file in `AGENTS.md` with the question it answers, so agents know when to read it.

## Pick the task

| Task | Read |
| --- | --- |
| Set the layout up, or move an existing `CLAUDE.md`, `.claude/` or `.codex/` setup into it | [references/adopt.md](references/adopt.md) |
| Fill in or review `AGENTS.md` | [references/agents-md.md](references/agents-md.md) |
| Write or review `policy.yaml` | [references/policy.md](references/policy.md) |
| An agent ignores the instructions, asks when it should not, or runs what it should not | [references/investigate.md](references/investigate.md) |
| Understand why the layout is shaped this way, or argue for changing it | [references/design.md](references/design.md) |

## Where a change goes

| To | Edit | Then |
| --- | --- | --- |
| Tell every agent something | `AGENTS.md` | Commit |
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

The diff adds `Bash(./gradlew test *)` to `.agents/adapters/claude/settings.json` and a `prefix_rule` to
`.agents/adapters/codex/rules/cook.rules`, and `cook.lock` records them. The first command entry also creates that
rules file, its `.codex/rules` link and the `.gitignore` line that commits the link.

## Guidance

- **Edit the policy, not what is rendered from it.** `.agents/adapters/` and the links come from `policy.yaml`. A hand
  edit reaches one harness only, and the next cook proposes undoing it.
- **Keep `CLAUDE.md`, `.claude/CLAUDE.md` and `CLAUDE.local.md` out of the repository.** While any of them exists here
  or in a directory above, Claude stops reading `AGENTS.md`. `.gitignore` keeps them out of commits; a local one still
  affects whoever has it. Personal notes fit `~/.claude/CLAUDE.md`, which does not count.
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
