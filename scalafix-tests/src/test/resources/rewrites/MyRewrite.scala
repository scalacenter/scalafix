package foo.bar

import scalafix._
import scala.meta._

case object MyRewrite extends Rewrite {
  def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.rename(n, Term.Name(n.syntax + "1"))
    }.asPatch
  }
}

case class MyRewrite2(mirror: Mirror) extends SemanticRewrite(mirror) {
  def rewrite(ctx: RewriteCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable.Seq")
}
