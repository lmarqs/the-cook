# policy.yaml

`.agents/permission/policy.yaml` says which commands and tools coding agents may use in this repository. Cooking
`io.github.lmarqs.cookbook:agents#connect` renders it into each harness's own settings under `.agents/adapters/`. It is the only
file to edit for permissions.

## Top level

| Field | Type | Required | Holds |
| --- | --- | --- | --- |
| `version` | `2` | Yes | The schema version this page describes |
| `commands` | [Decisions](#decisions) of [entries](#entries) | No | Shell commands, in every session |
| `tools` | [Decisions](#decisions) of [harness entries](#harness-entry) | No | Tools other than the shell, such as web fetches or MCP tools |
| `roles` | Map from a role name to a [role](#role) | No | Denies added for one kind of run |

Cook refuses another version or any other top-level field, naming it.

## Decisions

| Field | Type | Claude | Codex | Effect |
| --- | --- | --- | --- | --- |
| `allow` | List of entries | `permissions.allow` | `decision = "allow"` | Runs without asking |
| `ask` | List of entries | `permissions.ask` | `decision = "prompt"` | Asks a person first; refused when nobody can answer |
| `deny` | List of entries | `permissions.deny` | `decision = "forbidden"` | Never runs |

When several entries match one command, `deny` wins over `ask` and `ask` over `allow`, in both harnesses and across
every settings file. A command that no entry matches gets the harness's own behavior for unlisted commands.

## Entries

An entry has one of two forms.

### Prefix

A list of the words a command starts with. It matches that command followed by any arguments.

| Entry | Matches | Does not match |
| --- | --- | --- |
| `[git, push]` | `git push`, `git push origin main` | `git pull`, `git -C sub push` |
| `[git, push, --force]` | `git push --force origin main` | `git push origin main --force` |
| `[gh, pr, [view, list]]` | `gh pr view 12`, `gh pr list --state open` | `gh pr merge 12` |
| `[mise, run, "db:restore"]` | `mise run db:restore` | `mise run db:migrate` |

- A word holds letters, digits and `_ . / : = @ + -`, with no spaces, quotes, wildcards or shell syntax. Quote a word
  YAML would read as something else, such as `"on"` or `"1"`.
- A list in place of a word gives alternatives for that position: `[gh, pr, [view, list]]` is `[gh, pr, view]` and
  `[gh, pr, list]`.
- Claude gets one `Bash(<words> *)` rule per combination; Codex gets one `prefix_rule` that keeps the alternatives.

### Harness entry

A map with one key, the harness, holding a rule in that harness's own syntax. Use it for what a prefix cannot say.

| Entry | In | Renders as |
| --- | --- | --- |
| `{claude: "git push * --force*"}` | `commands` | `Bash(git push * --force*)` in Claude's settings |
| `{claude: "WebFetch(domain:docs.gradle.org)"}` | `tools` | `WebFetch(domain:docs.gradle.org)` in Claude's settings |
| `{claude: "mcp__github__get_*"}` | `tools` | `mcp__github__get_*` in Claude's settings |

Under `commands` the pattern is wrapped in `Bash(...)`; under `tools` it is the whole rule. Other harnesses do not
enforce it: Codex's rules file lists it in a comment, so a reader sees the gap. Prefer a prefix wherever one can
express the rule, because a prefix reaches every harness.

## Role

| Field | Type | Required | Holds |
| --- | --- | --- | --- |
| `deny` | List of entries | Yes | Commands this kind of run never runs, on top of `commands` |

A role name is lower case with hyphens, such as `worker`; it names the role's files. A role only adds denies, so a run
in a role does at most what any session does. Claude gets `.agents/adapters/claude/roles/<role>.json`, added at launch
with `claude --settings`. Codex gets `.agents/adapters/codex/roles/<role>.rules`; a way to add it per run is pending.

## Examples

### A small team

Everyone may read, commit and push branches; merging asks; rewriting published history never happens.

```yaml
version: 2
commands:
  allow:
    - [git, [status, diff, log, show, add, commit, switch, fetch, pull, push]]
    - [gh, pr, [view, list, diff, checks, create, comment]]
    - [./gradlew, [test, check]]
  ask:
    - [gh, pr, merge]
  deny:
    - [git, push, [--force, -f]]
    - [git, reset, --hard]
    - claude: "git push * --force*"
```

The last entry catches the flag written after the branch, which no prefix can match; Codex does not enforce it.

### Unattended workers

Workers run alone in a worktree, and an orchestrator verifies and merges their result after they exit, so nothing
may leave the worktree before then.

```yaml
roles:
  worker:
    deny:
      - [git, [commit, push, merge, rebase, stash, worktree]]
      - [gh]
```

A worker started with Claude's worker overlay gets `git status` from `commands` and is refused `git commit` by its
role. A person at the keyboard still commits.

### Read-only tools for everyone

```yaml
tools:
  allow:
    - claude: "WebFetch(domain:docs.gradle.org)"
    - claude: "mcp__datadog__search_*"
```

### Mistakes this format catches or prevents

| Written | Result | Instead |
| --- | --- | --- |
| `- git push` | Refused: an entry is a list of words or a harness entry | `- [git, push]` |
| `- [git, "push --force"]` | Refused: a word holds no spaces | `- [git, push, --force]` |
| `- [rm, "-rf", "*"]` | Refused: no wildcards in a prefix | `- [rm, -rf]`, or a `claude:` pattern |
| `roles: {worker: {allow: [...]}}` | Refused: a role only denies | Add it to `commands.allow` for everyone |
| `model: opus` at the top level | Refused: unknown field | Personal settings, or the orchestrator's config |
