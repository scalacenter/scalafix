package scalafix.v1

trait Symtab {
  def info(symbol: Symbol): Option[SymbolInfo]
}
