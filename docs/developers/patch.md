---
id: patch
title: Patch
---

Scalafix rules return a `Patch` data structure that describes how to rewrite a
source file and how to report diagnostics. Patch is an immutable sealed data
structure similar to `Option` or `Either`. A `Patch` can be combined with
another `Patch` using the `+` operator.

## Patch reference

The code examples below assume we have a `doc` variable in scope with access to
the syntax tree of the following input source file

```scala mdoc:passthrough
import scalafix.docs.PatchDocs._
import scalafix.v1._
import scala.meta._
implicit var doc = scalafix.docs.PatchDocs.fromString("""
import scala.concurrent.{Future, Await}
import scala.util._, hashing.{MurmurHash3 => Hash}

object Program extends App {
  println("Hello world!")
}
""")
```

The `showDiff()` method in the code examples can be ignored, it is only used for
documentation purposes and is not part of the public Scalafix API.

### `addLeft()`

The `addLeft()` patch allows you to insert a string value to the left of a tree
node.

```scala mdoc
doc.tree.collect {
  case name @ Name("Program") =>
    Patch.addLeft(name, "/* => */ ")
}.showDiff()
```

It's also possible to add left of a token instead of a tree node.

```scala mdoc
doc.tokens.collect {
  case brace @ Token.KwExtends() =>
    Patch.addLeft(brace, "/* => */ ")
}.showDiff()
```

It's possible to apply `addLeft()` twice to the same tree node.

```scala mdoc
doc.tree.collect {
  case name @ Name("Program") =>
    Patch.addLeft(name, "/* first */ ") +
      Patch.addLeft(name, "/* second */ ")
}.showDiff()
```

### `addRight()`

The `addRight()` patch works the same way as `addLeft()` except it inserts a
string to the right of a tree node or a token.

```scala mdoc
doc.tree.collect {
  case name @ Name("println") =>
    Patch.addRight(name, s" /* <= */ ")
}.showDiff()
```

It's also possible to add right of a token instead of a tree node.

```scala mdoc
doc.tokens.collect {
  case brace @ Token.LeftParen() =>
    Patch.addRight(brace, " /* <= */ ")
}.showDiff()
```

### `removeToken()`

The `removeToken()` patch replaces a token with the empty string

```scala mdoc
doc.tokens.collect {
  case helloWorld @ Token.Constant.String("Hello world!") =>
    Patch.removeToken(helloWorld)
}.showDiff()
```

### `removeTokens()`

The `removeTokens()` patch works like `removeToken()` except it accepts an
`Iterable[Token]` argument. A common use-case for `removeTokens()` is to remove
tree nodes

```scala mdoc
doc.tree.collect {
  // Match against literal tree node instead of string constant token
  case helloWorld @ Lit.String("Hello world!") =>
    Patch.removeTokens(helloWorld.tokens)
}.showDiff()
```

### `replaceTree()`

The `replaceTree()` patch is a convenience operator for `removeTokens` and
`addRight`.

```scala mdoc
doc.tree.collect {
  case println @ Term.Apply(Name("println"), _) =>
    Patch.replaceTree(println, "print(40 + 2)")
}.showDiff()
```

It's best to target as precise tree nodes as possible when using `replaceTree()`
to avoid conflicts with other patches.

A common mistake is to use `replaceTree()` in combination with `

### `removeImportee()`

The `removeImportee()` patch removes an a `scala.meta.Importee` tree node. An
importee is a tree node that  is either a name import, a rename import or a
wildcard import.

```scala
import a.B        // name importee
import a.{B => C} // rename importee
import a._        // wildcard importee
```

The `removeImportee()` patch takes care of these three cases and makes sure to
clean up commas and redundant curly braces

```scala mdoc
doc.tree.collect {
  case future @ Importee.Name(Name("Future")) =>
    Patch.removeImportee(future)
}.showDiff()
doc.tree.collect {
  case hash @ Importee.Rename(Name("MurmurHash3"), _) =>
    Patch.removeImportee(hash)
}.showDiff()
doc.tree.collect {
  case wildcard @ Importee.Wildcard() =>
    Patch.removeImportee(wildcard)
}.showDiff()
```

Beware that `removeImportee()` requires the importee tree node reference to
appear in the source code. For example, quasiquotes will always produce empty
diffs even if the source file contains an importee with a matching structure.

```scala mdoc
Patch.removeImportee(importee"Future").showDiff()
```

### `lint()`

The `lint()` patch indicates that a diagnostic message should be reported at a
particular position. The `lint()` patch does not produce a diff.

```scala mdoc
case class Println(position: Position) extends Diagnostic {
  def message = "Use loggers instead of println"
}
doc.tree.collect {
  case println @ Term.Apply(Term.Name("println"), _) =>
    Patch.lint(Println(println.pos))
}.showLints()
```

### `atomic`

The `atomic` patch describes how a patch should integrate with the Scalafix
[suppression mechanism](../users/suppression.md). By using `atomic`, a patch
opts-into respecting suppressions such as `// scalafix:off` comments. A patch
without `atomic` will ignore suppressions. A patch with `atomic` guarantees that
either all underlying changes of the patch get applied or none of the changes
get applied.

To illustrate how `atomic` works, imagine we have the following input.

```scala mdoc:passthrough
doc = scalafix.docs.PatchDocs.fromString("""
object Main extends App {
  val x = 42
  // scalafix:off
  println(x)
}
""")
```

To rename `x` into `zzz` we have three options:

- skip `atomic`, ignoring the `// scalafix:off` comment
- wrap all changes into a single `atomic`
- wrap each individual change into a separate `atomic`

If we skip `atomic` then the patches will ignore the `// scalafix:off` comment
and all `replaceTree()` go through.

```scala mdoc
doc.tree.collect {
  case x @ Term.Name("x") =>
    Patch.replaceTree(x, "zzz")
}.asPatch.showDiff()
```

If we wrap the final patch with `atomic` then none of the `replaceTree()` get
applied because the second rename is invalidated by the `// scalafix:off`
comment.

```scala mdoc
doc.tree.collect {
  case x @ Term.Name("x") =>
    Patch.replaceTree(x, "z")
}.asPatch.atomic.showDiff()
```

If we place `atomic` at each individual `replaceTree()` then the first rename
goes through and the second rename is invalidated.

```scala mdoc
doc.tree.collect {
  case x @ Term.Name("x") =>
    Patch.replaceTree(x, "z").atomic
}.asPatch.showDiff()
```
