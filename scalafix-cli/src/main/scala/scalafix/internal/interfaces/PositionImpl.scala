package scalafix.internal.interfaces

import java.util.Optional
import scala.meta.inputs.Position
import scalafix.interfaces.ScalafixInput
import scalafix.interfaces.ScalafixPosition
import scalafix.internal.util.PositionSyntax._

object PositionImpl {
  def optionalFromScala(pos: Position): Optional[ScalafixPosition] =
    if (pos == Position.None) Optional.empty()
    else Optional.of(fromScala(pos))
  def fromScala(pos: Position): ScalafixPosition =
    new ScalafixPosition {
      override def formatMessage(severity: String, message: String): String =
        pos.formatMessage(severity, message)
      override def startOffset(): Int = pos.start
      override def startLine(): Int = pos.startLine
      override def startColumn(): Int = pos.startColumn
      override def endOffset(): Int = pos.end
      override def endLine(): Int = pos.endLine
      override def endColumn(): Int = pos.endColumn
      override def input(): ScalafixInput =
        ScalafixInputImpl.fromScala(pos.input)
    }
}
