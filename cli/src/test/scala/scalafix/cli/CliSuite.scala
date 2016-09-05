package scalafix.cli

import scalafix.util.DiffAssertions
import scalafix.util.FileOps

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import org.scalatest.FunSuite

object BasicTest {
  val original = """|object Main {
                    |  def foo() {
                    |    println(1)
                    |  }
                    |}
                 """.stripMargin
  val expected = """|object Main {
                    |  def foo(): Unit = {
                    |    println(1)
                    |  }
                    |}
                 """.stripMargin

}

class CliSuite extends FunSuite with DiffAssertions {

  println(Cli.helpMessage)

  import BasicTest._

  test("testMain") {
    val expected = ScalafixOptions(
      files = List("foo", "bar"),
      inPlace = true
    )
    val Right(obtained) = Cli.parse(Seq("-i", "foo", "bar"))
    assertEqual(obtained, expected)
  }

  test("write fix to file") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    Cli.runOn(
      ScalafixOptions(files = List(file.getAbsolutePath), inPlace = true))
    assertNoDiff(FileOps.readFile(file), expected)
  }

  test("print to stdout does not write to file") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    val baos = new ByteArrayOutputStream()
    Cli.runOn(
      ScalafixOptions(
        common = CommonOptions(
          out = new PrintStream(baos)
        ),
        files = List(file.getAbsolutePath)
      ))
    assertNoDiff(FileOps.readFile(file), original)
    assertNoDiff(new String(baos.toByteArray), expected)
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

    Cli.runOn(
      ScalafixOptions(files = List(dir.getAbsolutePath), inPlace = true))
    assertNoDiff(FileOps.readFile(file1), expected)
    assertNoDiff(FileOps.readFile(file2), expected)
  }
}
