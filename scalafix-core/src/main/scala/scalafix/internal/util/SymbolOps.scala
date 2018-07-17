package scalafix.internal.util

import scala.meta._
import scalafix.v0._

object SymbolOps {
  object SignatureName {
    def unapply(arg: Signature): Option[String] = arg match {
      case Signature.Term(a) => Some(a)
      case Signature.Type(a) => Some(a)
      case Signature.Package(a) => Some(a)
      case _ => None
    }
  }
  object SymbolType {
    def unapply(arg: Symbol): Boolean = arg match {
      case Symbol.Global(_, Signature.Type(_)) => true
      case _ => false
    }
  }
  object Root {
    def apply(): Symbol.Global =
      Symbol.Global(Symbol.None, Signature.Package("_root_"))
    def apply(signature: Signature): Symbol.Global =
      Symbol.Global(apply(), signature)
    def unapply(arg: Symbol): Option[Signature] = arg match {
      case Symbol.Global(
          Symbol.Global(Symbol.None, Signature.Package("_root_")),
          sig) =>
        Some(sig)
      case _ =>
        None
    }
  }
  def toTermRef(symbol: Symbol): Term.Ref = {
    symbol match {
      case Root(signature) =>
        Term.Name(signature.name)
      case Symbol.Global(qual, signature) =>
        Term.Select(toTermRef(qual), Term.Name(signature.name))
    }
  }
  def toImporter(symbol: Symbol): Option[Importer] = {
    symbol match {
      case Root(SignatureName(name)) =>
        None
      case Symbol.Global(qual, SignatureName(name)) =>
        Some(
          Importer(
            toTermRef(qual),
            List(Importee.Name(Name.Indeterminate(name)))))
      case _ => None
    }
  }
  def normalize(symbol: Symbol): Symbol = symbol match {
    case Symbol.Multi(syms) =>
      Symbol.Multi(syms.map(normalize))
    case Symbol.Global(sym, Signature.Term("package")) =>
      normalize(sym)
    case Symbol.Global(sym, sig) =>
      Symbol.Global(normalize(sym), Signature.Term(sig.name))
    case x => x
  }
  def underlyingSymbols(symbol: Symbol): Seq[Symbol] = symbol match {
    case Symbol.Multi(symbols) => symbols
    case _ => List(symbol)
  }
  def isSameNormalized(a: Symbol, b: Symbol): Boolean = {
    val syms = underlyingSymbols(a).map(normalize)
    val otherSyms = underlyingSymbols(b).map(normalize)
    syms.exists(otherSyms.contains)
  }
}
