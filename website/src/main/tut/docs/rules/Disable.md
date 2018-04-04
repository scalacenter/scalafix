---
layout: docs
title: Disable
---

# Disable

_Since 0.5.0_

This rule reports errors when a "disallowed" symbol is referenced.

Example:

```scala
MyCode.scala:7: error: [DisallowSymbol.asInstanceOf] asInstanceOf is disabled.
  myValue.asInstanceOf[String]
          ^
```

## Configuration

This rule has several different options.

<table><thead><tr><th>Name</th><th>Type</th><th>Description</th></tr></thead><tbody><tr><td><code>symbols</code></td><td>List[scalafix.CustomMessage[org.langmeta.Symbol.Global]]</td><td>The list of symbols to disable.</td></tr></tbody></table>#### Defaults

```
Disable.symbols = []
```

#### Examples

```
Disable.symbols = [
  # With custom message (recommended)
  {
    symbol = "scala.Predef.any2stringadd"
    message = "Use explicit toString before calling +"
  }
  {
    symbol = "scala.Any"
    message = "Explicitly type annotate Any if this is intentional"
  }
  # Without custom message (discouraged)
  "com.Lib.implicitConversion"
]
```

_Since 0.6.0_

This rule reports errors when a "disallowed" symbol is referenced.

It has several different modes:
- Default mode. In this mode symbols are banned in the actual source code.
- `unlessInsideBlock`
In this mode rule bans usages of "disallowed" symbols unless they appear in a "safe" block.
Any inner blocks (e.g. anonymous functions or classes)
within the given "safe" blocks are banned again, to avoid leakage.
- `ifSynthetic` This mode allows to block symbols in the generated code,
for example expanded macros.

Example:

```scala
MyCode.scala:7: error: [DisallowSymbol.asInstanceOf] asInstanceOf is disabled.
  myValue.asInstanceOf[String]
          ^
```

## Configuration

This rule has several different options.

```tut:invisible
import scalafix.internal.config._
```
```tut:passthrough
println(
scalafix.website.config[UnlessInsideBlock]("UnlessInsideBlock")
)
println(
scalafix.website.config[DisableConfig]("DisableConfig")
)
println(
scalafix.website.defaults("DisableConfig", DisableConfig.default)
)
println(
scalafix.website.examples[DisableConfig]("DisableConfig")
)
```

The the example configuration above, Scalafix will report the following warnings:
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
