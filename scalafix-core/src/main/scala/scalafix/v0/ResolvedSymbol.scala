package scalafix.v0

final case class ResolvedSymbol(symbol: Symbol, denotation: Denotation) {
  def syntax: String = s"${symbol.syntax} => ${denotation.syntax}"
  def structure: String =
    s"""ResolvedSymbol(${symbol.structure}, ${denotation.structure})"""
  override def toString = syntax
}
