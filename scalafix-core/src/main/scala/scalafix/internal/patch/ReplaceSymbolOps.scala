package scalafix.internal.patch

import scala.meta._
import scala.meta.internal.trees._

import scalafix.internal.util.SymbolOps
import scalafix.internal.util.SymbolOps.Root
import scalafix.internal.util.SymbolOps.SignatureName
import scalafix.patch.Patch
import scalafix.patch.Patch.internal.ReplaceSymbol
import scalafix.syntax._
import scalafix.v0._

object ReplaceSymbolOps {
  private object Select {
    def unapply(arg: Ref): Option[(Ref, Name)] = arg match {
      case Term.Select(a: Ref, b) => Some(a -> b)
      case Type.Select(a, b) => Some(a -> b)
      case _ => None
    }
  }

  def naiveMoveSymbolPatch(
      moveSymbols: Seq[ReplaceSymbol]
  )(implicit ctx: RuleCtx, index: SemanticdbIndex): Patch = {
    if (moveSymbols.isEmpty) return Patch.empty
    val moves: Map[String, Symbol.Global] =
      moveSymbols.iterator.flatMap {
        case ReplaceSymbol(
              term @ Symbol.Global(_, Signature.Method(_, _)),
              to
            ) =>
          (term.syntax -> to) :: Nil
        case ReplaceSymbol(
              term @ Symbol.Global(qual, Signature.Term(name)),
              to
            ) =>
          (term.syntax -> to) ::
            (Symbol.Global(qual, Signature.Type(name)).syntax -> to) ::
            Nil
      }.toMap
    def loop(ref: Ref, sym: Symbol, isImport: Boolean): (Patch, Symbol) = {
      (ref, sym) match {
        // same length
        case (a @ Name(_), Symbol.Global(Symbol.None, SignatureName(b))) =>
          ctx.replaceTree(a, b) -> Symbol.None
        // ref is shorter
        case (a @ Name(_), sym @ Symbol.Global(_, SignatureName(b))) =>
          if (isImport) {
            val qual = SymbolOps.toTermRef(sym)
            ctx.replaceTree(a, qual.syntax) -> Symbol.None
          } else {
            ctx.replaceTree(a, b) -> sym
          }
        // ref is longer
        case (
              Select(qual, Name(_)),
              Symbol.Global(Symbol.None, SignatureName(b))
            ) =>
          ctx.replaceTree(qual, b) -> Symbol.None
        // recurse
        case (
              Select(qual: Ref, a @ Name(_)),
              Symbol.Global(symQual, SignatureName(b))
            ) =>
          val (patch, toImport) = loop(qual, symQual, isImport)
          (patch + ctx.replaceTree(a, b)) -> toImport
      }
    }
    object Moved {
      def unapply(arg: Symbol): Option[Symbol.Global] = {
        moves.get(arg.syntax).orElse(moves.get(arg.normalized.syntax))
      }
    }
    object Move {
      def unapply(name: Name): Option[Symbol.Global] = {
        val result = name.symbol.iterator
          .flatMap {
            case Symbol.Multi(syms) => syms
            case els => els :: Nil
          }
          .collectFirst { case Moved(out) =>
            out
          }
        result
      }
    }
    object Identifier {
      def unapply(tree: Tree): Option[(Name, Symbol)] = tree match {
        case n: Name => n.symbol.map(s => n -> s)
        case Init(n: Name, _, _) => n.symbol.map(s => n -> s)
        case _ => None
      }
    }
    val patches = ctx.tree.collect { case n @ Move(to) =>
      // was this written as `to = "blah"` instead of `to = _root_.blah`
      val isSelected = to match {
        case Root(_) => false
        case _ => true
      }
      n.parent match {
        case Some(i @ Importee.Name(_)) =>
          ctx.removeImportee(i)
        case Some(i @ Importee.Rename(name, rename)) =>
          i.parent match {
            case Some(Importer(ref, `i` :: Nil)) =>
              Patch.replaceTree(ref, SymbolOps.toTermRef(to.owner).syntax) +
                Patch.replaceTree(name, to.signature.name)
            case Some(Importer(_, _)) =>
              Patch.removeImportee(i) +
                Patch.addGlobalImport(
                  Importer(
                    SymbolOps.toTermRef(to.owner),
                    List(
                      Importee.Rename(Term.Name(to.signature.name), rename)
                    )
                  )
                )
            case _ => Patch.empty
          }
        case Some(parent @ Select(_, `n`)) if isSelected =>
          val (patch, imp) =
            loop(parent, to, isImport = n.parents.exists(_.is[Import]))
          ctx.addGlobalImport(imp) + patch
        case Some(Identifier(parent, Symbol.Global(_, sig)))
            if sig.name != parent.value =>
          Patch.empty // do nothing because it was a renamed symbol
        case Some(_) =>
          val addImport =
            if (n.isDefinition) Patch.empty
            else ctx.addGlobalImport(to)
          addImport + ctx.replaceTree(n, to.signature.name)
        case _ =>
          Patch.empty
      }
    }
    patches.asPatch
  }
}
