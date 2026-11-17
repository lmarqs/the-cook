# Memory

`.agents/memory/` holds what a session learned about working in one area of the repository, that a later session
needs and no other file owns. One file per area, as `.agents/memory/migrations.md`, read only when a task touches the
area: `AGENTS.md` names each file under Deep dive with the question it answers, and a file no `AGENTS.md` names is
never read.

## Where a learned fact goes

Most of what a session learns has an owner already, and a copy in memory drifts from it. Take the first row that
fits:

| The fact is | It goes to |
| --- | --- |
| A secret, or about a person | Nowhere in the repository |
| What this session did, what is left to do, or a plan | The tracker, or the commit message |
| One person's preference | That person's harness settings, outside the repository |
| Shown by the code, a config file, the README or the history | Nowhere; name the file where a reader needs it |
| Something a check, a hook or the policy can enforce | The check, the hook or `policy.yaml` |
| Needed by every task | `AGENTS.md`: a rule, or one of its five gotchas |
| A procedure an agent repeats | A skill in `.agents/skills/` |
| What a person changing the code needs too: a decision, how a feature behaves, a standard, a trap of the product | The repository's documentation, where it keeps some; the page owns it and memory links it |
| How to work in one area, learned from a correction or a failure, that nobody would look up in the documentation | `.agents/memory/<area>.md` |

The last row is narrow on purpose. In a repository with documentation, memory holds mostly how agents go wrong
there: the command that looks right and is not, the file that must change with another, the generated directory
that must not be edited. In a repository without it, memory also holds the decisions and traps a page would own,
until the team writes that page.

## What earns an entry

An entry is worth its place when each answer is yes:

- Would a fresh session make the mistake again without it?
- Is it still true once this task is merged?
- Is it true for everyone who works here, with any harness?
- Did it come from something that happened: a person's correction, a failed check, a command's output? A guess
  about why something works stays out.
- Does it name the area it applies to, so a reader outside that area can skip it?

One benchmark run with no `AGENTS.md` wrote its session note into a versioned memory file
([agents-md.md](agents-md.md)). A note of what a session did fails the second question, and the tracker owns it.

## How an entry reads

The fact first, as an instruction a reader can follow. Then why: what happened, and what it cost. Then when it
applies, where that is not plain. The reason lets a reader judge a case the words leave open, and tell when the
entry has stopped being true. `.agents/memory/migrations.md` in a made-up project:

```markdown
# Migrations

What to know before adding or changing a file in `db/migrations/`.

- Number a new migration from `main`, not from the branch: run `git fetch` and take the next number after
  `origin/main`'s last. Two branches each took `0042`, both passed their checks, and the deploy ran only one.
- `make db-reset` drops the local database without asking. Use `make db-migrate` to apply new migrations; a
  session reset a database that held an afternoon of hand-entered test data.
```

A file opens with the question it answers, the one `AGENTS.md` repeats under Deep dive. It states what is true now,
with no dates, authors or log of changes: the history holds those.

## When an entry leaves

Memory that only grows is read less each time it is opened. An entry leaves in the change that makes it untrue or
gives it another owner:

- A check, a hook or a policy entry comes to enforce it.
- The code changes so the trap is gone.
- Every task comes to need it, and it moves to `AGENTS.md`.
- People come to need it, and it moves to a documentation page.

A file that answers two questions is split, and each half named under Deep dive. An empty file is deleted with its
line in `AGENTS.md`.

## Who writes it

An agent proposes an entry when a person corrects it, or when a failure reveals a trap the next session would meet,
and says so in its report. The entry is committed and reviewed like every file the team owns: whoever changes it
steers every agent that reads it.

A harness's own memory is another thing. Claude Code's auto memory is written by Claude under
`~/.claude/projects/<project>/memory/` and stays on one person's machine (Claude's docs; not tested), so nothing
there reaches a teammate, another harness or a loop runner's worker. What the team needs is written here.
