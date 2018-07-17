package scalafix.internal.util

import scalafix.v0.Patch
import scalafix.v0.SemanticdbIndex

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
  case class MismatchingSemanticdbIndex(a: SemanticdbIndex, b: SemanticdbIndex)
      extends Failure(
        InvariantFailedException(
          s"""Cannot mix two different SemanticdbIndex inside the same patch.
             |SemanticdbIndex 1: $a
             |RuleCtx 2: $b
             |""".stripMargin))
  case class Unsupported(msg: String)
      extends Failure(new UnsupportedOperationException(msg))
  case class InvariantFailedException(msg: String)
      extends Failure(new Exception(msg))
}
