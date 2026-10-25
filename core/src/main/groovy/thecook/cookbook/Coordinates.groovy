package thecook.cookbook

import thecook.model.Diagnostic

import groovy.transform.EqualsAndHashCode

import java.util.regex.Matcher

/**
 * An address, `group:artifact:version[#recipe]`: one artifact of a cookbook release, or one recipe in it. Only exact
 * release versions are accepted, so an address names the same jar wherever it resolves.
 */
@EqualsAndHashCode
final class Coordinates {
  private static final String PATTERN =
  /([a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)*):([a-z][a-z0-9-]*):([0-9]+\.[0-9]+\.[0-9]+)(?:#([a-z][a-z0-9-]*))?/

  final String group
  final String artifact
  final String version
  /** The recipe in the artifact, or `null` for the artifact itself. */
  final String recipe

  private Coordinates(String group, String artifact, String version, String recipe) {
    this.group = group
    this.artifact = artifact
    this.version = version
    this.recipe = recipe
  }

  /** Whether `operand` is written as coordinates rather than as a path, a name or a URL. */
  static boolean written(String operand) {
    operand.contains(':') && !operand.contains('://') && !operand.endsWith('.groovy')
  }

  /** Whether `text` is coordinates that `parse` accepts. */
  static boolean valid(String text) {
    text ==~ "^${PATTERN}\$"
  }

  /** Parses `text`, rejecting anything but `group:artifact:version[#recipe]` with an exact release version. */
  static Coordinates parse(String text) {
    Matcher match = text =~ "^${PATTERN}\$"

    if (!match.matches()) {
      throw new IllegalArgumentException(Diagnostic.text("Invalid recipe coordinates: ${text}",
      'expected group:artifact:version[#recipe] with an exact release version, such as ' +
      'io.github.lmarqs.cookbook:agents:1.2.3#seed'))
    }

    new Coordinates(match.group(1), match.group(2), match.group(3), match.group(4))
  }

  /** The artifact's address, without the recipe. */
  String getArtifactAddress() {
    "${group}:${artifact}:${version}"
  }

  /** The short name of the recipe this address names: `group:artifact#recipe`, the address without its version. */
  String getRecipeName() {
    if (!recipe) {
      throw new IllegalArgumentException(
      Diagnostic.text("${this} names an artifact, not a recipe", null, "write ${this}#<recipe>"))
    }

    "${group}:${artifact}#${recipe}"
  }

  /** The address of `name`, a recipe in this artifact. */
  String recipeAddress(String name) {
    "${artifactAddress}#${name}"
  }

  /** The jar's path inside a Maven-layout repository. */
  String getJarPath() {
    "${group.replace('.', '/')}/${artifact}/${version}/${artifact}-${version}.jar"
  }

  /** The directory holding the artifact's files inside its jar: its package path. */
  String getPackagePath() {
    "${group.replace('.', '/')}/${artifact}/"
  }

  @Override String toString() {
    recipe ? recipeAddress(recipe) : artifactAddress
  }
}
