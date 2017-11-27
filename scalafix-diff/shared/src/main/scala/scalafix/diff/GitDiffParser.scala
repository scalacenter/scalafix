package scalafix.diff

import scala.util.matching.Regex

case class Range(start: Int, length: Int)

sealed trait GitDiff
case class NewFile(path: String) extends GitDiff
case class ModifiedFile(path: String, changes: List[Range]) extends GitDiff

private[diff] case class Hunk(
    originalLine: Int,
    originalCount: Int,
    revisedLine: Int,
    revisedCount: Int)

private[diff] object HunkExtractor {
  private val HunkHeader =
    "^@@ -(\\d+)(,(\\d+))? \\+(\\d+)(,(\\d+))? @@(.*)$".r
  def unapply(line: String): Option[Hunk] = {
    line match {
      case HunkHeader(
          originalLine,
          _,
          originalCount,
          revisedLine,
          _,
          revisedCount,
          _) => {
        Some(
          Hunk(
            originalLine.toInt,
            Option(originalCount).map(_.toInt).getOrElse(1),
            revisedLine.toInt,
            Option(revisedCount).map(_.toInt).getOrElse(1)
          ))
      }
      case _ => None
    }
  }
}

private[diff] class PeekableSource[T](it: Iterator[T]) {
  private def getNext(): Option[T] = {
    if (it.hasNext) Some(it.next())
    else None
  }
  private def setPeeker(): Unit = peeker = getNext()
  private var peeker: Option[T] = None
  setPeeker()

  def hasNext: Boolean = !peeker.isEmpty || it.hasNext
  def next(): T = {
    val ret = peeker
    setPeeker()
    ret.get
  }
  def peek: Option[T] = peeker
  def drop(n: Int): Unit = {
    it.drop(n - 1)
    setPeeker()
  }
}

class GitDiffParser(input: Iterator[String]) {
  private val lines = new PeekableSource(input)

  private val Command = "^diff --git a/(.*) b/(.*)$".r
  private val Deleted = "^deleted file mode [0-9]{6}$".r
  private val New = "^new file mode [0-9]{6}$".r
  private val Index = "^index [a-z0-9]{8}..[a-z0-9]{8}$".r
  private val Index2 = "^index [a-z0-9]{8}..[a-z0-9]{8} [0-9]{6}$".r
  private val Similarity = "similarity index \\d{2}%".r
  private val OriginalFile = "--- (.*)$".r
  private val RevisedFile = "^\\+\\+\\+ (.*)$".r
  private val RenameFrom = "^rename from (.*)$".r
  private val RenameTo = "^rename to (.*)$".r
  private val NoNewLine = "\\ No newline at end of file"

  private def accept(regex: Regex): Unit = {
    val line = lines.next()
    if (regex.findFirstIn(line).isEmpty) {
      throw new Exception(s"Unexpected: $line")
    }
  }
  private def optional(expr: String): Unit = {
    lines.peek match {
      case Some(line) if line == expr => skip(1)
      case _ => ()
    }
  }

  private def skip(n: Int): Unit = lines.drop(n)

  private def acceptHunks(): List[Range] = {
    val ranges = List.newBuilder[Range]
    accept(OriginalFile)
    accept(RevisedFile)

    var hasHunks = true
    while (hasHunks) {
      lines.peek match {
        case Some(HunkExtractor(
              Hunk(_, originalCount, revisedLine, revisedCount))) => {
          skip(originalCount + revisedCount + 1)
          hasHunks = true
          if (revisedCount > 0) {
            ranges += Range(revisedLine, revisedCount)
          }
        }
        case _ => {
          hasHunks = false
        }
      }
    }

    ranges.result()
  }
  def parse(): List[GitDiff] = {
    val diffs = List.newBuilder[GitDiff]

    while (lines.hasNext) {
      val Command(originalPath, revisedPath) = lines.next()

      lines.next() match {
        case Deleted() => {
          accept(Index)
          accept(OriginalFile)
          accept(RevisedFile)
          lines.next() match {
            case HunkExtractor(Hunk(_, originalCount, _, _)) =>
              skip(originalCount)
            case line =>
              throw new Exception(s"expected HunkHeader, got '$line'")
          }
          optional(NoNewLine)
        }
        case New() => {
          accept(Index)
          accept(OriginalFile)
          accept(RevisedFile)
          lines.next() match {
            case HunkExtractor(Hunk(_, originalCount, _, revisedCount)) => {
              diffs += NewFile(revisedPath)
              skip(originalCount + revisedCount)
            }
            case line =>
              throw new Exception(s"expected HunkHeader, got '$line'")
          }
        }
        case Index2() => {
          diffs += ModifiedFile(revisedPath, acceptHunks())
        }
        case Similarity() => {
          accept(RenameFrom)
          accept(RenameTo)
          accept(Index2)
          diffs += ModifiedFile(revisedPath, acceptHunks())
        }
        case line => throw new Exception(s"other: '$line'")
      }
    }
    diffs.result()
  }
}
