package scalafix.v1

import scala.meta.internal.metap.PrinterSymtab
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb._
import scalafix.internal.v1.SymtabFromProtobuf
import scala.meta.metap.Format

/**
 * Describes metadata about a symbol, which is a definitions such as a method, class or trait.
 *
 * To learn more about SymbolInfo, refer to the SemanticDB specification for
 *
 * <ul>
 *   <li>
 *     <a href="https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-symbolinformation">Scala SymbolInformation</a>
 *   </li>
 *   <li>
 *     <a href="https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#java-symbolinformation">Java SymbolInformation</a>
 *   </li>
 * </ul>
 *
 * @groupname Ungrouped Misc
 * @groupprio Ungrouped 9
 *
 * @groupname language Language
 * @groupprio language 14
 * @groupdesc language Describes which language the symbol is defined in.
 *
 * @groupname kind Kind
 * @groupprio kind 11
 * @groupdesc kind Describes what kind this symbol is. It is only possible for a symbol to have one kind.
 *            For example, it's not possible for a symbol to be both a class and an interface.
 *
 * @groupname property Property
 * @groupprio property 12
 * @groupdesc property Describes the properties of this symbol. It is possible for a symbol to have multiple properties.
 *            For example, a symbol can be both implicit and final.
 *
 * @groupname access Access
 * @groupprio access 13
 * @groupdesc access Describes the visibility of this symbol. It is only possible for a symbol to have one access.
 *            For example, a symbol is either private or privateThis, it cannot be both.
 *
 *           To learn more about Access, refer to the <a href="https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#scala-access">SemanticDB specification.</a>
 */
final class SymbolInfo private[scalafix] (
    private[scalafix] val info: s.SymbolInformation
)(implicit symtab: Symtab) { self =>
  private[this] val x = 2

  def symbol: Symbol = Symbol(info.symbol)
  def owner: Symbol = Symbol(info.symbol).owner
  def displayName: String = info.displayName
  def signature: Signature =
    new SymtabFromProtobuf(symtab).ssignature(info.signature)
  def annotations: List[Annotation] =
    info.annotations.iterator
      .map(annot => new SymtabFromProtobuf(symtab).sannotation(annot))
      .toList
  def isSetter: Boolean = displayName.endsWith("_=")

  /** @group language */
  def isScala: Boolean = info.isScala
  /** @group language */
  def isJava: Boolean = info.isJava

  /** @group kind */
  def isLocal: Boolean = info.isLocal
  /** @group kind */
  def isField: Boolean = info.isField
  /**
   * Returns true for `val`, `var` and `def` symbols in Scala, and true for Java methods.
   *
   * Use the absence of isVal and isVar to detect a `def` in Scala.
   *
   * @group kind
   */
  def isMethod: Boolean = info.isMethod
  /** @group kind */
  def isConstructor: Boolean = info.isConstructor
  /** @group kind */
  def isMacro: Boolean = info.isMacro
  /** @group kind */
  def isType: Boolean = info.isType
  /** @group kind */
  def isParameter: Boolean = info.isParameter
  /** @group kind */
  def isSelfParameter: Boolean = info.isSelfParameter
  /** @group kind */
  def isTypeParameter: Boolean = info.isTypeParameter
  /** @group kind */
  def isObject: Boolean = info.isObject
  /** @group kind */
  def isPackage: Boolean = info.isPackage
  /** @group kind */
  def isPackageObject: Boolean = info.isPackageObject
  /** @group kind */
  def isClass: Boolean = info.isClass
  /** @group kind */
  def isInterface: Boolean = info.isInterface
  /** @group kind */
  def isTrait: Boolean = info.isTrait

  /** @group property */
  def isAbstract: Boolean = info.isAbstract
  /** @group property */
  def isFinal: Boolean = info.isFinal
  /** @group property */
  def isSealed: Boolean = info.isSealed
  /** @group property */
  def isImplicit: Boolean = info.isImplicit
  /** @group property */
  def isLazy: Boolean = info.isLazy
  /** @group property */
  def isCase: Boolean = info.isCase
  /** @group property */
  def isCovariant: Boolean = info.isCovariant
  /** @group property */
  def isContravariant: Boolean = info.isContravariant
  /** @group property */
  def isVal: Boolean = info.isVal
  /** @group property */
  def isVar: Boolean = info.isVar
  /** @group property */
  def isStatic: Boolean = info.isStatic
  /** @group property */
  def isPrimary: Boolean = info.isPrimary
  /** @group property */
  def isEnum: Boolean = info.isEnum
  /** @group property */
  def isDefault: Boolean = info.isDefault

  /** @group access */
  def isPrivate: Boolean = info.isPrivate
  /** @group access */
  def isPrivateThis: Boolean = info.isPrivateThis
  /** @group access */
  def isPrivateWithin: Boolean = info.isPrivateWithin
  /** @group access */
  def isProtected: Boolean = info.isProtected
  /** @group access */
  def isProtectedThis: Boolean = info.isPrivateThis
  /** @group access */
  def isProtectedWithin: Boolean = info.isPrivateWithin
  /** @group access */
  def isPublic: Boolean = info.isPublic
  /** @group access */
  def within: Option[Symbol] = info.within.map(Symbol(_))

  override def toString: String = {
    val printerSymtab = new PrinterSymtab {
      override def info(symbol: String): Option[SymbolInformation] =
        symtab.info(Symbol(symbol)).map(_.info)
    }
    s.Print.info(Format.Compact, info, printerSymtab)
  }
}
