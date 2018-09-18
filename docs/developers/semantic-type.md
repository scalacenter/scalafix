---
id: semantic-type
title: SemanticType
---

`SemanticType` is a sealed data structure that encodes the Scala type system.

```scala mdoc:file:scalafix-core/src/main/scala/scalafix/v1/SemanticType.scala
sealed abstract class SemanticType...
...NoType extends
```

## SemanticDB

The structure of `SemanticType` mirrors SemanticDB `Type`. For comprehensive
documentation about SemanticDB types, consult the SemanticDB specification:

- [General types](https://scalameta.org/docs/semanticdb/specification.html#type)
- [Scala types](https://scalameta.org/docs/semanticdb/specification.html#scala-type)
- [Java types](https://scalameta.org/docs/semanticdb/specification.html#java-type)

The `SemanticType` data structure diverges from SemanticDB `Type` in few minor
details.

### `SemanticType` instead of `Type`

Scalafix uses the name `SemanticType` instead of `Type` in order to avoid
ambiguous references with `scala.meta.Type` when importing the two packages
together.

```scala
import scalafix.v1._
import scala.meta._
```

### `List[SymbolInformation]` instead of `Scope`

The `SemanticType` data structure uses `List[SymbolInformation]` instead of
`Scope`, where applicable. This change avoids the notion of "soft-linked" and
"hard-linked" symbols, resulting in a higher-level API without loss of
expressiveness.
