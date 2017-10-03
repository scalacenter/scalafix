---
layout: docs
title: NoValInForComprehension
---

# NoValInForComprehension

Removes `val` from definitions in for-comprehension:

```scala
// before
for {
  n <- List(1, 2, 3)
  val inc = n + 1
} yield inc
// after
for {
  n <- List(1, 2, 3)
  inc = n + 1
} yield inc
```

The two syntaxes are equivalent and the presence of the `val` keyword has been deprecated since Scala 2.10.
