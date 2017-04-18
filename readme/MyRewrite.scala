package foo.bar
import scalafix._
import scala.meta._
case object MyRewrite extends Rewrite[Any] {
  def rewrite[T](ctx: RewriteCtx[T]): Patch = {
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.rename(n, Term.Name(n.syntax + "1"))
    }.asPatch
  }
}
