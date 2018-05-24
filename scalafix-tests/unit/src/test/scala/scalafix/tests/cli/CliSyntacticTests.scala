package scalafix.tests.cli

import scala.collection.immutable.Seq
import scalafix.cli._
import scalafix.internal.rule._

class CliSyntacticTests extends BaseCliTest {

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
    name = "empty rule",
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
      "scala:scalafix.tests.cli.LintError",
      "foobar.scala"
    ),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.LinterError,
    outputAssert = { out =>
      assert(out.contains("Error!"))
    }
  )

  check(
    name = "linter warning promoted to error",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Seq(
      "--settings.lint.error",
      "LintWarning.warning",
      "-r",
      "scala:scalafix.tests.cli.LintWarning",
      "foobar.scala"
    ),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.LinterError,
    outputAssert = { out =>
      assert(out.contains("foobar.scala:1:1"))
    }
  )

  check(
    name = "--exclude is respected",
    originalLayout = s"""|/ignoreme.scala
                         |$original
                         |/fixme.scala
                         |$original""".stripMargin,
    args = Seq(
      "--exclude",
      "**ignoreme.scala",
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
    expectedExit = ExitStatus.NoFilesError, { output =>
      assert(output.contains("No files to fix"))
    }
  )

  check(
    name = "--out-from --out-to change output path",
    originalLayout = """
                       |/src/shared/a.scala
                       |object a { def foo { println(1) } }
                       |""".stripMargin,
    args = List(
      "-r",
      ProcedureSyntax.toString,
      "--out-from",
      "shared",
      "--out-to",
      "fixed",
      "src"
    ),
    expectedLayout = """
                       |/src/fixed/a.scala
                       |object a { def foo: Unit = { println(1) } }
                       |
                       |/src/shared/a.scala
                       |object a { def foo { println(1) } }
                       |""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "--format sbt",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Seq(
      "--format",
      "sbt",
      "-r",
      "scala:scalafix.tests.cli.LintError",
      "foobar.scala"
    ),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.LinterError,
    outputAssert = { out =>
      assert(out.contains("[error] "))
    }
  )
}
