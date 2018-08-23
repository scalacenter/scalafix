package scalafix.v1

import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb._
import scalafix.internal.v1.FromProtobuf
import scala.meta.internal.semanticdb.{Language => l}
import scala.meta.internal.semanticdb.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}
import scala.meta.metap.Format

final class SymbolInfo private[scalafix] (
    private[scalafix] val info: s.SymbolInformation
) { self =>

  def sym: Symbol = Symbol(info.symbol)
  def owner: Symbol = Symbol(info.symbol).owner
  def displayName: String = info.displayName
  def signature(implicit doc: SemanticDoc): Signature =
    new FromProtobuf(doc).ssignature(info.signature)

  // FIXME: use these methods instead of copy-paste: https://github.com/scalameta/scalameta/pull/1750
  def isScala: Boolean = info.language == l.SCALA
  def isJava: Boolean = info.language == l.JAVA
  def isLocal: Boolean = info.kind == k.LOCAL
  def isField: Boolean = info.kind == k.FIELD
  def isMethod: Boolean = info.kind == k.METHOD
  def isConstructor: Boolean = info.kind == k.CONSTRUCTOR
  def isMacro: Boolean = info.kind == k.MACRO
  def isType: Boolean = info.kind == k.TYPE
  def isParameter: Boolean = info.kind == k.PARAMETER
  def isSelfParameter: Boolean = info.kind == k.SELF_PARAMETER
  def isTypeParameter: Boolean = info.kind == k.TYPE_PARAMETER
  def isObject: Boolean = info.kind == k.OBJECT
  def isPackage: Boolean = info.kind == k.PACKAGE
  def isPackageObject: Boolean = info.kind == k.PACKAGE_OBJECT
  def isClass: Boolean = info.kind == k.CLASS
  def isInterface: Boolean = info.kind == k.INTERFACE
  def isTrait: Boolean = info.kind == k.TRAIT
  def isAbstract: Boolean = (info.properties & p.ABSTRACT.value) != 0
  def isFinal: Boolean = (info.properties & p.FINAL.value) != 0
  def isSealed: Boolean = (info.properties & p.SEALED.value) != 0
  def isImplicit: Boolean = (info.properties & p.IMPLICIT.value) != 0
  def isLazy: Boolean = (info.properties & p.LAZY.value) != 0
  def isCase: Boolean = (info.properties & p.CASE.value) != 0
  def isCovariant: Boolean = (info.properties & p.COVARIANT.value) != 0
  def isContravariant: Boolean = (info.properties & p.CONTRAVARIANT.value) != 0
  def isVal: Boolean = (info.properties & p.VAL.value) != 0
  def isVar: Boolean = (info.properties & p.VAR.value) != 0
  def isStatic: Boolean = (info.properties & p.STATIC.value) != 0
  def isPrimary: Boolean = (info.properties & p.PRIMARY.value) != 0
  def isEnum: Boolean = (info.properties & p.ENUM.value) != 0
  def isDefault: Boolean = (info.properties & p.DEFAULT.value) != 0
  def isPrivate: Boolean =
    info.access.isInstanceOf[PrivateAccess]
  def isPrivateThis: Boolean =
    info.access.isInstanceOf[PrivateThisAccess]
  def isPrivateWithin: Boolean =
    info.access.isInstanceOf[s.PrivateWithinAccess]
  def isProtected: Boolean =
    info.access.isInstanceOf[ProtectedAccess]
  def isProtectedThis: Boolean =
    info.access.isInstanceOf[s.ProtectedThisAccess]
  def isProtectedWithin: Boolean =
    info.access.isInstanceOf[s.ProtectedWithinAccess]
  def isPublic: Boolean =
    info.access.isInstanceOf[PublicAccess]
  def within: Option[String] = info.access match {
    case PrivateWithinAccess(symbol) => Some(symbol)
    case ProtectedWithinAccess(symbol) => Some(symbol)
    case _ => None
  }

  override def toString: String = {
    val symtab = new scala.meta.internal.metap.PrinterSymtab {
      def info(symbol: String): Option[SymbolInformation] =
        if (symbol == self.info.symbol) Some(self.info)
        else None
    }
    s.Print.info(Format.Compact, info, symtab)
  }
}
