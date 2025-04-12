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

- Enable the Scala compiler option `-Ywarn-unused` (2.12), `-Wunused` (2.13),
  or `-Wunused:all` (3.3.4+).
- Disable `-Xfatal-warnings` if you have it enabled. This is required so the
  compiler warnings do not fail the build before running Scalafix. If you are
  running 2.12.13+ or 2.13.2+, you may keep `-Xfatal-warnings` by modifying how
  specific warnings are handled via `scalacOptions += "-Wconf:cat=unused:info"`.

## Examples

### Remove unused imports

```scala
// before
import scala.List
import scala.collection.{immutable, mutable}
object Foo { immutable.Seq.empty[Int] }

// after
import scala.collection.immutable
object Foo { immutable.Seq.empty[Int] }
```

### Remove unused local variables

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

### Remove unused private variables

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

### Remove unused pattern match variables

```scala
case class AB(a: Int, b: String)
// before
object Main {
  val example = AB(42, "lol")
  example match {
    case AB(a, b) => println("Not used")
  }
}
// after
object Main {
  val example = AB(42, "lol")
  example match {
    case AB(_, _) => println("Not used")
  }
}
```

On Scala 3, `-Wunused:unsafe-warn-patvars` is required.

On Scala 2.13.15+, canonical patterns (vars with the same names as the
attributes) do not trigger unused warnings, so the input above will not
be rewritten. See https://github.com/scala/bug/issues/13035.

### Remove unused function parameters

```scala
// before
object Main {
  val f: String => Unit = unused => println("f")
}
// after
object Main {
  val f: String => Unit = _ => println("f")
}
```

On Scala 3, this is only supported for 3.7.0+.

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

## More granular scalac options

You may request more granular warnings to the compiler if you opt-out
from some rewrites in the rule configuration.

```
$ scala212 -Ywarn-unused:help
Enable or disable specific `unused' warnings
  imports    Warn if an import selector is not referenced.
  patvars    Warn if a variable bound in a pattern is unused.
  privates   Warn if a private member is unused.
  locals     Warn if a local definition is unused.
  explicits  Warn if an explicit parameter is unused.
  implicits  Warn if an implicit parameter is unused.
  nowarn     Warn if a @nowarn annotation does not suppress any warnings.
  params     Enable -Ywarn-unused:explicits,implicits.
  linted     -Xlint:unused.
Default: All choices are enabled by default.
```

```
$ scala213 -Wunused:help
Enable or disable specific `unused` warnings
  imports     Warn if an import selector is not referenced.
  patvars     Warn if a variable bound in a pattern is unused.
  privates    Warn if a private member is unused.
  locals      Warn if a local definition is unused.
  explicits   Warn if an explicit parameter is unused.
  implicits   Warn if an implicit parameter is unused.
  synthetics  Warn if a synthetic implicit parameter (context bound) is unused.
  nowarn      Warn if a @nowarn annotation does not suppress any warnings.
  params      Enable -Wunused:explicits,implicits,synthetics.
  linted      -Xlint:unused.
Default: All choices are enabled by default.
```

```
$ scala3 -W
...
-Wunused  Enable or disable specific `unused` warnings
          Choices :
          - nowarn,
          - all,
          - imports :
            Warn if an import selector is not referenced.,
          - privates :
            Warn if a private member is unused,
          - locals :
            Warn if a local definition is unused,
          - explicits :
            Warn if an explicit parameter is unused,
          - implicits :
            Warn if an implicit parameter is unused,
          - params :
            Enable -Wunused:explicits,implicits,
          - patvars :
            Warn if a variable bound in a pattern is unused,
          - linted :
            Enable -Wunused:imports,privates,locals,implicits,
          - strict-no-implicit-warn :
            Same as -Wunused:import, only for imports of explicit named members.
            NOTE : This overrides -Wunused:imports and NOT set by -Wunused:all,
          - unsafe-warn-patvars :
            Deprecated alias for `patvars`
```
