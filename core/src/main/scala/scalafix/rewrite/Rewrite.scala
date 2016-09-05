package scalafix.rewrite

import scala.meta._
import scalafix.Fixed

abstract class Rewrite {

  def rewrite(code: Input): Fixed

  protected def withParsed(code: Input)(f: Tree => Fixed): Fixed = {
    code.parse[Source] match {
      case Parsed.Success(ast) => f(ast)
      case Parsed.Error(pos, msg, details) =>
        Fixed.ParseError(pos, msg, details)
    }
  }
}

object Rewrite {
  private def nameMap[T](t: sourcecode.Text[T]*): Map[String, T] = {
    t.map(x => x.source -> x.value).toMap
  }

  val name2rewrite: Map[String, Rewrite] = nameMap[Rewrite](
    ProcedureSyntax,
    VolatileLazyVal
  )

  val default: Seq[Rewrite] = name2rewrite.values.toSeq
}
