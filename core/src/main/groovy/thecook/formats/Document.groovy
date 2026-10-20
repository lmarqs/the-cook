package thecook.formats

import thecook.rendering.RegionLocation

interface Document {
  String getText()
  boolean isEmpty()
  String read(String identity, RegionLocation location)

  /** `read`, before `content` replaces the location, so a refusal can say how `content` would be written. */
  String readBefore(String identity, RegionLocation location, String content)

  String render(Object value)
  void write(String identity, RegionLocation location, String content)
  void remove(String identity, RegionLocation location)
}
