import _root_.scalafix.Versions

val suppress = project.settings(
  scalaVersion := Versions.scala212
)

TaskKey[Unit]("check") := {
  val s = streams.value
  val assertContentMatches: ((String, String) => Boolean) =
    ScalafixTestUtility.assertContentMatches(s) _

  assert(
    assertContentMatches(
      "suppress/src/main/scala/Main.scala",
      """
        |object Main {
        |  println(1 + 2.asInstanceOf/* scalafix:ok */[Double])
        |}
    """.stripMargin
    ))
}
