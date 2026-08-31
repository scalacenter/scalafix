package scalafix.internal.symtab

import scala.meta.internal.{semanticdb => s}

/**
 * Symbol table capability owned by scalafix-core.
 *
 * Deliberately compiler-free: core must not depend on `semanticdb-scalac-core`,
 * so this declares only what core needs and scalafix-reflect adapts scalameta's
 * `scala.meta.internal.symtab.SymbolTable` onto it. It must NOT reuse
 * scalameta's fully-qualified name — a same-FQCN copy shadows the real trait
 * wherever both are on the classpath, which breaks `GlobalSymbolTable` at link
 * time once the real trait carries concrete members.
 */
trait SymbolTable {

  /**
   * Returns the SymbolInformation for the given symbol, or None if the symbol
   * is missing.
   */
  def info(symbol: String): Option[s.SymbolInformation]
}
