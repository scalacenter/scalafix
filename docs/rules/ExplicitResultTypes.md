---
layout: docs
id: ExplicitResultTypes
title: ExplicitResultTypes
---

This rewrite inserts type annotations for inferred public members.

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

Scala 3 support is recent and therefore not widely tested. Expect annotations
to be be less precise than the ones added to sources compiled with Scala 2.x.

### Imports ordering

The rewrite inserts imports at the bottom of the global import list. Users are
expected to organize the imports according to the conventions of their codebase.

For example, the rewrite may produce the following diff.

```diff
  import java.io.File
  import scala.collection.mutable
+ import java.util.UUID
```

To workaround this issue use the built-in [`OrganizeImports` rule](OrganizeImports.md). 
A few more workarounds:

- use https://github.com/NeQuissimus/sort-imports
- run "organize imports" refactoring in IntelliJ
