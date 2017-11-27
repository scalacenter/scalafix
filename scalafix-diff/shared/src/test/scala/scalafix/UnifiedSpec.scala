// import difflib.{DiffUtils => DU}
// DU.parseUnifiedDiff

// sealed trait GitDiff
// case class FileAdd(path: String) extends GitDiff
// case class FileMod(path: String, patches: List[Patch]) extends GitDiff
// case class Patch(startLine: Int, lineCount: Int)

package scalafix.diff

import org.scalatest.FunSuite
import scala.util.matching.Regex

class UnifiedSpec extends FunSuite {

  test("parse chunks") {
    val chunkHeaders = List(
      "@@ -1,18 +0,0 @@",
      "@@ -11 +11 @@ import scalafix.util.SymbolMatcher",
      "@@ -14 +14,3 @@ import scalafix.syntax._",
    )

    val chunks = List(
      Chunk(1, 18, 0, 0),
      Chunk(11, 1, 11, 1),
      Chunk(14, 1, 14, 3)
    )

    chunkHeaders.zip(chunks).map {
      case (header, expected) =>
        header match {
          case ChunkExtractor(chunk) =>
            assert(chunk == expected)
        }
    }
  }

  case class Chunk(
      originalLine: Int,
      originalCount: Int,
      revisedLine: Int,
      revisedCount: Int)
  object ChunkExtractor {
    private val ChunkHeader =
      "^@@ -(\\d+)(,(\\d+))? \\+(\\d+)(,(\\d+))? @@(.*)$".r
    def unapply(line: String): Option[Chunk] = {
      line match {
        case ChunkHeader(
            originalLine,
            _,
            originalCount,
            revisedLine,
            _,
            revisedCount,
            _) => {
          Some(
            Chunk(
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

  test("boom") {
    import scala.io.Source
    val source =
      Source.fromURL(getClass.getClassLoader.getResource("./git.diff"))
    val lines = source.getLines

    val Command = "^diff --git a/(.*) b/(.*)$".r
    val Deleted = "^deleted file mode [0-9]{6}$".r
    val New = "^new file mode [0-9]{6}$".r
    val Index = "^index [a-z0-9]{8}..[a-z0-9]{8}$".r
    val Index2 = "^index [a-z0-9]{8}..[a-z0-9]{8} [0-9]{6}$".r
    val OriginalFile = "--- (.*)$".r
    val RevisedFile = "^\\+\\+\\+ (.*)$".r
    val ChunkHeader = "^@@ -(\\d+)(,(\\d+))? \\+(\\d+)(,(\\d+))? @@(.*)$".r

    val NoNewLine = "\\ No newline at end of file".r

    def accept(regex: Regex): Unit = {
      val line = lines.next()
      if (regex.findFirstIn(line).isEmpty) {
        throw new Exception(s"Unexpected: $line")
      }
    }

    def skip(n: Int): Unit = lines.drop(n)

    while (lines.hasNext) {
      val Command(p1, p2) = lines.next()

      lines.next() match {
        case Deleted() => {
          accept(Index)
          accept(OriginalFile)
          accept(RevisedFile)
          lines.next match {
            case ChunkExtractor(Chunk(_, originalCount, _, _)) =>
              skip(originalCount)
            case line =>
              throw new Exception(s"expected ChunkHeader, got '$line'")
          }
          accept(NoNewLine)
        }
        case New() => {
          accept(Index)
          accept(OriginalFile)
          accept(RevisedFile)
          lines.next match {
            case ChunkExtractor(Chunk(_, originalCount, _, revisedCount)) => {
              println(s"added file: $p1")
              skip(originalCount + revisedCount)
            }
            case line =>
              throw new Exception(s"expected ChunkHeader, got '$line'")
          }
        }
        case Index2() => {
          accept(OriginalFile)
          accept(RevisedFile)
          lines.next match {
            case ChunkExtractor(_) => {
              // println(s"got 2 $a $b $c $d $rest")
              // skip(b + d - 1)
            }
            case line => {
              // println(p1)
              throw new Exception(s"expected ChunkHeader, got '$line'")
            }
          }
        }
        case line => throw new Exception(s"other: '$line'")
      }
    }

    // @@ -1,18 +0,0 @@

    // old mode <mode>
    // new mode <mode>
    // deleted file mode <mode>
    // new file mode <mode>
    // copy from <path>
    // copy to <path>
    // rename from <path>
    // rename to <path>
    // similarity index <number>
    // dissimilarity index <number>
    // index <hash>..<hash> <mode>

    source.close()
  }
}
