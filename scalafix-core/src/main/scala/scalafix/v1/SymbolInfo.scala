package scalafix.v1

import scala.meta.internal.metap.PrinterSymtab
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb._
import scalafix.internal.v1.FromProtobuf
import scala.meta.metap.Format

final class SymbolInfo private[scalafix] (
    private[scalafix] val info: s.SymbolInformation
)(implicit symtab: Symtab) { self =>

  def sym: Symbol = Symbol(info.symbol)
  def owner: Symbol = Symbol(info.symbol).owner
  def displayName: String = info.displayName
  def signature(implicit doc: SemanticDoc): Signature =
    new FromProtobuf().ssignature(info.signature)

  def isScala: Boolean = info.isScala
  def isJava: Boolean = info.isJava
  def isLocal: Boolean = info.isLocal
  def isField: Boolean = info.isField
  def isMethod: Boolean = info.isMethod
  def isConstructor: Boolean = info.isConstructor
  def isMacro: Boolean = info.isMacro
  def isType: Boolean = info.isType
  def isParameter: Boolean = info.isParameter
  def isSelfParameter: Boolean = info.isSelfParameter
  def isTypeParameter: Boolean = info.isTypeParameter
  def isObject: Boolean = info.isObject
  def isPackage: Boolean = info.isPackage
  def isPackageObject: Boolean = info.isPackageObject
  def isClass: Boolean = info.isClass
  def isInterface: Boolean = info.isInterface
  def isTrait: Boolean = info.isTrait
  def isAbstract: Boolean = info.isAbstract
  def isFinal: Boolean = info.isFinal
  def isSealed: Boolean = info.isSealed
  def isImplicit: Boolean = info.isImplicit
  def isLazy: Boolean = info.isLazy
  def isCase: Boolean = info.isCase
  def isCovariant: Boolean = info.isCovariant
  def isContravariant: Boolean = info.isContravariant
  def isVal: Boolean = info.isVal
  def isVar: Boolean = info.isVar
  def isStatic: Boolean = info.isStatic
  def isPrimary: Boolean = info.isPrimary
  def isEnum: Boolean = info.isEnum
  def isDefault: Boolean = info.isDefault
  def isPrivate: Boolean = info.isPrivate
  def isPrivateThis: Boolean = info.isPrivateThis
  def isPrivateWithin: Boolean = info.isPrivateWithin
  def isProtected: Boolean = info.isProtected
  def isProtectedThis: Boolean = info.isPrivateThis
  def isProtectedWithin: Boolean = info.isPrivateWithin
  def isPublic: Boolean = info.isPublic
  def within: Option[Symbol] = info.within.map(Symbol(_))

  override def toString: String = {
    val printerSymtab = new PrinterSymtab {
      override def info(symbol: String): Option[SymbolInformation] =
        symtab.info(Symbol(symbol)).map(_.info)
    }
    s.Print.info(Format.Compact, info, printerSymtab)
  }
}
