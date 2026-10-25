package thecook

import org.junit.Test
import thecook.cookbook.Coordinates

class CoordinatesTest {
  @Test void comparesReleaseComponentsNumerically() {
    [
      ['9.10.0', '9.9.0'],
      ['10.0.0', '9.99.99'],
      ['9.1.10', '9.1.9'],
      ['999999999999999999999.0.0', '999999999999999999998.0.0'],
    ].each { versions ->
      assert Coordinates.compareVersions(versions[0], versions[1]) > 0
      assert Coordinates.compareVersions(versions[1], versions[0]) < 0
    }
    assert Coordinates.compareVersions('9.10.0', '9.10.0') == 0
    assert Coordinates.compareVersions('09.010.0', '9.10.0') == 0
  }

  @Test void changesOnlyTheVersionOfCoordinates() {
    ['acme:notes:1.2.3', 'acme:notes:1.2.3#notes'].each { text ->
      def recorded = Coordinates.parse(text)
      def target = recorded.at('2.0.0')
      assert target == Coordinates.parse(text.replace('1.2.3', '2.0.0'))
      assert recorded.toString() == text
      assert shouldFail(IllegalArgumentException) { recorded.at('2.0.0-SNAPSHOT') }
    }
  }

  @Test void parsesCoordinatesWithAnExactReleaseVersion() {
    def coordinates = Coordinates.parse('io.github.lmarqs.cookbook:agents:0.1.0#instructions')
    assert [coordinates.group, coordinates.artifact, coordinates.version, coordinates.recipe] ==
    ['io.github.lmarqs.cookbook', 'agents', '0.1.0', 'instructions']
    assert coordinates.jarPath == 'io/github/lmarqs/cookbook/agents/0.1.0/agents-0.1.0.jar'
    assert coordinates.recipeName == 'io.github.lmarqs.cookbook:agents#instructions'
    assert coordinates.toString() == 'io.github.lmarqs.cookbook:agents:0.1.0#instructions'
    def artifact = Coordinates.parse('io.github.lmarqs.cookbook:agents:0.1.0')
    assert artifact.recipe == null
    assert shouldFail(IllegalArgumentException) { artifact.recipeName }.message ==
    'io.github.lmarqs.cookbook:agents:0.1.0 names an artifact, not a recipe\n  write ' +
    'io.github.lmarqs.cookbook:agents:0.1.0#<recipe>'
    assert artifact.recipeAddress('permissions') == 'io.github.lmarqs.cookbook:agents:0.1.0#permissions'
    [
      'io.github.lmarqs.cookbook:agents',
      'io.github.lmarqs.cookbook:agents#connect',
      'io.github.lmarqs.cookbook:agents:0.1',
      'io.github.lmarqs.cookbook:agents:0.1.0-SNAPSHOT',
      'io.github.lmarqs.cookbook:agents:[0.1,)',
      'io.github.lmarqs.cookbook:agents:main',
      'io.github.lmarqs.cookbook:agents:3f5a2c1',
      'acme:notes:1.2.3:notes',
    ]
    .each { text ->
      def error = shouldFail(IllegalArgumentException) { Coordinates.parse(text) }
      assert error.message.startsWith("Invalid recipe coordinates: ${text}")
      // The example names a recipe the agents artifact has.
      assert error.message.endsWith('such as io.github.lmarqs.cookbook:agents:1.2.3#seed')
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
