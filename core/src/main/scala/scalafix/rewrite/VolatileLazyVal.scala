package scalafix.rewrite

import scala.meta._
import scalafix.util.Patch
import scala.collection.immutable.Seq
import scalafix.util.TokenPatch

class VolatileLazyVal[T]() extends Rewrite[T] {
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
  override def rewrite(ctx: RewriteCtx[T]): Seq[Patch] = {
    ctx.tree.collect {
      case NonVolatileLazyVal(tok) =>
        TokenPatch.AddLeft(tok, s"@volatile ")
    }
  }
}

object VolatileLazyVal {
  def apply[T](): VolatileLazyVal[T] = new VolatileLazyVal[T]()
}
