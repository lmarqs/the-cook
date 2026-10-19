@help('''
Declares a mise task that shows one Beads issue and then runs Ralph on it.

## Before cooking

mise and `bd` must be on the PATH, and the Beads tracker must contain the supplied `--issue`.
The `ralph:run` task must use the Beads tracker; cook `io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui` with
`--tracker=beads --mode=mise` to supply it and pin Bun and Ralph TUI.

## Cook

```sh
./cook io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui --tracker=beads --mode=mise \\
       io.github.lmarqs.cookbook:ralph:0.1.0#bead-task --issue=cook-42
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| `mise.toml` | `tasks."ralph:bead"`, owned whole | Always |

## With other recipes

`io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui` supplies the `ralph:run` task with `--tracker=beads --mode=mise`.
`io.github.lmarqs.cookbook:ralph:0.1.0#prd-story` selects one story from a JSON PRD instead; it uses
`io.github.lmarqs.cookbook:ralph:0.1.0#ralph-tui` with the JSON tracker pointed at the selected PRD.
Other recipes can add tasks under different names; the last recipe declaring `tasks."ralph:bead"` owns it whole.
''')
@yield('''
Run `mise install`, then `mise run ralph:bead` to show the issue and run Ralph on it.
''')
import io.github.lmarqs.cookbook.mise.Mise

@param(value = ['issue'], doc = 'The Beads issue to work on') String issue

@ingredient(Mise) def mise

mise.task('ralph:bead', [run: [['bd', 'show', issue], ['mise', 'run', 'ralph:run', issue]]])
