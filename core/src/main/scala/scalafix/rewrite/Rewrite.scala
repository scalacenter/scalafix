package scalafix.rewrite

import scala.meta._
import scalafix.FixResult

abstract class Rewrite {

  def rewrite(code: Input): FixResult

  protected def withParsed(code: Input)(f: Tree => FixResult): FixResult = {
    code.parse[Source] match {
      case Parsed.Success(ast) => f(ast)
      case Parsed.Error(pos, msg, details) =>
        FixResult.ParseError(pos, msg, details)
    }
  }
}

object Rewrite {
  val default: List[Rewrite] = List(
    ProcedureSyntax
  )
}
