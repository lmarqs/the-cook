package thecook

import static thecook.Kitchens.cook

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.Coordinates

class HelloCookTest {
  private static final String HELLO = 'io.github.lmarqs.cookbook:hello:0.1.0#hello'

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void cooksFromTheJarTheBuildPackages() {
    def kitchen = folder.newFolder('kitchen')
    def jar = new File(kitchen, "repository/${Coordinates.parse(HELLO).jarPath}")
    jar.parentFile.mkdirs()
    jar.bytes = new File(System.getProperty('cook.helloJar')).bytes

    def proposal = cook(kitchen, HELLO)

    assert proposal.recipes*.yieldText == ['Hello, world.']
    proposal.commit()
    assert kitchen.list() as Set == ['repository', 'cook.lock'] as Set
  }
}
