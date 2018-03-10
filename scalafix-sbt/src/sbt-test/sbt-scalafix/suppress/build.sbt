import _root_.scalafix.Versions

val suppress = project.settings(
  scalaVersion := Versions.scala212
)

TaskKey[Unit]("check") := {
  val s = streams.value

  assert(
    ScalafixTestUtility.assertContentMatches(s)(
      "suppress/src/main/scala/Main.scala",
      """
        |object Main {
        |  println(1 + 2.asInstanceOf/* scalafix:ok */[Double])
        |}
    """.stripMargin
    ))
}
