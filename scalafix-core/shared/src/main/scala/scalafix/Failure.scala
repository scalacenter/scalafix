package scalafix

// TODO(olafur) this signature makes no sense. We should try to avoid exception
// try/catch dodgeball court whenever possible.
sealed abstract class Failure(val ex: Throwable)
    extends Exception(ex.getMessage) {
  override def getCause: Throwable = ex
}

object Failure {
  case class TokenPatchMergeError(a: Patch, b: Patch)
      extends Failure(
        new UnsupportedOperationException(
          s"""Unable to merge two token patches:
          |Token Patch 1: $a
          |Token Patch 2: $b"""
        ))
  case class MismatchingSemanticCtx(a: SemanticCtx, b: SemanticCtx)
      extends Failure(
        InvariantFailedException(
          s"""Cannot mix two different SemanticCtx inside the same patch.
             |SemanticCtx 1: $a
             |RewriteCtx 2: $b
             |""".stripMargin))
  case class Unsupported(msg: String)
      extends Failure(new UnsupportedOperationException(msg))
  case class InvariantFailedException(msg: String)
      extends Failure(new Exception(msg))
}
