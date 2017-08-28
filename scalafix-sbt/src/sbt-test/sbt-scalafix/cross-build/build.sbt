import _root_.scalafix.Versions
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
lazy val scala211 = project.settings(scalaVersion := Versions.scala211)
lazy val scala212 = project.settings(scalaVersion := Versions.scala212)
lazy val customSourceroot = project.settings(
  scalaVersion := Versions.scala212,
  scalafixSourceroot := sourceDirectory.value
)

TaskKey[Unit]("check") := {
  val s = streams.value
  val assertContentMatches: ((String, String) => Boolean) =
    ScalafixTestUtility.assertContentMatches(s) _
  val expected =
    """object Main {
      |  def foo(a: (Int, String)) = a
      |  foo((1, "str"))
      |  def main(args: Array[String]): Unit = {
      |    println(1)
      |  }
      |}""".stripMargin
  val testExpected = expected.replaceFirst("Main", "MainTest")
  // NOTE: ProcedureSyntax runs since it's syntactic. Only semantic rewrites
  // don't trigger.
  def unchanged(code: String) =
    code
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

  assert(
    assertContentMatches(
      "sbtfix-me.sbt",
      """
        |lazy val x = {
        |  unmanagedSourceDirectories.in(Compile) += Def.setting(file("nested")).value
        |  "x"
        |}
        |
        |unmanagedSourceDirectories.in(Compile) += Def.setting(file("top")).value
        |""".stripMargin
    )
  )
}
