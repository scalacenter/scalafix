package scalafix.v1

import scala.meta.Tree
import scalafix.internal.util.SymbolOps

trait SymbolMatcher { self =>

  def matches(sym: Symbol): Boolean
  final def matches(tree: Tree)(implicit sdoc: SemanticDocument): Boolean =
    matches(sdoc.internal.symbol(tree))

  final def unapply(sym: Symbol): Option[Symbol] =
    if (matches(sym)) Some(sym)
    else None
  final def unapply(tree: Tree)(implicit sdoc: SemanticDocument): Option[Tree] =
    if (matches(tree.symbol)) Some(tree)
    else None

  final def +(other: SymbolMatcher): SymbolMatcher = new SymbolMatcher {
    override def matches(sym: Symbol): Boolean =
      self.matches(sym) || other.matches(sym)
    override def toString: String = s"CombinedSymbolMatcher($self,$other)"
  }
}

object SymbolMatcher {
  def exact(symbols: String*): SymbolMatcher = {
    val matchesSymbols = symbols.toSet
    new SymbolMatcher {
      override def matches(sym: Symbol): Boolean =
        matchesSymbols(sym.value)
      override def toString: String =
        s"ExactSymbolMatcher(${matchesSymbols.mkString(",")})"
    }
  }

  def normalized(symbols: String*): SymbolMatcher = {
    val matchesSymbols =
      symbols.iterator.map { s =>
        val symbol = SymbolOps.inferTrailingDot(s)
        SymbolOps.normalize(Symbol(symbol))
      }.toSet
    new SymbolMatcher {
      override def matches(sym: Symbol): Boolean =
        matchesSymbols(SymbolOps.normalize(sym))
      override def toString: String =
        s"NormalizedSymbolMatcher(${matchesSymbols.mkString(",")})"
    }
  }
}
