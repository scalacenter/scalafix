---
layout: docs
title: DisableUnless
---

# DisableUnless

_Since 0.5.8_

This rule bans usages of "disabled" symbols unless they appear in a "safe" block.

Any inner blocks (e.g. anonymous functions or classes)
within the given "safe" blocks are banned again, to avoid leakage.

## Configuration

By default, this rule does allows all symbols.

```tut:invisible
import scalafix.internal.config._
```
```tut:passthrough
println(
scalafix.website.config[UnlessConfig]("UnlessConfig")
)
println(
scalafix.website.config[DisableUnlessConfig]("DisableUnlessConfig")
)
println(
scalafix.website.defaults("DisableUnless", DisableUnlessConfig.default)
)
println(
scalafix.website.examples[DisableUnlessConfig]("DisableUnless")
)
```

The the example configuration above, Scalafix will report the following warnings:
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