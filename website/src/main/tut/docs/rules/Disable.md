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

```tut:invisible
import scalafix.internal.config._
```
```tut:passthrough
println(
scalafix.website.rule("Disable", DisableConfig.default)
)
```

