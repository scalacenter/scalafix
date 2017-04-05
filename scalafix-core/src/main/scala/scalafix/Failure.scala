package scalafix

import scala.meta.inputs.Position
import scalafix.rewrite.Rewrite
import scalafix.rewrite.ScalafixRewrites

class Failure(val ex: Throwable) extends Exception(ex.getMessage)

object Failure {
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
  case class UnknownRewrite(name: String)
      extends Failure(new IllegalArgumentException(
        s"Unknown rewrite '$name', expected one of ${ScalafixRewrites.name2rewrite.keys
          .mkString(", ")}"))
  case class Unexpected(e: Throwable) extends Failure(e)
  case class MissingSemanticApi(operation: String)
      extends Failure(
        new UnsupportedOperationException(
          s"Operation '$operation' requires the semantic api. " +
            "This may indicate a configuration or build integration error. " +
            "See sbt-scalahost, sbt-scalafix or scala.meta.Mirror for instructions on " +
            "how to setup a semantic api."
        ))
}
