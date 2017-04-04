package scalafix.test

import scala.meta._
import scala.collection.immutable.Seq
import scalafix.rewrite.Rewrite
import scalafix.rewrite.RewriteCtx
import scalafix.util.Patch
import scalafix.util.TreePatch.AddGlobalImport

case object FqnRewrite extends Rewrite[Any] {
  override def rewrite[B <: Any](ctx: RewriteCtx[B]): Seq[Patch] =
    Seq(AddGlobalImport(importer"scala.meta._"))
}
