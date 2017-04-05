package scalafix.cli

class FatCliTest extends org.scalatest.FunSuite {
  test("--scalahost-nsc-plugin-path is not necessary") {
    assert(
      Cli
        .parse(List("--sourcepath", "foo.scala", "--classpath", "bar"))
        .isRight)
  }
}
