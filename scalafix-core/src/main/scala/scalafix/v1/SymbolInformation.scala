package scalafix.v1

import scala.meta.internal.metap.PrinterSymtab
import scala.meta.internal.{semanticdb => s}
import scala.meta.metap.Format

import scalafix.internal.v1.SymbolInformationAnnotations._
import scalafix.internal.v1.SymtabFromProtobuf

/**
 * Describes metadata about a symbol such as a method, class or trait.
 *
 * To learn more about SymbolInformation, refer to the SemanticDB specification
 * for
 *
 * <ul> <li> <a
 * href="https://scalameta.org/docs/semanticdb/specification.html#scala-symbolinformation">Scala
 * SymbolInformation</a> </li> <li> <a
 * href="https://scalameta.org/docs/semanticdb/specification.html#java-symbolinformation">Java
 * SymbolInformation</a> </li> </ul>
 *
 * @groupname Ungrouped
 *   Misc
 * @groupprio Ungrouped
 *   9
 *
 * @groupname kind
 *   Kind
 * @groupprio kind
 *   11
 * @groupdesc kind
 *   Describes what kind this symbol is. It is only possible for a symbol to
 *   have one kind. For example, it's not possible for a symbol to be both a
 *   class and an interface.
 *
 * @groupname property
 *   Property
 * @groupprio property
 *   12
 * @groupdesc property
 *   Describes the properties of this symbol. It is possible for a symbol to
 *   have multiple properties. For example, a symbol can be both implicit and
 *   final.
 *
 * @groupname access
 *   Access
 * @groupprio access
 *   13
 * @groupdesc access
 *   Describes the visibility of this symbol. It is only possible for a symbol
 *   to have one access. For example, a symbol is either private or privateThis,
 *   it cannot be both. To learn more about Access, refer to the <a
 *   href="https://scalameta.org/docs/semanticdb/specification.html#scala-access">SemanticDB
 *   specification.</a>
 *
 * @groupname utility
 *   Utility methods
 * @groupprio utility
 *   14
 * @groupdesc utility
 *   Helper methods for frequent queries based on properties, kinds, names and
 *   languages.
 *
 * @groupname language
 *   Language
 * @groupprio language
 *   15
 * @groupdesc language
 *   Describes which language the symbol is defined in.
 */
final class SymbolInformation private[scalafix] (
    private[scalafix] val info: s.SymbolInformation
)(implicit symtab: Symtab) { self =>
  def symbol: Symbol = Symbol(info.symbol)
  def owner: Symbol = Symbol(info.symbol).owner
  def displayName: String = info.displayName
  def signature: Signature =
    new SymtabFromProtobuf(symtab).ssignature(info.signature)
  def annotations: List[Annotation] =
    info.annotations.iterator
      .map(annot => new SymtabFromProtobuf(symtab).sannotation(annot))
      .toList

  /** @group utilty */
  @utility def isSetter: Boolean = displayName.endsWith("_=")
  /** @group utilty */
  @utility def isDef: Boolean =
    isMethod && isScala && !isVal && !isVar

  /** @group language */
  @language def isScala: Boolean = info.isScala
  /** @group language */
  @language def isJava: Boolean = info.isJava

  /** @group kind */
  @kind def isLocal: Boolean = info.isLocal
  /** @group kind */
  @kind def isField: Boolean = info.isField
  /**
   * Returns true for `val` `var` and `def` symbols in Scala, and true for Java
   * methods.
   * @group kind
   */
  @kind def isMethod: Boolean = info.isMethod
  /** @group kind */
  @kind def isConstructor: Boolean = info.isConstructor
  /** @group kind */
  @kind def isMacro: Boolean = info.isMacro
  /** @group kind */
  @kind def isType: Boolean = info.isType
  /** @group kind */
  @kind def isParameter: Boolean = info.isParameter
  /** @group kind */
  @kind def isSelfParameter: Boolean = info.isSelfParameter
  /** @group kind */
  @kind def isTypeParameter: Boolean = info.isTypeParameter
  /** @group kind */
  @kind def isObject: Boolean = info.isObject
  /** @group kind */
  @kind def isPackage: Boolean = info.isPackage
  /** @group kind */
  @kind def isPackageObject: Boolean = info.isPackageObject
  /** @group kind */
  @kind def isClass: Boolean = info.isClass
  /** @group kind */
  @kind def isInterface: Boolean = info.isInterface
  /** @group kind */
  @kind def isTrait: Boolean = info.isTrait

  /** @group property */
  @property def isAbstract: Boolean = info.isAbstract
  /** @group property */
  @property def isFinal: Boolean = info.isFinal
  /** @group property */
  @property def isSealed: Boolean = info.isSealed
  /** @group property */
  @property def isImplicit: Boolean = info.isImplicit
  /** @group property */
  @property def isLazy: Boolean = info.isLazy
  /** @group property */
  @property def isCase: Boolean = info.isCase
  /** @group property */
  @property def isCovariant: Boolean = info.isCovariant
  /** @group property */
  @property def isContravariant: Boolean = info.isContravariant
  /** @group property */
  @property def isVal: Boolean = info.isVal
  /** @group property */
  @property def isVar: Boolean = info.isVar
  /** @group property */
  @property def isStatic: Boolean = info.isStatic
  /** @group property */
  @property def isPrimary: Boolean = info.isPrimary
  /** @group property */
  @property def isEnum: Boolean = info.isEnum
  /** @group property */
  @property def isDefault: Boolean = info.isDefault

  /** @group access */
  @access def isPrivate: Boolean = info.isPrivate
  /** @group access */
  @access def isPrivateThis: Boolean = info.isPrivateThis
  /** @group access */
  @access def isPrivateWithin: Boolean = info.isPrivateWithin
  /** @group access */
  @access def isProtected: Boolean = info.isProtected
  /** @group access */
  @access def isProtectedThis: Boolean = info.isProtectedThis
  /** @group access */
  @access def isProtectedWithin: Boolean = info.isProtectedWithin
  /** @group access */
  @access def isPublic: Boolean = info.isPublic
  /** @group access */
  @access def within: Option[Symbol] = info.within.map(Symbol(_))

  override def toString: String = {
    val printerSymtab = new PrinterSymtab {
      override def info(symbol: String): Option[s.SymbolInformation] =
        symtab.info(Symbol(symbol)).map(_.info)
    }
    s.Print.info(Format.Compact, info, printerSymtab)
  }
}
