---
id: RemoveUnused
title: RemoveUnused
---

```scala mdoc:rule:RemoveUnused

```

See [slick/slick/pulls#1736](https://github.com/slick/slick/pull/1736) for an
example diff from running `sbt "scalafix RemoveUnused"`.

## Installation

To use this rule:

- Enable the Scala compiler option `-Ywarn-unused`. In sbt, this is done with
  `scalacOptions += "-Ywarn-unused"`.
- Disable `-Xfatal-warnings` if you have it enabled. This is required so the
  compiler warnings do not fail the build before running Scalafix.
  Unfortunately, it's not possible exclude unused imports from -Xfatal-warnings.

## Examples

Remove unused imports:

```scala
// before
import scala.List
import scala.collection.{immutable, mutable}
object Foo { immutable.Seq.empty[Int] }

// after
import scala.collection.immutable
object Foo { immutable.Seq.empty[Int] }
```

Remove unused local variables:

```scala
// before
def app = {
  val unused = "message"
  println("Hello world!")
}
// after
def app = {
  println("Hello world!")
}
```

Remove unused private variables:

```scala
// before
object Main {
  private def unused = "remove me"
  def main() = println("Hello!")
}
// after
object Main {
  def main() = println("Hello!")
}
```

## Formatting

> This rule does a best-effort at preserving original formatting. In some cases,
> the rewritten code may be formatted weirdly

```scala
// before
import scala.concurrent.{
  CancellationException, // A comment
  TimeoutException
}
// after
import scala.concurrent. // A comment
TimeoutException
```

It's recommended to use a code formatter like
[Scalafmt](https://scalameta.org/scalafmt/) after running this rule.

## Configuration

```scala mdoc:passthrough
import scalafix.internal.rule._
```

```scala mdoc:passthrough
println(scalafix.website.rule("RemoveUnused", RemoveUnusedConfig.default))
```

## -Ywarn-unused

Consult `scala -Y` in the command-line for more information about using
`-Ywarn-unused`.

For Scala @SCALA212@

```
$ scala -Ywarn-unused:help
Enable or disable specific `unused' warnings
  imports    Warn if an import selector is not referenced.
  patvars    Warn if a variable bound in a pattern is unused.
  privates   Warn if a private member is unused.
  locals     Warn if a local definition is unused.
  explicits  Warn if an explicit parameter is unused.
  implicits  Warn if an implicit parameter is unused.
  params     Enable -Ywarn-unused:explicits,implicits.
  linted     -Xlint:unused.
Default: All choices are enabled by default.
```

For Scala @SCALA211@

```
$ scala -Y | grep warn-unused
  -Ywarn-unused                           Warn when local and private vals, vars, defs, and types are unused.
  -Ywarn-unused-import                    Warn when imports are unused.
```
