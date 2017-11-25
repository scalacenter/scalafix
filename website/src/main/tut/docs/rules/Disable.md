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

By default, this rule does allows all symbols. To disallow a symbol:

```scala
Disable.symbols = [
  "scala.Option.get"
  "scala.Any.asInstanceOf"
]
```

## Custom error messages

_Since 0.5.4_

It's possible to provide a custom error message.

```
scala/test/Disable.scala:47: error: [Disable.get]
Option.get is the root of all evils

If you really want to do this do:
... // scalafix:ok Option.get
  Option(1).get
            ^
```

```
rules = Disable
Disable.symbols = [
  "scala.Any.asInstanceOf"
  "test.Disable.D.disabledFunction"
  {
    symbol = "scala.Option.get"
    message = 
"""|Option.get is the root of all evils
   |
   |If you really want to do this do:
   |... // scalafix:ok Option.get"""

  }
]
````