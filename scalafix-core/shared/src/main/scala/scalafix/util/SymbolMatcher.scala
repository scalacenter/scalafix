package scalafix
package util

import scala.meta.Symbol
import scala.meta.Tree
import scalafix.internal.util.SymbolOps

/**
  * Utility to match against a particular symbol.
  *
  * Can be used both in pattern matching and regular condition testing.
  * {{{
  *   val myMethod = SymbolMatcher(Symbol("_root_.myMethod"))
  *   myMethod.matches(Tree)
  *   Tree match {
  *     case myMethod(_) => // act on tree
  *   }
  *   myMethod.matches(Tree)
  * }}}
  * @param symbols the symbols to match against.
  * @param isEqual which equality to use, can be normalized or structural.
  * @param sctx the semantic context to lookup symbols of trees.
  */
final class SymbolMatcher(
    symbols: List[Symbol],
    isEqual: (Symbol, Symbol) => Boolean)(implicit sctx: SemanticCtx) {
  def matches(tree: Tree): Boolean =
    sctx.symbol(tree).fold(false)(matches)
  def matches(symbol: Symbol): Boolean =
    symbols.exists(x => isEqual(x, symbol))
  // Returns Option[Tree] to aid composing multiple unapplies, example:
  // case myMethod(Name(n)) =>
  // If it returned a Boolean, then it would not be possible to deconstruct @
  // bindings, example:
  // case n @ myMethod() =>  // impossible to deconstruct `n`
  def unapply(tree: Tree): Option[Tree] =
    if (matches(tree)) Some(tree)
    else None
}

object SymbolMatcher {

  /** Construct SymbolMatcher with structural equality. */
  def exact(symbol: Symbol*)(implicit sctx: SemanticCtx): SymbolMatcher =
    new SymbolMatcher(symbol.toList, _ == _)

  /** Construct SymbolMatcher with normalized equality. */
  def normalized(symbol: Symbol*)(implicit sctx: SemanticCtx): SymbolMatcher =
    new SymbolMatcher(symbol.toList, SymbolOps.isSameNormalized)
}
