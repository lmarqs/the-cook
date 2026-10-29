@help('''
Gives the repository a `docs/` tree that people and coding agents share as the source of truth: a map at
`docs/README.md` that says which page owns each fact, the kinds of page with their criteria, and the `docs-layout`
skill that writes pages, reviews them and runs the check.

## API

```sh
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=.agents/skills
# Offering two kinds only
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=.agents/skills --kinds=adr --kinds=runbooks
# Also stating the rule where agents and people read
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=.agents/skills --instructions=AGENTS.md
```

`--skills` names the directory the skill is written under; the harnesses read a skill at
`<skills>/docs-layout/SKILL.md`. `--instructions` names a Markdown file by its path from the repository root, and
repeats for each file. The file must exist, written by the team or by a recipe cooked earlier in the same invocation.
What it publishes for people and other recipes:

| Name | What it is |
| --- | --- |
| `docs/README.md` | The map: each kind offered, what it holds, its criteria, and where a fact goes |
| `docs/<kind>/` | The pages of one kind, `adr`, `api`, `features`, `runbooks` or `ux`, \
with a guide `README.md` listing them; created with the kind's first page |
| `docs/adr/NNNN-<slug>.md` | One decision record per file, numbered in sequence |
| `docs/glossary.md` | The terms, with the synonyms the team rejected; created with the first term |
| `sh <skills>/docs-layout/scripts/check.sh` | The check: links, anchors, guides and each kind's sections; \
exits 1 on a failure |

## Consumes

Only the files `--instructions` names, which must exist. The check runs with POSIX `sh`, `awk`, `grep`, `sed` and
`find`. The skill is written for Claude Code 2.1.312 and Codex 0.160.1.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `docs/README.md` | The title and an introduction the team edits | Always | seed |
| `docs/README.md` | The section `kinds`: the argument line as an HTML comment, each kind offered with its \
criteria, where a fact goes, what a page holds, terms, guides and the check command | Always | owned |
| `<skills>/docs-layout/` | `SKILL.md`, `references/`, `scripts/check.sh` and `templates/` | Always | owned |
| Each `--instructions` file | The section `docs`: the argument line as an HTML comment, then a list item \
saying `docs/` is the source of truth, where a learned fact goes, and the check and skill to use | \
`--instructions` | owned |

The kind directories, their guides and the glossary appear with their first page, written by the skill from its
templates, so the tree holds only what has content.

Never touches: the pages under `docs/`, the rest of `docs/README.md` and of each `--instructions` file.

## Connections

| Contact | Made by |
| --- | --- |
| Where the harnesses read skills | The person, by `--skills`, such as `.agents/skills` from the `agents` artifact |
| The rule in the files agents and people read | The person, by `--instructions`; the section is appended at the \
file's end, and stays where the team moves it |
| The check in CI or a Done check | The team, by running the check command the map names |
| The pages themselves | People and agents, with the `docs-layout` skill |

## Re-cooking

Repeat the arguments on the opening line of the `kinds` section in `docs/README.md`, such as
`<!-- io.github.lmarqs.cookbook:docs#seed --skills=.agents/skills -->`. Cooking with other `--kinds` rewrites the
section; the pages stay. An upgrade updates the section, the skill and each instructions section; the map's
introduction is never rewritten. Cooking without a file's `--instructions` removes its section.

## Traps

- The check resolves heading anchors for ASCII text: lowercase, punctuation removed, spaces as hyphens. A link to a
  heading with non-ASCII letters passes unverified, so open such a link once where the pages render to confirm it.
''')
@yield('''
Write the first page with the skill, such as `/docs-layout adr <title>` in Claude Code, and run
`sh <skills>/docs-layout/scripts/check.sh` before the change is done. Cooked without `--instructions`, cook again
with it, such as `--instructions=AGENTS.md`, so agents read the rule.
''')
import io.github.lmarqs.cookbook.docs.Docs

@param(value = ['instructions'], many = true, sorted = true, default = [], path = true,
    doc = 'A Markdown file, from the repository root, where the recipe states the docs rule for agents and people')
    String[] instructions
@param(value = ['kinds'], many = true, sorted = true, default = ['adr', 'api', 'features', 'runbooks', 'ux'],
    choices = ['adr', 'api', 'features', 'runbooks', 'ux'],
    doc = 'A kind of page the map offers; all five by default')
    String[] kinds
@param(value = ['skills'], path = true,
    doc = 'The directory, from the repository root, the docs-layout skill is written under, such as .agents/skills')
    String skills

@recipe(arguments = true) String cookedWith
@ingredient(Docs) def docs

docs.instructions(instructions as List<String>)
docs.layout(cookedWith, skills, kinds as List<String>)
