scalaVersion := "2.11.8" // 2.11.8 is required for "-Ybackend:GenBCode"

TaskKey[Unit]("check") := {
  val obtained =
    scala.io.Source
      .fromFile("src/main/scala/Test.scala")
      .getLines()
      .mkString("\n")
  val expected =
    """|package main
       |
       |object Main {
       |  def main(args: String): Unit = {
       |    println("hello")
       |  }
       |}
       |
       |class Y
       |
       |class X {
       |  implicit val y: main.Y = new Y
       |}t¬
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
