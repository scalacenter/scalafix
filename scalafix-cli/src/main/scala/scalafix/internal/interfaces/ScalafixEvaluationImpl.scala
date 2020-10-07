package scalafix.internal.interfaces

import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.ScalafixError
import scalafix.interfaces.ScalafixEvaluation
import scalafix.interfaces.ScalafixFileEvaluation
import scalafix.internal.util.OptionOps._

final case class ScalafixEvaluationImpl(
    error: ExitStatus,
    errorMessage: Option[String],
    fileEvaluations: Seq[ScalafixFileEvaluationImpl]
) extends ScalafixEvaluation {

  override def getErrors: Array[ScalafixError] =
    ScalafixErrorImpl.fromScala(error)

  override def getMessageError: Optional[String] =
    errorMessage
      .orElse({
        val fileToError = fileEvaluations
          .map { eval =>
            (eval.originalPath, eval.errorMessage)
          }
          .toMap
          .collect { case (path, Some(msg)) => (path, msg) }
        if (fileToError.isEmpty) None else Some(fileToError.toString())
      })
      .asJava

  override def isSuccessful: Boolean = getErrors.isEmpty

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
    new ScalafixEvaluationImpl(error, errorMessage, Nil)
  }

  def from(
      fileEvaluations: Seq[ScalafixFileEvaluationImpl],
      exitStatus: ExitStatus
  ): ScalafixEvaluationImpl = {
    ScalafixEvaluationImpl(exitStatus, None, fileEvaluations)
  }

}
