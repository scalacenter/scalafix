---
layout: docs
title: DisableUnless
---

# DisableUnless

_Since 0.5.8_

This rule bans usages of "disabled" symbols unless in a "safe" block. 

Any inner blocks (e.g. anonymous functions or classes) 
within the given "safe" blocks are banned again, to avoid leakage. 

## Configuration

By default, this rule does allows all symbols. To disallow a symbols in a block:
```
DisableUnless.symbols = [
  {
      unless = "scala.Option"
      symbols = [
        {
          symbol = "test.DisableUnless.dangerousFunction"
          message = "the function may return null"
        }
        "test.DisableUnless.anotherFunction"
      ]
  }
]
```
You can use objects or regular strings to specify blocked symbols, 
`message` is optional parameter and could be used to provide custom errors. 

## Example
With the given config:
```
DisableUnless.symbols = [
  {
      unless = "test.DisableUnless.IO"
      symbols = [
        {
          symbol = "scala.Predef.println"
          message = "println has side-effects"
        }
      ]
  }
]
```

We got several linter errors in the following code:
```
package test

object Test {
  object IO {
    def apply[T](run: => T) = ???
  }

  
  println("hi") // not ok
  
  IO {
    println("hi") // ok
  }
  
  IO {
    def sideEffect(i: Int) = println("not good!") // not ok
    (i: Int) => println("also not good!") // not ok
  }
```