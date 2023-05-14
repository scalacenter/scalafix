package scalafix.test

import scala.meta._

import scalafix.v0._

object NoDummy extends Rule("NoDummy") {
  val error: LintCategory = LintCategory.error("Dummy!")

  override def check(ctx: RuleCtx): Seq[Diagnostic] = {
    ctx.tree.collect {
      case tree @ Name(name) if name.toLowerCase.contains("dummy") =>
        error.at(tree.pos)
    }
  }
}
