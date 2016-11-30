package scalafix



sealed abstract class Fixed {
  def toEither: Either[Failure, String] = this match {
    case Fixed.Failed(e) => Left(e)
    case Fixed.Success(code) => Right(code)
  }
  def get: String = this match {
    case Fixed.Success(code) => code
    case Fixed.Failed(e) => throw e
  }
}

object Fixed {
  case class Failed(e: Failure) extends Fixed
  case class Success(code: String) extends Fixed
}
