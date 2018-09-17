---
id: symbol-information
title: SymbolInformation
---

`SymbolInformation` is a data structure containing metadata about a `Symbol`
definition. A symbol information describes the symbols's

- display name: the identifier used to reference this symbol
- language: Scala, Java
- kind: `class`, `trait`, `object`, ...
- properties: `final`, `abstract`, `implicit`
- type signature: class declarations, class parents, method parameters, ...
- visibility access: `private`, `protected`, ...

```scala mdoc:passthrough
import scalafix.internal.v1.SymbolInformationAnnotations._
import scalafix.docs.PatchDocs.documentSymbolInfoCategory
```

## SemanticDB

The structure of `SymbolInformation` in Scalafix mirrors SemanticDB
`SymbolInformation`. For comprehensive documentation about SemanticDB symbol
information consult the SemanticDB specification:

- [General symbol information](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#symbolinformation)
- [Scala symbol information](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-symbolinformation)
- [Java symbol information](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#java-symbolinformation)

### Language

SemanticDB supports two languages: Scala and Java. Every symbol is either
defined in Scala or Java. To determine if a symbol is defined in Scala or in
Java, use the `isScala` and `isJava` methods. A symbol cannot be defined in both
Java and Scala.

### Kind

Every symbol has exactly one kind such as being a class or an interface. A
symbol can't have two kinds, for example it's not possible to be both a
constructor and a method. The available symbol kinds are:

```scala mdoc:passthrough
documentSymbolInfoCategory(classOf[kind])
```

Some kinds are limited to specific languages. For example, Scala symbols cannot
be fields and Java symbols cannot be traits.

### Properties

A symbol can have zero or more properties such as `implicit` or `final`. The
available symbol properties are:

```scala mdoc:passthrough
documentSymbolInfoCategory(classOf[property])
```

Consult the SemanticDB specification to learn which properties are valid for
each kind. For example, Scala traits can only be sealed, it is not valid for a
trait to be implicit or final.

### Signature

`Signature` is a sealed data structure that describes the shape of a symbol
definition.

```scala mdoc:file:scalafix-core/src/main/scala/scalafix/v1/Signature.scala
sealed abstract class Signature...
...NoSignature extends
```

To learn more about SemanticDB signatures, consult the specification:

- [General signatures](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#signature)
- [Scala signatures](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-signature)
- [Java signatures](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#java-signature)

### Annotation

To learn more about SemanticDB annotations, consult the specification:

- [General annotations](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#annotation)
- [Scala annotations](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-annotation)
- [Java annotations](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#java-annotation)

### Access

Some symbols are only accessible within restricted scopes, such as the enclosing
class or enclosing package. A symbol can only have one access, for example is
not valid for a symbol to be both private and private within. The available
access methods are:

```scala mdoc:passthrough
documentSymbolInfoCategory(classOf[access])
```

To learn more about SemanticDB visibility access, consult the specification:

- [General access](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#access)
- [Scala access](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-access)
- [Java access](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#java-access)

### Utility methods

Some attributes of symbols are derived from a combination of language, kind and
property values. The following methods are available on `SymbolInformation` to
address common use-cases:

- `isDef`: returns true if this symbol is a Scala `def`.
- `isSetter`: returns true if this is a setter symbol. For example, every global
  `var` symbol has a corresponding setter symbol. Setter symbols are
  distinguished by having a display name that ends with `_=`.
