package scalafix.rewrite
import scala.collection.immutable.Seq
import scala.meta._
import scalafix.util.Patch
import scala.meta.internal.ast.Helpers._

import org.scalameta.logger

case object Xor2Either extends Rewrite {
  override def rewrite(code: Tree, rewriteCtx: RewriteCtx): Seq[Patch] = {
    implicit val semantic = getSemanticApi(rewriteCtx)
    object traverser extends Traverser {
      override def apply(tree: Tree): Unit = {
        Importee
        tree match {
          case ref: Term.Ref if ref.isStableId =>
            logger.elem(ref)
          case _ => super.apply(tree)
        }
      }
    }
    traverser(code)
    Nil
  }
}
