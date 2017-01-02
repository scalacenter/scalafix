lazy val root = project
  .in(file("."))
  .aggregate(
    p1,
    p2,
    p3
  )

lazy val p1 = project.settings(scalaVersion := "2.11.8")
lazy val p2 = project.settings(scalaVersion := "2.10.5")
lazy val p3 = project.settings(scalaVersion := "2.12.1")

TaskKey[Unit]("check") := {
  val assertContentMatches: ((String, String) => Boolean) =
    scalafix.sbt.ScalafixTestUtility.assertContentMatches(streams.value) _
  val expected =
    """object Main {
      |  implicit val x: Int = 2
      |  lazy val y = 2
      |  def main(args: Array[String]): Unit = {
      |    println("hello")
      |  }
      |}""".stripMargin
  val testExpected = expected.replaceFirst("Main", "TestMain")
  val unchanged =
    """object Main {
      |  implicit val x = 2
      |  lazy val y = 2
      |  def main(args: Array[String]) {
      |    println("hello")
      |  }
      |}
    """.stripMargin
  val unchangedTest = unchanged.replaceFirst("Main", "TestMain")

  val results: Seq[Boolean] =
    Seq("p1/", "p3/").flatMap { prefix =>
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
    } ++ Seq(
      // 2.10 projects are left unchanged.
      assertContentMatches(
        "p2/src/main/scala/Test.scala",
        unchanged
      ),
      assertContentMatches(
        "p2/src/test/scala/Test.scala",
        unchangedTest
      )
    )
  if (results.contains(false)) ??? // non-zero exit code
}
