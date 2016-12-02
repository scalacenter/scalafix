package scalafix

import scala.meta.inputs.Position
import scalafix.rewrite.Rewrite

class Failure(e: Throwable) extends Exception(e.getMessage)

object Failure {

  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
  case class MissingSemanticApi(rewrite: Rewrite)
      extends Failure(new IllegalStateException(
        s"The semantic API is required to run rewrite $rewrite. " +
          s"Use the scalafix-nsc compiler plugin to access the semantic API."))
  case class Unexpected(e: Throwable) extends Failure(e)
}
