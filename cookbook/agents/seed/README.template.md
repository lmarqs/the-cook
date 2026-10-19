# Shared agent layout

Every coding agent in this repository reads the same knowledge and the same policy. What differs between two people
or two runs is only what each one selects at launch: the harness, the model, whether someone is watching, and the
role. The `agents-layout` skill in `skills/` explains the layout in full: where a change goes, how to adopt it, how to
investigate an agent that misbehaves, and why it is shaped this way.

## What lives where

| Path | Holds | Decided by |
| --- | --- | --- |
| `/AGENTS.md` | Instructions for every agent, whatever its harness or role. Claude and Codex both read it natively | The team |
| `skills/`, `commands/`, `hooks/`, `memory/` | Shared skills, slash commands, hook scripts and durable facts | The team |
| `permission/policy.yaml` | What agents may run: `commands` and `tools` for every session, `roles` that narrow it | The team |
| `adapters/<harness>/` | The policy in each harness's own format; generated, never edited by hand | Cook |
| `adapters/claude/roles/` | One file of denies per role, added at launch | Cook |
| `/.claude/`, `/.codex/` links | Committed, so a fresh clone, a cloud session or CI reads the setup with no cooking | Cook |
| `~/.claude/`, `~/.codex/`, `/.claude/settings.local.json` | Personal preferences and standing approvals | Each person; Cook never writes them |

After editing `permission/policy.yaml`, cook the adapters again with every harness the team uses, using the version in
`cook.lock`:

```sh
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
```

## Ways of working

| Axis | Examples | Chosen | Lives in |
| --- | --- | --- | --- |
| Role | developer, worker, reviewer | Per run, at launch | `permission/policy.yaml` `roles`, rendered to `adapters/claude/roles/` |
| Mode | attended, unattended | Per run, at launch | Launch flags: unattended runs refuse what would ask |
| Harness | Claude, Codex | Per person or orchestrator | `adapters/<harness>/` |
| Model | Opus, Sonnet, GPT | Per person, or per role in an orchestrator | Personal settings or the orchestrator's config, never the policy |

## Launching

The developer role is the base, with no role file. Add `--model <model>` to any line. The roles below are examples:
a role exists when `permission/policy.yaml` defines it.

| Harness | Mode | Role | Command |
| --- | --- | --- | --- |
| Claude | attended | developer | `claude` |
| Claude | attended | reviewer | `claude --settings .agents/adapters/claude/roles/reviewer.json` |
| Claude | unattended | developer | `claude -p --permission-prompts none "<task>"` |
| Claude | unattended | worker | `claude -p --permission-prompts none --settings .agents/adapters/claude/roles/worker.json "<task>"` |
| Codex | attended | developer | `codex` |
| Codex | unattended | developer | `codex exec -c approval_policy=never "<task>"` |

Codex has no roles: it has no option that adds a rules file to one run, so every Codex run gets the policy's
`commands` alone. A role's denies reach only Claude runs.

Claude applies the project's allow rules only in a workspace someone has trusted, so start `claude` here once,
interactively, before relying on unattended runs. Codex loads `.codex/rules` only in a trusted project.

An orchestrator such as ralph-tui runs its coordinator attended in the developer role and starts each worker with the
unattended line of the harness it uses.

## Personal settings

Each person customizes in their harness's own files, never in this directory:

- `~/.claude/settings.json`: default mode, theme, model, across every project.
- `.claude/settings.local.json`: this project only. Claude writes "don't ask again" approvals here.
- `~/.codex/config.toml` and `~/.codex/rules/`: the same for Codex.

Claude reads `AGENTS.md` only while no `CLAUDE.md`, `.claude/CLAUDE.md` or `CLAUDE.local.md` exists here or in a
directory above; any of them makes it skip `AGENTS.md`. `.gitignore` keeps them out of commits, so none reaches the
team, but a local one still takes effect for whoever has it. Keep personal notes in `~/.claude/CLAUDE.md`, which does
not count. Reading `AGENTS.md` needs Claude Code 2.1.277 or later.

This directory sets what the team shares, not what a person may do. A personal `allow` does not outrank a team `ask`
or `deny`, but anyone can still override the shared setup on their machine: `claude --setting-sources user` skips the
project's settings, `claude --dangerously-skip-permissions` skips every check, and `codex exec --ignore-rules` skips
the rules. Enforcing a rule on everyone takes the organization's managed settings, outside the repository.
