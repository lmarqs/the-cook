@help('''
Gives the repository the `docs-layout` skill, which teaches where a fact goes in `docs/` and why, writes the map at
`docs/README.md` with the team in its own words, and writes and reviews pages by that map.

## API

```sh
./cook io.github.lmarqs.cookbook:docs:<version>#layout --skills-root=.agents/skills
# For Claude Code and Codex without a shared skills directory
./cook io.github.lmarqs.cookbook:docs:<version>#layout --skills-root=.agents/skills --skills-root=.claude/skills
```

`--skills-root` names a directory the skill is written under, and repeats for each directory a harness reads
skills from; a harness reads the skill at `<skills-root>/docs-layout/SKILL.md`. What it publishes for people and
other recipes:

| Name | What it is |
| --- | --- |
| `<skills-root>/docs-layout/SKILL.md` | The skill: `map`, `<kind> [title]`, `term`, `review`, `review --fix` and \
`adopt` |
| `docs/README.md` | The map, written by the skill with the team and kept by the team: the kinds of page this \
repository keeps, when a fact belongs to each and what a good page of each achieves |

## Produces

| Target | Part | When | Kind |
| --- | --- | --- | --- |
| `<skills-root>/docs-layout/`, for each `--skills-root` | `SKILL.md` and `references/`: one reference per kind of \
page, the map, the glossary, adopting a tree, and the reasons behind each choice | Always | owned |

The map, the kind directories with their guides, the one-page kinds and the glossary are written by the skill when
there is content for them, so the tree holds only what has content.

Never touches: `docs/`, and any file outside the skill, such as `README.md` or `AGENTS.md`.

## Connections

| Contact | Made by |
| --- | --- |
| Where the harnesses read skills | The person, by `--skills-root`, such as `.agents/skills` from the `agents` \
artifact |
| The map and the pages | The team, with the `docs-layout` skill |

## Re-cooking

Repeat `--skills-root`. A newer version updates the skill and nothing under `docs/`. A repository cooked with the
`seed` recipe of an earlier version cooks `#layout` with the same `--skills-root`, then runs
`./cook discard io.github.lmarqs.cookbook:docs#seed`, which removes the old skill files and the `kinds` section the
old recipe wrote into `docs/README.md`; `/docs-layout map` then writes the kinds back in the team's words.
''')
@yield('''
Write the map with the skill, such as `/docs-layout map` in Claude Code.
''')
import io.github.lmarqs.cookbook.docs.Docs

@param(value = ['skills-root'], many = true, sorted = true, path = true,
    doc = 'A directory, from the repository root, the docs-layout skill is written under, such as .agents/skills')
    String[] skillsRoots

@ingredient(Docs) def docs

docs.layout(skillsRoots as List<String>)
