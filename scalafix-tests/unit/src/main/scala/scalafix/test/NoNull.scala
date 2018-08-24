package scalafix.test

import scala.meta._
import scalafix.v0._

object NoNull extends Rule("NoNull") {
  val error = LintCategory.error("Nulls are not allowed.")

  override def check(ctx: RuleCtx): List[Diagnostic] = ctx.tree.collect {
    case nil @ q"null" => error.at(nil.pos)
  }
}
