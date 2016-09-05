package scalafix.rewrite

import scala.meta._
import scalafix.Fixed
import scalafix.util.logger

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
  override def rewrite(code: Input): Fixed = {
    withParsed(code) { ast =>
      val toPrepend: Seq[Token] = ast.collect {
        case NonVolatileLazyVal(tok) => tok
      }
      val sb = new StringBuilder
      ast.tokens.foreach { token =>
        if (toPrepend.contains(token)) {
          sb.append("@volatile ")
        }
        sb.append(token.syntax)
      }
      val result = sb.toString()
      Fixed.Success(result)
    }
  }
}
