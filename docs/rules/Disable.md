---
id: Disable
title: Disable
---

This rule reports errors when a "disallowed" symbol is referenced.

It has several different modes:

- Default mode. In this mode symbols are banned in the actual source code.
- `unlessInsideBlock` In this mode rule bans usages of "disallowed" symbols
  unless they appear in a "safe" block. Any inner blocks (e.g. anonymous
  functions or classes) within the given "safe" blocks are banned again, to
  avoid leakage.
- `ifSynthetic` This mode allows to block symbols in the generated code, for
  example expanded macros.

Example:

```scala
MyCode.scala:7: error: [DisallowSymbol.asInstanceOf] asInstanceOf is disabled.
  myValue.asInstanceOf[String]
          ^
```

## Configuration

This rule has several different options.

```scala mdoc:passthrough
import scalafix.internal.config._
```

```scala mdoc:passthrough
println(
scalafix.website.config[DisableConfig]("Disable")
)
println(
scalafix.website.config[UnlessInsideBlock]("UnlessInsideBlock")
)
println(
scalafix.website.defaults("Disable", DisableConfig.default)
)
println(
scalafix.website.examples[DisableConfig]("Disable")
)
```

The the example configuration above, Scalafix will report the following
warnings:

```scala
import scala.util.Try

object Test {
  val z = 1.asInstanceOf[String] // default mode, not ok

  List(1) + "any2stringadd" // ifSynthetic mode, not ok

  Option.empty[Int].get //  unlessInsideBlock mode, not ok
  Try {
    Option.empty[Int].get // ok
  }
}
```
