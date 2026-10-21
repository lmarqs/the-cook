@help('''
Sets a repository up for a Beads tracker around what `bd init` writes: the ignore rules for the issues that stay out
of commits, the prime text `bd prime` prints, and, at the path the person names, a session-start script that prints
it, so `bd init` is the only step left by hand.

## API

```sh
# Issues kept in each clone, with no remote
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=clone
# Issues the team shares through a Dolt remote
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=team
# With the agents layout: its session-start runner prints the script's output in every harness it connects
./cook io.github.lmarqs.cookbook:beads:<version>#tracker --share=clone \
--session-start=.agents/hooks/session-start.d/beads.sh
```

`--session-start` names the file by its path from the repository root; the person picks its name, so a prefix such as
`20-beads.sh` places it among a runner's other scripts. Without it, the recipe writes nothing for session start.

Paths and scripts other recipes, runners and people rely on:

| Name | What it is |
| --- | --- |
| `.beads/PRIME.md` | The team's workflow text, which `bd prime` prints in place of bd's own |
| The `--session-start` file | An executable `sh` script that ignores its arguments, prints what `bd prime` prints, \
and prints nothing when bd or its database is missing; it always exits 0. Any session-start runner that runs \
executables and adds their standard output to the session can run it |

## Consumes

- `bd` at run time, written for bd 1.3.1.
- Nothing in the draft: what it writes follows from its arguments alone.
- By hand first: nothing. By hand after: install bd and run `bd init`, as After cooking says.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `.gitignore` | The section `beads`: the argument line, then `/.beads/*` and `!/.beads/PRIME.md` | \
`--share=clone` | owned |
| `.gitignore` | The section `beads`: the argument line, then `/.beads/issues.jsonl`, \
the export that carries whatever people paste into issues; bd's own `.beads/.gitignore` covers its other files | \
`--share=team` | owned |
| `.beads/PRIME.md` | The workflow and rules for that share, \
including that a worker never closes its bead or pushes | Always, when absent | seed |
| The `--session-start` file | Mode 0755: prints `bd prime`, and nothing when bd or its database is missing | \
`--session-start` | owned |

`bd prime` prints `.beads/PRIME.md` and bd's stored memories, about 5.5 KB with bd's default text, on every session
start that runs it, each worker iteration of a loop runner included.

Never touches: `AGENTS.md`, `.beads/config.yaml`, `.beads/metadata.json`, `.beads/.gitignore`, bd's git hooks, the
database, the Dolt remote, `core.hooksPath`, any harness's settings or hook files, and the rest of `.gitignore`.

## Connections

| Contact | Made by |
| --- | --- |
| The prime text at session start | The person, by `--session-start`, naming where a session-start runner looks; \
without it, the repository runs `bd prime` from its own session-start hooks |
| The script's place among the runner's other scripts | The person, by the file name in `--session-start` |
| What agents and workers do with the issues | The team, in the seeded `.beads/PRIME.md` |
| Where the issues sync | The team, with `bd config set sync.remote <url>`, for `--share=team` |
| The line on Beads in `AGENTS.md` | The team, from After cooking |
| Which issue a session works on | The session |

With the agents layout, `io.github.lmarqs.cookbook:agents:<version>#connect` runs every `*.sh` in
`.agents/hooks/session-start.d/` at session start, with the harness as `$1`, and adds their output to Claude Code and
Codex sessions, so the path there ends in `.sh`. The script is read at session start, so cook the two in either order.

## Run-time effects

The `--session-start` script, run with any arguments, prints what `bd prime` prints and exits 0. Without `bd` on the
`PATH`, or before `bd init`, it prints nothing.

## Re-cooking

Repeat the arguments on the opening line of the `beads` section in `.gitignore`, such as
`# io.github.lmarqs.cookbook:beads#tracker --share=clone --session-start=.agents/hooks/session-start.d/beads.sh`. A
newer version rewrites the section and the script; `.beads/PRIME.md` keeps the team's edits, so delete it and cook
again to take a newer one. Cooking with another `--session-start` moves the script, and cooking without it removes the
script. Under `--check`, the script changes only with the arguments, never with other files in the draft.

## Traps

- `bd init` appends unrooted lines of its own, such as `*.db` and `.dolt/`, to the root `.gitignore`, outside the
  section; they match in every directory, so the team may delete them.
- `bd init` without `--skip-agents` edits `AGENTS.md`.
- With `--share=clone`, the section ignores a `--session-start` path under `.beads/`, so the script is never
  committed: name a path outside `.beads/`.
''')
@yield('''
Install bd with `mise use -g aqua:steveyegge/beads@1.3.1`; without `-g`, mise pins it in the project's `mise.toml`
instead, which is the team's choice.

Run `bd init --prefix <p> --skip-hooks --skip-agents --non-interactive`, where `<p>` starts every issue id. It makes a
git commit of the root `.gitignore`, where it appends unrooted lines the team may delete, and with `--share=team` of
the `.beads/` files it leaves tracked.

For `--share=team`, then run `bd config set sync.remote <url>`, `bd config set export.auto true`,
`bd hooks install --beads` and `git config core.hooksPath .beads/hooks`, and commit `.beads/`. `core.hooksPath` is set
per clone, so each teammate runs that last command, and it replaces any hooks path the project already uses.

Without `--session-start`, give sessions the prime text from the repository's own session-start hook, running
`if command -v bd >/dev/null; then bd prime; fi`, which exits 0 in a clone without bd, or from bd's own adapters:
`bd setup claude` installs Claude Code SessionStart hooks, and `bd setup codex` a Codex skill, `AGENTS.md` guidance
and native hooks; `bd setup <agent> --check` shows what is installed, and `--remove` removes it. With the agents
layout, cook with `--session-start` instead: `io.github.lmarqs.cookbook:agents:<version>#connect` owns
`hooks.SessionStart` in both harnesses, so a hook written there is replaced on its next cook.

Add to `AGENTS.md`: "Beads tracks this project's work; its rules arrive from `bd prime` at session start. If they are
missing, run `bd prime`."
''')
import io.github.lmarqs.cookbook.beads.Beads

@param(value = ['share'], choices = ['team', 'clone'],
    doc = 'Who shares the issues: team syncs them through a Dolt remote; clone keeps them in this clone')
    String share
@param(value = ['session-start'], default = [],
    doc = 'Where to write the script that prints bd prime for a session-start runner, from the repository root')
    String sessionStart

@ingredient(Beads) def beads

if (sessionStart != null) {
  beads.sessionStart(sessionStart)
}

def arguments = ["--share=${share}"] + (sessionStart != null ? ["--session-start=${sessionStart}"] : [])
beads.track(share, (['io.github.lmarqs.cookbook:beads#tracker'] + arguments).join(' '))
