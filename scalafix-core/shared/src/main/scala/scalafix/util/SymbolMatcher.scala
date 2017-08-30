package scalafix
package util

import scala.meta.Symbol
import scala.meta.Tree
import scalafix.internal.util.SymbolOps

class SymbolMatcher(symbols: List[Symbol])(implicit sctx: SemanticCtx) {
  def matches(symbol: Symbol): Boolean =
    symbols.exists(x => SymbolOps.isSameNormalized(x, symbol))
  def unapply(tree: Tree): Option[Tree] =
    sctx.symbol(tree).filter(matches).map(_ => tree)
}

object SymbolMatcher {
  def apply(symbol: Symbol*)(implicit sctx: SemanticCtx): SymbolMatcher =
    new SymbolMatcher(symbol.toList)
}
