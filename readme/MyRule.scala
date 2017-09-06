package foo.bar
import scalafix._
import scala.meta._

case object MyRule extends Rule("MyRule") {
  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.replaceTree(n, n.syntax + "1")
    }.asPatch
  }
}
