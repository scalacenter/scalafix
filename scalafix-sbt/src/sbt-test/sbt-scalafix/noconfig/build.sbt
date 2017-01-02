scalaVersion := "2.11.8"

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
  assert(
    assertContentMatches(
      "src/main/scala/Test.scala",
      expected
    )
  )
}
