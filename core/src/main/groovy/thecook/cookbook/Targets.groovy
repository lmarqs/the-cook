package thecook.cookbook

import thecook.formats.BlockDocument
import thecook.formats.KeyDocument
import thecook.formats.KeyDocuments
import thecook.model.Diagnostic
import thecook.reconciliation.Permissions

import java.nio.file.Paths

/**
 * A part of a destination file that a recipe owns: it writes the part, withdraws it, and is what `cook.lock` records.
 * Two targets are the same when the lock records them the same.
 */
abstract class Target {
  final String file

  protected Target(String file) {
    this.file = file
  }

  /** `section`, `key`, `file` or `link`; `cook.lock` groups targets by it. */
  abstract String getKind()

  /** What `cook.lock` records: a map of fields, or the path for a whole file or link. */
  abstract Object getEntry()

  /** Writes `value` into the draft as this part. */
  abstract void write(Draft draft, Object value)

  /** Removes this part from the draft. */
  abstract void withdraw(Draft draft)

  /**
   * `error` with this target named on the first line and every line of its cause indented beneath it, so a cause laid
   * out as what, why and next keeps its own indentation one level deeper.
   */
  final IllegalArgumentException failed(Exception error) {
    String cause = error.message ?: error.class.simpleName
    new IllegalArgumentException(([toString()] + cause.readLines().collect { "  ${it}" }).join('\n'), error)
  }

  /** Whether writing this target changes `other`: the same section, overlapping keys, or a whole file or link. */
  final boolean overlaps(Target other) {
    file == other.file && (whole || other.whole || other.getClass() == getClass() && sharesPart(other))
  }

  /** Whether the target is its whole file, so it overlaps every target in that file. */
  protected boolean isWhole() {
    false
  }

  /** Whether this target and `other`, of the same class and file, share a part. */
  @SuppressWarnings('UnusedMethodParameter')
  protected boolean sharesPart(Target other) {
    false
  }

  /** The text of a file this target edits in place; a symbolic link is refused, since its text would be its target. */
  protected String editable(Draft draft) {
    def target = draft.linkTarget(file)

    if (target != null) {
      throw new IllegalArgumentException(Diagnostic.text("${file} is a symbolic link to ${target}",
      'Cook writes sections and keys only into regular files'))
    }

    draft.read(file)
  }

  @Override
  boolean equals(Object other) {
    other instanceof Target && other.getClass() == getClass() && ((Target) other).entry == entry
  }

  @Override
  int hashCode() {
    Objects.hash(getClass(), entry)
  }
}

/** A marked section of a text file. */
final class SectionTarget extends Target {
  /** The marker id, `<recipe>/<region>`. */
  final String id
  // A recorded section has no declared comment; withdrawing it reads the comment from its marker.
  private final String comment
  private final boolean failsOnMissing

  SectionTarget(String file, String id, String comment = null, boolean failsOnMissing = false) {
    super(file)
    this.id = id
    this.comment = comment
    this.failsOnMissing = failsOnMissing
  }

  String getKind() {
    'section'
  }

  Object getEntry() {
    [file: file, id: id]
  }

  void write(Draft draft, Object lines) {
    if (failsOnMissing && draft.kind(file) == null) {
      throw new IllegalArgumentException(Diagnostic.text("${file} is missing",
      'the section goes into a file the team keeps, and the recipe never creates it',
      'create the file, or cook the recipe that writes it earlier in the same invocation'))
    }

    def document = blocks(editable(draft), comment)
    document.write(id, document.render(((List) lines).join('\n')))
    draft.replace(file, document)
  }

  void withdraw(Draft draft) {
    String text = editable(draft)
    def begin = ~/.*cook:begin ${java.util.regex.Pattern.quote(id)}(?:\s.*)?/
    def marker = text?.readLines()?.find { it ==~ begin }

    if (marker != null) {
      def document = blocks(text, marker.substring(0, marker.indexOf('cook:begin ')).trim())
      document.remove(id)
      draft.replace(file, document)
    }
  }

  @Override
  @SuppressWarnings('UnusedMethodParameter')
  protected boolean sharesPart(Target other) {
    id == ((SectionTarget) other).id
  }

  @Override
  String toString() {
    "${file} section ${id}"
  }

  private BlockDocument blocks(String text, String opening) {
    if (file ==~ /(?i).*\.(md|markdown)/ || opening == '<!--') {
      return new BlockDocument(text ?: '', '<!-- ', ' -->')
    }

    new BlockDocument(text ?: '', "${opening ?: '#'} ", '')
  }
}

/** A key of a JSON, TOML or YAML file, by its path of segments. */
final class KeyTarget extends Target {
  final List<String> path

  KeyTarget(String file, List<String> path) {
    super(file)
    this.path = path
  }

  String getKind() {
    'key'
  }

  Object getEntry() {
    [file: file, path: path]
  }

  /** Leaves a key that already holds `value` as it is, so the file keeps its formatting. */
  void write(Draft draft, Object value) {
    def document = open(draft)

    if (document.set(checkedPath(), value)) {
      draft.replace(file, document)
    }
  }

  void withdraw(Draft draft) {
    def document = open(draft)
    document.remove(checkedPath())
    draft.replace(file, document)
  }

  @Override
  @SuppressWarnings('UnusedMethodParameter')
  protected boolean sharesPart(Target other) {
    List<String> otherPath = ((KeyTarget) other).path
    path.take(otherPath.size()) == otherPath || otherPath.take(path.size()) == path
  }

  @Override
  String toString() {
    "${file} key ${identity}"
  }

  private String getIdentity() {
    path.join('.')
  }

  private KeyDocument open(Draft draft) {
    KeyDocuments.open(file, editable(draft))
  }

  private List<String> checkedPath() {
    if (path.isEmpty() || path.any { it.isEmpty() || it.contains('\n') }) {
      throw new IllegalArgumentException('Invalid key path')
    }

    path
  }
}

/** A whole file, with the permissions its declaration names. */
class FileTarget extends Target {
  // Permissions are written, not owned, so the lock does not record them.
  private final String mode

  FileTarget(String file, String mode = null) {
    super(file)
    this.mode = mode ?: null
  }

  String getKind() {
    'file'
  }

  Object getEntry() {
    file
  }

  void write(Draft draft, Object content) {
    draft.write(file, content.toString(), mode ? Permissions.parse(mode) : null)
  }

  void withdraw(Draft draft) {
    draft.delete(file)
  }

  @Override
  protected boolean isWhole() {
    true
  }

  @Override
  String toString() {
    file
  }
}

/**
 * A symbolic link. It replaces a link at its path, and refuses a file or directory there, whose contents would stop
 * being read.
 */
final class LinkTarget extends FileTarget {
  // Where the contents of a refused file or directory belong; the lock does not record it.
  private final String instead

  LinkTarget(String file, String instead = null) {
    super(file)
    this.instead = instead
  }

  @Override
  String getKind() {
    'link'
  }

  @Override
  void write(Draft draft, Object target) {
    def existing = draft.kind(file)

    if (existing in ['file', 'directory']) {
      def contents = existing == 'file' ? 'its contents' : 'its files'
      throw new IllegalArgumentException(Diagnostic.text(
      "${file} is a ${existing}, and this recipe makes it a link to ${target}",
      "${contents} would stop being read",
      "move ${contents} into ${instead ?: resolved(target.toString(), existing)}, delete it, and cook again"))
    }

    draft.link(file, target.toString())
  }

  private String resolved(String target, String existing) {
    def path = Paths.get(file)
        .resolveSibling(target)
        .normalize()

    if (path.absolute || path.startsWith('..')) {
      return target
    }

    existing == 'directory' ? "${path}/" : path.toString()
  }

  @Override
  String toString() {
    "${file} link"
  }
}
