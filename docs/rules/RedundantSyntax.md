---
layout: docs
id: RedundantSyntax
title: RedundantSyntax
---

This rule removes redundant syntax.

## Configuration

By default, this rule rewrites any syntax known as redundant.

```scala mdoc:passthrough
import scalafix.internal.rule._
import scalafix.website._
```

```scala mdoc:passthrough
println(
defaults("RedundantSyntax", flat(RedundantSyntaxConfig.default))
)
```

## Features

### `final` keyword on an `object`

```diff
- final object foo
+ object Foo
```

Note: in Scala 2.12 and earlier removing the `final` modifier will slightly change the resulting bytecode -
see [this bug ticket](https://github.com/scala/bug/issues/11094) for further information.

### String interpolators

`RedundantSyntax` removes unnecessary [string interpolators](https://docs.scala-lang.org/overviews/core/string-interpolation.html). 
Only out-of-the-box interpolators (`s`, `f` and `raw`) are supported.

Example:

```diff
- println(s"Foo")
+ println("Foo")

- println(f"Bar")
+ println("Bar")

- println(raw"Baz")
+ println("Baz")

// No change as `raw` is not redundant.
println(raw"Foo\nBar")
```
