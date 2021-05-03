package scalafix.internal.interfaces

import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.ScalafixError
import scalafix.interfaces.ScalafixEvaluation
import scalafix.interfaces.ScalafixEvaluationError
import scalafix.interfaces.ScalafixFileEvaluation
import scalafix.internal.util.OptionOps._

final case class ScalafixEvaluationImpl(
    exitStatus: ExitStatus,
    errorMessage: Option[String],
    fileEvaluations: Seq[ScalafixFileEvaluationImpl]
) extends ScalafixEvaluation {

  override def getError: Optional[ScalafixEvaluationError] =
    exitStatus match {
      case ExitStatus.Ok => None.asJava
      case ExitStatus.NoFilesError =>
        Some(ScalafixEvaluationError.NoFilesError).asJava
      case ExitStatus.CommandLineError =>
        Some(ScalafixEvaluationError.CommandLineError).asJava
      case _ => Some(ScalafixEvaluationError.UnexpectedError).asJava
    }

  override def getErrorMessage: Optional[String] =
    errorMessage.asJava

  override def isSuccessful: Boolean = !getError.isPresent

  override def getFileEvaluations: Array[ScalafixFileEvaluation] =
    fileEvaluations.toArray

  override def apply(): Array[ScalafixError] = {
    fileEvaluations.flatMap(o => o.applyPatches()).toArray
  }
}

object ScalafixEvaluationImpl {

  def apply(
      exitStatus: ExitStatus,
      errorMessage: Option[String] = None
  ): ScalafixEvaluationImpl = {
    new ScalafixEvaluationImpl(exitStatus, errorMessage, Nil)
  }

  def from(
      fileEvaluations: Seq[ScalafixFileEvaluationImpl],
      exitStatus: ExitStatus
  ): ScalafixEvaluationImpl = {
    ScalafixEvaluationImpl(exitStatus, None, fileEvaluations)
  }

}
