package scalafix.cli

import scala.collection.immutable.Seq
import scalafix.cli.CliCommand.PrintAndExit
import scalafix.cli.CliCommand.RunScalafix
import scalafix.internal.rule.ProcedureSyntax

class CliArgsTest extends BaseCliTest {
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

  test("--rules") {
    val RunScalafix(runner) =
      parse(Seq("--rules", "DottyVolatileLazyVal"))
    assert(runner.rule.name.value == "DottyVolatileLazyVal")
    assert(parse(Seq("--rules", "Foobar")).isError)
  }

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
}
