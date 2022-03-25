---
layout: docs
id: ExplicitResultTypes
title: ExplicitResultTypes
---

This rewrite inserts type annotations for inferred public members. Only compatible with 
scala 2.11, 2.12 & 2.13.

Example:

```diff
- def myComplexMethod = 1.to(10).map(i => i -> i.toString).toMap
+ def myComplexMethod: Map[Int, String] = 1.to(10).map(i => i -> i.toString).toMap
```

## Configuration

By default, only rewrite adds type annotations for public members that have
non-trivial bodies.

```scala mdoc:passthrough
import scalafix.internal.rule._
```

```scala mdoc:passthrough
println(
scalafix.website.rule("ExplicitResultTypes", ExplicitResultTypesConfig.default)
)
```

## Known limitations

This rule has several known limitations, which are most likely fixable with some
effort. At the time of this writing, there are no short-term plans to address
these issues however.

### Imports ordering

The rewrite inserts imports at the bottom of the global import list. Users are
expected to organize the imports according to the conventions of their codebase.

For example, the rewrite may produce the following diff.

```diff
import java.io.File
import scala.collection.mutable
+ import java.util.UUID
```

Potential workarounds:

- use https://github.com/NeQuissimus/sort-imports
- use https://github.com/liancheng/scalafix-organize-imports
- run "organize imports" refactoring in IntelliJ
