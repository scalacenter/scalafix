package scalafix.cli

import scalafix.util.DiffAssertions

import java.io.{File => JFile}

import better.files.File.OpenOptions
import better.files._

import org.scalatest.FunSuite

class CliSuite extends FunSuite with DiffAssertions {

  test("testMain") {
    val expected = Cli.Config(
        Set(new JFile("foo"), new JFile("bar"))
    )
    val obtained = Cli.parser.parse(Seq(
                                        "--files",
                                        "bar",
                                        "--files",
                                        "foo"
                                    ),
                                    Cli.default)
    assert(obtained.get === expected)
  }

  test("write fix to file") {
    val original = """
                     |object Main {
                     |  def foo() {
                     |   println(1)
                     |  }
                     |}
      """.stripMargin
    val expected = """
                     |object Main {
                     |  def foo(): Unit = {
                     |    println(1)
                     |  }
                     |}
      """.stripMargin
    val file = JFile.createTempFile("prefix", ".scala")
    file.toScala.write(original.getBytes())(OpenOptions.default)
    Cli.runOn(Cli.Config(Set(file), inPlace = true))

    assertNoDiff(file.toScala.contentAsString, expected)
  }
}
