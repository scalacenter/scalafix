---
id: suppression
title: Suppressing rules
---

Sometimes you need to ignore the output from Scalafix. Scalafix provides two
methods for suppressing rules: annotations and comments.

- Annotations: great for disabling regions of code but some source code
  locations are impossible to annotate.
- Comments: great for targeting precise source code locations where annotations
  don't work.

## Annotations

It's possible to suppress Scalafix with `@SuppressWarnings` annotations. The
`@SuppressWarnings` annotation is supported by many different tools so it's
recommended to prefix the suppression with `scalafix:`.

For example, imagine we have configured the `DisableSyntax` rule to report
errors for the usage of `null` and `asInstanceOf` but we want to suppress those
errors inside a particular `getUser` method

```scala
@SuppressWarnings(Array(
  "scalafix:DisableSyntax.keywords.null",
  "scalafix:DisableSyntax.asInstanceOf"
))
def getUser(name: String): User = {
  if (name != null) database.getUser(name)
  else defaultPerson.asInstanceOf[User]
}
```

The `scalafix:` prefix can be left out if desired. However, when the `scalafix:`
prefix is included then you benefit from "unused suppression" warnings that
Scalafix reports for suppressions that can be removed.

> The `@SuppressWarnings` annotation is detected without compilation. Any
> annotation matching the syntax `@SuppressWarnings(..)` regardless if it's
> `java.lang.SuppressWarnings` or `com.example.SuppressWarnings` will will
> trigger the suppression mechanism. This is done in order to support
> `@SuppressWarnings` for syntactic rules like `DisableSyntax`.

### Suppress all rules with "all"

A convenient way to disable all rules is to inform the standard keyword `all` in
the annotation:

```scala
@SuppressWarnings(Array("all"))
def foo: Unit = {
    foo(null)
    1.asInstanceOf[String]
}
```

## Comments

It's possible to suppress Scalafix diagnostics and rewrites with
`// scalafix:on/off/ok` comments. Comments can target more source code precisely
than annotations since annotations are limited to definitions like `val` and
`class`. For example, comments can disable a group of methods or an entire
source file.

There are two alternatives to suppress rules via comments: single expression
using `scalafix:ok` and regions using the `scalafix:off`/`scalafix:on` toggles.

### Suppress expressions with `scalafix:ok`

Use `// scalafix:ok` comments to suppress Scalafix for an entire expression. The
`scalafix:ok` comment can appear after an expression like this

```scala
List(1, "") // scalafix:ok
```

The `scalafix:ok` comment can also appear before an expression like this

```scala
// scalafix:ok
List(1, "")
```

The benefit of `scalafix:ok` comments is that the suppression is automatically
enabled only for the range of the expression. There is no need for a separate
`scalafix:off`.

### Suppress regions with `scalafix:off` and `scalafix:on`

Use `// scalafix:off` and `// scalafix:on` comments to suppress Scalafix over an
arbitrary range of lines. This is the last resort when neither
`@SuppressWarnings` annotations or `scalafix:ok` comments are sufficient.

A `scalafix:off` can optionally be matched with a `scalafix:on` comment to
re-enable Scalafix

```scala
// scalafix:off
foo(null)
1.asInstanceOf[String]
// scalafix:on
```

A `scalafix:off` with no matching `scalafix:on` will suppress Scalafix for the
rest of the source file.

### Selectively suppress individual rules

Both `scalafix:ok` and `scalafix:off` can selectively disable a list of rules.
You can provide the list of rules to be disabled separated by commas:

```scala
// scalafix:off DisableSyntax.keywords.null, Disable.asInstanceOf
foo(null)
1.asInstanceOf[String]
// scalafix:on
```

```scala
List(1, "") // scalafix:ok Disable.Any
```

### Document reason for suppression

You can optionally include a description after a semicolon `;` for why a
suppression is needed. The description is only for informational purposes for
the person reading the code and will be ignored by Scalafix:

```scala
var x: Int = 0 // scalafix:ok DisableSyntax.keywords.var; I need mutability

// scalafix:off; temporarily disabling all rules until the code below gets refactored
```

## Unused suppression warnings

As code evolves, a `scalafix:ok` comment or `@SuppressWarnings("scalafix:Rule")`
annotation may become outdated. Scalafix reports warnings when it encounters
unused Scalafix suppressions, which you are safe to remove

```scala
project/Dependencies.scala:4:19: warning: Unused Scalafix suppression,
                                          this can be removed
[warn]   val x = List(1) // scalafix:ok
[warn]                   ^^^^^^^^^^^^^^
```

## Overlapping annotations and comments

Suppression via comments and `@SuppressWarnings` can be combined in the same
source file. Be mindful not to introduce overlaps between the two as it can
cause confusion and counter-intuitive behavior. Scalafix handles overlaps by
giving precedence to the `@SuppressWarnings` annotation:

```scala
@SuppressWarnings(Array("scalafix:DisableSyntax.keywords.null"))
def overlap(): Unit = {
  val a = null
  // scalafix:on DisableSyntax.keywords.null
  val b = null // rule is still disabled because the annotation takes precedence over the comment
}
```
