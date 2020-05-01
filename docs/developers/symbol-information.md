---
id: symbol-information
title: SymbolInformation
---

Source:
<a href="https://scalameta.org/metabrowse/#/scalafix/scalafix-core/src/main/scala/scalafix/v1/SymbolInformation.scala" target="_blank">
<code>SymbolInformation.scala</code> </a>

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
import scalafix.docs.SymbolInformationDocs.documentSymbolInfoCategory
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
def println(a: Any): Unit = PatchDocs.println(a)
import scalafix.docs.PatchDocs._
implicit var doc: SemanticDocument = null
```

### Get symbol of a tree

Use `Tree.symbol` to get the symbol of a tree. Consider the following code.

```scala mdoc:passthrough
doc = PatchDocs.fromStatement("""
println(42)
println()
""")
```

To get the `println` symbol we match against the `Term.Name("println")` tree
node.

```scala mdoc
doc.tree.collect {
  case apply @ Term.Apply(println @ Term.Name("println"), _) =>
    (apply.syntax, println.symbol)
}
```

### Lookup method return type

Use `MethodSignature.returnType` to inspect the return type of a method.

```scala mdoc
def printReturnType(symbol: Symbol): Unit = {
  symbol.info.get.signature match {
    case signature @ MethodSignature(_, _, returnType) =>
      println("returnType = " + returnType)
      println("signature  = " + signature)
      println("structure  = " + returnType.structure)
  }
}
printReturnType(Symbol("scala/Int#`+`()."))
printReturnType(Symbol("scala/Int#`+`(+4)."))
printReturnType(Symbol("scala/Option#map()."))
```

The return type for constructor method signatures is always `NoType`.

```scala mdoc
printReturnType(Symbol("scala/Some#`<init>`()."))
```

### Lookup method parameters

Consider the following program.

```scala mdoc:passthrough
doc = fromString("""
package example
class Main(val constructorParam: Int) {
  def magic: Int = 42
  def typeParam[T]: T = ???
  def annotatedParam(@deprecatedName('a) e: Int): Int = e
  def curried(a: Int)(b: Int) = a + b
}
""")
```

Use `MethodSignature.parameterLists` to look up parameters of a method.

```scala mdoc
def printMethodParameters(symbol: Symbol): Unit = {
  symbol.info.get.signature match {
    case signature @ MethodSignature(typeParameters, parameterLists, _) =>
      if (typeParameters.nonEmpty) {
        println("typeParameters")
        println(typeParameters.mkString("  ", "\n  ", ""))
      }
      parameterLists.foreach { parameterList =>
        println("parametersList")
        println(parameterList.mkString("  ", "\n  ", ""))
      }
  }
}
printMethodParameters(Symbol("example/Main#magic()."))
printMethodParameters(Symbol("example/Main#typeParam()."))
printMethodParameters(Symbol("example/Main#annotatedParam()."))
printMethodParameters(Symbol("example/Main#`<init>`()."))
```

Curried methods are distinguished by a `MethodSignature` with a parameter list
of length greater than 1.

```scala mdoc
printMethodParameters(Symbol("example/Main#curried()."))
printMethodParameters(Symbol("scala/Option#fold()."))
```

### Test if method is nullary

A "nullary method" is a method that is declared with no parameters and without
parentheses.

```scala mdoc:passthrough
doc = fromString("""
package example
object Main {
  def nullary: Int = 1
  def nonNullary(): Unit = println(2)
  def toString = "Main"
}
""")
```

Nullary method signatures are distinguished by having an no parameter lists:
`List()`.

```scala mdoc
def printParameterList(symbol: Symbol): Unit = {
  symbol.info.get.signature match {
    case MethodSignature(_, parameterLists, _) =>
      println(parameterLists)
  }
}
printParameterList(Symbol("example/Main.nullary()."))
printParameterList(Symbol("scala/collection/Iterator#hasNext()."))
```

Non-nullary methods such as `Iterator.next()` have a non-empty list of
parameters: `List(List())`.

```scala mdoc
printParameterList(Symbol("example/Main.nonNullary()."))
printParameterList(Symbol("scala/collection/Iterator#next()."))
```

Java does not have nullary methods so Java methods always have a non-empty list:
`List(List())`.

```scala mdoc
printParameterList(Symbol("java/lang/String#isEmpty()."))
printParameterList(Symbol("java/lang/String#toString()."))
```

Scala methods that override Java methods always have non-nullary signatures even
if the Scala method is defined as nullary without parentheses.

```scala mdoc
printParameterList(Symbol("example/Main.toString()."))
```

### Lookup type alias

Use `TypeSignature` to inspect type aliases.

```scala mdoc
def printTypeAlias(symbol: Symbol): Unit = {
  symbol.info.get.signature match {
    case signature @ TypeSignature(typeParameters, lowerBound, upperBound) =>
      if (lowerBound == upperBound) {
        println("Type alias where upperBound == lowerBound")
        println("signature      = '" + signature + "'")
        println("typeParameters = " + typeParameters.structure)
        println("bound          = " + upperBound.structure)
      } else {
        println("Different upper and lower bounds")
        println("signature = '" + signature + "'")
        println("structure = " + signature.structure)
      }
  }
}
```

Consider the following program.

```scala mdoc:passthrough
doc = fromString("""
package example
object Main {
  type Number = Int
  type Sequence[T] = Seq[T]
  type Unbound
  type LowerBound >: Int
  type UpperBound <: String
  type UpperAndLowerBounded  >: String <: CharSequence
}
""")
```

```scala mdoc
printTypeAlias(Symbol("example/Main.Number#"))
printTypeAlias(Symbol("example/Main.Sequence#"))
printTypeAlias(Symbol("example/Main.Unbound#"))
printTypeAlias(Symbol("example/Main.LowerBound#"))
printTypeAlias(Symbol("example/Main.UpperBound#"))
printTypeAlias(Symbol("example/Main.UpperAndLowerBounded#"))
```

### Lookup class parents

Use `ClassSignature.parents` and `TypeRef.symbol` to lookup the class hierarchy.

```scala mdoc
def getParentSymbols(symbol: Symbol): Set[Symbol] =
  symbol.info.get.signature match {
    case ClassSignature(_, parents, _, _) =>
      Set(symbol) ++ parents.flatMap {
        case TypeRef(_, symbol, _) => getParentSymbols(symbol)
      }
  }
getParentSymbols(Symbol("java/lang/String#"))
```

### Lookup class methods

Use `ClassSignature.declarations` and `SymbolInformation.isMethod` to query
methods of a class. Use `ClassSignature.parents` to query methods that are
inherited from supertypes.

```scala mdoc
def getClassMethods(symbol: Symbol): Set[SymbolInformation] =
  symbol.info.get.signature match {
    case ClassSignature(_, parents, _, declarations) =>
      val methods = declarations.filter(_.isMethod)
      methods.toSet ++ parents.flatMap {
        case TypeRef(_, symbol, _) => getClassMethods(symbol)
      }
    case _ => Set.empty
  }
getClassMethods(Symbol("scala/Some#")).take(5)
getClassMethods(Symbol("java/lang/String#")).take(5)
getClassMethods(Symbol("scala/collection/immutable/List#")).take(5)
```

For Java methods, use `SymbolInformation.isStatic` to separate static methods
from non-static methods.

```scala mdoc
getClassMethods(Symbol("java/lang/String#")).filter(_.isStatic).take(3)
getClassMethods(Symbol("java/lang/String#")).filter(!_.isStatic).take(3)
```

### Lookup class primary constructor

A primary constructor is the constructor that defined alongside the class
declaration.

```scala mdoc:passthrough
doc = fromString("""
package example
class User(name: String, age: Int) {      // primary constructor
  def this(name: String) = this(name, 42) // secondary constructor
}
""", filename = "User.scala")
```

Use `SymbolInformation.{isConstructor,isPrimary}` to distinguish a primary
constructor.

```scala mdoc
def getConstructors(symbol: Symbol): List[SymbolInformation] =
  symbol.info.get.signature match {
    case ClassSignature(_, parents, _, declarations) =>
      declarations.filter { declaration =>
        declaration.isConstructor
      }
    case _ => Nil
  }
getConstructors(Symbol("example/User#")).filter(_.isPrimary)

// secondary constructors are distinguished by not being primary
getConstructors(Symbol("example/User#")).filter(!_.isPrimary)
```

Java constructors cannot be primary, "primary constructor" is a Scala-specific
feature.

```scala mdoc
getConstructors(Symbol("java/lang/String#")).take(3)
getConstructors(Symbol("java/lang/String#")).filter(_.isPrimary)
getConstructors(Symbol("java/util/ArrayList#")).filter(_.isPrimary)
```

### Lookup case class fields

Consider the following program.

```scala mdoc:passthrough
doc = fromString("""
package example
case class User(name: String, age: Int) {
  def this(secondaryName: String) = this(secondaryName, 42)
  val upperCaseName = name.toUpperCase
}
""", filename = "User.scala")
```

On the symbol information level, there is no difference between `name` and
`upperCaseName`, both are `val method`.

```scala mdoc
println(Symbol("example/User#name.").info)
println(Symbol("example/User#upperCaseName.").info)
```

> See
> [scalameta/scalameta#1492](https://github.com/scalameta/scalameta/issues/1492)
> for a discussion about adding `isSynthetic` to distinguish between `name` and
> `upperCaseName`.

Use the primary constructor to get the names of the case class fields

```scala mdoc
getConstructors(Symbol("example/User#")).foreach {
  case ctor if ctor.isPrimary =>
    ctor.signature match {
      case MethodSignature(_, parameters :: _, _) =>
        val names = parameters.map(_.displayName)
        println("names: " + names.mkString(", "))
    }
  case _ => // secondary constructor, ignore `this(secondaryName: String)`
}
```

### Lookup method overloads

Use `SymbolInformation.{isMethod,displayName}` to query for overloaded methods.

```scala mdoc
def getMethodOverloads(classSymbol: Symbol, methodName: String): Set[SymbolInformation] =
  classSymbol.info.get.signature match {
    case ClassSignature(_, parents, _, declarations) =>
      val overloadedMethods = declarations.filter { declaration =>
        declaration.isMethod &&
        declaration.displayName == methodName
      }
      overloadedMethods.toSet ++ parents.flatMap {
        case TypeRef(_, symbol, _) => getMethodOverloads(symbol, methodName)
      }
    case _ => Set.empty
  }
getMethodOverloads(Symbol("java/lang/String#"), "substring")
getMethodOverloads(Symbol("scala/Predef."), "assert")
getMethodOverloads(Symbol("scala/Predef."), "println")
getMethodOverloads(Symbol("java/io/PrintStream#"), "print").take(3)
```

Overloaded methods can be inherited from supertypes.

```scala mdoc:passthrough
doc = fromString("""
package example
class Main {
  def toString(width: Int): String = ???
}
""")
```

```scala mdoc
getMethodOverloads(Symbol("example/Main#"), "toString")
```

### Test if symbol is from Java or Scala

Use `SymbolInformation.{isScala,isJava}` to test if a symbol is defined in Java
or Scala.

```scala mdoc
def printLanguage(symbol: Symbol): Unit =
  if (symbol.info.get.isJava) println("java")
  else if (symbol.info.get.isScala) println("scala")
  else println("unknown")

printLanguage(Symbol("java/lang/String#"))
printLanguage(Symbol("scala/Predef.String#"))
```

Package symbols are neither defined in Scala or Java.

```scala mdoc
printLanguage(Symbol("scala/"))
printLanguage(Symbol("java/"))
```

### Test if symbol is private

Access modifiers such as `private` and `protected` control the visibility of a
symbol.

```scala mdoc
def printAccess(symbol: Symbol): Unit = {
  val info = symbol.info.get
  println(
         if (info.isPrivate) "private"
    else if (info.isPrivateThis) "private[this]"
    else if (info.isPrivateWithin) s"private[${info.within.get.displayName}]"
    else if (info.isProtected) "protected"
    else if (info.isProtectedThis) "protected[this]"
    else if (info.isProtectedWithin) s"protected[${info.within.get.displayName}]"
    else if (info.isPublic) "public"
    else "<no access>"
  )
}
```

Consider the following program.

```scala mdoc:passthrough
doc = fromString("""
package example
class Main {
                     def publicMethod          = 1
  private            def privateMethod         = 1
  private[this]      def privateThisMethod     = 1
  private[example]   def privateWithinMethod   = 1
  protected          def protectedMethod       = 1
  protected[this]    def protectedThisMethod   = 1
  protected[example] def protectedWithinMethod = 1
}
""")
```

The methods have the following access modifiers.

```scala mdoc
printAccess(Symbol("example/Main#publicMethod()."))
printAccess(Symbol("example/Main#privateMethod()."))
printAccess(Symbol("example/Main#privateThisMethod()."))
printAccess(Symbol("example/Main#privateWithinMethod()."))
printAccess(Symbol("example/Main#protectedMethod()."))
printAccess(Symbol("example/Main#protectedThisMethod()."))
printAccess(Symbol("example/Main#protectedWithinMethod()."))
```

Observe that a symbol can only have one kind of access modifier, for example
`isPrivate=false` for symbols where `isPrivateWithin=true`.

Java does supports smaller set of access modifiers, there is no `private[this]`,
`protected[this]` and `protected[within]` for Java symbols.

```scala mdoc
printAccess(Symbol("java/lang/String#"))

println(Symbol("java/lang/String#value.").info)
printAccess(Symbol("java/lang/String#value."))

println(Symbol("java/lang/String#`<init>`(+15).").info)
printAccess(Symbol("java/lang/String#`<init>`(+15)."))
```

Package symbols have no access restrictions.

```scala mdoc
printAccess(Symbol("scala/"))
printAccess(Symbol("java/"))
```

### Lookup symbol annotations

Definitions such as classes, parameters and methods can be annotated with
`@annotation`.

```scala mdoc:passthrough
doc = fromString("""
package example
object Main {
  @deprecated("Use add instead", "1.0")
  def +(a: Int, b: Int) = add(a, b)

  class typed[T] extends scala.annotation.StaticAnnotation
  @typed[Int]
  def add(a: Int, b: Int) = a + b
}
""")
```

Use `SymbolInformation.annotations` to query the annotations of a symbol.

```scala mdoc
def printAnnotations(symbol: Symbol): Unit =
  println(symbol.info.get.annotations.structure)

printAnnotations(Symbol("example/Main.`+`()."))
printAnnotations(Symbol("example/Main.add()."))
printAnnotations(Symbol("scala/Predef.identity()."))
printAnnotations(Symbol("scala/Function2#[T1]"))
```

It is not possible to query the term arguments of annotations. For example,
observe that the annotation for `Main.+` does not include the "Use add instead"
message.

## Known limitations

### Lookup method overrides

Consider the following program.

```scala
trait A {
  def add(a: Int, b: Int): Int
}
class B extends A {
  override def add(a: Int, b: Int): Int = a + b
}
```

There is no API to go from the symbol `B#add().` to the symbol it overrides
`A#add()`. There is also no `.isOverride` helper to test if a method overrides
another symbol.

## SemanticDB

The structure of `SymbolInformation` in Scalafix mirrors SemanticDB
`SymbolInformation`. For comprehensive documentation about SemanticDB symbol
information consult the SemanticDB specification:

- [General symbol information](https://scalameta.org/docs/semanticdb/specification.html#symbolinformation)
- [Scala symbol information](https://scalameta.org/docs/semanticdb/specification.html#scala-symbolinformation)
- [Java symbol information](https://scalameta.org/docs/semanticdb/specification.html#java-symbolinformation)

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

- [General signatures](https://scalameta.org/docs/semanticdb/specification.html#signature)
- [Scala signatures](https://scalameta.org/docs/semanticdb/specification.html#scala-signature)
- [Java signatures](https://scalameta.org/docs/semanticdb/specification.html#java-signature)

### Annotation

To learn more about SemanticDB annotations, consult the specification:

- [General annotations](https://scalameta.org/docs/semanticdb/specification.html#annotation)
- [Scala annotations](https://scalameta.org/docs/semanticdb/specification.html#scala-annotation)
- [Java annotations](https://scalameta.org/docs/semanticdb/specification.html#java-annotation)

### Access

Some symbols are only accessible within restricted scopes, such as the enclosing
class or enclosing package. A symbol can only have one access, for example is
not valid for a symbol to be both private and private within. The available
access methods are:

```scala mdoc:passthrough
documentSymbolInfoCategory(classOf[access])
```

To learn more about SemanticDB visibility access, consult the specification:

- [General access](https://scalameta.org/docs/semanticdb/specification.html#access)
- [Scala access](https://scalameta.org/docs/semanticdb/specification.html#scala-access)
- [Java access](https://scalameta.org/docs/semanticdb/specification.html#java-access)

### Utility methods

Some attributes of symbols are derived from a combination of language, kind and
property values. The following methods are available on `SymbolInformation` to
address common use-cases:

- `isDef`: returns true if this symbol is a Scala `def`.
- `isSetter`: returns true if this is a setter symbol. For example, every global
  `var` symbol has a corresponding setter symbol. Setter symbols are
  distinguished by having a display name that ends with `_=`.
