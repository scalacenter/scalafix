---
id: api
title: API Documentation
---

The Scalafix public API documentation is composed of several packages.

## Scalafix v1

Latest Scaladoc:
[v@VERSION@](https://static.javadoc.io/ch.epfl.scala/scalafix-core_2.12/@VERSION@/scalafix/v1/index.html)

The Scalafix v1 API is available through `import scalafix.v1._`. Key data
structures include:

- `Patch`: to describe source code rewrites such as removing or replacing tokens
  and trees.
- `Diagnostic`: a linter error message that can be reported at a source file
  location.
- `SyntacticRule`: super class for all syntactic rules.
- `SyntacticDocument`: context about a single source file containing syntactic
  information such as trees/tokens.
- `SemanticRule`: super class for all semantic rules.
- `SemanticDocument`: context about a single source file containing syntactic
  information such as trees/tokens and semantic information such as
  symbols/types/synthetics.
- `Symbol`: a unique identifier for a definition. For example, the `String`
  class has the symbol `java/lang/String#`. The type alias to `String` in the
  Scala standard library has the symbol `scala/Predef.Symbol#`.
- `SymbolInfo`: metadata about a `Symbol`, including accessibility
  (private/protected/...), kind (val/def/class/trait/...), properties
  (final/abstract/implicit), language (Scala/Java) and type signature.
- `SType`: an ADT that encodes the full Scala type system.
- `STree`: an ADT that encodes synthetic tree nodes such as inferred `.apply`,
  inferred type parameters, implicit arguments and implicit conversions.

## Scalafix v0

Latest Scaladoc:
[v@VERSION@](https://static.javadoc.io/ch.epfl.scala/scalafix-core_2.12/@VERSION@/scalafix/v0/index.html)

This is a legacy API that exists to smoothen the migration for existing Scalafix
v0.5 rules. If you are writing a new Scalafix rule, please use the v1 API.

## Scalameta Trees

Latest Scaladoc:
[v@SCALAMETA@](https://static.javadoc.io/org.scalameta/trees_2.12/@SCALAMETA@/scala/meta/index.html)

The tree API is available through `import scala.meta._`. Key data structures
include:

- `Tree`: the supertype of all tree nodes. Key methods include:
  - `pos: Position`: the source code position of this tree node
  - `symbol: Symbol`: extension method made available via
    `import scalafix.v1._`, requires an implicit `SemanticDocument`. Returns the
    the symbol of this tree node
  - `syntax: String`: the pretty-printed tree node
  - `structure: String`: the raw structure of this tree node, useful for
    figuring out how to pattern match against a particular tree node.
- `Term`: the supertype for all term nodes in "term position". Terms are the
  parts of programs that get executed at runtime such as `run(42)` in
  `val x: String = run(42)`.
- `Type`: the supertype for all type nodes in "type position". Type are the
  parts of programs that only exist at compile time, such as `String` in
  `val x: String = ""`.
- `Stat`: the supertype for all tree nodes that can appear in "statement
  position". Statement position is the
- `Defn`: the supertype for all definitions

## Scalameta Tokens

Latest Scaladoc:
[v@SCALAMETA@](https://static.javadoc.io/org.scalameta/tokens_2.12/@SCALAMETA@/scala/meta/tokens/Token.html)

The type `scala.meta.Token` is a representation of all lexical tokens in the
Scala language. Each token kind has it's own type, such as `Token.Space`, and
`Token.KwClass` ("keyword `class`").

The token API is available through `import scala.meta._`. Key data structures
include:

- `Token`: the supertype for all token kinds. Sub-types of `Token` include
  `Token.Space`, `Token.KwClass` ("keyword `class`"), `Token.LeftParen` and
  `Token.Colon`.
- `Tokens`: a sequence of tokens with efficient collection operations such as
  slice, take and drop. It's important to keep in mind that a normal source file
  can have tens of thousands of tokens so operations like `filter` may perform
  badly.
