package scalafix.patch

import scalafix.syntax._
import scala.meta.{Symbol => _, _}
import scala.meta.semantic.v1._
import scala.collection.immutable.Seq
import scalafix.patch.TreePatch._
import scalafix.patch.TokenPatch._
import scala.meta.internal.ast.Helpers._
import scala.util.Try
import scalafix.rewrite.RewriteCtx
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.Rename

import org.scalameta.logger

private[this] class Replacer(implicit ctx: RewriteCtx, mirror: Mirror) {
  import ctx._
  object `:withSymbol:` {
    def unapply(ref: Ref): Option[(Ref, Symbol)] =
      Try(
        mirror.symbol(ref) match {
          case Completed.Success(symbol) =>
            Some(ref -> symbol.normalized)
          case _ => None
        }
      ).toOption.flatten
  }

  def toTokenPatches(ast: Tree, replacements: Seq[Replace]): Seq[Patch] = {
    if (replacements.isEmpty) return Nil
    val builder = Seq.newBuilder[Patch]
    object traverser extends Traverser {
      override def apply(tree: Tree): Unit = {
        tree match {
          case (ref: Ref) `:withSymbol:` symbol =>
            builder ++=
              replacements
                .find { x =>
                  x.from == symbol
                }
                .toList
                .flatMap(
                  replace =>
                    ctx.addLeft(ref.tokens.head, replace.to.syntax) +:
                      (ref.tokens.map(TokenPatch.Remove.apply) ++
                      replace.additionalImports.map(x => AddGlobalImport(x))))
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
    new Replacer().toTokenPatches(
      ast,
      replacements ++ ctx.config.patches.all.collect {
        case r: Replace => r
      }
    )
  }
}

object Renamer {
  def toTokenPatches(renamePatches: Seq[RenamePatch])(
      implicit ctx: RewriteCtx,
      mirror: Mirror): Seq[TokenPatch] = {
    if (renamePatches.isEmpty) return Nil
    import ctx._
    val renames = renamePatches.collect { case r: Rename => r }
    val renameSymbols = renamePatches.collect { case r: RenameSymbol => r }
    object MatchingRename {
      def unapply(arg: Name): Option[(Token, Name)] =
        if (renames.isEmpty) None
        else
          for {
            rename <- renames.find(_.from eq arg)
            tok <- arg.tokens.headOption
          } yield tok -> rename.to
    }
    object MatchingRenameSymbol {
      def unapply(arg: Name): Option[(Token, Name)] =
        if (renameSymbols.isEmpty) None
        else
          for {
            // TODO(olafur) avoid Try() once don't require `Mirror` in Replacer.
            symbol <- mirror.symbol(arg).toOption
            rename <- renameSymbols.find(_.matches(symbol))
            tok <- arg.tokens.headOption
          } yield tok -> rename.to
    }
    object ToRename {
      def unapply(arg: Name): Option[(Token, Name)] =
        MatchingRename
          .unapply(arg)
          .orElse(MatchingRenameSymbol.unapply(arg))
    }
    ctx.tree.collect {
      case ToRename(tok, to) =>
        Seq(
          Remove(tok),
          TokenPatch.Add(tok, to.syntax, "")
        )
    }.flatten
  }
}
