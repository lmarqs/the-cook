@help('''
Writes a copy of a JSON PRD reduced to one user story, where Ralph will read it.

## Before cooking

The source PRD must exist in Ralph's JSON format and contain exactly one `userStories` entry with the supplied
`--story` id. The source is read as earlier recipes in the invocation leave it.

## Cook

```sh
./cook io.github.lmarqs.cookbook:ralph:0.1.0#prd-story --story=US-007 \\
       io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui --prd=.ralph-tui/selected-prd.json --mode=mise
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| The `--selected` path, `.ralph-tui/selected-prd.json` by default | The whole file: the source PRD with `userStories` reduced to the selected story | Always, when the source contains exactly one matching story |

## With other recipes

Cook `io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui` after this recipe, with `--prd` pointing at the same `--selected` path.
`io.github.lmarqs.cookbook:ralph:0.1.0#bead-task` supplies a task for a Beads issue instead of a JSON story; it uses
`io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui` with `--tracker=beads --mode=mise`.
''')
@yield('''
Run `mise install`, then `mise run ralph:run` to work on the selected story with the configuration above.
''')
import io.github.lmarqs.cookbook.ralph.RalphTui

@param(value = ['prd'], default = 'tasks/prd.json', doc = 'The source PRD') String prd
@param(value = ['story'], doc = 'The id of the one story to keep') String story
@param(value = ['selected'], default = '.ralph-tui/selected-prd.json', doc = 'Where the reduced PRD is written') String selected

@ingredient(RalphTui) def ralph

ralph.focus(prd, story, selected)
