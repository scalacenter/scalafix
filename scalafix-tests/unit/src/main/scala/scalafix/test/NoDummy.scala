package scalafix.test

import scalafix.LintCategory
import scalafix.LintMessage
import scalafix.Rule
import scalafix.RuleCtx

import scala.meta._

object NoDummy extends Rule("NoDummy") {
  val error = LintCategory.error("Dummy!")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case tree @ Name(name) if name.toLowerCase.contains("dummy") =>
        error.at(tree.pos)
    }
  }
}
