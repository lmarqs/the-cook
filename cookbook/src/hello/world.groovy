@help('''
Writes nothing but its own entry in `cook.lock`, so a person sees one whole cook: the recipe fetched, its diff, the
approval and the lock. Its After cooking text says what happened and where to go next.

## API

```sh
cook io.github.lmarqs.cookbook:hello:<version>#world
```

## Produces

Nothing but this recipe's entry in `cook.lock`.

Never touches: any other file.
''')
@yield('''
Hello, world.

Cook fetched this recipe, showed its diff and, once approved, recorded it in cook.lock. Every recipe goes through
these steps.

Remove it again:  cook discard io.github.lmarqs.cookbook:hello#world
List a cookbook:  cook help io.github.lmarqs.cookbook:agents:<version>, at the version cook --version prints
Read a recipe:    cook help <recipe>

The official cookbook is io.github.lmarqs.cookbook, with the artifacts agents, git, docs and ralph. A team's own
recipes come from the repositories its cook.toml lists, or from .groovy files inside the repository.

Coding agents: read cook help io.github.lmarqs.cookbook:hello:<version>#agent.
Docs: https://lmarqs.github.io/the-cook/
''')
import groovy.transform.Field

@Field final String greeting = 'Hello, world.'
