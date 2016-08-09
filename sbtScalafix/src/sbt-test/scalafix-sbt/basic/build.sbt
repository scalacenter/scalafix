sbtPlugin := true


TaskKey[Unit]("check") := {
  val obtained =
    scala.io.Source
      .fromFile("src/main/scala/Test.scala")
        .getLines()
        .mkString("\n")
  val expected =
    """
      |object Main {
      |  def main(args: Array[String]): Unit = {
      |    println("hello")
      |  }
      |}
    """.stripMargin

  if (obtained.trim != expected.trim) {
    val msg =
      s"""Obtained output:
         |$obtained
         |Expected:
         |$expected
         |""".stripMargin
    System.err.println(msg)
    throw new Exception(msg)
  }
}
