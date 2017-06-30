package scalafix.internal.patch

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.patch.Patch
import scalafix.patch.TokenPatch
import scalafix.patch.TokenPatch._
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch._
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._

private[this] class Replacer(implicit ctx: RewriteCtx, mirror: Mirror) {
  object `:withSymbol:` {
    def unapply(ref: Ref): Option[(Ref, Symbol)] =
      ref.symbolOpt.map(sym => ref -> sym.normalized)
  }

  def toTokenPatches(ast: Tree, replacements: Seq[Replace]): Seq[Patch] = {
    if (replacements.isEmpty) return Nil
    val builder = Seq.newBuilder[Patch]
    object traverser extends Traverser {
      override def apply(tree: Tree): Unit = {
        tree match {
          case (ref: Ref) `:withSymbol:` symbol =>
            val patches = replacements
              .find { x =>
                x.from == symbol
              }
              .toList
              .flatMap { replace =>
                ctx.addLeft(ref.tokens.head, replace.to.syntax) +:
                  (ref.tokens.map(TokenPatch.Remove.apply) ++
                  replace.additionalImports.map(x => AddGlobalImport(x)))
              }
            builder ++= patches
          case imp: Import => // Do nothing
          case _ => super.apply(tree)
        }
      }
    }
    traverser(ast)
    builder.result()
  }
}

object Replacer {
  def toTokenPatches(ast: Tree, replacements: Seq[Replace])(
      implicit ctx: RewriteCtx,
      mirror: Mirror): Seq[Patch] = {
    new Replacer().toTokenPatches(ast, replacements)
  }
}

object Renamer {
  def toTokenPatches(renamePatches: Seq[RenamePatch])(
      implicit ctx: RewriteCtx,
      mirror: Mirror): Seq[TokenPatch] = {
    if (renamePatches.isEmpty) return Nil
    val renameSymbols = renamePatches.collect { case r: RenameSymbol => r }
    object MatchingRenameSymbol {
      def unapply(arg: Name): Option[(Token, Name)] =
        if (renameSymbols.isEmpty) None
        else
          for {
            symbol <- arg.symbolOpt
            rename <- renameSymbols.find(_.matches(symbol))
            tok <- arg.tokens.headOption
          } yield tok -> rename.to
    }
    ctx.tree.collect {
      case MatchingRenameSymbol(tok, to) =>
        Seq(
          Remove(tok),
          TokenPatch.Add(tok, to.syntax, "")
        )
    }.flatten
  }
}
