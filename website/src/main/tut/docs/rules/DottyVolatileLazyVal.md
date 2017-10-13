---
layout: docs
title: DottyVolatileLazyVal
---

# DottyVolatileLazyVal

Adds a `@volatile` annotation to lazy vals.
The `@volatile` annotation is needed to maintain thread-safe behaviour of lazy vals in Dotty.

```scala
// before
lazy val x = ...
// after
@volatile lazy val x = ...
```

With `@volatile`, Dotty uses a deadlock free scheme that is comparable-if-not-faster than the scheme used in scalac.
