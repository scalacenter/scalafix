package scalafix.internal.rewrite

import scala.meta._
import scalafix.Patch
import scalafix.rewrite.Rule
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.RewriteName

case object DottyVolatileLazyVal
    extends Rule()(
      RewriteName("DottyVolatileLazyVal").withOldName(
        name = "VolatileLazyVal",
        message = "Use DottyVolatileLazyVal instead.",
        since = "0.5.0")
    ) {
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
  override def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case NonVolatileLazyVal(tok) =>
        ctx.addLeft(tok, s"@volatile ")
    }.asPatch
  }
}
