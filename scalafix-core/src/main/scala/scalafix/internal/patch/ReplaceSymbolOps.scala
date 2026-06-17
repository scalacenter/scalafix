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

  private def globalImports(tree: Tree): Seq[Import] = tree match {
    case t: Pkg => extractImports(t.body.stats)
    case t: Source => extractImports(t.stats)
    case _ => Nil
  }

  // The rightmost `Name` of an importer ref, e.g. `mutable` in
  // `import scala.collection.mutable._`, used to resolve the importer's owner.
  private def refTerminalName(ref: Term.Ref): Option[Name] = ref match {
    case Term.Select(_, name) => Some(name)
    case name: Term.Name => Some(name)
    case _ => None
  }

  @tailrec
  private def isAncestor(ancestor: Tree, tree: Tree): Boolean =
    tree.parent match {
      case Some(p) => (p eq ancestor) || isAncestor(ancestor, p)
      case None => false
    }

  private def getNamesOfExplicitlyImportedSymbols(
      tree: Tree,
      isMoved: Name => Boolean
  ): Set[String] = {
    // pre-compute global imported symbols for O(1) collision detection
    // since ctx.addGlobalImport adds imports at global scope
    // exclude names whose symbols are being moved, as those imports
    // will be removed and should not count as collisions
    globalImports(tree).flatMap { importStat =>
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

    // Wildcard imports nested below the top level (inside a class/object/def
    // body), paired with the normalized symbol of their owner. A top-level
    // wildcard shares the scope of the global import we add for the replacement,
    // where a named import already takes precedence over a wildcard; only a
    // wildcard in an inner scope can clash with that named import and produce the
    // ambiguous import reported in scalacenter/scalafix#376.
    lazy val nestedWildcardImports: List[(Import, Importer, String)] = {
      val topLevel = globalImports(ctx.tree)
      ctx.tree
        .collect { case i: Import => i }
        .filterNot(i => topLevel.exists(_ eq i))
        .flatMap { importStat =>
          for {
            importer <- importStat.importers
            if importer.importees.exists(_.is[Importee.Wildcard])
            ownerName <- refTerminalName(importer.ref).toList
            ownerSym <- ownerName.symbol.toList
          } yield (importStat, importer, SymbolOps.normalize(ownerSym).syntax)
        }
    }
    @annotation.nowarn("msg=Exhaustivity|match may not be exhaustive")
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
      def unapply(name: Name): Option[(Symbol.Global, Symbol.Global)] = {
        name.symbol.iterator
          .flatMap {
            case Symbol.Multi(syms) => syms
            case els => els :: Nil
          }
          .collectFirst(Function.unlift {
            case from: Symbol.Global => Moved.unapply(from).map(from -> _)
            case _ => None
          })
      }
    }
    object Identifier {
      def unapply(tree: Tree): Option[(Name, Symbol)] = tree match {
        case n: Name => n.symbol.map(s => n -> s)
        case Init.Initial(n: Name, _, _) => n.symbol.map(s => n -> s)
        case _ => None
      }
    }
    // The short name of an import selector that renames-or-names a replaced
    // symbol whose replacement keeps the same short name (the only case that
    // can clash with the top-level named import we add). `None` otherwise.
    def sameNameMoved(nm: Name): Option[String] =
      Move.unapply(nm).collect {
        case (from, to) if from.signature.name == to.signature.name => nm.value
      }

    // Nested wildcard importers that must be rewritten to unimport a replaced
    // name, mapped to those names. Without this, the wildcard keeps bringing the
    // old binding into an inner scope where it clashes with the top-level named
    // import added for the replacement, producing the ambiguous import reported
    // in scalacenter/scalafix#376. A name needs unimporting when it is:
    //   - selected explicitly alongside the wildcard (`{Future, _}`), or renamed
    //     alongside it (`{Future => F, _}`) -- removing/renaming the selector
    //     would otherwise let the wildcard re-introduce the old binding; or
    //   - referenced by its own (unrenamed) short name and brought in only by
    //     the wildcard (`import p._; Future...`).
    // Top-level wildcards are excluded (see `nestedWildcardImports`): there a
    // named import already takes precedence over the wildcard.
    val wildcardRewrites: Map[Importer, Set[String]] = {
      val selectorPairs = nestedWildcardImports.flatMap {
        case (_, importer, _) =>
          importer.importees.flatMap {
            case Importee.Name(nm) => sameNameMoved(nm).map(importer -> _)
            case Importee.Rename(nm, _) => sameNameMoved(nm).map(importer -> _)
            case _ => None
          }
      }
      val usagePairs = ctx.tree.collect {
        case n @ Move((from, to))
            if from.signature.name == to.signature.name &&
              n.value == from.signature.name &&
              !n.parent.exists(_.is[Importee]) =>
          val ownerKey = SymbolOps.normalize(from.owner).syntax
          nestedWildcardImports.collect {
            case (importStat, importer, owner)
                if owner == ownerKey &&
                  importStat.parent.exists(isAncestor(_, n)) =>
              importer -> from.signature.name
          }
      }.flatten
      (selectorPairs ++ usagePairs).groupBy(_._1).map { case (importer, ps) =>
        importer -> ps.map(_._2).toSet
      }
    }

    val patches = ctx.tree.collect { case n @ Move((_, to)) =>
      // was this written as `to = "blah"` instead of `to = _root_.blah`
      val isSelected = to match {
        case Root(_) => false
        case _ => true
      }
      // A nested wildcard rewrite (below) replaces the whole importer, dropping
      // every moved selector itself, so the per-importee branches must not also
      // edit selectors of such an importer or the patches would overlap.
      def rewrittenByWildcard(importee: Importee): Boolean =
        importee.parent.exists {
          case importer: Importer => wildcardRewrites.contains(importer)
          case _ => false
        }
      n.parent match {
        case Some(i @ Importee.Name(_)) =>
          if (rewrittenByWildcard(i)) Patch.empty else ctx.removeImportee(i)
        case Some(i @ Importee.Rename(_, _)) if rewrittenByWildcard(i) =>
          Patch.empty
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
    }

    // Rewrite each affected wildcard import in place so that it unimports the
    // replaced names while keeping the rest, e.g.
    // `import scala.collection.mutable._`,
    // `import scala.collection.mutable.{ListBuffer, _}` and
    // `import scala.collection.mutable.{ListBuffer => LB, _}` all become
    // `import scala.collection.mutable.{ListBuffer => _, _}`. Every moved
    // selector is dropped (its replacement is imported at the use site); the
    // unimport then keeps the wildcard from re-introducing the old binding.
    // Rewriting in place (rather than adding a global import) preserves the
    // wildcard's lexical scope, which is what makes this work.
    val unimportPatches = wildcardRewrites.toList.flatMap {
      case (importer, names) =>
        val remaining = importer.importees.filterNot {
          case Importee.Name(nm) => Move.unapply(nm).isDefined
          case Importee.Rename(nm, _) => Move.unapply(nm).isDefined
          case _ => false
        }
        val unimports: List[Importee] = names.toList
          .filterNot(name =>
            importer.importees.exists {
              case Importee.Unimport(n) => n.value == name
              case _ => false
            }
          )
          .map(name => Importee.Unimport(Name.Indeterminate(name)))
        if (unimports.isEmpty && remaining.length == importer.importees.length)
          Nil
        else {
          val rewritten = importer.copy(importees = unimports ++ remaining)
          List(Patch.replaceTree(importer, rewritten.syntax))
        }
    }

    (patches ++ unimportPatches).asPatch
  }
}
