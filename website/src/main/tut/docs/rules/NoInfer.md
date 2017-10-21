---
layout: docs
title: NoInfer
tag: rule
---

# NoInfer

_Since 0.5.0_

This rule reports errors when the compiler infers certain types.

Example:

```scala
MyCode.scala:7: error: [NoInfer.any] Inferred Any
  List(1, "")
            ^
```

## Configuration

By default the rule reports on the following types:

- `Serializable`
- `Any`
- `AnyVal`
- `Product`

It's possible to configure which types should not be inferred. In .scalafix.conf:

```scala
NoInfer.symbols = [
  "scala.Predef.any2stringadd"
]
```

and it would report 

```scala
MyCode.scala:7: error: [NoInfer.any2stringadd] Inferred any2stringadd
  def sum[A](a: A, b: String): String = { a + b }
                                          ^
```

**Note:** when a configuration for NoInfer is given it completely overwrites the defaults so they have to be explicitely added to the list of symbols if needed.


## Known limitations

- Scalafix does not yet expose a way to disable rules across regions of code, track [issue #241 for updates](https://github.com/scalacenter/scalafix/issues/241).
