package scalafix

import scala.meta.inputs.Position

sealed abstract class Failure(val ex: Throwable)
    extends Exception(ex.getMessage) {
  override def getCause: Throwable = ex
}

object Failure {
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
  case class MissingSemanticApi(operation: String)
      extends Failure(
        new UnsupportedOperationException(
          s"Operation '$operation' requires the semantic api. " +
            "This may indicate a configuration or build integration error. " +
            "See sbt-scalahost, sbt-scalafix or scala.meta.Mirror for instructions on " +
            "how to setup a semantic api."
        ))
  case class Unexpected(e: Throwable) extends Failure(e)
}
