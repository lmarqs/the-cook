package thecook

import org.junit.Test
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookRepository

class CookbookRepositoriesTest {
  @Test void choosesTheNewestReleaseWithinTheCompatibleRange() {
    [
      ['8.0.0', ['8.0.1', '8.1.1', '9.0.1'], '8.1.1', '9.0.1'],
      ['0.3.0', ['0.3.2', '0.4.0', '1.0.0'], '0.3.2', '1.0.0'],
      ['0.0.3', ['0.0.4', '0.1.0'], null, '0.1.0'],
    ].each { recorded, versions, compatible, newest ->
      def repositories = repositoriesWith(versions)
      def coordinates = Coordinates.parse("acme:notes:${recorded}#notes")
      def selection = repositories.selectRelease(coordinates, false)
      assert selection.known
      assert selection.target == (compatible ? coordinates.at(compatible) : null)
      assert selection.newestOverall == coordinates.at(newest)

      def unrestricted = repositories.selectRelease(coordinates, true)
      assert unrestricted.known
      assert unrestricted.target == coordinates.at(newest)
      assert unrestricted.newestOverall == null
    }
  }

  @Test void comparesNumericVersionsAndIgnoresNonReleasesAcrossRepositories() {
    def recorded = Coordinates.parse('acme:notes:9.0.0#notes')
    def repositories = repositoriesWith(['9.9.0', '9.1.0-SNAPSHOT'],
    ['9.10.0', '9.1.0', '10.0.0-SNAPSHOT', 'latest', '11.0', '12.0.0#other'])

    [false, true].each { major ->
      def selection = repositories.selectRelease(recorded, major)
      assert selection.known
      assert selection.target == recorded.at('9.10.0')
      assert selection.newestOverall == null
    }
    assert repositoriesWith(['9.10.0'], ['9.9.0']).selectRelease(recorded, false).target ==
    recorded.at('9.10.0')
  }

  @Test void neverChoosesAnEqualOrOlderRelease() {
    def recorded = Coordinates.parse('acme:notes:9.10.0')

    [[], ['9.9.0', '9.10.0', '09.010.0', '8.99.99', '10.0.0-SNAPSHOT']].each { versions ->
      [false, true].each { major ->
        def selection = repositoriesWith(versions).selectRelease(recorded, major)
        assert selection.known
        assert selection.target == null
        assert selection.newestOverall == null
      }
    }
    def absent = repositoriesWith().selectRelease(recorded, false)
    assert absent.known
    assert absent.target == null
    assert absent.newestOverall == null
  }

  @Test void reportsANewerMajorEvenWhenNoCompatibleUpdateExists() {
    def recorded = Coordinates.parse('acme:notes:8.1.1')
    def selection = repositoriesWith(['8.1.1', '9.0.1']).selectRelease(recorded, false)
    assert selection.known
    assert selection.target == null
    assert selection.newestOverall == recorded.at('9.0.1')
  }

  @Test void reportsUnknownWhenAnyRepositoryCannotListReleases() {
    def recorded = Coordinates.parse('acme:notes:8.0.0')

    [[null], [null, ['9.0.1']], [['9.0.1'], null]].each { listings ->
      [false, true].each { major ->
        def repositories = repositoriesWith(*listings)
        def selection = repositories.selectRelease(recorded, major)
        assert !selection.known
        assert selection.unlisted == repositories.repositories.findAll { it.listing == null }
        assert selection.target == null
        assert selection.newestOverall == null
      }
    }
  }

  private static CookbookRepositories repositoriesWith(List<String>... listings) {
    def repositories = (listings == null ? [null] : listings).collect { new ListedRepository(it) }
    new CookbookRepositories(repositories, 'test repositories')
  }

  private static final class ListedRepository implements CookbookRepository {
    private final List<String> listing

    ListedRepository(List<String> listing) {
      this.listing = listing
    }

    @Override CookbookArtifact artifact(Coordinates coordinates) {
      throw new AssertionError('Release selection must use listings')
    }

    @Override List<String> versions(String group, String artifact) {
      assert group == 'acme'
      assert artifact == 'notes'
      listing
    }
  }
}
