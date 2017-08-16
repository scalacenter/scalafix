package scalafix.internal.rewrite

import scala.meta._
import scalafix.Patch
import scalafix.rewrite.Rewrite
import scalafix.rewrite.RewriteCtx

case object VolatileLazyVal extends Rewrite {
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
