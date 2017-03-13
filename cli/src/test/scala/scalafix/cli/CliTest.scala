package scalafix.cli

import scalafix.config.ScalafixConfig
import scalafix.rewrite.VolatileLazyVal
import scalafix.util.DiffAssertions
import scalafix.util.FileOps

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import caseapp.core.WithHelp
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

class CliTest extends FunSuite with DiffAssertions {

  println(Cli.helpMessage)

  import BasicTest._

  test("testMain") {
    val Right(WithHelp(false, false, obtained)) =
      Cli.parse(
        Seq(
          "--debug",
          "--config",
          "imports.groupByPrefix=true",
          "--single-thread",
          "--files",
          "a.scala",
          "b.scala",
          "-i",
          "foo.scala",
          "bar.scala"
        ))
    assert(obtained.inPlace)
    assert(obtained.debug)
    assert(obtained.singleThread)
    assert(obtained.resolvedConfig.imports.groupByPrefix)
    assert(
      obtained.files == List("a.scala", "b.scala", "foo.scala", "bar.scala"))
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

  test("--rewrites") {
    assert(Cli.parse(Seq("--rewrites", "VolatileLazyVal")).isRight)
    assert(Cli.parse(Seq("--rewrites", "Foobar")).isLeft)
  }
}
