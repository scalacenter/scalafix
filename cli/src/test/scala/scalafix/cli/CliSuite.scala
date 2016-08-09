package scalafix.cli

import scalafix.util.DiffAssertions
import scalafix.util.FileOps

import java.io.File

import org.scalatest.FunSuite

class CliSuite extends FunSuite with DiffAssertions {

  test("testMain") {
    val expected = Cli.Config(
        Set(new File("foo"), new File("bar")),
        inPlace = true
    )
    val obtained = Cli.parser.parse(
        Seq(
            "--files",
            "bar",
            "--files",
            "foo",
            "-i"
        ),
        Cli.default
    )
    assert(obtained.get === expected)
  }

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

  test("write fix to file") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    Cli.runOn(Cli.Config(Set(file), inPlace = true))
    assertNoDiff(FileOps.readFile(file), expected)
  }

  test("print to stdout does not write to file") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    Cli.runOn(Cli.Config(Set(file)))
    assertNoDiff(FileOps.readFile(file), original)
  }

  test("write fix to directory") {
    val dir = File.createTempFile("project/src/main/scala", "sbt")
    dir.delete()
    dir.mkdirs()
    assert(dir.isDirectory)

    def createFile(): File = {
      val file = File.createTempFile("file", ".scala", dir)
      FileOps.writeFile(file, original)
      file
    }
    val file1, file2 = createFile()

    Cli.runOn(Cli.Config(Set(dir), inPlace = true))
    assertNoDiff(FileOps.readFile(file1), expected)
    assertNoDiff(FileOps.readFile(file2), expected)
  }
}
