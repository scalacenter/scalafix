package scalafix.v1

import scala.meta.Tree
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb.Scala._

final class Sym private (val value: String) {
  def isNone: Boolean = value.isNone
  def isRootPackage: Boolean = value.isRootPackage
  def isEmptyPackage: Boolean = value.isEmptyPackage
  def isGlobal: Boolean = value.isGlobal
  def isLocal: Boolean = value.isLocal
  def owner: Sym = Sym(value.owner)
  def info(doc: SemanticDoc): Sym.Info = doc.info(this)

  override def toString: String =
    if (isNone) "Sym.None"
    else value
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: Sym => value == s.value
      case _ => false
    })
  override def hashCode(): Int = value.hashCode
}

object Sym {
  val RootPackage: Sym = new Sym(Symbols.RootPackage)
  val EmptyPackage: Sym = new Sym(Symbols.EmptyPackage)
  val None: Sym = new Sym(Symbols.None)
  def apply(sym: String): Sym = {
    if (sym.isEmpty) Sym.None
    else {
      if (!sym.startsWith("local")) {
        sym.desc // assert that it parses as a symbol
      }
      new Sym(sym)
    }
  }

  object Local {
    def unapply(sym: Sym): Option[Sym] =
      if (sym.isLocal) Some(sym) else scala.None
  }

  object Global {
    def unapply(sym: Sym): Option[(Sym, Sym)] =
      if (sym.isGlobal) {
        val owner = Sym(sym.value.owner)
        Some(owner -> sym)
      } else {
        scala.None
      }
  }

  final class Info private[scalafix] (
      private[scalafix] val info: s.SymbolInformation
  ) {
    def isNone: Boolean = info.symbol.isEmpty
    def sym: Sym = new Sym(info.symbol)
    def owner: Sym = new Sym(info.symbol).owner
    def name: String = info.name
    def kind: Kind = new Kind(info)
    def props: Properties = new Properties(info.properties)
    def access: Access = new Access(info.access)

    override def toString: String = s"Sym.Info(${info.symbol})"
  }
  object Info {
    val empty = new Sym.Info(s.SymbolInformation())
  }

  final class Kind private[Sym] (info: s.SymbolInformation) {
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
    private[this] def k = info.kind
    private[this] def is(property: s.SymbolInformation.Property): Boolean =
      (info.properties & property.value) != 0
    private[this] def isSetter = info.name.endsWith("_=")
  }

  final class Properties private[Sym] (props: Int) {
    def isAbstract: Boolean = is(p.ABSTRACT)
    def isFinal: Boolean = is(p.FINAL)
    def isSealed: Boolean = is(p.SEALED)
    def isImplicit: Boolean = is(p.IMPLICIT)
    def isLazy: Boolean = is(p.LAZY)
    def isCase: Boolean = is(p.CASE)
    def isCovariant: Boolean = is(p.COVARIANT)
    def isContravariant: Boolean = is(p.CONTRAVARIANT)
    def isStatic: Boolean = is(p.STATIC)
    def isPrimary: Boolean = is(p.PRIMARY)
    def isEnum: Boolean = is(p.ENUM)
    def isVal: Boolean = is(p.VAL)
    def isVar: Boolean = is(p.VAR)

    // privates
    private[this] def is(property: s.SymbolInformation.Property): Boolean =
      (props & property.value) != 0
  }

  final class Access private[Sym] (a: s.Access) {
    def isPrivate: Boolean = a.isInstanceOf[s.PrivateAccess]
    def isPrivateThis: Boolean = a.isInstanceOf[s.PrivateThisAccess]
    def privateWithin: Option[Sym] = a match {
      case s.PrivateWithinAccess(symbol) => Some(Sym(symbol))
      case _ => scala.None
    }
    def isProtected: Boolean = a.isInstanceOf[s.ProtectedAccess]
    def isProtectedThis: Boolean = a.isInstanceOf[s.ProtectedThisAccess]
    def protectedWithin: Option[Sym] = a match {
      case s.ProtectedWithinAccess(symbol) => Some(Sym(symbol))
      case _ => scala.None
    }
    def isPublic: Boolean = a.isInstanceOf[s.PublicAccess]
    def isNone: Boolean = a == s.NoAccess
  }

  final class Matcher private (doc: SemanticDoc, syms: Seq[Sym]) {
    def matches(sym: Sym): Boolean =
      syms.contains(sym)
    def matches(tree: Tree): Boolean =
      syms.contains(doc.symbol(tree))

    def unapply(sym: Sym): Boolean = matches(sym)
    def unapply(tree: Tree): Boolean = matches(tree)
  }
  object Matcher {
    def exact(doc: SemanticDoc, sym: Sym) = new Matcher(doc, sym :: Nil)
    def exact(doc: SemanticDoc, syms: Seq[Sym]) = new Matcher(doc, syms)
  }
}
