package scalafix.v1

import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.{semanticdb => s}

final class SymbolKind private[scalafix] (
    private[scalafix] val info: s.SymbolInformation
) {
  def isLocal: Boolean = k.isLocal
  def isField: Boolean = k.isField
  def isMethod: Boolean = k.isMethod
  def isConstructor: Boolean = k.isConstructor
  def isMacro: Boolean = k.isMacro
  def isType: Boolean = k.isType
  def isParameter: Boolean = k.isParameter
  def isSelfParameter: Boolean = k.isSelfParameter
  def isTypeParameter: Boolean = k.isTypeParameter
  def isObject: Boolean = k.isObject
  def isPackage: Boolean = k.isPackage
  def isPackageObject: Boolean = k.isPackageObject
  def isClass: Boolean = k.isClass
  def isTrait: Boolean = k.isTrait
  def isInterface: Boolean = k.isInterface

  override def toString: String = s"SymbolKind(${info.kind.toString()})"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: SymbolKind => this.info.kind == s.info.kind
      case _ => false
    })
  override def hashCode(): Int = info.kind.##

  // privates
  private[this] def k: SymbolInformation.Kind = info.kind
}
