---
id: scala-type
title: ScalaType
---

`ScalaType` is a sealed data structure that encodes the Scala type system.

```scala mdoc:file:scalafix-core/src/main/scala/scalafix/v1/ScalaType.scala
sealed abstract class ScalaType...
...case object NoType
```

## SemanticDB

The structure of `ScalaType` mirrors SemanticDB `Type`. For comprehensive
documentation about SemanticDB types, consult the SemanticDB specification:

- [General types](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#type)
- [Scala types](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-type)
- [Java types](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#java-type)

The `ScalaType` data structure diverges from SemanticDB `Type` in few minor
details.

### `ScalaType` instead of `Type`

Scalafix uses the name `ScalaType` instead of `Type` in order to avoid ambiguous
references with `scala.meta.Type` when importing the two packages together.

```scala
import scalafix.v1._
import scala.meta._
```

### `List[SymbolInfo]` instead of `Scope`

The `ScalaType` data structure uses `List[SymbolInfo]` instead of `Scope`, where
applicable. This change avoids the notion of "soft-linked" and "hard-linked"
symbols, resulting in a higher-level API without loss of expressiveness.
