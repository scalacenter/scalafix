package scalafix.util

import scalafix.syntax._
import scala.meta.{Symbol => _, _}
import scala.meta.semantic.v1._
import scala.collection.immutable.Seq
import scalafix.rewrite.ScalafixCtx
import scalafix.util.TreePatch._
import scalafix.util.TokenPatch._
import scala.meta.internal.ast.Helpers._
import scala.util.Try
import scalafix.rewrite.RewriteCtx
import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.Rename

private[this] class Replacer(implicit ctx: RewriteCtx[Mirror]) {
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
                    TokenPatch.AddLeft(ref.tokens.head, replace.to.syntax) +:
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
      implicit ctx: RewriteCtx[Mirror]): Seq[Patch] = {
    new Replacer().toTokenPatches(
      ast,
      replacements ++ ctx.config.patches.all.collect {
        case r: Replace => r
      }
    )
  }
}

object Renamer {
  def toTokenPatches[T](renames: Seq[Rename])(
      implicit ctx: RewriteCtx[T]): Seq[TokenPatch] = {
    object MatchingRename {
      def unapply(arg: Name): Option[(Token, Name)] =
        for {
          rename <- renames.find(_.from eq arg)
          tok <- arg.tokens.headOption
        } yield tok -> rename.to
    }
    ctx.tree.collect {
      case MatchingRename(tok, to) =>
        Seq(
          Remove(tok),
          AddLeft(tok, to.syntax)
        )
    }.flatten
  }
}
