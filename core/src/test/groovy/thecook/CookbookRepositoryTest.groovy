package thecook

import org.junit.Test
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookRepository

class CookbookRepositoryTest {
  @Test void defaultsToAnEmptyReleaseListing() {
    def repository = new ArtifactOnlyRepository()

    assert repository.versions('acme', 'notes') == []
    assert CookbookRepository.getMethod('versions', String, String).isDefault()
  }

  private static final class ArtifactOnlyRepository implements CookbookRepository {
    @Override CookbookArtifact artifact(Coordinates coordinates) {
      null
    }
  }
}
