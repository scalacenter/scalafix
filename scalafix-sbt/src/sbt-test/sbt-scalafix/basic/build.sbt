lazy val root = project
  .in(file("."))
  .aggregate(
    p1,
    p2,
    p3
  )

lazy val p1 = project.settings(scalaVersion := "2.11.10")
lazy val p2 = project.settings(scalaVersion := "2.10.5")
lazy val p3 = project.settings(scalaVersion := "2.12.1")

TaskKey[Unit]("check") := {
  val assertContentMatches: ((String, String) => Boolean) =
    ScalafixTestUtility.assertContentMatches(streams.value) _
  val expected =
    """object Main {
      |  implicit val x = 2
      |  lazy val y = 2
      |  def main(args: Array[String]): Unit = {
      |    println("hello")
      |  }
      |}""".stripMargin
  val testExpected = expected.replaceFirst("Main", "TestMain")

  val results: Seq[Boolean] =
    Seq("p1/", "p2/", "p3/").flatMap { prefix =>
      Seq(
        assertContentMatches(
          prefix + "src/test/scala/Test.scala",
          testExpected
        ),
        assertContentMatches(
          prefix + "src/main/scala/Test.scala",
          expected
        )
      )
    }
  if (results.contains(false)) ??? // non-zero exit code
}
