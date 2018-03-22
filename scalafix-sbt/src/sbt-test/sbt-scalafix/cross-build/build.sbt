import _root_.scalafix.Versions
inThisBuild(
  List(
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    scalacOptions += "-Ywarn-adapted-args", // For NoAutoTupling
    resolvers += Resolver.sonatypeRepo("releases")
  )
)
lazy val root = project
  .in(file("."))
  .aggregate(
    javaProject,
    customSourceroot,
    scala211,
    scala210,
    scala212
  )

lazy val scala210 = project.settings(
  scalaVersion := "2.10.5"
)
lazy val scala211 = project.settings(
  scalaVersion := Versions.scala211
)
lazy val scala212 = project
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    inConfig(IntegrationTest)(scalafixConfigSettings),
    scalaVersion := Versions.scala212
  )
lazy val customSourceroot = project.settings(
  scalaVersion := Versions.scala212,
  scalafixSourceroot := sourceDirectory.value
)
lazy val javaProject = project.settings(
  scalaVersion := Versions.scala212
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
  // NOTE: ProcedureSyntax runs since it's syntactic. Only semantic rules
  // don't trigger.
  def unchanged(code: String) =
    code
      .replace("((", "(")
      .replace("\"))", "\")")

  val results: Seq[Boolean] =
    assertContentMatches(
      "scala212/src/it/scala/Main.scala",
      expected
    ) +:
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
      """// sbtfixed
        |""".stripMargin
    )
  )
}
