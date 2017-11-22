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

_Since 0.5.3_

## Custom error messages

It's possible to provide a custom error message.

```
scala/test/Disable.scala:47: error: [Disable.get]
Option.get is the root of all evils

If you really want to do this do:
// scalafix:off Option.get
...
// scalafix:on Option.get
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
   |// scalafix:off Option.get
   |...
   |// scalafix:on Option.get"""

  }
]
````