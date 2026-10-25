package thecook.cookbook;

import java.util.Collections;
import java.util.List;

/** A repository that cookbook artifacts resolve from. Its {@code toString} names it in messages. */
public interface CookbookRepository {
  /** The artifact that {@code coordinates} name, or {@code null} when this repository does not hold it. */
  CookbookArtifact artifact(Coordinates coordinates);

  /**
   * The releases listed for {@code group} and {@code artifact}, an empty list when this repository does not hold the
   * artifact, or {@code null} when it holds the artifact but cannot list its releases.
   */
  default List<String> versions(String group, String artifact) {
    return Collections.emptyList();
  }
}
