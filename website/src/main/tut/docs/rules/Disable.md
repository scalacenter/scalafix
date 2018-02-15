---
layout: docs
title: Disable
---

# Disable

_Since 0.6.0_

This rule reports errors when a "disallowed" symbol is referenced.

It has several different modes:
- Default mode. In this mode symbols are banned in the actual source code.
- `unlessInsideBlock`
In this mode rule bans usages of "disallowed" symbols unless they appear in a "safe" block.
Any inner blocks (e.g. anonymous functions or classes)
within the given "safe" blocks are banned again, to avoid leakage.
- `unlessSynthetic` This mode allows to block symbols in the generated code,
for example expanded macros.

Example:

```scala
MyCode.scala:7: error: [DisallowSymbol.asInstanceOf] asInstanceOf is disabled.
  myValue.asInstanceOf[String]
          ^
```

## Configuration

This rule has several different options.

#### UnlessInsideBlock

<table><thead><tr><th>Name</th><th>Type</th><th>Description</th></tr></thead><tbody><tr><td><code>unless</code></td><td>Symbol</td><td>The symbol that indicates a 'safe' block.</td></tr><tr><td><code>symbols</code></td><td>List[Message[Symbol]]</td><td>The unsafe symbols that are banned unless inside a 'safe' block</td></tr></tbody></table>
#### DisableConfig

<table><thead><tr><th>Name</th><th>Type</th><th>Description</th></tr></thead><tbody><tr><td><code>symbols</code></td><td>List[Message[Symbol]]</td><td>The list of symbols to disable only in the actual sources.</td></tr><tr><td><code>unlessSynthetic</code></td><td>List[Message[Symbol]]</td><td>The list of symbols to disable, also blocks symbols in the generated code</td></tr><tr><td><code>unlessInsideBlock</code></td><td>List[UnlessInsideBlock]</td><td>The list of symbols to disable unless they are in the given block.</td></tr></tbody></table>
#### Defaults

```
DisableConfig.symbols = []
DisableConfig.unlessSynthetic = []
DisableConfig.unlessInsideBlock = []
```


#### Examples

```
DisableConfig.symbols =
[
  {
    symbol = "scala.Any.asInstanceOf"
    message = "use patter-matching instead"
  }
]
DisableConfig.unlessSynthetic =
[
  {
    symbol = "scala.Predef.any2stringadd"
    message = "use explicit toString be calling +"
  }
]
DisableConfig.unlessInsideBlock =
[
  {
    unless = "scala.util.Try"
    symbols = [
      {
        symbol = "scala.Option.get"
        message = "the function may throw an exception"
      }
    ]
  }
]

```

The the example configuration above, Scalafix will report the following warnings:
```scala
import scala.util.Try

object Test {
  val z = 1.asInstanceOf[String] // default mode, not ok

  List(1) + "any2stringadd" // unlessSynthetic mode, not ok

  Option.empty[Int].get //  unlessInsideBlock mode, not ok
  Try {
    Option.empty[Int].get // ok
  }
}
```

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

```tut:invisible
import scalafix.internal.config._
```
```tut:passthrough
println(
scalafix.website.rule("Disable", DisableConfig.default)
)
```
