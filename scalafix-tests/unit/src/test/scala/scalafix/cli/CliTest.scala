package scalafix.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.collection.immutable.Seq
import scalafix.cli.CliCommand.PrintAndExit
import scalafix.cli.CliCommand.RunScalafix
import scalafix.internal.rewrite.ProcedureSyntax

class CliTest extends BaseCliTest {

  test("parse") {
    val RunScalafix(runner) = parse(
      Seq(
        "--verbose",
        "--config-str",
        "fatalWarnings=true",
        "--single-thread",
        "-r",
        ProcedureSyntax.toString,
        "--files",
        "build.sbt",
        "project/Mima.scala",
        "--stdout",
        "project/Dependencies.scala"
      ))
    val obtained = runner.cli
    assert(!runner.writeMode.isWriteFile)
    assert(runner.config.fatalWarnings)
    assert(obtained.verbose)
    assert(obtained.singleThread)
    assert(
      obtained.files == List(
        "build.sbt",
        "project/Mima.scala",
        "project/Dependencies.scala"
      )
    )
  }

  check(
    name = "fix file",
    originalLayout = s"""/hello.scala
                        |$original
                        |""".stripMargin,
    args = Seq("-r", ProcedureSyntax.toString, "hello.scala"),
    expectedLayout = s"""/hello.scala
                        |$expected
                        |""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "fix directory",
    originalLayout = s"""|/dir/a.scala
                         |$original
                         |/dir/b.scala
                         |$original""".stripMargin,
    args = Seq(
      "-r",
      ProcedureSyntax.toString,
      "dir"
    ),
    expectedLayout = s"""|/dir/a.scala
                         |$expected
                         |/dir/b.scala
                         |$expected""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "file not found",
    originalLayout = s"/foobar.scala\n",
    args = Seq("unknown-file.scala"),
    expectedLayout = "/foobar.scala",
    expectedExit = ExitStatus.InvalidCommandLineOption
  )

  check(
    name = "empty rewrite",
    originalLayout = s"/foobar.scala\n",
    args = Seq("foobar.scala"),
    expectedLayout = "/foobar.scala",
    expectedExit = ExitStatus.InvalidCommandLineOption
  )

  check(
    name = "--test error",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Seq("--test", "-r", ProcedureSyntax.toString, "foobar.scala"),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.TestFailed
  )

  check(
    name = "--test OK",
    originalLayout = s"""/foobar.scala
                        |$expected""".stripMargin,
    args = Seq("--test", "-r", ProcedureSyntax.toString, "foobar.scala"),
    expectedLayout = s"""/foobar.scala
                        |$expected""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "linter error",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Seq(
      "-r",
      "scala:scalafix.cli.TestRewrites.LintError",
      "foobar.scala"
    ),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.LinterError
  )

  check(
    name = "linter warning promoted to error",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Seq(
      "--config-str",
      "lint.error=LintWarning.warning",
      "-r",
      "scala:scalafix.cli.TestRewrites.LintWarning",
      "foobar.scala"
    ),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.LinterError
  )

  check(
    name = "--exclude is respected",
    originalLayout = s"""|/ignoreme.scala
                         |$original
                         |/fixme.scala
                         |$original""".stripMargin,
    args = Seq(
      "--exclude",
      "ignoreme.scala",
      "-r",
      ProcedureSyntax.toString,
      "ignoreme.scala",
      "fixme.scala"
    ),
    expectedLayout = s"""|/fixme.scala
                         |$expected
                         |/ignoreme.scala
                         |$original""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "--stdout does not write to file",
    originalLayout = s"""|/a.scala
                         |$original
                         |""".stripMargin,
    args = Seq(
      "--stdout",
      "-r",
      ProcedureSyntax.toString,
      "a.scala"
    ),
    expectedLayout = s"""|/a.scala
                         |$original""".stripMargin,
    expectedExit = ExitStatus.Ok,
    output => assertNoDiff(output, expected)
  )

  test("--rewrites") {
    val RunScalafix(runner) =
      parse(Seq("--rewrites", "DottyVolatileLazyVal"))
    assert(runner.rewrite.name == "DottyVolatileLazyVal")
    assert(parse(Seq("--rewrites", "Foobar")).isError)
  }

  check(
    name = "parse errors return exit code",
    originalLayout = s"""|/a.scala
                         |objec bar
                         |""".stripMargin,
    args = Seq(
      "-r",
      ProcedureSyntax.toString,
      "a.scala"
    ),
    expectedLayout = s"""|/a.scala
                         |objec bar""".stripMargin,
    expectedExit = ExitStatus.ParseError
  )

  check(
    name = "fix sbt files",
    originalLayout = s"""|/a.sbt
                         |def foo { println(1) }
                         |lazy val bar = project
                         |""".stripMargin,
    args = Seq(
      "-r",
      ProcedureSyntax.toString,
      "a.sbt"
    ),
    expectedLayout = s"""|/a.sbt
                         |def foo: Unit = { println(1) }
                         |lazy val bar = project
                         |""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "deprecated name emits warning",
    originalLayout = s"""|/a.scala
                         |object a {
                         |  @volatile lazy val x = 2
                         |}
                         |""".stripMargin,
    args = Seq(
      "-r",
      "VolatileLazyVal",
      "a.scala"
    ),
    expectedLayout = s"""|/a.scala
                         |object a {
                         |  @volatile lazy val x = 2
                         |}
                         |""".stripMargin,
    expectedExit = ExitStatus.Ok,
    output => assert(output.contains("Use DottyVolatileLazyVal instead"))
  )

  check(
    name = "no files to fix is error",
    originalLayout = s"""|/dir/a.java
                         |package a;
                         |class A
                         |""".stripMargin,
    args = Seq(
      "-r",
      ProcedureSyntax.toString,
      "dir"
    ),
    expectedLayout = s"""|/dir/a.java
                         |package a;
                         |class A
                         |""".stripMargin,
    expectedExit = ExitStatus.InvalidCommandLineOption, { output =>
      assert(output.contains("No files to fix!") && output.contains("dir"))
    }
  )

  test("--zsh") {
    val obtained = parse(Seq("--zsh"))
    assert(obtained.isOk)
    assert(obtained.isInstanceOf[PrintAndExit])
  }

  test("--bash") {
    val obtained = parse(Seq("--bash"))
    assert(obtained.isOk)
    assert(obtained.isInstanceOf[PrintAndExit])
  }
}
