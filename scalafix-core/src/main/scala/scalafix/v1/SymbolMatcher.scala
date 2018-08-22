package scalafix.v1

import scala.meta.Tree

final class SymbolMatcher private (doc: SemanticDoc, syms: Seq[Symbol]) {
  def matches(sym: Symbol): Boolean =
    syms.contains(sym)
  def matches(tree: Tree): Boolean =
    syms.contains(doc.symbol(tree))

  def unapply(sym: Symbol): Boolean = matches(sym)
  def unapply(tree: Tree): Boolean = matches(tree)
}

object SymbolMatcher {
  def exact(doc: SemanticDoc, sym: Symbol) = new SymbolMatcher(doc, sym :: Nil)
  def exact(doc: SemanticDoc, syms: Seq[Symbol]) = new SymbolMatcher(doc, syms)
}