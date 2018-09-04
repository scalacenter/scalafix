package scalafix.v1

/**
 * A symbol table that returns SymbolInfo given a Symbol.
 */
trait Symtab {
  def info(symbol: Symbol): Option[SymbolInfo]
}
