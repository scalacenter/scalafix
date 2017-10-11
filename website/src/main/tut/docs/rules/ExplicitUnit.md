---
layout: docs
title: ExplicitUnit
---

# ExplicitUnit

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

Such members already have a result type of `Unit` and sometimes this is unexpected. Adding an explicit result type makes it more obvious.
