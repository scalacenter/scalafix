package scalafix.internal.interfaces

import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.ScalafixError
import scalafix.interfaces.ScalafixEvaluation
import scalafix.interfaces.ScalafixFileEvaluation
import scalafix.interfaces.{
  ScalafixError,
  ScalafixEvaluation,
  ScalafixFileEvaluation
}
import scalafix.internal.util.OptionOps._
import scalafix.internal.v1.ValidatedArgs

final case class ScalafixEvaluationImpl(
    error: ExitStatus,
    getMessageError: Optional[String],
    fileEvaluations: Seq[ScalafixFileEvaluationImpl]
) extends ScalafixEvaluation {

  override def getErrors: Array[ScalafixError] = {
    ScalafixErrorImpl.fromScala(error)
  }
  override def isSuccessful: Boolean =
    getErrors.toList.filter(_ != ScalafixError.LinterError).isEmpty

  override def getFileEvaluations: Array[ScalafixFileEvaluation] =
    fileEvaluations.toArray

  override def apply(): Array[ScalafixError] = {
    fileEvaluations.flatMap(o => o.applyPatches()).toArray
  }
}

object ScalafixEvaluationImpl {

  def apply(
      error: ExitStatus,
      errorMessage: Option[String] = None
  ): ScalafixEvaluationImpl = {
    new ScalafixEvaluationImpl(error, errorMessage.asJava, Nil)
  }

  def from(
      fileEvaluations: Seq[ScalafixFileEvaluationImpl],
      exitStatus: ExitStatus
  ): ScalafixEvaluationImpl = {
    ScalafixEvaluationImpl(exitStatus, Optional.empty(), fileEvaluations)
  }

}
