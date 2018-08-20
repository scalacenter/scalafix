---
layout: docs
id: DisableSyntax
title: DisableSyntax
---

This rule reports errors when a "disallowed" syntax is used. This is a syntactic
rule, which means it does not require compilation to run unlike the `Disable`
rule.

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

By default, this rule does not disable any particular syntax, every setting is
opt-in.

```scala mdoc:passthrough
import scalafix.internal.config._
```

```scala mdoc:passthrough
println(
scalafix.website.rule("DisableSyntax", DisableSyntaxConfig.default)
)
```
