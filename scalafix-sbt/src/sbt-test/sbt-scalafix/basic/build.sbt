scalaVersion in ThisBuild := "2.11.8"
lazy val root = project
  .in(file("."))
  .aggregate(p1)

lazy val p1 = project

TaskKey[Unit]("check") := {
  def assertContentMatches(file: String, expected: String): Unit = {
    val obtained =
      scala.io.Source
        .fromFile(file)
        .getLines()
        .mkString("\n")

    if (obtained.trim != expected.trim) {
      val msg =
        s"""File: $file
           |Obtained output:
           |$obtained
           |Expected:
           |$expected
           |""".stripMargin
      streams.value.log.error(file)
      throw new Exception(msg)
    } else {
      streams.value.log.success(file)
    }
  }
  assertContentMatches(
    "src/main/scala/Test.scala",
    """
      |object Main {
      |  implicit val x: Int = 23
      |  def main(args: Array[String]) {
      |    println("hello")
      |  }
      |}
    """.stripMargin
  )

  val expected =
    """
      |object TestMain {
      |  implicit val x: Int = 2
      |}
    """.stripMargin

  assertContentMatches(
    "src/test/scala/Test.scala",
    expected
  )

  assertContentMatches(
    "p1/src/test/scala/Test.scala",
    expected
  )

}
