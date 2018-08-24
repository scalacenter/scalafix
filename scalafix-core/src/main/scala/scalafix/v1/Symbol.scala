package scalafix.v1

import metaconfig.ConfDecoder
import scala.meta.internal.semanticdb.Scala._
import scalafix.internal.util.SymbolOps

final class Symbol private (val value: String) {
  def isNone: Boolean = value.isNone
  def isRootPackage: Boolean = value.isRootPackage
  def isEmptyPackage: Boolean = value.isEmptyPackage
  def isGlobal: Boolean = value.isGlobal
  def isLocal: Boolean = value.isLocal
  def owner: Symbol = Symbol(value.owner)
  def displayName: String = value.desc.name.value
  def info(implicit doc: SemanticDoc): Option[SymbolInfo] =
    doc.internal.info(this)
  def normalized: Symbol = SymbolOps.normalize(this)
  def asNonEmpty: Option[Symbol] =
    if (isNone) None
    else Some(this)

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
  implicit val decoder: ConfDecoder[Symbol] =
    ConfDecoder.stringConfDecoder.map { string =>
      if (string.isGlobal) Symbol(string)
      else Symbol(string + ".")
    }
}
