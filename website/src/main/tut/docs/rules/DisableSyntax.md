---
layout: docs
title: Disable
---

# DisableSyntax

_Since 0.5.4_

This rule reports errors when a "disallowed" syntax is used. We take advantage of the ScalaMeta parser to quickly
scan the source code. This rules like the other syntaxtic rules does not require any compilation.

Example:

```scala
MyCode.scala:7: error: [DisableSyntax.xml] xml is disabled.
  <a>xml</a>
  ^
```

## Configuration

By default, this rule does not disable syntax.

It contains the following elements:

* keywords such as: `null, throw, var, return`
* semicolons (`;`)
* tabs
* xml literals

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
```