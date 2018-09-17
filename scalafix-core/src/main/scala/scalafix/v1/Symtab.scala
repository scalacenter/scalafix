package scalafix.v1

/**
 * A symbol table that returns SymbolInformation given a Symbol.
 */
trait Symtab {
  def info(symbol: Symbol): Option[SymbolInformation]
}
