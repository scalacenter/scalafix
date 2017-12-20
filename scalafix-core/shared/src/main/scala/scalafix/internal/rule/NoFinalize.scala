package scalafix.internal.rule

import scala.meta._

import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory

case object NoFinalize extends Rule("NoFinalize") {
  private val error =
    LintCategory.error(
      explain = """|there is no guarantee that finalize will be called and 
                   |overriding finalize incurs a performance penalty""".stripMargin
    )

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case Defn.Def(_, name @ q"finalize", _, Nil | Nil :: Nil, _, _) =>
        error.at("finalize should not be used", name.pos)
    }
  }
}
