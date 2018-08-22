package scalafix.v1

import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.{semanticdb => s}

final class SymbolKind private[scalafix] (info: s.SymbolInformation) {
  def isField: Boolean = k.isField
  def isMethod: Boolean = k.isMethod
  def isConstructor: Boolean = k.isConstructor
  def isMacro: Boolean = k.isMacro
  def isType: Boolean = k.isType
  def isParameter: Boolean = k.isParameter
  def isSelfParameter: Boolean = k.isSelfParameter
  def isTypeParameter: Boolean = k.isTypeParameter
  def isPackage: Boolean = k.isPackage
  def isPackageObject: Boolean = k.isPackageObject
  def isClass: Boolean = k.isClass
  def isObject: Boolean = k.isObject
  def isTrait: Boolean = k.isTrait
  def isInterface: Boolean = k.isInterface

  override def toString: String = s"Sym.Kind(${info.symbol})"

  // privates
  private[this] def k: SymbolInformation.Kind = info.kind
}
