@help('''
Gives the repository a `docs/` tree that people and coding agents share as the source of truth: a map at
`docs/README.md` that says which page owns each fact, the kinds of page with their criteria, and the `docs-layout`
skill that writes and reviews pages.

## API

```sh
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=.agents/skills
# Offering two kinds only
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=.agents/skills --kinds=adrs --kinds=runbooks
# Also stating the rule where agents and people read
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=.agents/skills --instructions=AGENTS.md
```

`--skills` names the directory the skill is written under; the harnesses read a skill at
`<skills>/docs-layout/SKILL.md`. `--instructions` names a Markdown file by its path from the repository root, and
repeats for each file. The file must exist, written by the team or by a recipe cooked earlier in the same invocation.
What it publishes for people and other recipes:

| Name | What it is |
| --- | --- |
| `docs/README.md` | The map: each kind offered, what it holds, its criteria, where a fact goes, and how a team \
declares a kind of its own |
| `docs/<kind>/` | The pages of one kind, `adrs`, `api`, `features`, `runbooks` or `ux`, \
with a guide `README.md` listing them; created with the kind's first page |
| `docs/architecture.md`, `docs/philosophy.md` | The kinds of one page: how the parts fit together, and the tenets \
that guide decisions; created with their content |
| `docs/adrs/NNNN-<slug>.md` | One decision record per file, numbered in sequence |
| `docs/glossary.md` | The terms, with the synonyms the team rejected; created with the first term |

## Consumes

Only the files `--instructions` names, which must exist.

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `docs/README.md` | The title and an introduction the team edits | Always | seed |
| `docs/README.md` | The section `kinds`: the argument line as an HTML comment, each kind offered with its \
criteria, where a fact goes, what a page holds, terms, guides and the skill | Always | owned |
| `<skills>/docs-layout/` | `SKILL.md`, `references/` and `templates/` | Always | owned |
| Each `--instructions` file | The section `docs`: the argument line as an HTML comment, then a list item \
saying `docs/` is the source of truth, that the map says which page owns a fact, and the skill that writes pages | \
`--instructions` | owned |

The kind directories, their guides, the one-page kinds and the glossary appear with their first
content, written by the skill from its templates, so the tree holds only what has content.

Never touches: the pages under `docs/`, the rest of `docs/README.md` and of each `--instructions` file, and the
root `README.md`.

## Connections

| Contact | Made by |
| --- | --- |
| Where the harnesses read skills | The person, by `--skills`, such as `.agents/skills` from the `agents` artifact |
| The rule in the files agents and people read | The person, by `--instructions`; the section is appended at the \
file's end, and stays where the team moves it |
| The pages themselves | People and agents, with the `docs-layout` skill |
| A kind of the team's own | The team, by a heading in `docs/README.md` outside the `kinds` section |

## Re-cooking

Repeat the arguments on the opening line of the `kinds` section in `docs/README.md`, such as
`<!-- io.github.lmarqs.cookbook:docs#seed --skills=.agents/skills -->`. Cooking with other `--kinds` rewrites the
section; the pages stay. An upgrade updates the section, the skill and each instructions section; the map's
introduction is never rewritten. Cooking without a file's `--instructions` removes its section.
''')
@yield('''
Write the first page with the skill, such as `/docs-layout adrs <title>` in Claude Code. Cooked without
`--instructions`, cook again with it, such as `--instructions=AGENTS.md`, so agents find the map.
''')
import io.github.lmarqs.cookbook.docs.Docs

@param(value = ['instructions'], many = true, sorted = true, default = [], path = true,
    doc = 'A Markdown file, from the repository root, where the recipe states the docs rule for agents and people')
    String[] instructions
@param(value = ['kinds'], many = true, sorted = true,
    default = ['adrs', 'api', 'architecture', 'features', 'philosophy', 'runbooks', 'ux'],
    choices = ['adrs', 'api', 'architecture', 'features', 'philosophy', 'runbooks', 'ux'],
    doc = 'A kind of page the map offers; all seven by default')
    String[] kinds
@param(value = ['skills'], path = true,
    doc = 'The directory, from the repository root, the docs-layout skill is written under, such as .agents/skills')
    String skills

@recipe(arguments = true) String cookedWith
@ingredient(Docs) def docs

docs.instructions(instructions as List<String>)
docs.layout(cookedWith, skills, kinds as List<String>)
