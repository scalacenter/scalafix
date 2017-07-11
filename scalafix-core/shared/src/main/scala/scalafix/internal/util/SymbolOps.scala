package scalafix.internal.util

import scala.meta._

object SymbolOps {
  object SignatureName {
    def unapply(arg: Signature): Option[String] = arg match {
      case Signature.Term(a) => Some(a)
      case Signature.Type(a) => Some(a)
      case _ => None
    }
  }
  object BottomSymbol {
    def unapply(arg: Symbol): Boolean = arg match {
      case Symbol.Global(Symbol.None, Signature.Term("_root_")) => true
      case _ => false
    }
  }
  def toTermRef(symbol: Symbol): Term.Ref = {
    symbol match {
      case Symbol.Global(BottomSymbol(), signature) =>
        Term.Name(signature.name)
      case Symbol.Global(qual, signature) =>
        Term.Select(toTermRef(qual), Term.Name(signature.name))
    }
  }
  def toImporter(symbol: Symbol): Option[Importer] = {
    symbol match {
      case Symbol.Global(BottomSymbol(), SignatureName(name)) =>
        None
      case Symbol.Global(qual, SignatureName(name)) =>
        Some(
          Importer(
            toTermRef(qual),
            List(Importee.Name(Name.Indeterminate(name)))))
      case _ => None
    }
  }
}
