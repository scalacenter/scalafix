package scalafix

import scala.meta.inputs.Position

abstract sealed class Fixed {
  def get: String = this match {
    case Fixed.Success(code) => code
    case Fixed.Failure(e) => throw e
  }
}

object Fixed {
  case class Success(code: String) extends Fixed
  class Failure(val e: Throwable) extends Fixed
  object Failure {
    def apply(exception: Throwable): Failure = new Failure(exception)
    def unapply(arg: Failure): Option[Throwable] = {
      Some(arg.e)
    }
  }

  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
}
