---
layout: docs
title: Vocabulary
---

# Vocabulary
The following sections explain useful vocabulary when working with Scalafix.

## Rule
A rule is a small program/function that can produce diffs.
To implement a rule, you extend the
[Rule](https://github.com/scalacenter/scalafix/blob/master/scalafix-core/src/main/scala/scalafix/rule/Rule.scala)
class.
To run a rule, users execute `scalafix --rules MyRule`.
Multiple rules can be composed into a single rule.
For example, the migration for Dotty may involve {% doc_ref ProcedureSyntax %},
{% doc_ref ExplicitUnit %}, {% doc_ref DottyVarArgPattern %}, {% doc_ref ExplicitResultTypes %} and a few other rules. It is possible to combine all of those rules into a single `Dotty` rule so users can run `scalafix --rules Dotty`.

## RuleCtx
A rule context contains data structures and utilities to rule a single
source file. For example, the rule context contains the parsed {% vocabulary_ref Tree %},
{% vocabulary_ref Tokens %}, lookup tables for matching parentheses and more.

## Patch
A "Patch" is a data structure that describes how to produce a diff.
Two patches can be combined into a single patch with the `+` operator.
A patch can also be empty. Patches can either be low-level "token patches",
that operate on the token level or high-level "tree patches" that operate
on parsed abstract syntax tree nodes. The public API for patch
operations is available in `PatchOps.scala`.

// TODO(gabro): Should we include the `PatchOps` source, or can we link to the apidocs?
// @hl.ref(wd/"scalafix-core"/"shared"/"src"/"main"/"scala"/"scalafix"/"patch"/"PatchOps.scala", start = "trait PatchOps")

Some things are typically easier to do on the token level and other
things are easier to do on the tree level.
The Patch API is constantly evolving and we regularly add more
utility methods to accomplish common tasks.
If you experience that it's difficult to implement something that
seems simple then don't hesitate to ask on {% gitter %}.

## LintMessage
Rules are able to emit "lint messages" with info/warn/error severity
using `ctx.lint(lintCategory.at(String/Position)): Patch`.
To report a lint message, first create a {% vocabulary_ref LintCategory %} and then report it as a `Patch`.

```scala
val divisionByZero = LintCategory.error("Division by zero is unsafe!")
def rule(ctx: RuleCtx): Patch = {
  val tree: Tree = // ...
  ctx.lint(divisionByZero.at(tree.pos))
}
```

## LintCategory
A LintCategory is group of lint messages of the same kind.
A LintCategory has a default severity level (info/warn/error) at which
it will be reported. Scalafix users can override the default severity
with {% doc_ref Configuration, lint %}.

## Scalameta
Scalafix uses [Scalameta](http://scalameta.org/) to implement
rules.
Scalameta is a clean-room implementation of a metaprogramming toolkit for Scala.
This means it's not necessary to have experience with Scala compiler internals
to implement Scalafix rules.
In fact, Scalafix doesn't even depend on the Scala compiler.
Since Scalafix is not tied to a single compiler, this means that Scalafix
rules in theory can work with any Scala compiler, including [Dotty](http://dotty.epfl.ch/) and
IntelliJ Scala Plugin.

## SemanticDB
SemanticDB is a language agnostic schema for semantic information such
as resolved names, symbols signatures, reported compiler messages
and more. See the [Scalameta documentation](http://scalameta.org/tutorial/#SemanticDB).

## semanticdb-scalac
semanticdb-scalac is a compiler plugin for Scala 2.x in the {% vocabulary_ref Scalameta %} project
that collects information to build a {% vocabulary_ref SemanticDB %}.
For more information about semanticdb-scalac, see
the [Scalameta documentation](http://scalameta.org/tutorial/#semanticdb-scalac).

## Token
A token is, for example, an identifier `println`, a delimiter `[` `)`, or a whitespace character like space or newline.
In the context of Scalafix, a `Token` means the data structure `scala.meta.Token`.
See [Scalameta tutorial](http://scalameta.org/tutorial/#Tokens) for more details.
See [Wikipedia](https://en.wikipedia.org/wiki/Lexical_analysis#Token) for a more general definition.

## Tokens
`Tokens` is a list of {% vocabulary_ref Token %}.
See [Scalameta tutorial](http://scalameta.org/tutorial/#Tokens).

## Tree
A `Tree` is a parsed abstract syntax tree.
In the context of Scalafix, a `Tree` means the data structure `scala.meta.Tree`.
See [Scalameta tutorial](http://scalameta.org/tutorial/#Trees) for more details.
See [Wikipedia](https://en.wikipedia.org/wiki/Abstract_syntax_tree) for a more general definition.

## Syntactic
A {% vocabulary_ref Rule %} is "syntactic" when it does not require information
from type-checking such as resolved names (`println` => `scala.Predef.println`),
types or terms, or inferred implicit arguments.
A syntactic rule can use {% vocabulary_ref Tokens %} and {% vocabulary_ref Tree %}, but not {% vocabulary_ref SemanticdbIndex %}.

## Semantic
A {% vocabulary_ref Rule %} is "semantic" if it requires information from the compiler
such as types, symbols and reported compiler messages.
A semantic rule can use a {% vocabulary_ref SemanticCtx %}.

## SemanticdbIndex
A SemanticdbIndex encapsulates a compilation context, providing
capabilities to perform semantic operations for {% vocabulary_ref Semantic %} rules.
To learn more about SemanticdbIndex and its associated data structures (Symbol, Denotation, ...),
see the [Scalameta tutorial](http://scalameta.org/tutorial/#Mirror).

## SemanticCtx
"SemanticCtx" is the old name for {% vocabulary_ref SemanticdbIndex %}.

## Rule
A scalafix "Rule" can report lint messages and provide auto-fix patches
to violations of some kind of rule/coding style/convention/breaking change.
The default scalafix rues are listed in {% doc_ref Rules %}.

## Rewrites
"Rewrite" is the old name for {% vocabulary_ref Rule %}.
