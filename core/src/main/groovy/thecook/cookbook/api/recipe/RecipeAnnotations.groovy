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

/** An input bound from `--name=value`; required without a `default`, repeatable with `many`. */
@Target([ElementType.FIELD, ElementType.LOCAL_VARIABLE])
@Retention(RetentionPolicy.RUNTIME)
@interface param {
  String[] value()
  String doc()
  String[] 'default'() default []
  String[] choices() default []
  boolean many() default false
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
