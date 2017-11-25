package org.langmeta.internal

import scala.compat.Platform.EOL
import org.langmeta._

object ScalafixLangmetaHacks {
  // Workaround for https://github.com/scalameta/scalameta/issues/1115
  def formatMessage(
      pos: Position,
      severity: String,
      message: String): String = {
    if (pos != Position.None) {
      val input = pos.input
      val header =
        s"${input.syntax}:${pos.startLine + 1}: $severity: $message"
      val line = {
        val start = input.lineToOffset(pos.startLine)
        val notEof = start < input.chars.length
        val end = if (notEof) input.lineToOffset(pos.startLine + 1) else start
        val count = end - start
        val eolOffset =
          if (input.chars.lift(start + count - 1).contains('\n')) -1 else 0
        new String(input.chars, start, math.max(0, count + eolOffset))
      }
      val caret = " " * pos.startColumn + "^"
      header + EOL + line + EOL + caret
    } else {
      s"$severity: $message"
    }
  }
}
