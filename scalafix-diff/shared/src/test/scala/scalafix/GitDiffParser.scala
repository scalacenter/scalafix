package scalafix.diff

import org.scalatest.FunSuite
import scala.io.Source

class GitDiffParserSpec extends FunSuite {

  test("parse chunks") {
    val hunkHeaders = List(
      "@@ -1,18 +0,0 @@",
      "@@ -11 +11 @@ import scalafix.util.SymbolMatcher",
      "@@ -14 +14,3 @@ import scalafix.syntax._",
    )

    val hunks = List(
      Hunk(1, 18, 0, 0),
      Hunk(11, 1, 11, 1),
      Hunk(14, 1, 14, 3)
    )

    hunkHeaders.zip(hunks).map {
      case (header, expected) =>
        header match {
          case HunkExtractor(hunk) =>
            assert(hunk == expected)
        }
    }
  }

  def show(diffs: List[GitDiff]): Unit = {
    println("== New Files ==")
    diffs.foreach {
      case NewFile(path) => println(path)
      case _ => ()
    }

    println("== Modified Files ==")
    diffs.foreach {
      case ModifiedFile(path, changes) => {
        println(path)
        changes.foreach {
          case Range(start, offset) => println(s"  [$start, ${start + offset}]")
        }
      }
      case _ => ()
    }
  }

  test("parse tests") {
    (1 to 2).foreach { i =>
      val source = Source.fromURL(
        getClass.getClassLoader.getResource(s"./git$i.diff")
      )
      val gitDiffparser = new GitDiffParser(source.getLines)
      val diffs = gitDiffparser.parse()
      assert(!diffs.isEmpty)
      // show(diffs)
      source.close()
    }
  }
}
