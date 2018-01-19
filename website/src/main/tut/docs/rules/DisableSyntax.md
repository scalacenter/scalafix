---
layout: docs
title: DisableSyntax
---

# DisableSyntax

_Since 0.5.4_

This rule reports errors when a "disallowed" syntax is used. This is a syntactic rule, which means it does not require compilation to run unlike the `Disable` rule.

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

By default, this rule does not disable syntax.

It contains the following elements:

* keywords such as: `null, throw, var, return`
* semicolons (`;`)
* tabs
* xml literals
* covariant and contravariant type parameters 

To disallow a syntax:

```
DisableSyntax.keywords = [
  var
  null
  return
  throw
]
DisableSyntax.noTabs = true
DisableSyntax.noSemicolons = true
DisableSyntax.noXml = true
DisableSyntax.noCovariantTypes = true
DisableSyntax.noContravariantTypes = true
DisableSyntax.regex = [
  {
    id = "offensive"
    pattern = "[P|p]imp"
    message = "Please consider a less offensive word such as Extension"
  }
]
```
