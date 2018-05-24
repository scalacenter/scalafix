package scalafix.v0

import scala.meta.Symbol

final case class ResolvedSymbol(symbol: Symbol, denotation: Denotation) {
  def syntax = s"${symbol.syntax} => ${denotation.syntax}"
  def structure =
    s"""ResolvedSymbol(${symbol.structure}, ${denotation.structure})"""
  override def toString = syntax
}
