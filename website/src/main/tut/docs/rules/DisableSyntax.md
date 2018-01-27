---
layout: docs
title: DisableSyntax
---

# DisableSyntax

_Since 0.5.4_

This rule reports errors when a "disallowed" syntax is used.
This is a syntactic rule, which means it does not require compilation to
run unlike the `Disable` rule.

Example:

```scala
MyCode.scala:7: error: [DisableSyntax.xml] xml is disabled.
  <a>xml</a>
  ^
```

```scala
MyCode.scala:10: error: [DisableSyntax.keywords.return] return is disabled.
  return
  ^
```

## Configuration

```tut:invisible
import scalafix.internal.config._
```
```tut:passthrough
println(
scalafix.website.rule("DisableSyntax", DisableSyntaxConfig.default)
)
```

