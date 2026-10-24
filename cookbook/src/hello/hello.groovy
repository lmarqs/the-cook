@help('''
Writes nothing and yields a greeting.

## Cook

```sh
./cook io.github.lmarqs.cookbook:hello:<version>#hello
```
''')
@yield('''
Hello, world.
''')
import groovy.transform.Field

@Field final String greeting = 'Hello, world.'
