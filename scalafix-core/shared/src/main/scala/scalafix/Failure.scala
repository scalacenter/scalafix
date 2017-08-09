package scalafix

import scala.meta._

// TODO(olafur) this signature makes no sense. We should try to avoid exception
// try/catch dodgeball court whenever possible.
sealed abstract class Failure(val ex: Throwable)
    extends Exception(ex.getMessage) {
  override def getCause: Throwable = ex
}

object Failure {
  case class ParseError(pos: Position, message: String, exception: Throwable)
      extends Failure(exception)
  case class TokenPatchMergeError(a: Patch, b: Patch)
      extends Failure(
        new UnsupportedOperationException(
          s"""Unable to merge two token patches:
          |Token Patch 1: $a
          |Token Patch 2: $b"""
        ))
  case class MissingSemanticApi(operation: String)
      extends Failure(
        new UnsupportedOperationException(
          s"Operation '$operation' requires the semantic api. " +
            "This may indicate a configuration or build integration error. " +
            "See sbt-scalafix or scala.meta.Database for instructions on " +
            "how to setup a semantic api."
        ))
  case class MissingTopLevelInCtx(patch: Patch)
      extends Failure(
        InvariantFailedException(
          s"""Expected an InCtx at top of Patch tree, obtained:
             |$patch """.stripMargin))
  case class MismatchingRewriteCtx(a: RewriteCtx, b: RewriteCtx)
      extends Failure(
        InvariantFailedException(
          s"""Cannot mix two different RewriteCtx inside the same patch.
             |RewriteCtx 1: $a
             |RewriteCtx 2: $b""".stripMargin))
  case class MismatchingMirror(a: Database, b: Database)
      extends Failure(
        InvariantFailedException(
          s"""Cannot mix two different Database inside the same patch.
             |Database 1: $a
             |${a}
             |RewriteCtx 2: $b
             |${b}
             |""".stripMargin))
  case class Unexpected(e: Throwable) extends Failure(e)
  case class Unsupported(msg: String)
      extends Failure(new UnsupportedOperationException(msg))
  case class InvariantFailedException(msg: String) extends Exception(msg)
  case class StaleSemanticDB(outPath: AbsolutePath)
      extends Failure(
        new Exception(
          s"Stale Semantic DB! Contents of $outPath have changed since " +
            s"creation of its correspondin .semanticdb file. Please recompile and run " +
            s"scalafix again.")
      )
}
