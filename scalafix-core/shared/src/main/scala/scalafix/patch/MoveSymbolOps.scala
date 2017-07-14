package scalafix.patch

import scalafix.config._
import scala.collection.mutable
import scala.meta._
import scalafix._
import scalafix.internal.util.SymbolOps.BottomSymbol
import scalafix.internal.util.SymbolOps.SignatureName
import scalafix.patch.TreePatch.MoveSymbol
import scalafix.syntax._
import metaconfig.Conf
import metaconfig.ConfDecoder

object MoveSymbolOps {
  private object Select {
    def unapply(arg: Ref): Option[(Ref, Name)] = arg match {
      case Term.Select(a: Ref, b) => Some(a -> b)
      case Type.Select(a, b) => Some(a -> b)
      case _ => None
    }
  }

  def naiveMoveSymbolPatch(moveSymbols: Seq[MoveSymbol])(
      implicit ctx: RewriteCtx,
      mirror: Database): Patch = {
    val moves: Map[Symbol, Symbol] =
      moveSymbols.toIterator.flatMap {
        case MoveSymbol(term @ Symbol.Global(qual, Signature.Term(name)), to) =>
          (term -> to) ::
            (Symbol.Global(qual, Signature.Type(name)) -> to) ::
            Nil
      }.toMap
    def loop(ref: Ref, sym: Symbol): (Patch, Symbol) = {
      (ref, sym) match {
        // same length
        case (a @ Name(_), Symbol.Global(Symbol.None, SignatureName(b))) =>
          ctx.rename(a, b) -> Symbol.None
        // ref is shorter
        case (a @ Name(_), sym @ Symbol.Global(qual, SignatureName(b))) =>
          ctx.rename(a, b) -> sym
        // ref is longer
        case (
            Select(qual, Name(_)),
            Symbol.Global(Symbol.None, SignatureName(b))) =>
          ctx.replaceTree(qual, b) -> Symbol.None
        // recurse
        case (
            Select(qual: Ref, a @ Name(_)),
            Symbol.Global(symQual, SignatureName(b))) =>
          val (patch, toImport) = loop(qual, symQual)
          (patch + ctx.rename(a, b)) -> toImport
      }
    }
    object Move {
      def unapply(name: Name): Option[Symbol] = {
        val result = mirror.names
          .get(name.pos)
          .toIterator
          .flatMap {
            case Symbol.Multi(syms) => syms
            case els => els :: Nil
          }
          .map(_.normalized)
          .collectFirst { case x if moves.contains(x) => moves(x) }
        result
      }
    }
    val toImport = mutable.Set.empty[Symbol]
    val patches = ctx.tree.collect {
      case n @ Move(to) =>
        n.parent match {
          case Some(i @ Importee.Name(_)) =>
            ctx.removeImportee(i)
          case Some(parent @ Select(_, `n`)) =>
            val (patch, imp) = loop(parent, to)
            toImport += imp
            patch
          case _ =>
            toImport += to
            ctx.rename(n, to.name)
        }
    }
    val importPatch = toImport.foldLeft(Patch.empty) {
      case (p, BottomSymbol()) => p
      case (p, Symbol.Global(BottomSymbol(), _)) => p
      case (p, sym) => p + ctx.addGlobalImport(sym)
    }
    importPatch ++ patches
  }
}
