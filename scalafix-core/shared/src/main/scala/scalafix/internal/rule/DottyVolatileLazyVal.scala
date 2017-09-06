package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.rule.RuleName

case object DottyVolatileLazyVal extends Rule {
  override def name: RuleName =
    RuleName("DottyVolatileLazyVal")
      .withDeprecatedName(
        name = "VolatileLazyVal",
        message = "Use DottyVolatileLazyVal instead.",
        since = "0.5.0")
  private object NonVolatileLazyVal {
    def unapply(defn: Defn.Val): Option[Token] = {
      defn.mods.collectFirst {
        case x if x.syntax == "@volatile" =>
          None
        case x if x.syntax == "lazy" =>
          Some(defn.mods.head.tokens.head)
      }
    }.flatten
  }
  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case NonVolatileLazyVal(tok) =>
        ctx.addLeft(tok, s"@volatile ")
    }.asPatch
  }
}
