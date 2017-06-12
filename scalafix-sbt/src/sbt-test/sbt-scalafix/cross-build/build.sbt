updateOptions in ThisBuild := updateOptions.value.withLatestSnapshots(false)
scalacOptions in ThisBuild += "-Ywarn-adapted-args" // For NoAutoTupling
lazy val root = project
  .in(file("."))
  .aggregate(
    customSourceroot,
    scala211,
    scala210,
    scala212
  )

lazy val scala210 = project.settings(scalaVersion := "2.10.5")
lazy val scala211 = project.settings(scalaVersion := "2.11.11")
lazy val scala212 = project.settings(scalaVersion := "2.12.2")
lazy val customSourceroot = project.settings(
  scalaVersion := "2.12.2",
  scalametaSourceroot := sourceDirectory.value
)

TaskKey[Unit]("check") := {
  val assertContentMatches: ((String, String) => Boolean) =
    ScalafixTestUtility.assertContentMatches(streams.value) _
  val expected =
    """object Main {
      |  def foo(a: (Int, String)) = a
      |  foo((1, "str"))
      |  def main(args: Array[String]): Unit = {
      |    println(1)
      |  }
      |}""".stripMargin
  val testExpected = expected.replaceFirst("Main", "MainTest")
  def unchanged(code: String) =
    code
      .replace(": Unit =", "")
      .replace("((", "(")
      .replace("\"))", "\")")

  val results: Seq[Boolean] =
    Seq(scala210, scala211, scala212, customSourceroot).flatMap { project =>
      val prefix = project.id
      Seq(
        assertContentMatches(
          prefix + "/src/test/scala/MainTest.scala",
          if (prefix.contains("210")) unchanged(testExpected)
          else testExpected
        ),
        assertContentMatches(
          prefix + "/src/main/scala/Main.scala",
          if (prefix.contains("210")) unchanged(expected)
          else expected
        )
      )
    }

  if (results.contains(false)) sys.error("Assertions failed.")
}
