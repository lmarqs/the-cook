@help('''
Sets a repository up for a Beads tracker around what `bd init` writes: the ignore rules for the issues that stay out
of commits, the prime text `bd prime` prints, and, when a harness is named or the agents layout is in the draft, the
session-start hook that gives each agent session that text, so `bd init` is the only step left by hand.

## API

```sh
# Issues kept in each clone, with no remote
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=clone
# Issues the team shares through a Dolt remote
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=team
# Without the agents layout: Claude Code and Codex run bd prime from their own hook files
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=clone --harness=claude --harness=codex
# With the agents layout: leave out --harness, and the layout's session-start hook runs bd prime
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=clone io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude
```

Paths other recipes, runners and people rely on:

| Path | What it is |
| --- | --- |
| `.beads/PRIME.md` | The team's workflow text, which `bd prime` prints in place of bd's own |
| `.agents/hooks/session-start.d/beads.sh` | The layout's session-start script for Beads, when the layout is in the draft |

## Consumes

- `bd` at run time, written for bd 1.3.1; Claude Code 2.1.306 and Codex 0.160.1 run the hooks.
- `.agents/hooks/.gitkeep` or `.agents/hooks/session-start` in the draft: either one shows the agents layout, which
  adds the session-start script and refuses `--harness`.
- `.claude/settings.json` and `.codex/hooks.json` in the draft, with `--harness`: their other keys stay as they are,
  and a symbolic link there is refused, since Cook writes keys only into regular files.
- By hand first: nothing. By hand after: `bd init`, as After cooking says.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.gitignore` | The section `beads`: the argument line, then `/.beads/*` and `!/.beads/PRIME.md` | `--share=clone` | owned |
| `.gitignore` | The section `beads`: the argument line, then `/.beads/issues.jsonl`, the export that carries whatever people paste into issues; bd's own `.beads/.gitignore` covers its other files | `--share=team` | owned |
| `.beads/PRIME.md` | The workflow and rules for that share, including that a worker never closes its bead or pushes | Always, when absent | seed |
| `.claude/settings.json` | The key `hooks.SessionStart`: one command hook running `bd prime` | `--harness=claude` | owned |
| `.codex/hooks.json` | The key `hooks.SessionStart`: one entry matching `startup\\|resume\\|clear` running `bd codex-hook SessionStart` | `--harness=codex` | owned |
| `.agents/hooks/session-start.d/beads.sh` | Mode 0755: prints `bd prime`, and nothing when bd or its database is missing | The agents layout is in the draft | owned |

The hook keys hold one command each, so the team's own session-start hooks go in another file or in the layout's
`session-start.d/`. `bd prime` prints `.beads/PRIME.md` and bd's stored memories, about 5.5 KB with bd's default text,
on every session start, each worker iteration of a loop runner included.

Never touches: `AGENTS.md`, `.beads/config.yaml`, `.beads/metadata.json`, `.beads/.gitignore`, bd's git hooks, the
database, the Dolt remote, `core.hooksPath`, the other keys of the harnesses' files, and the rest of `.gitignore`.

## Connections

| Contact | Made by |
| --- | --- |
| The prime text to Claude Code or Codex, without the agents layout | The person, by `--harness` |
| The prime text to every harness the agents layout connects | Cook, writing `beads.sh` once `.agents/hooks/` is in the draft; `io.github.lmarqs.cookbook:agents:<version>#connect` runs it |
| What agents and workers do with the issues | The team, in the seeded `.beads/PRIME.md` |
| Where the issues sync | The team, with `bd config set sync.remote <url>`, for `--share=team` |
| The line on Beads in `AGENTS.md` | The team, from After cooking |
| Which issue a session works on | The session |

With the agents layout, leave out `--harness`: the layout links `.claude/settings.json` and `.codex/hooks.json` to its
adapters, which `#connect` owns, so this recipe refuses `--harness` once `.agents/hooks/` is in the draft and names the
`#connect` to cook instead. Cook `io.github.lmarqs.cookbook:agents:<version>#seed` before this recipe, or before it in
the same invocation, so the script is written on the first cook; `#connect` may come before or after.

## Run-time effects

`beads.sh`, run by the layout's `.agents/hooks/session-start` with the harness name as `$1`, prints what `bd prime`
prints and exits 0. Without `bd` on the `PATH`, or before `bd init`, it prints nothing. The harness hooks run `bd prime`
and `bd codex-hook SessionStart` directly.

## Re-cooking

Repeat the arguments on the opening line of the `beads` section in `.gitignore`, such as
`# io.github.lmarqs.cookbook:beads#tracker --share=clone --harness=claude`. A newer version rewrites the section, the
hook keys and `beads.sh`; `.beads/PRIME.md` keeps the team's edits, so delete it and cook again to take a newer one.
Cooking without a harness removes its hook key. Under `--check`, `beads.sh` shows as a change once the agents layout
appears in the draft, until the recipe is cooked again.

## Traps

- `bd init` appends unrooted lines of its own, such as `*.db` and `.dolt/`, to the root `.gitignore`, outside the
  section; they match in every directory, so the team may delete them.
- Without `bd` on the `PATH`, the `--harness` hooks fail at each session start; the session goes on without the text.
- Codex runs a project's hooks only once someone trusts that `hooks.json` path, so a new clone or worktree runs none
  until trusted, or until Codex starts with `--dangerously-bypass-hook-trust`.
- `bd setup claude` and `bd setup codex` write the same hook keys this recipe owns, and `bd init` without
  `--skip-agents` edits `AGENTS.md`.
''')
@yield('''
Run `bd init --prefix <p> --skip-hooks --skip-agents --non-interactive`, where `<p>` starts every issue id. It makes a
git commit of the root `.gitignore`, where it appends unrooted lines the team may delete, and with `--share=team` of
the `.beads/` files it leaves tracked. Never run
`bd setup`: this recipe writes the hooks bd's setup would.

For `--share=team`, then run `bd config set sync.remote <url>`, `bd config set export.auto true`,
`bd hooks install --beads` and `git config core.hooksPath .beads/hooks`, and commit `.beads/`. `core.hooksPath` is set
per clone, so each teammate runs that last command, and it replaces any hooks path the project already uses.

Add to `AGENTS.md`: "Beads tracks this project's work; its rules arrive from `bd prime` at session start. If they are
missing, run `bd prime`." With the agents layout, cook `io.github.lmarqs.cookbook:agents:<version>#connect` with the
team's harnesses so `beads.sh` runs at session start.
''')
import io.github.lmarqs.cookbook.beads.Beads

@param(value = ['share'], choices = ['team', 'clone'], doc = 'Who shares the issues: team syncs them through a Dolt remote; clone keeps them in this clone')
String share

@param(value = ['harness'], choices = ['claude', 'codex'], many = true, default = [], doc = 'Harnesses whose own hook file runs bd prime at session start; leave it out with the agents layout')
String[] harness

@ingredient(Beads) def beads

def harnesses = Beads.HARNESSES.findAll { it in harness }
def cookedWith = (["io.github.lmarqs.cookbook:beads#tracker --share=${share}"] + harnesses.collect { "--harness=${it}" })
    .join(' ')
beads.track(share, cookedWith)
beads.prime(harnesses)
