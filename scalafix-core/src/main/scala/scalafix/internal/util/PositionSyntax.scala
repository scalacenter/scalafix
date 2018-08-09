package scalafix.internal.util

import scala.meta.internal.{semanticdb => s}
import scala.meta._
import scala.meta.internal.ScalametaInternals

object PositionSyntax {

  implicit class XtensionPositionsScalafix(private val pos: Position)
      extends AnyVal {

    /** Returns a formatted string of this position including filename/line/caret. */
    def formatMessage(severity: String, message: String): String = pos match {
      case Position.None =>
        s"$severity: $message"
      case _ =>
        new java.lang.StringBuilder()
          .append(lineInput)
          .append(if (severity.isEmpty) "" else " ")
          .append(severity)
          .append(
            if (message.isEmpty) ""
            else if (severity.isEmpty) " "
            else if (message.startsWith("\n")) ":"
            else ": "
          )
          .append(message)
          .append("\n")
          .append(lineContent)
          .append("\n")
          .append(lineCaret)
          .toString
    }

    def lineInput: String =
      s"${pos.input.syntax}:${pos.startLine + 1}:${pos.startColumn + 1}:"

    def lineCaret: String = pos match {
      case Position.None =>
        ""
      case _ =>
        val caret =
          if (pos.start == pos.end) "^"
          else if (pos.startLine == pos.endLine) "^" * (pos.end - pos.start)
          else "^"
        (" " * pos.startColumn) + caret
    }

    def lineContent: String = pos match {
      case Position.None => ""
      case range: Position.Range =>
        val pos = ScalametaInternals.positionFromRange(
          range.input,
          s.Range(
            startLine = range.startLine,
            startCharacter = 0,
            endLine = range.startLine,
            endCharacter = Int.MaxValue
          )
        )
        pos.text
    }
  }

}
