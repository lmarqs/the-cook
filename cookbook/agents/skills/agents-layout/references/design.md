# Why the layout is shaped this way

Read this before changing the layout or arguing against one of its rules. Each decision states what it gains and what
it costs. Where a decision rests on how a harness behaves, the version that was tested is named: Claude Code 2.1.303
and Codex 0.160.1.

## Axes

Two runs in the same repository can differ in eight ways. The layout fixes the first two for everyone and leaves the
rest to each run.

| Axis | Examples | Decided | Lives in |
| --- | --- | --- | --- |
| Knowledge | Instructions, skills, slash commands, hook scripts, durable facts | By the team, in review | `AGENTS.md`, `.agents/skills/`, `commands/`, `hooks/`, `memory/` |
| Policy | What may run without asking, what asks, what never runs | By the team, in review | `.agents/permission/policy.yaml`, rendered to `.agents/adapters/` |
| Harness | Claude Code, Codex | Per person, or per role in an orchestrator | Which program is started |
| Mode | Attended, unattended | Per run | Launch flags |
| Role | A person at the keyboard, an unattended worker, a reviewer | Per run | Launch flags that add `.agents/adapters/claude/roles/<role>.json` |
| Model | Any the harness offers | Per person, or per role in an orchestrator | Personal settings, the orchestrator's config or `--model` |
| Environment | Sandbox, machine, network, credentials | Per machine, or by the organization | The harness's sandbox, managed settings, the machine |
| Person | Theme, default mode, standing approvals, personal notes | Each person | `~/.claude/`, `.claude/settings.local.json`, `~/.codex/` |

Committed: knowledge, the policy, and Cook's rendering of it. Chosen per run: everything else. The test for a new
setting is the same: if two people in the team could reasonably want different values, it is not shared.

## One AGENTS.md, and no notes for one harness

Both harnesses read the root `AGENTS.md` natively, so one file reaches every agent. Claude Code reads it from 2.1.277,
when no `CLAUDE.md` takes its place.

Notes for one harness would split the knowledge: what Claude is told, Codex is not. Codex has no file of its own to
hold them (it ignores `.codex/AGENTS.md`, tested), and Claude's `.claude/rules/` would put team files into a
directory that otherwise holds only links. A rule worth writing is worth giving to every agent; a rule that matters
only for some tasks fits a skill, which both harnesses load on demand.

`CLAUDE.md`, `.claude/CLAUDE.md` and `CLAUDE.local.md` are ignored, because any of them makes Claude skip `AGENTS.md`
(tested, including `CLAUDE.local.md`). `~/.claude/CLAUDE.md` does not, so it is where personal notes go. The cost: a
person cannot keep notes for this repository alone in Claude without hiding the team's.

## Links only where a harness reads a fixed path

| Harness | Link | Because |
| --- | --- | --- |
| Claude | `.claude/settings.json` | Claude reads project settings only there |
| Claude | `.claude/skills`, `.claude/commands` | Claude finds skills and commands only there; it does not read `.agents/skills` (tested) |
| Codex | `.codex/config.toml`, `.codex/rules`, `.codex/hooks.json` | Codex reads project configuration only there |

Codex reads `AGENTS.md` and `.agents/skills/` natively, so it needs no link for them. Hook scripts and memory files are
reached by their paths from the repository root, so neither harness needs a link for them. Each link is one more
thing to break, so there is no link a harness does not need.

## Links and adapters are committed

A fresh clone, a cloud session or a CI job works with no cooking: the links and the rendered settings are in the
repository. The review of a policy change shows exactly what each harness will enforce, because the rendered rules
are in the same diff.

The cost: generated files can drift from the policy and can conflict in a merge. Both are fixed the same way: resolve
`policy.yaml`, then cook `#connect` again. Windows checks links out as text files unless `core.symlinks` is on.

The rest of `.claude/` and `.codex/` stays ignored: `/.claude/*` plus one `!` line per link. The harnesses write
personal and per-machine files there, and ignoring the directory by default keeps them out of commits whatever their
names become.

## Seed files and the skill have different owners

`#seed` writes `AGENTS.md`, `policy.yaml` and `.agents/README.md` once, when absent, and never owns them: from then on
they are the team's, and Cook never overwrites them. The same recipe owns the `agents-layout` skill: Cook writes it
whole each time `#seed` is cooked, so the skill arrives with the layout it explains, and cooking a new release brings
its corrections. Edit a seeded file freely; an edit to this skill is undone by
the next cook, so propose it upstream instead.

## One policy file, rendered per harness

Writing permissions once for both harnesses means writing them in a form both can enforce. A prefix of literal words
is that form: Claude renders `Bash(<words> *)`, Codex a `prefix_rule`. Wildcards, quotes and shell syntax are refused,
because Codex cannot match them, and a rule that silently differs between harnesses is worse than one that is
refused.

Some rules need what only one harness can say, such as a flag after the branch. `{claude: "<pattern>"}` says it in
Claude's own syntax, and Codex's rules file lists it in a comment as not enforced, so the gap is visible where Codex's
rules are read. Tools other than the shell are named differently by each harness, so `tools` holds only these
harness entries.

## Why roles only deny

A role narrows `commands` for one kind of run; it never widens it. So no run, however it was started, gets more than a
person at the keyboard, and the policy's `commands` is the whole answer to "what can an agent here do at most".
Widening for one kind of run would make that answer depend on launch flags nobody reviews.

Claude merges a role's file over the project settings, and a deny there wins over a project allow (tested). The
role file holds only `permissions.deny`, so it cannot loosen anything else.

## Why Codex has no roles

A role needs a way for one run to add rules that another run does not get. Codex 0.160.1 has none:

- `CODEX_PERMISSION_PROFILE` is a variable Codex sets for the commands it runs, not one that selects a profile.
- `prefix_rules` in configuration exist only in `requirements.toml`, which an administrator controls.
- `--profile <name>` layers `$CODEX_HOME/<name>.config.toml`, a file in the person's home, not in the repository.
- No option names a rules file; Codex loads the project's `.codex/rules` whole, or skips them with `--ignore-rules`.

So Cook renders no Codex role, and a Codex run gets `commands` only. A team that runs Codex only as a worker can move
the worker's denies into `commands.deny`, at the cost of binding people at the keyboard too, or rely on Codex's
sandbox for workers.

## Why mode is not a policy field

Whether someone is watching is a property of one run, not of the repository: the same person runs attended in the
morning and leaves a worker running at night under the same policy. The policy says what asks; the mode decides what
happens to an ask: a person answers it, or an unattended run refuses it (`claude -p --permission-prompts none`,
`codex exec -c approval_policy=never`).

For the same reason the shared Claude settings hold no `defaultMode`: how a session asks is each person's choice, and
a shared value would override everyone's.

## Why the policy holds no model, and nobody has a personal policy

A model is chosen per person, or per role by an orchestrator, and changes more often than the policy. In shared
settings it would override every person's choice. Personal preferences already have a place in each harness, and the
harnesses merge them with the team's settings, with deny winning over ask and ask over allow. A personal policy in the
repository would duplicate that, and Cook would have to write files it does not own.

## Section markers name their region after a slash

A section Cook owns is marked `cook:begin <group>:<artifact>#<recipe>/<region>`, as in
`io.github.lmarqs.cookbook:agents#connect/harnesses`. `#` already separates the recipe from its artifact, so a second
`#` read as two recipes. The region is part of what the recipe writes, and `/` reads as that: a part inside it. A
recipe path can hold `/` too, so the region is what follows the last `/`.

## Parallel agents

Worktrees give each agent its own files, but some state is shared by every worktree of a clone: the stash, local
branches, an issue tracker kept in the clone, Docker volumes. A worker that stashes or deletes a branch reaches every
other worker. Such commands belong in a role's deny. Whether Claude reads `.claude/settings.local.json` from the main
checkout inside a worktree was not tested here.

## A guardrail, not enforcement

The policy records what the team agreed agents may do, and both harnesses apply it to the commands they run. It
cannot stop a command it does not recognize: `git -C sub push`, `env X=1 git push`, `bash -c '...'` and a script that
runs the command all start with other words. It cannot stop a person either: `--dangerously-skip-permissions`,
`--setting-sources user` and `--ignore-rules` skip it on one machine.

Containment comes from the harness's sandbox: Cook sets Codex to `workspace-write` with no network, and leaves Claude's
sandbox to each person. Rules every contribution must obey belong where nobody can skip them: the organization's
managed settings, branch protection and CI. Cook's own decision on this is [ADR 0028, Cook supports shared setup
without controlling how people
work](https://github.com/lmarqs/the-cook/blob/main/docs/adrs/0028-place-of-the-tool.md).
