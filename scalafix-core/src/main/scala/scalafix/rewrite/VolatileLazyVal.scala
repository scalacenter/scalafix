package scalafix
package rewrite

import scala.meta._
import scala.collection.immutable.Seq

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
