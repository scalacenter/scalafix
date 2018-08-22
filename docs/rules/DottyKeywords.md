---
layout: docs
id: DottyKeywords
title: DottyKeywords
---

Rewrite that backticks identifiers named `enum` and `inline` since those are
treated as keywords in Dotty.

```scala
// before
val enum = "enum"
val inline = "inline"
// after
val `enum` = "enum"
val `inline` = "inline"
```
