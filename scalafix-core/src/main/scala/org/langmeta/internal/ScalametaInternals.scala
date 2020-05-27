package scala.meta.internal

import scala.meta._
import scala.meta.internal.semanticdb.Scala.Descriptor
import scala.meta.internal.semanticdb.Scala.DescriptorParser
import scala.meta.internal.trees.Origin
import scala.meta.internal.{semanticdb => s}

object ScalametaInternals {
  private val EOL = System.lineSeparator()

  def symbolOwnerAndDescriptor(symbol: String): (String, Descriptor) = {
    val (desc, owner) = DescriptorParser(symbol)
    (owner, desc)
  }

  def withOrigin[T <: Tree](tree: T, origin: Origin): T =
    tree.withOrigin(origin)

  def positionFromRange(input: Input, range: Option[s.Range]): Position =
    range match {
      case Some(r) => positionFromRange(input, r)
      case _ => Position.None
    }

  def positionFromRange(input: Input, range: s.Range): Position = {
    val inputEnd = Position.Range(input, input.chars.length, input.chars.length)
    def lineLength(line: Int): Int = {
      val isLastLine = line == inputEnd.startLine
      if (isLastLine) inputEnd.endColumn
      else input.lineToOffset(line + 1) - input.lineToOffset(line) - 1
    }
    val start = input.lineToOffset(range.startLine) +
      math.min(range.startCharacter, lineLength(range.startLine))
    val end = input.lineToOffset(range.endLine) +
      math.min(range.endCharacter, lineLength(range.endLine))
    Position.Range(input, start, end)
  }

  // Workaround for https://github.com/scalameta/scalameta/issues/1115
  def formatMessage(
      pos: Position,
      severity: String,
      message: String
  ): String = {
    if (pos != Position.None) {
      val input = pos.input
      val startLine = pos.startLine + 1
      val startColumn = pos.startColumn + 1
      val header =
        s"${input.syntax}:$startLine:$startColumn: $severity: $message"
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
