package scalafix.internal.rule

import scala.meta._
import scalafix.v0._

case object DottyVolatileLazyVal
    extends Rule(
      RuleName("DottyVolatileLazyVal")
        .withDeprecatedName(
          name = "VolatileLazyVal",
          message = "Use DottyVolatileLazyVal instead.",
          since = "0.5.0")) {
  override def description: String =
    "Rewrite all lazy vals to Dotty's volatile ones for safe publishing (default semantics of pre-Dotty Scala)"
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
