scalaVersion := "2.11.8"

scalafixConfig in ThisBuild := Some(file("myscalafix.conf"))

TaskKey[Unit]("check") := {
  val assertContentMatches: ((String, String) => Boolean) =
    scalafix.sbt.ScalafixTestUtility.assertContentMatches(streams.value) _
  val expected =
    """object Main {
      |  implicit val x = 2
      |  @volatile lazy val y = 2
      |  def main(args: Array[String]) {
      |    println("hello")
      |  }
      |}""".stripMargin
  assert(
    assertContentMatches(
      "src/main/scala/Test.scala",
      expected
    )
  )
}
