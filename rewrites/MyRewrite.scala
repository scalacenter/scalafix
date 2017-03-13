package foo.bar {

import scalafix._
import scalafix.rewrite._
import scalafix.util._
import scalafix.util.TreePatch._
import scala.collection.immutable.Seq
import scala.meta._

case object MyRewrite extends Rewrite[Any] {
  def rewrite[T](ctx: RewriteCtx[T]): Seq[Patch] = {
    ctx.tree.collect {
      case n: scala.meta.Name => Rename(n, Term.Name(n.syntax + "1"))
    }
  }
}

case object MyRewrite2 extends Rewrite[Any] {
  def rewrite[T](ctx: RewriteCtx[T]): Seq[Patch] = {
    Seq(
      AddGlobalImport(importer"scala.collection.immutable.Seq")
    )
  }
}
}
