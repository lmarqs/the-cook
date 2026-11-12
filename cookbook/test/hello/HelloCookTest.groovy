package thecook

import static thecook.Kitchens.cook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Coordinates

class HelloCookTest {
  private static final String WORLD = 'io.github.lmarqs.cookbook:hello:0.1.0#world'
  private static final String AGENT = 'io.github.lmarqs.cookbook:hello:0.1.0#agent'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void bothRecipesCookFromTheJarTheBuildPackagesAndWriteOnlyTheLock() {
    def kitchen = kitchenWithTheHelloJar()

    def proposal = cook(kitchen, [(WORLD): [:], (AGENT): [:]])

    def (world, agent) = proposal.recipes*.yieldText
    assert world.startsWith('Hello, world.')
    assert world.contains('cook discard io.github.lmarqs.cookbook:hello#world')
    assert world.contains('cook help io.github.lmarqs.cookbook:hello:<version>#agent')
    assert agent.contains('cook --dry-run <recipe> [arguments]... and show the person the diff')
    assert agent.contains('cook discard io.github.lmarqs.cookbook:hello#agent')
    proposal.commit()
    assert kitchen.list() as Set == ['repository', 'cook.lock'] as Set
  }

  private File kitchenWithTheHelloJar() {
    def kitchen = folder.newFolder('kitchen')
    def jar = new File(kitchen, "repository/${Coordinates.parse(WORLD).jarPath}")
    jar.parentFile.mkdirs()
    jar.bytes = new File(System.getProperty('cook.helloJar')).bytes
    kitchen
  }
}
