package scalafix.internal.util

import scala.meta.internal.{semanticdb => s}

/**
  * A table to lookup information about symbols.
  *
  * This trait is not exposed in the public Scalafix API because s.SymbolInformation
  * is not part of the public API. Expect breaking changes.
  */
trait SymbolTable {
  def info(symbol: String): Option[s.SymbolInformation]
}

object SymbolTable {
  val empty: SymbolTable = new SymbolTable {
    override def info(symbol: String): Option[s.SymbolInformation] = None
  }
}
