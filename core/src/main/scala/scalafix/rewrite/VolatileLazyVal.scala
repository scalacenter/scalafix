package scalafix.rewrite

import scala.meta._
import scalafix.util.Patch

object VolatileLazyVal extends Rewrite {
  private object NonVolatileLazyVal {
    def unapply(defn: Defn.Val): Option[Token] = {
      defn.mods.collectFirst {
        case x if x.syntax == "@volatile" =>
          None
        case x if x.syntax == "lazy" =>
          Some(x.tokens.head)
      }
    }.flatten
  }
  override def rewrite(ast: Tree, ctx: RewriteCtx): Seq[Patch] = {
    ast.collect {
      case NonVolatileLazyVal(tok) => Patch(tok, tok, "@volatile lazy")
    }
  }
}
