package scalafix.internal.patch

import scala.annotation.tailrec

import scala.meta._
import scala.meta.internal.trees._

import scalafix.internal.util.SymbolOps
import scalafix.internal.util.SymbolOps.Root
import scalafix.internal.util.SymbolOps.SignatureName
import scalafix.patch.Patch
import scalafix.patch.Patch.internal.ReplaceSymbol
import scalafix.syntax._
import scalafix.util.TreeOps
import scalafix.v0._

object ReplaceSymbolOps {
  private object Select {
    def unapply(arg: Ref): Option[(Ref, Name)] = arg match {
      case Term.Select(a: Ref, b) => Some(a -> b)
      case Type.Select(a, b) => Some(a -> b)
      case _ => None
    }
  }

  @tailrec
  private def extractImports(stats: List[Stat]): Seq[Import] = stats match {
    case (p: Pkg) :: Nil => extractImports(p.body.stats)
    case _ => stats.collect { case i: Import => i }
  }

  private def getNamesOfExplicitlyImportedSymbols(
      tree: Tree,
      isMoved: Name => Boolean
  ): Set[String] = {
    val globalImports = tree match {
      case t: Pkg => extractImports(t.body.stats)
      case t: Source => extractImports(t.stats)
      case _ => Nil
    }

    // pre-compute global imported symbols for O(1) collision detection
    // since ctx.addGlobalImport adds imports at global scope
    // exclude names whose symbols are being moved, as those imports
    // will be removed and should not count as collisions
    globalImports.flatMap { importStat =>
      importStat.importers.flatMap { importer =>
        importer.importees.collect {
          case Importee.Name(name) if !isMoved(name) => name.value
          case Importee.Rename(_, rename) => rename.value
        }
      }
    }.toSet
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
        case _ => Nil
      }.toMap

    lazy val globalImportedNames =
      getNamesOfExplicitlyImportedSymbols(ctx.tree, Move.unapply(_).isDefined)
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
        case Init.Initial(n: Name, _, _) => n.symbol.map(s => n -> s)
        case _ => None
      }
    }
    TreeOps.collectTree { case n @ Move(to) =>
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
          lazy val mayCauseCollision =
            globalImportedNames.contains(to.signature.name)
          val addImport =
            if (n.isDefinition || mayCauseCollision) Patch.empty
            else ctx.addGlobalImport(to)
          if (mayCauseCollision)
            addImport + ctx.replaceTree(n, SymbolOps.toTermRef(to).syntax)
          else
            addImport + ctx.replaceTree(n, to.signature.name)
        case _ =>
          Patch.empty
      }
    }(ctx.tree).asPatch
  }
}
