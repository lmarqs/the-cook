package thecook

import org.junit.Test
import thecook.cookbook.Coordinates

class CoordinatesTest {
  @Test void parsesCoordinatesWithAnExactReleaseVersion() {
    def coordinates = Coordinates.parse('io.github.lmarqs:agents:0.1.0#instructions')
    assert [coordinates.group, coordinates.artifact, coordinates.version, coordinates.recipe] ==
    ['io.github.lmarqs', 'agents', '0.1.0', 'instructions']
    assert coordinates.jarPath == 'io/github/lmarqs/agents/0.1.0/agents-0.1.0.jar'
    assert coordinates.recipeName == 'io.github.lmarqs:agents#instructions'
    assert coordinates.toString() == 'io.github.lmarqs:agents:0.1.0#instructions'
    def artifact = Coordinates.parse('io.github.lmarqs:agents:0.1.0')
    assert artifact.recipe == null
    assert shouldFail(IllegalArgumentException) { artifact.recipeName }.message ==
    'io.github.lmarqs:agents:0.1.0 names an artifact, not a recipe; write io.github.lmarqs:agents:0.1.0#<recipe>'
    assert artifact.recipeAddress('permissions') == 'io.github.lmarqs:agents:0.1.0#permissions'
    [
      'io.github.lmarqs:agents',
      'io.github.lmarqs:agents:0.1',
      'io.github.lmarqs:agents:0.1.0-SNAPSHOT',
      'io.github.lmarqs:agents:[0.1,)',
      'io.github.lmarqs:agents:main',
      'io.github.lmarqs:agents:3f5a2c1',
      'acme:notes:1.2.3:notes',
    ]
    .each { text ->
      def error = shouldFail(IllegalArgumentException) { Coordinates.parse(text) }
      assert error.message.startsWith("Invalid recipe coordinates: ${text}")
    }
    assert Coordinates.written('acme:notes:1.2.3')
    assert !Coordinates.written('https://example.com/notes.groovy')
    assert !Coordinates.written('notes.groovy')
  }

  private static Throwable shouldFail(Class<? extends Throwable> type, Closure action) {
    try {
      action()
    } catch (Throwable error) {
      assert type.isInstance(error)
      return error
    }

    throw new AssertionError("Expected ${type.simpleName}")
  }
}
