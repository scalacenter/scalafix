---
layout: docs
title: Disable
tag: rule
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

By default, this rule does allows all symbols. To disallow a symbol:

```scala
Disable.symbols = [
  "scala.Option.get"
  "scala.Any.asInstanceOf"
]
```
