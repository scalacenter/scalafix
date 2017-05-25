package scalafix.cli

import scala.collection.immutable.Seq
import scalafix.rewrite.ExplicitReturnTypes
import scalafix.rewrite.ProcedureSyntax
import scalafix.testkit.DiffAssertions
import scalafix.util.FileOps
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import scalafix.cli.CliCommand.RunScalafix
import caseapp.core.WithHelp
import org.scalatest.FunSuite
import scalafix.syntax._
import org.scalameta.logger

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

  val devNull = CommonOptions(
    err = new PrintStream(new ByteArrayOutputStream())
  )

  import BasicTest._

  test("Cli.parse") {
    val RunScalafix(runner) = Cli.parse(
      Seq(
        "--verbose",
        "--config-str",
        "imports.groupByPrefix=true",
        "--single-thread",
        "--files",
        "a.scala",
        "b.scala",
        "--stdout",
        "foo.scala",
        "bar.scala"
      ))
    val obtained = runner.cli
    assert(!runner.writeMode.isWriteFile)
    assert(runner.config.imports.groupByPrefix)
    assert(obtained.verbose)
    assert(obtained.singleThread)
    assert(
      obtained.files == List("a.scala", "b.scala", "foo.scala", "bar.scala"))
  }

  test("write fix to file") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    Cli.runOn(
      ScalafixOptions(
        rewrites = List(ProcedureSyntax.toString),
        files = List(file.getAbsolutePath),
        inPlace = true
      ))
    assertNoDiff(FileOps.readFile(file), expected)
  }

  test("print to stdout does not write to file") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, original)
    val baos = new ByteArrayOutputStream()
    val exit = Cli.runOn(
      ScalafixOptions(
        common = CommonOptions(
          out = new PrintStream(baos)
        ),
        rewrites = List(ProcedureSyntax.toString),
        files = List(file.getAbsolutePath),
        stdout = true
      ))
    assert(exit == ExitStatus.Ok)
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
      ScalafixOptions(rewrites = List(ProcedureSyntax.toString),
                      files = List(dir.getAbsolutePath)))
    assertNoDiff(FileOps.readFile(file1), expected)
    assertNoDiff(FileOps.readFile(file2), expected)
  }

  test("--rewrites") {
    val RunScalafix(runner) =
      Cli.parse(Seq("--rewrites", "VolatileLazyVal"))
    assert(runner.rewrite.name == "VolatileLazyVal")
    assert(Cli.parse(Seq("--rewrites", "Foobar")).isError)
  }

  test("--sourcepath --classpath") {
    assert(Cli.parse(List("--sourcepath", "foo.scala")).isError)
    assert(Cli.parse(List("--classpath", "foo")).isError)
    // NOTE: This assertion should fail by default, but scalafix-cli % Test
    // depends on testkit, which has scalahost-nsc as a dependency.
    assert(
      Cli
        .parse(List("--sourcepath", "/foo.scala", "--classpath", "/bar"))
        .isOk
    )
    assert(
      Cli
        .parse(
          List(
            "--sourcepath",
            "/foo.scala",
            "--classpath",
            "/bar"
          ))
        .isOk)
  }

  test("error returns failure exit code") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, "object a { implicit val x = ??? }")
    val code = Cli.runOn(
      ScalafixOptions(rewrites = List(ExplicitReturnTypes.toString),
                      files = List(file.getAbsolutePath),
                      common = devNull))
    assert(code == ExitStatus.InvalidCommandLineOption)
  }

  test(".sbt files get fixed with sbt dialect") {
    val file = File.createTempFile("prefix", ".sbt")
    FileOps.writeFile(file, "def foo { println(1) }\n")
    val code = Cli.runOn(
      ScalafixOptions(rewrites = List(ProcedureSyntax.toString),
                      files = List(file.getAbsolutePath),
                      common = devNull))
    assert(code == ExitStatus.Ok)
    assert(FileOps.readFile(file) == "def foo: Unit = { println(1) }\n")
  }
}
