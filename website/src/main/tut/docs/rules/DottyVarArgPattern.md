---
layout: docs
title: DottyVarArgPattern
tag: rule
---

# DottyVarArgPattern

Replaces `@` symbols in VarArg patterns with a colon (`:`). See [http://dotty.epfl.ch/docs/reference/changed/vararg-patterns.html](http://dotty.epfl.ch/docs/reference/changed/vararg-patterns.html)

```scala
// before
case List(1, 2, xs @ _*)
// after
case List(1, 2, xs : _*)
```
