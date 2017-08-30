package scalafix
package util

import scala.meta.Symbol
import scala.meta.Tree
import scalafix.internal.util.SymbolOps

class SymbolMatcher(symbols: List[Symbol])(implicit sctx: SemanticCtx) {
  def matches(tree: Tree): Boolean =
    sctx.symbol(tree).fold(false)(matches)
  def matches(symbol: Symbol): Boolean =
    symbols.exists(x => SymbolOps.isSameNormalized(x, symbol))
  def unapply(tree: Tree): Option[Tree] =
    if (matches(tree)) Some(tree)
    else None
}

object SymbolMatcher {
  def apply(symbol: Symbol*)(implicit sctx: SemanticCtx): SymbolMatcher =
    new SymbolMatcher(symbol.toList)
}
