package scalafix.sbt

import sbt.Keys.TaskStreams

/**
  * Created by ollie on 02/01/17.
  */
object ScalafixTestUtility {
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

    if (obtained.diff(expected).nonEmpty ||
        expected.diff(obtained).nonEmpty) {
      val msg =
        s"""File: $file
           |Obtained output:
           |$obtained
           |Expected:
           |$expected
           |Diff:
           |${obtained.diff(expected)}
           |${expected.diff(obtained)}
           |""".stripMargin
      streams.log.error(file)
      streams.log.error(msg)
      false
    } else {
      streams.log.success(file)
      true
    }
  }
}
