---
layout: docs
id: RemoveUnusedTerms
title: RemoveUnusedTerms
---

Rewrite that removes unused locals or privates that are warned by the Scala
compiler through `-Ywarn-unused:locals,privates`.

To use this rule

- enable `-Ywarn-unused:locals,privates`
- disable `-Xfatal-warnings`. Unfortunately, the Scala compiler does not support
  finer grained control over the severity level per message kind. See
  [scalameta/scalameta#924](https://github.com/scalameta/scalameta/issues/924)
  for a possible workaround in the near future.

```scala
// before
def app = {
  val unused = "message"
  println("Hello world!")
}
// after
def app = {
  println("Hello world!")
}
```
