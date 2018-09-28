---
id: semantic-type
title: SemanticType
---

Source:
<a href="https://scalameta.org/mbrowse/#/scalafix/scalafix-core/src/main/scala/scalafix/v1/SemanticType.scala" target="_blank">
<code>SemanticType.scala</code> </a>

`SemanticType` is a sealed data structure that encodes the Scala type system.

```scala mdoc:file:scalafix-core/src/main/scala/scalafix/v1/SemanticType.scala
sealed abstract class SemanticType...
...NoType extends
```

## Cookbook

All code examples in this document assume you have the following imports in
scope

```scala mdoc
import scalafix.v1._
import scala.meta._
```

```scala mdoc:passthrough
import scalafix.docs.PatchDocs
import scalafix.docs.PatchDocs._
implicit var doc: Symtab = scalafixSymtab
```

### Lookup symbol of type

Consider the following program.

```scala mdoc:passthrough
doc = fromString("""
package example
object Main {
  val a = 42
  val b = List(42)
}
""")
```

Use `MethodSignature.returnType` to get the types of vals.

```scala mdoc
def getType(symbol: Symbol): SemanticType =
  symbol.info.get.signature match {
    case MethodSignature(_, _, returnType) =>
      returnType
  }
println(getType(Symbol("example/Main.a.")))
println(getType(Symbol("example/Main.a.")).structure)

println(getType(Symbol("example/Main.b.")))
println(getType(Symbol("example/Main.b.")).structure)
```

Use `TypeRef.symbol` to obtain the symbols `scala/Int#` and
`scala/collection/immutable/List#` from the types `Int` or `List[Int]`.

```scala mdoc
def printTypeSymbol(tpe: SemanticType): Unit =
  tpe match {
    case TypeRef(_, symbol, _) =>
      println(symbol)
    case error =>
      println(s"MatchError: $error (of ${error.productPrefix})")
  }
printTypeSymbol(getType(Symbol("example/Main.a.")))
printTypeSymbol(getType(Symbol("example/Main.b.")))
```

Beware however that not all types are `TypeRef`.

```scala mdoc:passthrough
doc = fromString("""
package example
class Main
object Main {
  val a: Main with Serializable = ???
  val b: Main.type = this
}
""")
```

The `printTypeSymbol` method crashes on non-`TypeRef` types.

```scala mdoc
printTypeSymbol(getType(Symbol("example/Main.a.")))
```

```scala mdoc
printTypeSymbol(getType(Symbol("example/Main.b.")))
```

## Known limitations

The `SemanticType` API is new and intentionally comes with a small public API.
Some missing functionality is possible to implement outside of Scalafix.

### Lookup type of a term

Consider the following program.

```scala
object Main {
  val list = List(1).map(_ + 1)
}
```

There is no API to inspect the type of tree nodes such as `List(1)`. It is only
possible to lookup the signature of the symbol `Main.list`.

### Test for subtyping

Consider the following program.

```scala
sealed abstract class A[+T]
case class B(n: Int) extends A[Int]
```

There is no API to query whether the type `B` (structure:
`TypeRef(NoType, "B#", Nil)`) is a subtype of `A[Int]` (structure
`TypeRef(NoType, "A#", List(TypeRef(NoType, "scala/Int#", Nil)))`).

### Dealias types

Consider the following program.

```scala mdoc:passthrough
doc = fromString("""
package example
object Main {
  val a = "message"
  val b: String = "message"
  val c: List[String] = List()
}
""")
```

The `a` and `b` variables have semantically the same type but their types are
structurally different because the explicit `: String` annotation for `b`
references a type alias.

```scala mdoc
println(getType(Symbol("example/Main.a.")).structure)
println(getType(Symbol("example/Main.b.")).structure)
```

There is no public API to dealias type. It's possible to prototype roughly
similar functionality with the `simpleDealias` method below, but beware that it
is an incomplete implementation.

```scala mdoc
def simpleDealias(tpe: SemanticType): SemanticType = {
  def dealiasSymbol(symbol: Symbol): Symbol =
    symbol.info.get.signature match {
      case TypeSignature(_, lowerBound @ TypeRef(_, dealiased, _), upperBound)
          if lowerBound == upperBound =>
        dealiased
      case _ =>
        symbol
    }
  tpe match {
    case TypeRef(prefix, symbol, typeArguments) =>
      TypeRef(prefix, dealiasSymbol(symbol), typeArguments.map(simpleDealias))
  }
}
println(simpleDealias(getType(Symbol("example/Main.a."))).structure)
println(simpleDealias(getType(Symbol("example/Main.b."))).structure)
```

The `simpleDealias` method also handles `TypeRef.typeArguments` so that
`scala.List[scala.Predef.String]` becomes
`scala.collection.immutable.List[java.lang.String]`.

```scala mdoc
println(getType(Symbol("example/Main.c.")).structure)
println(simpleDealias(getType(Symbol("example/Main.c."))).structure)
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
