---
layout: docs
id: ExplicitUnit
title: ExplicitUnit
---

NOTE. This rule is deprecated since v0.6.0. Use `ProcedureSyntax` instead.

Adds an explicit `Unit` return type to `def` declarations without a result type:

```scala
// before
trait A {
  def doSomething
}
// after
trait A {
  def doSomething: Unit
}
```

Such members already have a result type of `Unit` and sometimes this is
unexpected. Adding an explicit result type makes it more obvious.
