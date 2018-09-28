---
id: symbol-matcher
title: SymbolMatcher
---

Source:
<a href="https://scalameta.org/metadoc/#/scalafix/scalafix-core/src/main/scala/scalafix/v1/SymbolMatcher.scala" target="_blank">
<code>SymbolMatcher.scala</code> </a>

Symbol matcher is a trait that is defined by a single method
`matches(Symbol): Boolean`.

```scala
trait SymbolMatcher {
  def matches(symbol: Symbol): Boolean
}
```

On top of `matches()`, symbol matchers provide additional methods to extract
interesting tree nodes with pattern matching.

All code examples in this document assume you have the following imports in
scope.

```scala mdoc
import scalafix.v1._
import scala.meta._
```

```scala mdoc:passthrough
import scalafix.docs.PatchDocs
import scalafix.docs.PatchDocs._
```

## SemanticDB

Scalafix symbols are based on SemanticDB symbols. To learn more about SemanticDB
symbols, consult the SemanticDB specification:

- [General symbols](https://scalameta.org/docs/semanticdb/specification.html#symbol)
- [Scala symbols](https://scalameta.org/docs/semanticdb/specification.html#scala-symbol)
- [Java symbols](https://scalameta.org/docs/semanticdb/specification.html#java-symbol)

## SymbolMatcher reference

There are two kinds of symbol matchers: exact and normalized. To illustrate the
differences between exact and normalized symbol matchers, we use the following
input source file as a running example.

```scala mdoc:passthrough
implicit var doc = PatchDocs.fromString("""
package com
class Main() {}            // com/Main# class
object Main extends App {  // com/Main. object
  println()                // println()   overload with no parameter
  println("Hello Predef!") // println(+1) overload with string parameter
}
""")
```

Observe that the name

- `Main` can refer to both the class or the companion object.
- `println` can refer to different method overloads.

### `exact()`

Exact symbol matchers allow precise comparison between symbols with the ability
to distinguish the differences between method overloads and a class from its
companion object.

To match only the `Main` class in the example above.

```scala mdoc
val mainClass  = SymbolMatcher.exact("com/Main#")
mainClass.matches(Symbol("com/Main#"))
mainClass.matches(Symbol("com/Main."))
```

To match only the `println(String)` method overload and not the `println()`
method.

```scala mdoc
val printlnString  = SymbolMatcher.exact("scala/Predef.println(+1).")
printlnString.matches(Symbol("scala/Predef.println()."))
printlnString.matches(Symbol("scala/Predef.println(+1)."))
```

### `normalized()`

Normalized symbol matchers ignore the differences between overloaded methods and
a class from its companion object. The benefit of normalized symbols is that
they use a simple symbol syntax: fully qualified names with a dot `.` separator.
For example,

- `java/lang/String#` is equal to `java.lang.String` normalized
- `scala/Predef.println(+1).` is equal to `scala.Predef.println` normalized

To match against either the `Main` class or companion object

```scala mdoc
val main = SymbolMatcher.normalized("com.Main")
main.matches(Symbol("com/Main#")) // Main class
main.matches(Symbol("com/Main.")) // Main object
```

To match against all overloaded `println` methods

```scala mdoc
val print = SymbolMatcher.normalized("scala.Predef.println")
print.matches(Symbol("scala/Predef#println()."))   // println()
print.matches(Symbol("scala/Predef#println(+1).")) // println(String)
```

### `unapply(Tree)`

Use the `unapply(Tree)` method to pattern match against tree nodes that resolve
to a specific symbol. Consider the following input file.

```scala mdoc:passthrough
doc = PatchDocs.fromString("""
object Main {
  util.Success(1)
}
""")
```

To match against the Scala `util` package symbol

```scala mdoc
val utilPackage = SymbolMatcher.exact("scala/util/")
doc.tree.traverse {
  case utilPackage(name) =>
    println(name.pos.formatMessage("info", "Here is the util package"))
}
```

A common mistake when using `unapply(Tree)` is to match multiple tree nodes that
resolve to the same symbol.

```scala mdoc
val successObject = SymbolMatcher.exact("scala/util/Success.")
doc.tree.traverse {
  case successObject(name) =>
    println(name.pos.formatMessage("info",
      "Matched Success for tree node " + name.productPrefix))
}
```

To ensure we match the symbol only once, refine the pattern match to `Name`.

```scala mdoc
doc.tree.traverse {
  case successObject(name @ Name(_)) =>
    println(name.pos.formatMessage("info", "Here is the Success name"))
}
```

Alternatively, enclose the symbol matcher guard inside a more complicated
pattern.

```scala mdoc
doc.tree.traverse {
  case function @ Term.Apply(successObject(_), List(argument)) =>
    println(function.pos.formatMessage("info",
      s"Argument of Success is $argument"))
}
```

### `+(SymbolMatcher)`

Use the `+(SymbolMatcher)` method to combine two symbol matchers.

```scala mdoc:silent
val printOrMain = main + print
```

The combined matcher returns true when either the `main` or `print` matchers
return true.

```scala mdoc
printOrMain.matches(Symbol("com/Main#"))
printOrMain.matches(Symbol("scala/Predef.println()."))
```
