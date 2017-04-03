scalaVersion := "2.11.8"

TaskKey[Unit]("check") := {
  val expected =
    """object Main {
      |  implicit val x = 2
      |  lazy val y = 2
      |  def main(args: Array[String]) {
      |    println("hello")
      |  }
      |}""".stripMargin
  assert(
    ScalafixTestUtility.assertContentMatches(streams.value)(
      "src/main/scala/Test.scala",
      expected
    )
  )
}
