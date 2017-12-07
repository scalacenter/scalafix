---
layout: docs
title: NoFinalize
---

** syntactic **

# NoFinalize

Report an error when java.lang.Object.finalize is overridden.

More info about finalize on this [blog](https://dzone.com/articles/javas-finalizer-is-still-there)

_Since 0.5.6_

...

Example:

```scala
MyCode.scala:7:27 error: [NoFinalize]
    override protected def finalize() = ()
                           ^
finalize should not be used
```