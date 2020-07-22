package scalafix.internal.interfaces

import java.lang
import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.{
  ScalafixError,
  ScalafixOutput,
  ScalafixResult
}
import scalafix.internal.util.OptionOps._

final case class ScalafixResultImpl(
    error: ExitStatus,
    getMessageError: Optional[String],
    scalafixOutputs: Seq[ScalafixOutputtImpl]
) extends ScalafixResult {

  override def isSuccessful: lang.Boolean = error.isOk

  override def getError: Optional[ScalafixError] =
    ScalafixErrorImpl.fromScala(error).asJava

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
      scalafixOutputtImpl: Seq[ScalafixOutputtImpl]
  ): ScalafixResultImpl = {
    val error = ExitStatus.merge(scalafixOutputtImpl.map(_.error))
    ScalafixResultImpl(error, Optional.empty(), scalafixOutputtImpl)

  }

}
