---
id: glossary
title: Glossary
---

The following sections explain useful vocabulary when working with Scalafix.

## Rule

A rule is a small program/function that can analyze your code, report messages
and automatically fix problems. To implement a rule, you extend the
`scalafix.v0.Rule` class. To run a rule, users execute
`scalafix --rules MyRule`. Multiple rules can be composed into a single rule.
For example, the migration for Dotty may involve `ProcedureSyntax`,
`ExplicitUnit`, `DottyVarArgPattern`, `ExplicitResultTypes` and a few other
rules. It is possible to combine all of those rules into a single `Dotty` rule
so users can run `scalafix --rules Dotty`.

## RuleCtx

A rule context contains data structures and utilities to rule a single source
file. For example, the rule context contains the parsed [Tree](#tree),
[Tokens](#tokens), lookup tables for matching parentheses and more.

## Patch

A "Patch" is a data structure that describes how to produce a diff. Two patches
can be combined into a single patch with the `+` operator. A patch can also be
empty. Patches can either be low-level "token patches", that operate on the
token level or high-level "tree patches" that operate on parsed abstract syntax
tree nodes. The public API for patch operations is available in `PatchOps`.

Some things are typically easier to do on the token level and other things are
easier to do on the tree level. The Patch API is constantly evolving and we
regularly add more utility methods to accomplish common tasks. If you experience
that it's difficult to implement something that seems simple then don't hesitate
to ask on @GITTER@.

It's possible to escape parts of a Patch with `// scalafix:ok`. If you want to
treat a Patch as a transaction use `.atomic`.

## LintMessage

Rules are able to emit "lint messages" with `Patch.lint(LintMessage): Patch`. To
report a lint message, extend the `LintMessage` trait like this

```scala
import scala.meta._
import scalafix.v1._
case class DivisionByZero(position: Position) extends LintMessage {
  def message = "Division by zero!"
}
def fix(doc: Doc): Patch = {
  Patch.lint(DivisionByZero(doc.tree.pos))
}
```

## Scalameta

Scalafix uses [Scalameta](http://scalameta.org/) to implement rules. Scalameta
is a clean-room implementation of a metaprogramming toolkit for Scala. This
means it's not necessary to have experience with Scala compiler internals to
implement Scalafix rules. In fact, Scalafix doesn't even depend on the Scala
compiler. Since Scalafix is not tied to a single compiler, this means that
Scalafix rules in theory can work with any Scala compiler, including
[Dotty](http://dotty.epfl.ch/) and IntelliJ Scala Plugin.

## SemanticDB

SemanticDB is a language agnostic schema for semantic information such as
resolved names, symbols signatures, reported compiler messages and more. See the
[SemanticDB][] specification.

## semanticdb-scalac

semanticdb-scalac is a compiler plugin for Scala 2.x in the
[Scalameta](#scalameta) project that collects information to build a
[SemanticDB](#semanticdb). For more information about semanticdb-scalac, see the
[SemanticDB][] spec.

## Token

A token is, for example, an identifier `println`, a delimiter `[` `)`, or a
whitespace character like space or newline. In the context of Scalafix, a
`Token` means the data structure `scala.meta.Token`. See
[Scalameta tutorial](http://scalameta.org/tutorial/#Tokens) for more details.
See [Wikipedia](https://en.wikipedia.org/wiki/Lexical_analysis#Token) for a more
general definition.

## Tokens

`Tokens` is a list of [Token](#token). See
[Scalameta tutorial](http://scalameta.org/tutorial/#Tokens).

## Tree

A `Tree` is a parsed abstract syntax tree. In the context of Scalafix, a `Tree`
means the data structure `scala.meta.Tree`. See
[Scalameta tutorial](http://scalameta.org/tutorial/#Trees) for more details. See
[Wikipedia](https://en.wikipedia.org/wiki/Abstract_syntax_tree) for a more
general definition.

## Syntactic

A [Rule](#rule) is "syntactic" when it does not require information from
type-checking such as resolved names (`println` => `scala.Predef.println`),
types or terms, or inferred implicit arguments. A syntactic rule can use
[Tokens](#tokens) and [Tree](#tree).

## Semantic

A [Rule](#rule) is "semantic" if it requires information from the compiler such
as types, symbols and reported compiler messages.

## Rule

A scalafix "Rule" can report lint messages and provide auto-fix patches to
violations of some kind of rule/coding style/convention/breaking change. The
default scalafix rules are listed in the [rules](rules/overview.md) section.

[semanticdb]:
  https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md
