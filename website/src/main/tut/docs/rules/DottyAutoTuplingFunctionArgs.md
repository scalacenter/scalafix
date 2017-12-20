---
layout: docs
title: DottyAutoTuplingFunctionArgs
---

# DottyAutoTuplingFunctionArgs

Removes pattern-matching decomposition if function arguments can be automatically tupled. See [http://dotty.epfl.ch/docs/reference/auto-parameter-tupling.html](http://dotty.epfl.ch/docs/reference/auto-parameter-tupling.html)

```scala
// before
xs.map { case (x, y) => x + y }
// after
xs.map((x, y) => x + y)
```

Auto-tupling is a language feature only supported in Dotty. Therefore, the code produced by this rewrite does not compile with Scala 2.x.