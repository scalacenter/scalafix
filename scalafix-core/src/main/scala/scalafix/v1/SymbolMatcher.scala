package scalafix.v1

import scala.meta.Tree
import scalafix.internal.util.SymbolOps

trait SymbolMatcher {

  def matches(sym: Symbol): Boolean
  final def matches(tree: Tree)(implicit sdoc: SemanticDoc): Boolean =
    matches(sdoc.internal.symbol(tree))

  final def unapply(sym: Symbol): Boolean =
    matches(sym)
  final def unapply(tree: Tree)(implicit sdoc: SemanticDoc): Boolean =
    unapply(sdoc.internal.symbol(tree))

  final def +(other: SymbolMatcher): SymbolMatcher = new SymbolMatcher {
    override def matches(sym: Symbol): Boolean =
      this.matches(sym) || other.matches(sym)
  }
}

object SymbolMatcher {
  def exact(symbols: String*): SymbolMatcher = {
    val matchesSymbols = symbols.toSet
    new SymbolMatcher {
      override def matches(sym: Symbol): Boolean =
        matchesSymbols(sym.value)
      override def toString: String =
        s"ExactSymbolMatcher($matchesSymbols)"
    }
  }

  def normalized(symbols: String*): SymbolMatcher = {
    val matchesSymbols =
      symbols.iterator.map(s => SymbolOps.normalize(Symbol(s))).toSet
    new SymbolMatcher {
      override def matches(sym: Symbol): Boolean =
        matchesSymbols(SymbolOps.normalize(sym))
      override def toString: String =
        s"NormalizedSymbolMatcher($matchesSymbols)"
    }
  }
}
