---
layout: docs
title: Disable
---

# Disable

_Since 0.6.0_

This rule reports errors when a "disallowed" symbol is referenced.

It has several different modes: 
- Default mode. In this mode symbols are banned in the actual source code. 
- `unlessInsideBlock`
In this mode rule bans usages of "disallowed" symbols unless they appear in a "safe" block.
Any inner blocks (e.g. anonymous functions or classes)
within the given "safe" blocks are banned again, to avoid leakage.

- `unlessSynthetic` This mode allows to block symbols in the generated code, 
for example expanded macros.

If `unlessInsideBlock` is enabled `unlessSynthetic` is ignored.  

Example:

```scala
MyCode.scala:7: error: [DisallowSymbol.asInstanceOf] asInstanceOf is disabled.
  myValue.asInstanceOf[String]
          ^
```

## Configuration

This rule has several different options.

```tut:invisible
import scalafix.internal.config._
```
```tut:passthrough
println(
scalafix.website.config[DisablePart]("DisablePart")
)
println(
scalafix.website.config[DisableConfig]("DisableConfig")
)
println(
scalafix.website.defaults("DisableConfig", DisableConfig.default)
)
println(
scalafix.website.examples[DisableConfig]("DisableConfig")
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
  val z = 1.asInstanceOf[String] // default mode, not ok
  
  List(1) + "any2stringadd" // unlessSynthetic mode, not ok

  println("hi") // unlessInsideBlock mode, not ok
  IO {
    println("hi") // ok
  }
  IO {
    def sideEffect(i: Int) = println("not good!") 
    //  unlessInsideBlock mode, not ok
    (i: Int) => println("also not good!") 
    //  unlessInsideBlock mode, not ok
  }

  Option.empty[Int].get //  unlessInsideBlock mode, not ok
  Try {
    Option.empty[Int].get // ok
  }
}
```
