package scalafix

import scala.meta.inputs.Position

sealed abstract class Failure(msg: String) extends Exception(msg)

object Failure {
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(message)
}
