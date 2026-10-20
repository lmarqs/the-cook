# Adopt the layout

Each section below starts from one kind of repository and ends with the layout in place. Read the ones that match;
most repositories match more than one. Commands write the recipe version as `<version>`: use the latest release, or
the one in `cook.lock` when the repository already cooks the agents artifact.

Every move ends the same way, so it is written once here:

```sh
./cook io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
git status --short
git diff
git add AGENTS.md .agents .claude .codex .gitignore cook.lock && git commit
```

Name every harness the team uses: `#connect` removes what it wrote for a harness left out. After the commit,
`git ls-files .claude .codex` lists only links, and `git ls-files -s .claude .codex` shows each with mode `120000`.
`git check-ignore --no-index $(find .claude .codex -maxdepth 1 -type l)` prints nothing; a link it prints is hidden
by an ignore rule, which [investigate.md](investigate.md) shows how to find.

## A new repository

1. Seed the layout:

   ```sh
   ./cook io.github.lmarqs.cookbook:agents:<version>#seed
   ```

   It writes `AGENTS.md`, `.agents/README.md`, `.agents/permission/policy.yaml`, the shared directories and this
   skill, in `.agents/skills/agents-layout/`. When
   `AGENTS.md` already exists, Cook refuses; add `--skip` before the recipe to keep it, or `--force` to replace it.
2. Ask an agent to fill in `AGENTS.md`; the file's first line tells it how.
3. Add to the policy what agents may run here. The starter grants nothing and ends with a commented example;
   [policy.md](policy.md) has the schema.
4. Connect, review and commit as above.

Before and after:

```text
before                     after
  README.md                  README.md
  src/                       src/
  cook.toml                  cook.toml
                             cook.lock
                             AGENTS.md
                             .gitignore                    with the harnesses section
                             .agents/README.md
                             .agents/permission/policy.yaml
                             .agents/adapters/claude/settings.json
                             .agents/adapters/codex/config.toml
                             .agents/skills/agents-layout/
                             .agents/commands/  hooks/  memory/
                             .claude/settings.json -> ../.agents/adapters/claude/settings.json
                             .claude/skills -> ../.agents/skills
                             .claude/commands -> ../.agents/commands
                             .codex/config.toml -> ../.agents/adapters/codex/config.toml
```

`.codex/rules` and `.agents/adapters/codex/rules/cook.rules` appear once the policy has a command entry.

## A root CLAUDE.md

Claude skips `AGENTS.md` while a `CLAUDE.md`, `.claude/CLAUDE.md` or `CLAUDE.local.md` exists in the directory or one
above, so the file has to go, not just stop being committed.

1. List every such file the repository tracks:

   ```sh
   git ls-files 'CLAUDE.md' '*/CLAUDE.md' 'CLAUDE.local.md' '*/CLAUDE.local.md'
   ```

2. Move each line every agent needs into `AGENTS.md`. Drop `@AGENTS.md` imports and lines the README already says.
   Lines that are one person's preference go to that person's `~/.claude/CLAUDE.md`.
3. Remove the files from git and from the disk. `.gitignore` does not untrack a committed file:

   ```sh
   git rm CLAUDE.md
   ```

4. Connect, review and commit.

Before:

```markdown
<!-- CLAUDE.md -->
@AGENTS.md

Use pnpm, never npm.
Run `pnpm test` before every commit.
I prefer short answers.
```

After: the first two rules are in `AGENTS.md`, the last line is in its author's `~/.claude/CLAUDE.md`, and
`CLAUDE.md` is gone.

## Claude path-scoped rules in .claude/rules/

Codex does not read `.claude/rules/`, and the layout keeps `.claude/` to links, so each rule moves to a place every
agent reads.

1. For a short rule, add a section to `AGENTS.md` that names the paths it covers.
2. For a long one, or one that matters only for some tasks, make it a skill: `.agents/skills/<name>/SKILL.md`, with a
   `description` saying when to use it. Both harnesses load a skill when its description matches the task.
3. `git rm -r .claude/rules`, then connect, review and commit.

Before:

```markdown
<!-- .claude/rules/migrations.md -->
---
paths: ["src/migrations/**"]
---
Never edit a migration that has been merged; add a new one.
```

After, in `AGENTS.md`:

```markdown
## Migrations (`src/migrations/`)

Never edit a migration that has been merged; add a new one.
```

## Bash rules in a committed .claude/settings.json

1. Translate each `permissions` rule into the policy:

   | In `.claude/settings.json` | In `policy.yaml` |
   | --- | --- |
   | `Bash(git status:*)`, the legacy prefix form | `[git, status]` under the same decision |
   | `Bash(npm run test *)` | `[npm, run, test]` |
   | `Bash(git push origin main)`, no wildcard | `[git, push, origin, main]`, which also matches later arguments, or `claude: "git push origin main"` to stay exact |
   | `Bash(git push * --force*)`, a wildcard in the middle | `claude: "git push * --force*"`; Codex does not enforce it |
   | `WebFetch(domain:docs.gradle.org)`, `mcp__github__get_*` | `claude: "<rule>"` under `tools` |

2. Move every other key out of the file. `model` and `defaultMode` are each person's choice: their own
   `~/.claude/settings.json`. Keys the team shares, such as `hooks` or `env`, go in a JSON file cooked with
   `io.github.lmarqs.cookbook:agents:<version>#claude-settings --settings=<file>`.
3. `git rm .claude/settings.json`; `#connect` makes it a link to the adapter.
4. Connect, review and commit. Compare `.agents/adapters/claude/settings.json` with the removed file: every rule is
   there, `Bash(<words> *)` in place of `Bash(<words>:*)`.

Before:

```json
{
  "model": "opus",
  "permissions": {
    "allow": ["Bash(git status:*)", "Bash(gh pr view:*)", "Bash(gh pr list:*)", "mcp__github__get_issue"],
    "deny": ["Bash(git push --force:*)"]
  }
}
```

After:

```yaml
version: 2
commands:
  allow:
    - [git, status]
    - [gh, pr, [view, list]]
  deny:
    - [git, push, --force]
tools:
  allow:
    - claude: "mcp__github__get_issue"
```

## A hand-made policy with its own renderer

Some repositories already keep a policy file and a script that writes both harnesses' settings from it. Version 2
holds the same ideas, so the move is mechanical.

1. Map the groups:

   | Hand-made | Version 2 |
   | --- | --- |
   | A group for every session, such as `shared: {allow, ask, deny}` | `commands: {allow, ask, deny}` |
   | A group for one kind of run, such as `worker: {deny}` | `roles: {worker: {deny}}` |
   | A quoted string read as a raw Claude pattern, such as `"mise * ::: *"` | `claude: "mise * ::: *"` |
   | A list of MCP or other tool patterns appended to Claude's allows | `claude:` entries under `tools.allow` |
   | Allows for one kind of run only | `commands` for everyone, after the team agrees; a role only denies |

2. Codex gets no role. A renderer that wrote worker denies into Codex's rules because Codex ran only as a worker now
   has two choices: add those entries to `commands.deny`, which also binds people at the keyboard, or keep Codex
   workers on `commands` alone and rely on its sandbox. [design.md](design.md#why-codex-has-no-roles) explains why.
3. Delete the renderer, its outputs and any check that runs it, then connect, review and commit:

   ```sh
   git rm .agents/permissions/render.py .claude/settings.json .codex/rules/workflow.rules
   git mv .agents/permissions/policy.yaml .agents/permission/policy.yaml
   ```

   The paths are an example; use the repository's own.

Before:

```yaml
shared:
  ask:
    - "gh pr merge *--admin*"
  deny:
    - [git, push, ["--force", "-f"], origin, main]
    - "git push origin main --force*"
  read_mcp:
    - "mcp__plugin_datadog_mcp__search_*"
worker:
  deny:
    - [git, [commit, push, stash]]
    - [gh]
```

After:

```yaml
version: 2
commands:
  ask:
    - claude: "gh pr merge *--admin*"
  deny:
    - [git, push, [--force, -f], origin, main]
    - claude: "git push origin main --force*"
tools:
  allow:
    - claude: "mcp__plugin_datadog_mcp__search_*"
roles:
  worker:
    deny:
      - [git, [commit, push, stash]]
      - [gh]
```

Keep the comments that explain why an entry is there; YAML comments survive the move and every reader needs them.

## A version 1 .agents/policy.yaml

An earlier release of the agents artifact used `io.github.lmarqs:agents` coordinates, a version 1 policy at
`.agents/policy.yaml` and a `CLAUDE.md` adapter. Cook refuses that policy with `version must be 2`.

1. Remove what the earlier release wrote. List its recipes, then discard each by the name `cook.lock` records:

   ```sh
   grep 'name:' cook.lock
   ./cook discard io.github.lmarqs:agents:0.0.0#connect io.github.lmarqs:agents:0.0.0#instructions
   ```

   Discard reads only `cook.lock`, so it works although those coordinates no longer resolve, and it ignores the
   version. It keeps `AGENTS.md` and the policy, which a seed wrote and nobody owns.
2. Move the policy and set its version:

   ```sh
   mkdir -p .agents/permission
   git mv .agents/policy.yaml .agents/permission/policy.yaml
   ```

   Change `version: 1` to `version: 2`. Move each line of `instructions` into `AGENTS.md`, then delete the field.
   `commands` keeps its shape; nested lists can now merge entries that differ in one word.
3. If a `CLAUDE.md` or `.claude/CLAUDE.md` remains, follow [A root CLAUDE.md](#a-root-claudemd).
4. Connect, review and commit.

Before:

```yaml
version: 1
instructions:
  - Run ./gradlew check before committing.
commands:
  allow:
    - [git, status]
    - [git, diff]
```

After, with the instruction line in `AGENTS.md`:

```yaml
version: 2
commands:
  allow:
    - [git, [status, diff]]
```
