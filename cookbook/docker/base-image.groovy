@help('''
Sets the base image of one named Dockerfile stage and declares a mise task that builds that stage.

## Before cooking

The selected Dockerfile must exist. The recipe reads it as preceding recipes in the invocation leave it;
the named stage must occur exactly once and use either the expected image or the requested image. The new
image must be a literal reference. mise and Docker must be available to run the build task.

## Cook

```sh
./cook io.github.lmarqs.cookbook:docker:<version>#base-image --stage=build --expected=eclipse-temurin:17 --image=eclipse-temurin:21
./cook io.github.lmarqs.cookbook:docker:<version>#base-image --dockerfile=Dockerfile.dev --stage=build --expected=eclipse-temurin:17 --image=eclipse-temurin:21
```

## Writes

| Target | Part | When |
| --- | --- | --- |
| The selected Dockerfile | The named stage's `FROM` line, owned independently | Always |
| `mise.toml` | `tasks."docker:verify"`, owned whole, building the named stage | Always |

A stage already using the requested image keeps its `FROM` line.

## With other recipes

Distinct stage lines compose. To change the same stage again in one invocation, set the later recipe's
expected image to the image the preceding recipe supplies. Each application declares `tasks."docker:verify"`,
so the last one selects its Dockerfile and stage. `io.github.lmarqs.cookbook:docker:<version>#compose-service` declares a
separate validation task.
''')
@yield('''
Run `mise run docker:verify` to build the named stage from the selected Dockerfile, using the destination
root as the build context.
''')
import io.github.lmarqs.cookbook.docker.Docker
import io.github.lmarqs.cookbook.mise.Mise

@param(value = ['dockerfile'], default = 'Dockerfile', doc = 'The Dockerfile containing the named stage') String dockerfile
@param(value = ['stage'], doc = 'The named build stage') String stage
@param(value = ['expected'], doc = 'The image the stage uses now') String expected
@param(value = ['image'], doc = 'The image to use instead') String image

@ingredient(Docker) def docker
@ingredient(Mise) def mise

docker.dockerfile = dockerfile
docker.baseImage(stage, image, expected)
mise.task('docker:verify', docker.buildCommand(stage))
