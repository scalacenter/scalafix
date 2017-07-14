package scalafix.internal.sbt

import sbt.complete.DefaultParsers._
import sbt.complete.Parser

object ScalafixCompletions {
  val names = List(
    "NoValInForComprehension",
    "RemoveXmlLiterals",
    "VolatileLazyVal",
    "ProcedureSyntax",
    "ExplicitUnit",
    "DottyVarArgPattern",
    "ExplicitReturnTypes",
    "RemoveUnusedImports",
    "NoAutoTupling"
  )
  val parser = {
    val rewrite: Parser[String] =
      names.map(literal).reduceLeft(_ | _)
    (token(Space) ~> token(rewrite)).* <~ SpaceClass.*
  }
}
