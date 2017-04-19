package scalafix.cli

import scalafix.config.ScalafixConfig
import scalafix.rewrite.ExplicitImplicit
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.VolatileLazyVal
import scalafix.testkit.DiffAssertions
import scalafix.util.FileOps

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import caseapp.core.WithHelp
import org.scalameta.logger
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

  val devNull = CommonOptions(
    err = new PrintStream(new ByteArrayOutputStream())
  )

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
    assert(obtained.resolvedConfig.get.imports.groupByPrefix)
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
        files = List(file.getAbsolutePath)
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
                      files = List(dir.getAbsolutePath),
                      inPlace = true))
    assertNoDiff(FileOps.readFile(file1), expected)
    assertNoDiff(FileOps.readFile(file2), expected)
  }

  test("--rewrites") {
    val Right(WithHelp(_, _, obtained)) =
      Cli.parse(Seq("--rewrites", "VolatileLazyVal"))
    assert(obtained.resolvedConfig.get.rewrite.name == "VolatileLazyVal")
    assert(Cli.parse(Seq("--rewrites", "Foobar")).isLeft)
  }

  test("--sourcepath --classpath") {
    assert(Cli.parse(List("--sourcepath", "foo.scala")).isLeft)
    assert(Cli.parse(List("--classpath", "foo")).isLeft)
    // NOTE: This assertion should fail by default, but scalafix-cli % Test
    // depends on testkit, which has scalahost-nsc as a dependency.
    assert(
      Cli
        .parse(List("--sourcepath", "foo.scala", "--classpath", "bar"))
        .isRight)
    // injected by javaOptions in build.sbt
    val path = sys.props("scalafix.scalahost.pluginpath")
    assert(
      Cli
        .parse(
          List(
            "--sourcepath",
            "foo.scala",
            "--classpath",
            "bar",
            "--scalahost-nsc-plugin-path",
            path
          ))
        .isRight)
  }

  test("error returns failure exit code") {
    val file = File.createTempFile("prefix", ".scala")
    FileOps.writeFile(file, "object a { implicit val x = ??? }")
    val code = Cli.runOn(
      ScalafixOptions(rewrites = List(ExplicitImplicit.toString),
                      files = List(file.getAbsolutePath),
                      inPlace = true,
                      common = devNull))
    assert(code == ExitStatus.UnexpectedError)
  }

  test(".sbt files get fixed with sbt dialect") {
    val file = File.createTempFile("prefix", ".sbt")
    FileOps.writeFile(file, "def foo { println(1) }\n")
    val code = Cli.runOn(
      ScalafixOptions(rewrites = List(ProcedureSyntax.toString),
                      files = List(file.getAbsolutePath),
                      inPlace = true,
                      common = devNull))
    assert(code == ExitStatus.Ok)
    assert(FileOps.readFile(file) == "def foo: Unit = { println(1) }\n")
  }
}
