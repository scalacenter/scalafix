package scalafix.cli

import scalafix.util.DiffAssertions
import scalafix.util.FileOps

import java.io.File

import org.scalatest.FunSuite

class CliSuite extends FunSuite with DiffAssertions {

  test("testMain") {
    val expected = Cli.Config(
        Set(new File("foo"), new File("bar"))
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
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    Cli.runOn(Cli.Config(Set(file), inPlace = true))

    assertNoDiff(FileOps.readFile(file), expected)
  }
}
