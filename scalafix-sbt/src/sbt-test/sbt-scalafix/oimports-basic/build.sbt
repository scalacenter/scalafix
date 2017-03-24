scalaVersion := "2.12.1"

scalafixConfig in ThisBuild := Some(file("myscalafix.conf"))

TaskKey[Unit]("check") := {
  val assertContentMatches: ((String, String) => Boolean) =
    ScalafixTestUtility.assertContentMatches(streams.value) _
  val expected = "object Test"

  assert(
    assertContentMatches(
      "src/main/scala/Test.scala",
      expected
    )
  )
}
