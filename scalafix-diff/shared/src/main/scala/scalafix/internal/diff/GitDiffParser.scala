package scalafix.internal.diff

import java.nio.file.Path

import scalafix.diff.{GitChange, GitDiff, NewFile, ModifiedFile}

import scala.util.matching.Regex

class GitParseException(msg: String) extends Exception(msg)

private[scalafix] class GitDiffParser(
    input: Iterator[String],
    workingDir: Path) {
  private val lines = new PeekableSource(input)

  private val Command = "^diff --git a/(.*) b/(.*)$".r
  private val Deleted = "^deleted file mode [0-9]{6}$".r
  private val New = "^new file mode [0-9]{6}$".r
  private val Index = "^index [a-z0-9]{1,40}..[a-z0-9]{1,40}$".r
  private val Index2 = "^index [a-z0-9]{1,40}..[a-z0-9]{1,40} [0-9]{6}$".r
  private val Similarity = "similarity index (\\d{2,3})%".r
  private val OriginalFile = "--- (.*)$".r
  private val RevisedFile = "^\\+\\+\\+ (.*)$".r
  private val RenameFrom = "^rename from (.*)$".r
  private val RenameTo = "^rename to (.*)$".r
  private val NoNewLine = "\\ No newline at end of file"

  def path(relative: String): Path =
    workingDir.resolve(relative)

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

  private def acceptHunks(): List[GitChange] = {
    val changes = List.newBuilder[GitChange]
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
            changes += GitChange(revisedLine, revisedCount)
          }
        }
        case _ => {
          hasHunks = false
        }
      }
    }

    changes.result()
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
              throw new GitParseException(s"expected HunkHeader, got '$line'")
          }
          optional(NoNewLine)
        }
        case New() => {
          accept(Index)
          accept(OriginalFile)
          accept(RevisedFile)
          lines.next() match {
            case HunkExtractor(Hunk(_, originalCount, _, revisedCount)) => {
              diffs += NewFile(path(revisedPath))
              skip(originalCount + revisedCount)
            }
            case line =>
              throw new GitParseException(s"expected HunkHeader, got '$line'")
          }
          optional(NoNewLine)
        }
        case Index2() => {
          diffs += ModifiedFile(path(revisedPath), acceptHunks())
        }
        case Similarity(percentage) => {
          accept(RenameFrom)
          accept(RenameTo)
          if (percentage != "100") {
            accept(Index2)
            diffs += ModifiedFile(path(revisedPath), acceptHunks())
          }
        }
        case line => throw new GitParseException(s"unexpected: '$line'")
      }
    }
    diffs.result()
  }
}
