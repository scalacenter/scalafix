package scalafix.test

import scalafix.v0._
import scala.meta._

object NoDummy extends Rule("NoDummy") {
  val error = LintCategory.error("Dummy!")

  override def check(ctx: RuleCtx): Seq[Diagnostic] = {
    ctx.tree.collect {
      case tree @ Name(name) if name.toLowerCase.contains("dummy") =>
        error.at(tree.pos)
    }
  }
}
