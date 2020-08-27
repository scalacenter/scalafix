package scalafix.internal.interfaces

import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.{
  ScalafixError,
  ScalafixFileEvaluation,
  ScalafixEvaluation
}
import scalafix.internal.util.OptionOps._

final case class ScalafixEvaluationImpl(
    error: ExitStatus,
    getMessageError: Optional[String],
    fileEvaluations: Seq[ScalafixFileEvaluationImpl]
) extends ScalafixEvaluation {

  override def isSuccessful: Boolean = error.isOk

  override def getErrors: Array[ScalafixError] = {
    ScalafixErrorImpl.fromScala(error)
  }

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
