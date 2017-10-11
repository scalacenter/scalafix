import scalafix._
import scala.meta._

case object Uppercase extends Rule("Uppercase") {
  override def fix(ctx: RuleCtx): Patch =
    ctx.tree.collect {
      case name @ Name(value) => ctx.replaceTree(name, value.toUpperCase())
    }.asPatch
}
