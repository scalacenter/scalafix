package foo.bar
import scalafix._
import scala.meta._
case object MyRewrite extends Rewrite {
  override def fix(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.replaceTree(n, n.syntax + "1")
    }.asPatch
  }
}
