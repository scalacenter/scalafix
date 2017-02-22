package scalafix

import scala.meta.inputs.Position
import scalafix.rewrite.Rewrite
import scalafix.rewrite.ScalafixRewrites

class Failure(e: Throwable) extends Exception(e.getMessage)

object Failure {
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
  case class UnknownRewrite(name: String)
      extends Failure(new IllegalArgumentException(
        s"Unknown rewrite '$name', expected one of ${ScalafixRewrites.name2rewrite.keys
          .mkString(", ")}"))
  case class Unexpected(e: Throwable) extends Failure(e)
}
