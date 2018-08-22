---
id: configuration
title: Configuration
---

## .scalafix.conf

Scalafix reads configuration from a file using
[HOCON](https://github.com/typesafehub/config) syntax. The convention is to keep
a file `.scalafix.conf` into the root directory of your project. Configuration
is not needed or is optional for most rules, so you may not need to create a
`.scalafix.conf`.

### rules

Configure which rule to run with `rules = [ ... ]`, for example

```scala
// Built in rules
rules = [
  // pre-installed rule from scalafix
  ExplicitResultTypes
  // custom rule on local disk
  "file:rules/MyRule.scala"
  // custom rule from library on GitHub
  "github:typelevel/cats/v1.0.0"
  // custom rule shared as a gist
  "https://git.io/vNXDG"
  // ...
]
```

Rules are referenced using URI syntax. By default, a URI with no scheme such as
`ExplicitResultTypes` is interpreted as a built-in Scalafix rule. See
[here](rules/overview.md) for the complete list of built-in Scalafix rules.
Scalafix supports loading custom rules with the following URI schemes: `scala:`,
`file:`, `http:` and `github:`.

### scala:

If a scalafix rule is already on the classpath, you can classload it with the
`scala:` protocol.

```scala
rule = "scala:scalafix.internal.rule.ProcedureSyntax"
```

### file:

If a rule is written in a single file on local disk, you can load it with the
`file:` protocol.

```scala
rule = "file:readme/MyRule.scala" // from local file on disk
```

### github:

If a rule is written in a single file and you use GitHub, you can use the
`github:` protocol for sharing your rule

```scala
rule = "github:typelevel/cats/v1.0.0"
// expands into "https://raw.githubusercontent.com/typelevel/cats/master/scalafix/rules/src/main/scala/fix/Cats_v1_0_0.scala"
```

### http:

If a rule is written in a single source file on the internet, you can load it
with the `https:` or `http:` protocol

```scala
rule = "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/f5fe47495c9b6e3ce0960b766ffa75be6d6768b2/DummyRule.scala"
```

### replace:

To replace usage of one class/object/trait/def with another. Note, does not move
definitions like "Move" does in an IDE. This only moves use-sites.

```scala
rule = "replace:com.company.App/io.company.App"
// From sbt shell: > scalafix replace:from/to
```

To rename a method

```scala
rule = "replace:com.company.App.start/init"
```

## Suppressing rules

Sometimes there are legitimate reasons for violating a given rule. In order to
cater for that, Scalafix provides two methods for suppressing rules over a
particular region of code.

### @SuppressWarnings

The `java.lang.SupppressWarnings` annotation is a standard way to suppress
messages from the compiler and is used by many linters. Scalafix supports this
annotation in any definition where the Scala language allows it to be placed
(classes, traits, objects, types, methods, constructors, parameters, vals and
vars).

One or more rules can be specified in the `@SuppressWarnings` annotation.
Although not mandatory, it's recommended to prefix the rule names with
`scalafix:` so that they can be easily identified. Another reason for prefixing
is that Scalafix can report warnings for unused/redundant rules.

```scala
@SuppressWarnings(Array(
  "scalafix:DisableSyntax.keywords.null",
  "scalafix:Disable.asInstanceOf"
))
def foo: Unit = {
    foo(null)
    1.asInstanceOf[String]
}
```

A convenient way to disable all rules is to inform the standard keyword `all` in
the annotation:

```scala
@SuppressWarnings(Array("all"))
def foo: Unit = {
    foo(null)
    1.asInstanceOf[String]
}
```

**Note:** The `@SuppressWarnings` annotation is detected without compilation.
Any annotation matching the syntax `@SuppressWarnings(..)` regardless if it's
`java.lang.SupressWarnings` or not will trigger the suppression mechanism. This
is done in order to support `@SuppressWarnings` for syntactic rules like
`DisableSyntax`.

### // scalafix: off

Comments allow code to be targeted more precisely than annotations. It's
possible to target from a single line of code to a region (e.g. a group of
methods) or the entire source file. Moreover, there are things that simply
cannot be annotated in Scala (e.g. a class name: you would have to disable the
rule for the entire class). For such scenarios, comments can be the best or the
only choice.

There are two alternatives to suppress rules via comments: single expression
using `scalafix:ok` and regions using the `scalafix:off`/`scalafix:on` toggles.

- single expression

```scala
List(1, "") // scalafix:ok
```

- region

```scala
// scalafix:off
foo(null)
1.asInstanceOf[String]
// scalafix:on
```

Both techniques can selectively disable a list of rules. You can provide the
list of rules to be disabled separated by commas:

```scala
// scalafix:off DisableSyntax.keywords.null, Disable.asInstanceOf
foo(null)
1.asInstanceOf[String]
// scalafix:on
```

```scala
List(1, "") // scalafix:ok Disable.Any
```

Optionally, you can include an arbitrary description at the end of any
suppression comment, following a semicolon. The description is only for
informational purposes and will be completely ignored by Scalafix:

```scala
var x: Int = 0 // scalafix:ok DisableSyntax.keywords.var; I need mutability

// scalafix:off; temporarily disabling all rules until the code below gets refactored
```

**Note:** Suppression via comments and `@SuppressWarnings` can be combined in
the same source file. Be mindful not to introduce overlaps between the two as it
can cause confusion and counter-intuitive behavior. Scalafix handles overlaps by
giving precedence to the `@SuppressWarnings` annotation:

```scala
@SuppressWarnings(Array("scalafix:DisableSyntax.keywords.null"))
def overlap(): Unit = {
  val a = null
  // scalafix:on DisableSyntax.keywords.null
  val b = null // rule is still disabled because the annotation takes precedence over the comment
}
```
