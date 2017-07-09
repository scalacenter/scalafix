import sbt.Keys.TaskStreams
import difflib.DiffUtils

/**
  * Created by ollie on 02/01/17.
  */
object ScalafixTestUtility {
  private def calcDiff(expected: String, obtained: String): Option[String] = {
    import scala.collection.JavaConverters._

    if (expected == obtained) None
    else {
      def jLinesOf(s: String) = s.lines.toSeq.asJava

      val expectedLines = jLinesOf(expected)
      val obtainedLines = jLinesOf(obtained)

      val diff = DiffUtils.diff(expectedLines, obtainedLines)
      val unifiedDiff = DiffUtils.generateUnifiedDiff(
        "expected",
        "obtained",
        expectedLines,
        diff,
        1)

      Some(unifiedDiff.asScala.drop(3).mkString("\n"))
    }
  }

  // returns true if test succeeded, else false.
  def assertContentMatches(streams: TaskStreams)(
      file: String,
      expectedUntrimmed: String): Boolean = {
    val expected = expectedUntrimmed.trim
    val obtained = new String(
      java.nio.file.Files.readAllBytes(
        java.nio.file.Paths.get(
          new java.io.File(file).toURI
        ))
    ).trim

    calcDiff(expected, obtained)
      .map { diff =>
        val msg =
          s"""File: $file
             |Obtained output:
             |----------------
             |$obtained
             |Expected:
             |---------
             |$expected
             |Diff:
             |-----
             |$diff""".stripMargin

        streams.log.error(file)
        streams.log.error(msg)
        false
      }
      .getOrElse {
        streams.log.success(file)
        true
      }
  }
}
