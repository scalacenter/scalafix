package scalafix.rewrite

import scala.meta._
import scalafix.FixResult

object ProcedureSyntax extends Rewrite {
  override def rewrite(code: Input): FixResult = {
    withParsed(code) { ast =>
      val toPrepend = ast.collect {
        case t: Defn.Def if t.decltpe.exists(_.tokens.isEmpty) =>
          t.body.tokens.head
      }.toSet
      val sb = new StringBuilder
      ast.tokens.foreach { token =>
        if (toPrepend.contains(token)) {
          if (sb.lastOption.contains(' ')) {
            sb.deleteCharAt(sb.length - 1)
          }
          sb.append(": Unit = ")
        }
        sb.append(token.syntax)
      }
      val result = sb.toString()
      FixResult.Success(result)
    }
  }
}
