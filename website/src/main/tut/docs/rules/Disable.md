---
layout: docs
title: Disable
---

# Disable

_Since 0.5.0_

This rule reports errors when a "disallowed" symbol is referenced.

Example:

```scala
MyCode.scala:7: error: [Disable.asInstanceOf] asInstanceOf is disabled.
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

_Since 0.5.4_

# Disable.keywords

This rule reports errors when a "disallowed" keyword is referenced.

Example:
 
```scala
MyCode.scala:7: error: [Disable.null] Some constructs are unsafe to use and should be avoided
  null
  ^
```

## Configuration

The following Scala keywords are supported:

Disable.keywords = [
  "null"
  "return"
  "throw"
  "var"
  "while"
]