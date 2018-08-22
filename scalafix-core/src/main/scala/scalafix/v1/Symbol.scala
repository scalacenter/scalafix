package scalafix.v1

import scala.meta.internal.semanticdb.Scala._

final class Symbol private (val value: String) {
  def isNone: Boolean = value.isNone
  def isRootPackage: Boolean = value.isRootPackage
  def isEmptyPackage: Boolean = value.isEmptyPackage
  def isGlobal: Boolean = value.isGlobal
  def isLocal: Boolean = value.isLocal
  def owner: Symbol = Symbol(value.owner)
  def info(doc: SemanticDoc): Option[SymbolInfo] = doc.info(this)

  override def toString: String =
    if (isNone) "Symbol.None"
    else value
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: Symbol => value == s.value
      case _ => false
    })
  override def hashCode(): Int = value.##
}

object Symbol {
  val RootPackage: Symbol = new Symbol(Symbols.RootPackage)
  val EmptyPackage: Symbol = new Symbol(Symbols.EmptyPackage)
  val None: Symbol = new Symbol(Symbols.None)
  def apply(sym: String): Symbol = {
    if (sym.isEmpty) Symbol.None
    else new Symbol(sym)
  }

  object Local {
    def unapply(sym: Symbol): Option[Symbol] =
      if (sym.isLocal) Some(sym) else scala.None
  }

  object Global {
    def unapply(sym: Symbol): Option[(Symbol, Symbol)] =
      if (sym.isGlobal) {
        val owner = Symbol(sym.value.owner)
        Some(owner -> sym)
      } else {
        scala.None
      }
  }
}
