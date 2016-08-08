package scalafix.rewrite

import scala.meta._
import scalafix.FixResult

abstract class Rewrite {

  def rewrite(code: String): FixResult

  protected def withParsed(code: String)(f: Tree => FixResult): FixResult = {
    code.parse[Source] match {
      case Parsed.Success(ast) => f(ast)
      case Parsed.Error(pos, msg, details) =>
        FixResult.ParseError(pos, msg, details)
    }
  }
}

object Rewrite {
  val default: Seq[Rewrite] = Seq(
      ProcedureSyntax
  )
}
