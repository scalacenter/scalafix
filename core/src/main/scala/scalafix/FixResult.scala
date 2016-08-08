package scalafix

import scala.meta.inputs.Position

abstract sealed class FixResult {
  def get: String = this match {
    case FixResult.Success(code) => code
    case FixResult.Failure(e) => throw e
    case e: FixResult.ParseError => throw e.exception
  }
}

object FixResult {
  case class Success(code: String) extends FixResult
  object Error {
    def unapply(fixResult: FixResult): Option[Throwable] = fixResult match {
      case Failure(e) => Some(e)
      case e: ParseError => Some(e.exception)
      case _ => None
    }
  }
  case class Failure(e: Throwable) extends FixResult
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends FixResult
}
