package scalafix

import scala.meta.inputs.Position
import scalafix.rewrite.Rewrite

class Failure(e: Throwable) extends Exception(e.getMessage)

object Failure {
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
  case class Unexpected(e: Throwable) extends Failure(e)
}
