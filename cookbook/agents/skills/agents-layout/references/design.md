# Why the layout is shaped this way

Read this before changing the layout or arguing against one of its rules. Each decision states what it gains and what
it costs. Tested with Claude Code 2.1.303 or 2.1.304 (2.1.304 for skill arguments, worktrees, bypass flags and
`--setting-sources`) and Codex 0.160.1.

## Axes

Two runs in the same repository can differ in eight ways. The layout fixes the first two for everyone and leaves the
rest to each run.

| Axis | Examples | Decided | Lives in |
| --- | --- | --- | --- |
| Knowledge | Instructions, skills, slash commands, hook scripts, durable facts | By the team, in review | `AGENTS.md`, `.agents/skills/`, `commands/`, `hooks/`, `memory/` |
| Policy | What may run without asking, what asks, what never runs | By the team, in review | `.agents/permission/policy.yaml`, rendered to `.agents/adapters/` |
| Harness | Claude Code, Codex | Per person, or per role in a loop runner | Which program is started |
| Mode | Attended, unattended | Per run | Launch flags |
| Role | A person at the keyboard, an unattended worker, a reviewer | Per run | Launch flags that add `.agents/adapters/claude/roles/<role>.json` |
| Model | Any the harness offers | Per person, or per role in a loop runner | Personal settings, the runner's configuration or `--model` |
| Environment | Sandbox, machine, network, credentials | Per machine, or by the organization | The harness's sandbox, managed settings, the machine |
| Person | Theme, default mode, standing approvals, personal notes | Each person | `.claude/settings.local.json` |

Committed: knowledge, the policy, and Cook's rendering of it. Chosen per run: everything else. The test for a new
setting is the same: if two people in the team could reasonably want different values, it is not shared.

## One AGENTS.md, and no notes for one harness

Both harnesses read the root `AGENTS.md` natively, so one file reaches every agent. Claude Code reads it from 2.1.277,
when no `CLAUDE.md` takes its place.

Notes for one harness would split the knowledge: what one harness is told, another is not. Codex has no file of its
own to hold them (it ignores `.codex/AGENTS.md`, tested), and Claude's `.claude/rules/` would put team files into a
directory that otherwise holds only links. A rule worth writing is worth giving to every agent; a rule that matters
only for some tasks fits a skill, which both harnesses load on demand.

`CLAUDE.md`, `.claude/CLAUDE.md` and `CLAUDE.local.md` are ignored, because any of them makes Claude skip `AGENTS.md`
(tested, including `CLAUDE.local.md`). The cost: a person cannot keep notes for this repository alone in Claude
without hiding the team's.

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

Ignoring `.claude/` and `.codex/` whole, and cooking the links in each checkout, would move six links out of Git and
nothing else: Claude Code and Codex read only their own directories, so the links must exist, and the adapters stay
committed either way. Every clone, worktree, loop runner's worker, cloud session and CI job would then need Cook and a
setup step before its first session. One that skips it runs the harness with none of the team's permissions, hooks
or skills, and with Claude's bypass mode allowed, and nothing warns. `cook.lock` would record links that a clean
checkout lacks, so `cook --check` fails in CI.

The cost: generated files can drift from the policy and can conflict in a merge. Both are fixed the same way: resolve
`policy.yaml`, then cook `#connect` again. Windows checks links out as text files unless `core.symlinks` is on.

The rest of `.claude/` and `.codex/` stays ignored: `/.claude/*` plus one `!` line per link. The harnesses write
personal and per-machine files there, and ignoring the directory by default keeps them out of commits whatever their
names become. The section opens each harness with `!/.claude/`, because git cannot re-include a file whose parent
directory a rule above excludes, such as a team's own `.claude/`. A rule below the section or in a global excludes
file still wins; [investigate.md](investigate.md) shows how to find it.

## Seed files and the skill have different owners

`#seed` writes `AGENTS.md`, `policy.yaml` and `.agents/README.md` once, when absent, and never owns them: from then on
they are the team's, and Cook never overwrites them. The same recipe owns the `agents-layout` skill: Cook writes it
whole each time `#seed` is cooked, so the skill arrives with the layout it explains, and cooking a new release brings
its corrections. Edit a seeded file freely; an edit to this skill is undone by
the next cook, so propose it upstream instead.

## Why a skill and not a plugin

The layout's procedures, `adopt` and `review`, ship as the `agents-layout` skill rather than as a plugin. Codex reads
`.agents/skills/` natively and Claude through the `.claude/skills` link, so one committed copy reaches both, and both
invoke it by name and pass the words after the name as text (tested). It needs no install per person: every clone
carries it, and `cook.lock` records the release that wrote it. A harness's plugin is installed by each person, and
neither harness reads the other's, so the same procedure would need two packages and an install step any teammate could
skip.

## One policy file, rendered per harness

Writing permissions once for both harnesses means writing them in a form both can enforce. A prefix of literal words
is that form: Claude renders `Bash(<words> *)`, Codex a `prefix_rule`. Wildcards, quotes and shell syntax are refused,
because Codex cannot match them, and a rule that silently differs between harnesses is worse than one that is
refused.

Some rules need what only one harness can say, such as a flag after the branch. `{claude: "<pattern>"}` says it in
Claude's own syntax, and Codex's rules file lists it in a comment as not enforced, so the gap is visible where Codex's
rules are read. Tools other than the shell are named differently by each harness, so `tools` holds only these
harness entries.

## Cook owns only the keys it renders

The policy covers permissions and nothing else, so Cook owns only what it renders from it: `permissions` for Claude,
the sandbox and approval keys and `cook.rules` for Codex. The one other key it owns is the session-start hook,
`hooks.SessionStart` in both harnesses, which runs the dispatcher `.agents/hooks/session-start` (see
[the next section](#tools-that-add-session-context)). The other hook events, `env`, MCP servers and every other key
stay in the harness's own file, in its own format, and cooking keeps them. A shared format for those would need a
translation per harness for no gain, and an input file per harness would only move the same keys one step further
from where the harness reads them.

## Tools that add session context

Tools such as an issue tracker want to tell every agent something as its session starts. Each harness takes that
context from a session-start hook, and each keeps those hooks in one key: Claude in `hooks.SessionStart` of its
settings, Codex in `hooks.SessionStart` of `hooks.json`. Cook owns a key whole, so if every tool's recipe wrote that
key, the last one cooked would remove the others, and each tool would need to know both harnesses' formats.

So `#connect` owns the key in each harness and points it at one dispatcher, `.agents/hooks/session-start`. The
dispatcher runs every `.agents/hooks/session-start.d/*.sh` in name order, with the harness name as `$1`, and prints
their joined output once, as `{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"<text>"}}`,
which Claude Code 2.1.306 and Codex 0.160.1 both add to the session (tested). A tool's recipe writes one script there
when the person names its path by a parameter of that recipe, never because it finds the directory, and the team
writes its own beside them; none of them learns a harness format, and none collides with another. The
dispatcher encodes the JSON with `sh` and `awk` alone, so the layout adds no dependency such as `jq` or Python; the
encoding round-trips quotes, backslashes, control characters and UTF-8 under dash and bash, with gawk and mawk
(tested).

Codex runs a relative hook command from the session's directory, so a session started in a subdirectory fails to find
`.agents/hooks/session-start` (tested). Its command starts from `git rev-parse --show-toplevel` instead. Claude's
command starts from `$CLAUDE_PROJECT_DIR`.

Codex trusts hooks per absolute path of `.codex/hooks.json`, recorded in `~/.codex/config.toml`, not per clone. A
repository at a new path runs none of its hooks until they are trusted there, and `codex exec` skips them without a
warning; `--dangerously-bypass-hook-trust` runs them for one run (tested in a new repository). A worktree has a path
of its own, so the same holds there. Claude runs the project's hooks even in a workspace nobody trusted (tested with
`claude --print`).

## Why roles only deny

A role narrows `commands` for one kind of run; it never widens it. So no run, however it was started, gets more than a
person at the keyboard, and the policy's `commands` is the whole answer to "what can an agent here do at most".
Widening for one kind of run would make that answer depend on launch flags nobody reviews.

Claude merges a role's file over the project settings, and a deny there wins over a project allow (tested). The
role file holds only `permissions.deny`, so it cannot loosen anything else.

## Why Codex has no roles

A role needs a way for one run to add rules that another run does not get. Codex, in the version above, has none:

- `CODEX_PERMISSION_PROFILE` is a variable Codex sets for the commands it runs, not one that selects a profile.
- `prefix_rules` in configuration exist only in `requirements.toml`, which an administrator controls.
- `--profile <name>` layers a profile from the person's own configuration, not from the repository.
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

A model is chosen per person, or per role by a loop runner, and changes more often than the policy. In shared
settings it would override every person's choice. Personal preferences already have a place in each harness, and the
harnesses merge them with the team's settings, with deny winning over ask and ask over allow. A personal policy in the
repository would duplicate that, and Cook would have to write files it does not own.

## Section markers name their region after a slash

A section Cook owns is marked `cook:begin <group>:<artifact>#<recipe>/<region>`, as in
`io.github.lmarqs.cookbook:agents#connect/harnesses`. `#` already separates the recipe from its artifact, so a second
`#` read as two recipes. The region is part of what the recipe writes, and `/` reads as that: a part inside it. A
recipe path can hold `/` too, so the region is what follows the last `/`.

## Worktrees and loop runners

A loop runner runs agents over a series of tasks, often several at once, each in its own git worktree. The layout
names no runner and configures none. It keeps a contract that any runner, script or person can rely on, and `#connect`
keeps these paths stable:

| What | Where |
| --- | --- |
| Instructions | The root `AGENTS.md`, which both harnesses read natively |
| Skills and commands | The `.claude/skills` and `.claude/commands` links for Claude; `.agents/skills/` for Codex |
| The policy, for every run | The `.claude/settings.json` link for Claude; the `.codex/config.toml` and `.codex/rules` links for Codex |
| A Claude role | `--settings .agents/adapters/claude/roles/<role>.json`, for each role `policy.yaml` defines |
| A Codex role | None: every Codex run gets `commands` |
| An unattended run | `claude -p --permission-prompts none`, adding a role's `--settings`; `codex exec -c approval_policy=never` |

The runner decides where its worktrees go, what it copies or sets up in each, how branches merge, how many run at
once and which model each uses. The project decides how a worktree becomes runnable and how one is removed leaving
nothing behind, in the Worktrees line of the Quick reference in `AGENTS.md`.

Every file of the layout is committed and every link is relative, so a worktree carries the whole setup: the links
resolve inside it and git reports it clean, with nothing to cook or copy (tested). Trust and personal settings follow
the clone, not the directory:

- Claude Code keys trust on the main checkout. In an untrusted worktree its warning names
  `projects["<main checkout>"]`, and in a worktree of a trusted clone it reads the main checkout's
  `.claude/settings.local.json` (tested, with a deny, in a worktree that had no local file of its own).
- Codex applies `.codex/rules` in a worktree inside the trusted checkout and in one beside it, and skips them
  in a plain clone nobody trusted (tested).
- Codex hooks are the exception: Codex trusts them per absolute path of `.codex/hooks.json`, so a worktree runs none
  of them, the session start included, without `--dangerously-bypass-hook-trust`
  ([why](#tools-that-add-session-context)).

So trusting the main checkout once covers every worktree of it, wherever the runner puts it, except for Codex hooks;
a fresh clone needs its own trust.

Where worktrees go is left to the project and its runner, because each location has a cost:

| Location | What follows (Claude Code, tested) |
| --- | --- |
| Inside the main checkout | The main checkout's `git status` lists the directory until the project ignores it. Claude reads the worktree's own `AGENTS.md`, not the main checkout's `AGENTS.md` or `CLAUDE.md`. A `CLAUDE.local.md` in the main checkout, or in any directory between it and the worktree, reaches the worktree and turns its `AGENTS.md` off |
| Beside the repository or elsewhere | Nothing in the main checkout's directory reaches it; trust and Claude's local settings still follow the main checkout |

Some state is shared by every worktree of a clone: git keeps one set of branches and one stash per clone, and an
issue tracker kept in the clone, Docker volumes and ports are shared too. A worker that stashes or deletes a branch
reaches every other worker, so such commands belong in a role's deny. So does `git worktree` itself: a runner or a
person creates and removes worktrees, not the worker inside one.

How common runners place worktrees and launch agents, read from each one's docs or source on 2026-10-20 and not
tested here. "Not verified" marks what the sources did not show:

| Runner | Worktrees | Copies or setup | Launches by default | Launch configurable through | Source |
| --- | --- | --- | --- | --- | --- |
| Claude Code `--worktree` | `.claude/worktrees/<name>/`; a `WorktreeCreate` hook replaces it, and `#connect` with a `--worktree-*` param writes one that uses `<root>/<branch>`, `.worktrees/<name>` by default | `.worktreeinclude`; with that hook, the `--worktree-setup` executable | Itself | Not applicable | [docs](https://code.claude.com/docs/en/worktrees) |
| Codex `--worktree` | `$CODEX_HOME/worktrees` | `.worktreeinclude` in the app; setup scripts, key not verified | Itself | Not applicable | [docs](https://learn.chatgpt.com/docs/environments/git-worktrees) |
| ralph-tui 0.12 | `../.ralph-worktrees/<project>/` in its code, `.ralph-tui/worktrees` in its docs; `[parallel].worktreeDir` | Its own configuration only | Claude with `--dangerously-skip-permissions`; `codex exec --full-auto`, an option `codex exec` rejects (tested) | `command`, `defaultFlags`, `skipPermissions`, `fullAuto` | [docs](https://ralph-tui.com/docs/parallel/configuration) |
| Conductor | `~/conductor/workspaces/<repo>/<workspace>` | `.worktreeinclude` or `file_include_globs`; `[scripts] setup` | Not verified | Not verified | [docs](https://conductor.build/docs/concepts/git-worktrees) |
| claude-squad | `~/.claude-squad/worktrees/` | None | `claude` in tmux; `--autoyes` answers prompts | `-p <program>` | [source](https://github.com/smtg-ai/claude-squad) |
| Gastown | `~/gt/<rig>/polecats/<name>/rig/` | `.runtime/setup-hooks/*` | Claude with `--dangerously-skip-permissions`; Codex with `--dangerously-bypass-approvals-and-sandbox` | `gt config agent set` | [source](https://github.com/gastownhall/gastown) |
| Scion | `.scion/agents/<name>/workspace`, in a container | `provision.py` | Claude with `--dangerously-skip-permissions`; Codex with `danger-full-access` and the bypass flag | Harness bundles | [source](https://github.com/GoogleCloudPlatform/scion) |
| Orca | `~/orca/workspaces/<repo>/<name>`, or `workspaceDir` | `.worktreeinclude`, literal paths; `worktree.sharedDirectories` | Bypass flags; "Manual" clears them | An arguments field | [source](https://github.com/stablyai/orca) |
| paperclip | `.paperclip/worktrees`, or `worktreeParentDir` | `provisionCommand` | Skip permissions and bypass, on by default | Strategy options | [source](https://github.com/paperclipai/paperclip) |
| vibe-kanban | `vibe-kanban/worktrees` in the system's temporary directory | `copy_files`, `setup_script` | `dangerously_skip_permissions: true`; Codex with `danger-full-access` | Profiles | [source](https://github.com/BloopAI/vibe-kanban) |
| Crystal | `<repo>/worktrees` | None found | Claude with `--dangerously-skip-permissions` | A mode per project | [source](https://github.com/stravu/crystal) |
| Symphony | None: one clone per issue under `<tmp>/symphony_workspaces` | `hooks.after_create` and others | `codex app-server`, rejecting approvals by default | Its configuration | [source](https://github.com/openai/symphony) |
| ForgeCode | `../<name>` | None | Itself | Not applicable | [source](https://github.com/tailcallhq/forgecode) |
| Cursor | Not verified | `.cursor/worktrees.json`, `setup-worktree` | Not verified | Not verified | [docs](https://cursor.com/docs/configuration/worktrees) |

Most of them launch with flags that skip a harness's asks, and nearly all let the command be changed. Point the
command at the unattended line of the contract; [Why bypass is refused by default](#why-bypass-is-refused-by-default)
says what each harness does when a runner keeps its bypass flags.

## Why bypass is refused by default

A harness's bypass mode runs every command that is not denied, without asking. Most loop runners start agents in it
by default, so a policy that only asks would mean nothing to their workers. While `policy.yaml` says `bypass: refuse`,
or says nothing, `#connect` writes `permissions.disableBypassPermissionsMode: "disable"` into the shared Claude
settings. Claude Code, tested in an untrusted workspace, and the first row in a trusted one too:

| Run | Without the key | With it |
| --- | --- | --- |
| `claude -p --dangerously-skip-permissions`, a command the policy does not allow | Runs | Refused: it needs approval, and nobody can give it |
| `--permission-mode bypassPermissions`, or `=bypassPermissions` | Runs | Refused |
| A command the policy denies, under `--dangerously-skip-permissions` | Refused | Refused |
| `--setting-sources user --dangerously-skip-permissions` | Runs | Runs: the project's settings, the key included, are skipped |

The key works from a `--settings` layer too (tested), and Claude's docs say it works from any scope
([permissions](https://code.claude.com/docs/en/permissions)).

It is a policy field, not a fixed rule, because teams differ: a team that runs every agent in a container may want
its workers in bypass mode, and `bypass: allow` leaves the key out. The choice is made once, in review, like the rest
of the policy.

Codex has no such setting, and needs one less: its rules hold under `--dangerously-bypass-approvals-and-sandbox`, which
refuses a `prompt` rule instead of running it (tested). What does step outside the rules is listed in
`cook.rules` itself, in a comment, while `bypass` is `refuse`: `--ignore-rules`, an untrusted project and
`--approve-for-me`.

There is no launcher, a script that would start each harness with the right flags. Claude already refuses bypass
from its settings, whatever starts it. Codex keeps its rules under its bypass flag, and has no roles for a launcher to
select. A launcher would reach only the runners already configured to call it, and those can as well be configured with
the unattended line of the contract.

## A guardrail, not enforcement

The policy records what the team agreed agents may do, and both harnesses apply it to the commands they run. These
step outside it:

| Route | Effect | How it is known |
| --- | --- | --- |
| `claude --dangerously-skip-permissions`, `--permission-mode bypassPermissions` | Refused while `bypass` is `refuse`; with `allow`, runs every command not denied | Tested |
| `claude --setting-sources user` | Skips the project's settings: the policy and the bypass refusal | Tested |
| `codex exec --ignore-rules` | Skips `cook.rules` | Tested |
| An untrusted Codex project, including a run with `--ignore-user-config` | Skips `.codex/`, so the rules and the sandbox settings | Tested |
| `codex exec --approve-for-me` | An automatic reviewer approves `prompt` rules; `forbidden` still holds | Tested |
| `codex exec --dangerously-bypass-approvals-and-sandbox`, `--sandbox danger-full-access` | The rules hold; the sandbox is gone | Rules tested; sandbox from `codex exec --help` |
| `codex exec -c <key>=<value>` | Sets any configuration key for one run, the sandbox and approval keys included | `codex exec --help`; not tested |
| `git -C sub push`, `env X=1 git push`, `bash -c '...'`, a script that runs the command | Starts with other words, so no prefix matches | How prefixes match ([policy.md](policy.md#prefix)) |
| A loop runner's default command | Most pass the bypass flags above | The runner table above, from docs and source |

Containment comes from the harness's sandbox: Cook sets Codex to `workspace-write` with no network, and leaves Claude's
sandbox to each person. Rules every contribution must obey belong where nobody can skip them: the organization's
managed settings, branch protection and CI. Cook's own decision on this is [ADR 0028, Cook supports shared setup
without controlling how people
work](https://github.com/lmarqs/the-cook/blob/main/docs/adrs/0028-place-of-the-tool.md).
