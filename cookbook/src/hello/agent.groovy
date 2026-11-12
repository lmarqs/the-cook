@help('''
Writes nothing but its own entry in `cook.lock`. Its After cooking text tells a coding agent how to find recipes,
cook them with the person's approval and keep the repository in step with `cook.lock`. `cook help` prints that
text, so reading this recipe's help is enough.

## API

```sh
cook help io.github.lmarqs.cookbook:hello:<version>#agent
cook --batch-approve io.github.lmarqs.cookbook:hello:<version>#agent
```

## Produces

Nothing but this recipe's entry in `cook.lock`.

Never touches: any other file.
''')
@yield('''
Cook writes a repository's configuration from recipes and records what it wrote in cook.lock.

Find recipes:
- The official cookbook is io.github.lmarqs.cookbook. Its artifacts are agents, git, docs and ralph, released at
  the version cook --version prints.
- cook help io.github.lmarqs.cookbook:agents:<version> lists an artifact's recipes, and
  cook help <coordinates>#<recipe> prints one recipe's help, arguments and what to do after cooking.
- A team's own recipes come from the repositories its cook.toml lists, or from .groovy files inside the
  repository. When the repository has a cook.toml, ask the person which recipes to use.

Cook:
1. Read each recipe's help before cooking it.
2. Run cook --dry-run <recipe> [arguments]... and show the person the diff.
3. Run cook --batch-approve <recipe> [arguments]... only after the person agrees. Without a terminal, Cook writes
   nothing unless --batch-approve is given.
4. Do what each recipe says after cooking.

Keep:
- Never edit cook.lock by hand. Commit it with the files it records.
- cook check exits 1 when the repository differs from cook.lock, and cook replicate cooks it again.
- cook discard <group>:<artifact>#<recipe> removes what a recipe wrote.
- Pin Cook to an exact version in mise.toml, such as "github:lmarqs/the-cook" = "<version>".

Remove this recipe's entry from cook.lock: cook discard io.github.lmarqs.cookbook:hello#agent
Docs: https://lmarqs.github.io/the-cook/
''')
import groovy.transform.Field

@Field final String guide = 'How a coding agent uses Cook.'
