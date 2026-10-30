@help('''
Gives the repository a `docs/` tree that people and coding agents share as the source of truth: a map at
`docs/README.md` that says which page owns each fact, the kinds of page with their criteria, and the `docs-layout`
skill that writes and reviews pages.

## API

```sh
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills-root=.agents/skills
# Offering two kinds only
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills-root=.agents/skills --kinds=adrs --kinds=runbooks
# For Claude Code and Codex without a shared skills directory
./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills-root=.agents/skills --skills-root=.claude/skills
```

`--skills-root` names a directory the skill is written under, and repeats for each directory a harness reads
skills from; a harness reads the skill at `<skills-root>/docs-layout/SKILL.md`. What it publishes for people and
other recipes:

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

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `docs/README.md` | The title and an introduction the team edits | Always | seed |
| `docs/README.md` | The section `kinds`: the argument line as an HTML comment, each kind offered with its \
criteria, where a fact goes, what a page holds, terms, guides and the skill | Always | owned |
| `<skills-root>/docs-layout/`, for each `--skills-root` | `SKILL.md`, `references/` and `templates/` | Always | owned |

The kind directories, their guides, the one-page kinds and the glossary appear with their first
content, written by the skill from its templates, so the tree holds only what has content.

Never touches: the pages under `docs/`, the rest of `docs/README.md`, and any file outside `docs/` and the skill,
such as `README.md` or `AGENTS.md`.

## Connections

| Contact | Made by |
| --- | --- |
| Where the harnesses read skills | The person, by `--skills-root`, such as `.agents/skills` from the `agents` \
artifact |
| The pages themselves | People and agents, with the `docs-layout` skill |
| A kind of the team's own | The team, by a heading in `docs/README.md` outside the `kinds` section |

## Re-cooking

Repeat the arguments on the opening line of the `kinds` section in `docs/README.md`, such as
`<!-- io.github.lmarqs.cookbook:docs#seed --skills-root=.agents/skills -->`. Cooking with other `--kinds` rewrites the
section; the pages stay. An upgrade updates the section and the skill; the map's
introduction is never rewritten.
''')
@yield('''
Write the first page with the skill, such as `/docs-layout adrs <title>` in Claude Code.
''')
import io.github.lmarqs.cookbook.docs.Docs

@param(value = ['kinds'], many = true, sorted = true,
    default = ['adrs', 'api', 'architecture', 'features', 'philosophy', 'runbooks', 'ux'],
    choices = ['adrs', 'api', 'architecture', 'features', 'philosophy', 'runbooks', 'ux'],
    doc = 'A kind of page the map offers; all seven by default')
    String[] kinds
@param(value = ['skills-root'], many = true, sorted = true, path = true,
    doc = 'A directory, from the repository root, the docs-layout skill is written under, such as .agents/skills')
    String[] skillsRoots

@recipe(arguments = true) String cookedWith
@ingredient(Docs) def docs

docs.layout(cookedWith, skillsRoots as List<String>, kinds as List<String>)
