package scalafix.util

import scalafix.syntax._
import scala.meta.{Symbol => _, _}
import scala.meta.semantic.v1._
import scala.collection.immutable.Seq
import scalafix.rewrite.RewriteCtx
import scalafix.util.TreePatch.Replace
import scala.meta.internal.ast.Helpers._
import scala.util.Try
import scalafix.util.TreePatch.AddGlobalImport

private[this] class Replacer(implicit ctx: RewriteCtx) {
  private implicit val mirror = ctx.semantic.get
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
                .flatMap(replace =>
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
      implicit ctx: RewriteCtx): Seq[Patch] =
    if (replacements.isEmpty) Nil
    else new Replacer().toTokenPatches(ast, replacements)
}
