package scalafix.tests.cli

import scalafix.cli._
import scalafix.internal.rule._

class CliSyntacticSuite extends BaseCliSuite {

  check(
    name = "--help",
    originalLayout = "",
    args = Array("--help"),
    expectedLayout = "",
    expectedExit = ExitStatus.Ok,
    outputAssert = { out =>
      assert(out.startsWith("Scalafix"))
      assert(out.contains("--rules"))
    }
  )

  check(
    name = "--zsh",
    originalLayout = "",
    args = Array("--zsh"),
    expectedLayout = "",
    expectedExit = ExitStatus.Ok,
    outputAssert = { out =>
      assert(out.contains("local -a scalafix_opts"))
    }
  )

  check(
    name = "--bash",
    originalLayout = "",
    args = Array("--bash"),
    expectedLayout = "",
    expectedExit = ExitStatus.Ok,
    outputAssert = { out =>
      assert(out.contains("complete -F _scalafix scalafix"))
    }
  )

  check(
    name = "fix file",
    originalLayout = s"""/hello.scala
                        |$original
                        |""".stripMargin,
    args = Array("-r", ProcedureSyntax.toString, "hello.scala"),
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
    args = Array(
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
    args = Array("unknown-file.scala"),
    expectedLayout = "/foobar.scala",
    expectedExit = ExitStatus.CommandLineError
  )

  check(
    name = "empty rule",
    originalLayout = s"/foobar.scala\n",
    args = Array("foobar.scala"),
    expectedLayout = "/foobar.scala",
    expectedExit = ExitStatus.CommandLineError
  )

  check(
    name = "TestError",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Array("--test", "-r", ProcedureSyntax.toString, "foobar.scala"),
    expectedLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    expectedExit = ExitStatus.TestError,
    outputAssert = { out =>
      assert(
        out.endsWith(
          """|<expected fix>
             |@@ -1,4 +1,4 @@
             | object Main {
             |-  def foo() {
             |+  def foo(): Unit = {
             |   }
             | }
             |""".stripMargin
        ))
    }
  )

  check(
    name = "--test OK",
    originalLayout = s"""/foobar.scala
                        |$expected""".stripMargin,
    args = Array("--test", "-r", ProcedureSyntax.toString, "foobar.scala"),
    expectedLayout = s"""/foobar.scala
                        |$expected""".stripMargin,
    expectedExit = ExitStatus.Ok
  )

  check(
    name = "linter error",
    originalLayout = s"""/foobar.scala
                        |$original""".stripMargin,
    args = Array(
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
    args = Array(
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
    args = Array(
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
    args = Array(
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
    name = "ParseError",
    originalLayout = s"""|/a.scala
                         |objec bar
                         |""".stripMargin,
    args = Array(
      "-r",
      ProcedureSyntax.toString,
      "a.scala"
    ),
    expectedLayout = s"""|/a.scala
                         |objec bar""".stripMargin,
    expectedExit = ExitStatus.ParseError,
    outputAssert = { out =>
      assert(
        out.endsWith(
          """|a.scala:1:1: error: expected class or object definition
             |objec bar
             |^
             |""".stripMargin
        )
      )
    }
  )

  check(
    name = "fix sbt files",
    originalLayout = s"""|/a.sbt
                         |def foo { println(1) }
                         |lazy val bar = project
                         |""".stripMargin,
    args = Array(
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
    args = Array(
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
    args = Array(
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
    args = Array(
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
    args = Array(
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
