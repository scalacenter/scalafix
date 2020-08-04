package scalafix.internal.interfaces

import java.lang
import java.nio.file.Path
import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.{ScalafixError, ScalafixOutput, ScalafixResult}
import scalafix.internal.util.OptionOps._

final case class ScalafixResultImpl(
    error: ExitStatus,
    getMessageError: Optional[String],
    scalafixOutputs: Seq[ScalafixOutputtImpl]
) extends ScalafixResult {

  override def isSuccessful: lang.Boolean = error.isOk

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
      scalafixOutputtImpl: Seq[ScalafixOutputtImpl],
      exitStatus: ExitStatus
  ): ScalafixResultImpl = {
    ScalafixResultImpl(exitStatus, Optional.empty(), scalafixOutputtImpl)
  }

}
