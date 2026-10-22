package thecook.cookbook.api.recipe

// Recipe annotations are lower case so they read as keywords in a recipe. They live apart from the ingredient
// annotations in `thecook.cookbook.api.ingredient` because `@ingredient` and `@Ingredient` would share one class file
// on a file system that ignores case.

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/** The recipe's description, in Markdown, on its first import. */
@Retention(RetentionPolicy.RUNTIME)
@interface help {
  String value()
}

/** A consequence the diff cannot show, on the recipe's first import. */
@Retention(RetentionPolicy.RUNTIME)
@interface confirm {
  String value()
}

/** What to do after the writes succeed, on the recipe's first import. */
@Retention(RetentionPolicy.RUNTIME)
@interface yield {
  String value()
}

/**
 * An input bound from `--name=value`; required without a `default`, repeatable with `many`, which binds the values
 * without duplicates, in the order given, or sorted with `sorted`. With `path`, each value is a path from the
 * repository root, refused when blank, absolute or holding a `..` segment.
 */
@Target([ElementType.FIELD, ElementType.LOCAL_VARIABLE])
@Retention(RetentionPolicy.RUNTIME)
@interface param {
  String[] value()
  String doc()
  String[] 'default'() default []
  String[] choices() default []
  boolean many() default false
  boolean sorted() default false
  boolean path() default false
}

/** An injected model instance, at the destination root or at the directory `at`. */
@Target([ElementType.FIELD, ElementType.LOCAL_VARIABLE])
@Retention(RetentionPolicy.RUNTIME)
@interface ingredient {
  Class value()
  String at() default '.'
}

/** The UTF-8 text of the file at `value`, beside a recipe path or in the recipe's artifact. */
@Target([ElementType.FIELD, ElementType.LOCAL_VARIABLE])
@Retention(RetentionPolicy.RUNTIME)
@interface resource {
  String value()
}

/**
 * The recipe's own name, as its section markers and `cook.lock` carry it: its coordinates without the version, or its
 * place in the kitchen, such as `recipes/setup`. With `arguments`, the name is followed by each param the recipe was
 * given, in declaration order, as bound, such as `recipes/setup --agent=claude --file=a.md`: the argument line its
 * owned sections open with.
 */
@Target([ElementType.FIELD, ElementType.LOCAL_VARIABLE])
@Retention(RetentionPolicy.RUNTIME)
@interface recipe {
  boolean arguments() default false
}
