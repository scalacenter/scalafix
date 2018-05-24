package scalafix.v0

import scala.meta.Symbol

final case class ResolvedSymbol(symbol: Symbol, denotation: Denotation) {
  // TODO: input?
//  def input: Input = Input.Denotation(denotation.signature, symbol)
  def syntax = s"${symbol.syntax} => ${denotation.syntax}"
  def structure = s"""ResolvedSymbol(${symbol.structure}, ${denotation.structure})"""
  override def toString = syntax
}
