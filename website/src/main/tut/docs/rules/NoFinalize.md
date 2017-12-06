---
layout: docs
title: NoFinalize
---

** syntactic **

# NoFinalize

Disallow implementing java.lang.Object.finalize.

More info about finalize on this [blog](https://dzone.com/articles/javas-finalizer-is-still-there)

_Since 0.5.6_

...

Example:

```scala
MyCode.scala:7: error: [NoFinalize]
    override protected def finalize() = ()
                           ^
```