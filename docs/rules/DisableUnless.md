---
layout: docs
id: DisableUnless
title: DisableUnless
---

This rule bans usages of "disabled" symbols unless they appear in a "safe"
block.

Any inner blocks (e.g. anonymous functions or classes) within the given "safe"
blocks are banned again, to avoid leakage.

## Configuration

By default, this rule does allows all symbols.

The the example configuration above, Scalafix will report the following
warnings:

```scala
package com

import scala.util.Try

object IO {
  def apply[T](run: => T) = ???
}

object Test {
  println("hi") // not ok
  IO {
    println("hi") // ok
  }
  IO {
    def sideEffect(i: Int) = println("not good!") // not ok
    (i: Int) => println("also not good!") // not ok
  }

  Option.empty[Int].get // not ok
  Try {
    Option.empty[Int].get // ok
  }
}
```

_Since 0.6.0_

NOTE. This rule is deprecated and from v0.6.0 onwards will be merged into the
`Disable` rule.
