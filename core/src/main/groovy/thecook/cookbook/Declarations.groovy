package thecook.cookbook

import thecook.cookbook.api.ingredient.Delete
import thecook.cookbook.api.ingredient.File as WholeFile
import thecook.cookbook.api.ingredient.Files
import thecook.cookbook.api.ingredient.Key
import thecook.cookbook.api.ingredient.Keys
import thecook.cookbook.api.ingredient.Links
import thecook.cookbook.api.ingredient.Section
import thecook.cookbook.api.ingredient.Sections
import thecook.cookbook.api.ingredient.Seed
import thecook.formats.KeyDocuments
import thecook.reconciliation.Permissions

import java.lang.annotation.Annotation
import java.lang.reflect.AccessibleObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/** Reads what model instances declare through their annotated members. */
final class Declarations {
  private static final List<Class<? extends Annotation>> KINDS =
  [Seed, Section, Sections, Key, Keys, WholeFile, Files, Links, Delete]

  private Declarations() { }

  /** `path` below `directory`, the instance base with `.` at the destination root. */
  static String join(String directory, String path) {
    if (path == '.') {
      return directory
    }

    directory in ['.', ''] ? path : "${directory}/${path}"
  }

  /**
   * Everything `models` declare when the recipe `recipe` uses them, member by member in name order; a member
   * returning `null` declares nothing.
   */
  static Declared of(List<CookbookModel> models, String recipe) {
    def declared = new Declared()
    models.each { model ->
      members(model.getClass()).each { member ->
        def value = read(model, member)

        if (value != null) {
          def annotation = KINDS.findResult { member.getAnnotation(it) }
          add(declared, model, recipe, "${model.getClass().simpleName}.${name(member)}", annotation, value)
        }
      }
    }
    declared
  }

  private static List<AccessibleObject> members(Class type) {
    List<AccessibleObject> found = []

    for (Class current = type; current != null && current != CookbookModel && current != Object;
        current = current.superclass) {
      def methods = current.declaredMethods.findAll { !it.synthetic && !it.bridge && it.parameterCount == 0 }
      found.addAll((methods + current.declaredFields.toList()).findAll { member ->
        KINDS.any {
          member.getAnnotation(it)
        }
      }
      .sort { name(it) })
    }

    found
  }

  private static String name(AccessibleObject member) {
    member instanceof Method ? "${member.name}()" : member.name
  }

  private static Object read(Object instance, AccessibleObject member) {
    member.accessible = true

    try {
      member instanceof Method ? member.invoke(instance) : member.get(instance)
    } catch (InvocationTargetException error) {
      throw error.cause
    }
  }

  private static void add(Declared declared, CookbookModel model, String recipe, String member, Annotation annotation,
      Object value) {
    String base = join(model.directory, expand(model, member, annotation.path()))

    switch (annotation) {
      case Seed:
        declared.seeds << new Seeding(member, base, value.toString(), failsOnExisting(member, (Seed) annotation),
        mode(member, ((Seed) annotation).mode()))
        break
      case Delete:
        if (value == true) {
          declared.deletes << new Deletion(member, base)
        }

        break
      case Section:
        def section = (Section) annotation
        def target = new SectionTarget(base, "${recipe}/${section.id()}".toString(), section.comment() ?: null)
        declared.owned << new Declaration(member, target, lines(member, value))
        break
      case Sections:
        def sections = (Sections) annotation
        boolean failsOnMissing = failsOnMissing(member, sections)
        entries(member, value).each { file, content ->
          def target = new SectionTarget(join(base, file.toString()), "${recipe}/${sections.id()}".toString(),
              sections.comment() ?: null, failsOnMissing)
          declared.owned << new Declaration(member, target, lines(member, content))
        }
        break
      case Key:
        declared.owned << new Declaration(member, key(member, base, ((Key) annotation).segments().toList()), value)
        break
      case Keys:
        def under = ((Keys) annotation).under().toList()
        entries(member, value).each { name, entry ->
          def path = under + (name instanceof List ? name*.toString() : [name.toString()])
          declared.owned << new Declaration(member, key(member, base, path), entry)
        }
        break
      case WholeFile:
        declared.owned << new Declaration(member, new FileTarget(base, mode(member, ((WholeFile) annotation).mode())),
        value.toString())
        break
      case Files:
        String mode = mode(member, ((Files) annotation).mode())
        entries(member, value).each { name, content ->
          def target = new FileTarget(join(base, name.toString()), mode)
          declared.owned << new Declaration(member, target, content.toString())
        }
        break
      case Links:
        String instead = ((Links) annotation).instead() ?: null
        entries(member, value).each { name, target ->
          def link = new LinkTarget(join(base, name.toString()), instead)
          declared.owned << new Declaration(member, link, target.toString())
        }
        break
      default:
        throw new IllegalStateException("${annotation.annotationType().simpleName} declares nothing")
    }
  }

  // The permissions a member's `mode` names, such as `0755`, in the `rwxr-xr-x` form the draft writes.
  private static String mode(String member, String mode) {
    if (!mode) {
      return null
    }

    try {
      Permissions.parse(mode)
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("${member}: ${error.message}", error)
    }
  }

  private static boolean failsOnExisting(String member, Seed seed) {
    if (!(seed.existing() in ['keep', 'fail'])) {
      throw new IllegalArgumentException("${member}: existing = '${seed.existing()}' is not keep or fail")
    }

    seed.existing() == 'fail'
  }

  private static boolean failsOnMissing(String member, Sections sections) {
    if (!(sections.missing() in ['create', 'fail'])) {
      throw new IllegalArgumentException("${member}: missing = '${sections.missing()}' is not create or fail")
    }

    sections.missing() == 'fail'
  }

  private static KeyTarget key(String member, String file, List<String> path) {
    if (!KeyDocuments.format(file)) {
      throw new IllegalArgumentException("${member} declares a key in ${file}, which is not a JSON, TOML or YAML file")
    }

    new KeyTarget(file, path)
  }

  private static String expand(Object instance, String member, String path) {
    path.replaceAll(/\$\{(\w+)}/) { all, property ->
      def value = instance."${property}"

      if (value == null) {
        throw new IllegalArgumentException("${member}: the path ${path} reads ${property}, which is not set")
      }

      value.toString()
    }
  }

  private static List<String> lines(String member, Object value) {
    if (!(value instanceof Collection)) {
      throw new IllegalArgumentException("${member} must return a list of lines")
    }

    value*.toString()
  }

  private static Map entries(String member, Object value) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException("${member} must return a map")
    }

    (Map) value
  }
}

/** What models declare: the parts they own, the files they seed and the files they delete. */
final class Declared {
  final List<Declaration> owned = []
  final List<Seeding> seeds = []
  final List<Deletion> deletes = []

  /** Every declaration, each read as `<member> declares ...`. */
  List<Object> getAll() {
    seeds + owned + deletes
  }
}

/** A part a model member declares: the target it owns and the value written there. */
final class Declaration {
  final String member
  final Target target
  final Object value

  Declaration(String member, Target target, Object value) {
    this.member = member
    this.target = target
    this.value = value
  }

  @Override
  String toString() {
    "${member} declares a ${target.kind} of ${target.file}"
  }
}

/**
 * A file a model member writes only when it does not exist yet, and never owns. `failsOnExisting` refuses the recipe
 * the first time it is cooked, when the file exists. `mode` sets the permissions of the file it writes.
 */
final class Seeding {
  final String member
  final String file
  final String text
  final boolean failsOnExisting
  final String mode

  Seeding(String member, String file, String text, boolean failsOnExisting, String mode) {
    this.member = member
    this.file = file
    this.text = text
    this.failsOnExisting = failsOnExisting
    this.mode = mode
  }

  @Override
  String toString() {
    "${member} declares a seed of ${file}"
  }
}

/** A file a model member deletes. */
final class Deletion {
  final String member
  final String file

  Deletion(String member, String file) {
    this.member = member
    this.file = file
  }

  @Override
  String toString() {
    "${member} declares a delete of ${file}"
  }
}
