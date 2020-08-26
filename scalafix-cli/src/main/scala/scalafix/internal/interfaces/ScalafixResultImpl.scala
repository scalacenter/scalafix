package scalafix.internal.interfaces

import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.{ScalafixError, ScalafixOutput, ScalafixEvaluation}
import scalafix.internal.util.OptionOps._

final case class ScalafixResultImpl(
    error: ExitStatus,
    getMessageError: Optional[String],
    scalafixOutputs: Seq[ScalafixOutputImpl]
) extends ScalafixEvaluation {

  override def isSuccessful: Boolean = error.isOk

  override def getError: Array[ScalafixError] = {
    ScalafixErrorImpl.fromScala(error)
  }

  override def getScalafixOutputs: Array[ScalafixOutput] =
    scalafixOutputs.toArray

  override def writeResult(): Array[ScalafixError] = {
    scalafixOutputs.flatMap(o => o.applyPatches()).toArray
  }
}

object ScalafixResultImpl {

  def apply(
      error: ExitStatus,
      errorMessage: Option[String] = None
  ): ScalafixResultImpl = {
    new ScalafixResultImpl(error, errorMessage.asJava, Nil)
  }

  def from(
      scalafixOutputtImpl: Seq[ScalafixOutputImpl],
      exitStatus: ExitStatus
  ): ScalafixResultImpl = {
    ScalafixResultImpl(exitStatus, Optional.empty(), scalafixOutputtImpl)
  }

}
