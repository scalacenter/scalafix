package scalafix.tests.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.scalatest._
import org.scalatest.FunSuite
import scalafix.cli.ExitStatus
import scalafix.testkit.DiffAssertions
import scalafix.internal.tests.utils.{Fs, Git}
import scalafix.internal.tests.utils.SkipWindows

class CliGitDiffSuite extends FunSuite with DiffAssertions {
  gitTest("addition", SkipWindows) { (fs, git, cli) =>
    val oldCode = "old.scala"
    val newCode = "new.scala"
    val newCodeAbsPath = fs.absPath(newCode)

    fs.add(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |}""".stripMargin)
    git.add(oldCode)
    addConf(fs, git)
    git.commit()

    git.checkout("pr-1")

    fs.add(
      newCode,
      """|object NewCode {
         |  // New code, no vars
         |  var newVar = 1
         |}""".stripMargin)
    git.add(newCode)
    git.commit()

    val obtained = runDiff(cli, ExitStatus.LinterError)

    val expected =
      s"""|$newCodeAbsPath:3:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 1
          |  ^^^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  gitTest("modification", SkipWindows) { (fs, git, cli) =>
    val oldCode = "old.scala"
    val oldCodeAbsPath = fs.absPath(oldCode)

    fs.add(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |}""".stripMargin)
    git.add(oldCode)
    addConf(fs, git)
    git.commit()

    git.checkout("pr-1")
    fs.replace(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |}
         |object NewCode {
         |  // It's not ok to add new vars
         |  var newVar = 2
         |}""".stripMargin
    )
    git.add(oldCode)
    git.commit()

    val obtained = runDiff(cli, ExitStatus.LinterError)

    val expected =
      s"""|$oldCodeAbsPath:7:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 2
          |  ^^^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  gitTest("rename", SkipWindows) { (fs, git, cli) =>
    val oldCode = "old.scala"
    val newCode = "new.scala"
    val newCodeAbsPath = fs.absPath(newCode)

    fs.add(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |}""".stripMargin)
    git.add(oldCode)
    addConf(fs, git)
    git.commit()

    git.checkout("pr-1")
    fs.replace(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |  // It's not ok to add new vars
         |  var newVar = 2
         |}""".stripMargin
    )
    fs.mv(oldCode, newCode)
    git.add(oldCode)
    git.add(newCode)
    git.commit()

    val obtained = runDiff(cli, ExitStatus.LinterError)

    val expected =
      s"""|$newCodeAbsPath:5:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 2
          |  ^^^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  test("not a git repo") {
    val fs = new Fs()
    addConf(fs)
    val cli = new Cli(fs.workingDirectory)
    val obtained = runDiff(cli, ExitStatus.CommandLineError)
    val expected = s"error: ${fs.workingDirectory} is not a git repository"

    assert(obtained.startsWith(expected))
  }

  gitTest("custom base", SkipWindows) { (fs, git, cli) =>
    val oldCode = "old.scala"
    val newCode = "new.scala"
    val newCodeAbsPath = fs.absPath(newCode)

    fs.add(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |}""".stripMargin)
    git.add(oldCode)
    addConf(fs, git)
    git.commit()

    val baseBranch = "2.10.X"

    git.checkout(baseBranch)
    git.deleteBranch("master")

    git.checkout("pr-1")
    fs.replace(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |  // It's not ok to add new vars
         |  var newVar = 2
         |}""".stripMargin
    )
    fs.mv(oldCode, newCode)
    git.add(oldCode)
    git.add(newCode)
    git.commit()

    val obtained =
      runDiff(cli, ExitStatus.LinterError, "--diff-base", baseBranch)

    val expected =
      s"""|$newCodeAbsPath:5:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 2
          |  ^^^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  gitTest("#483 unkown git hash") { (fs, git, cli) =>
    val oldCode = "old.scala"

    fs.add(
      oldCode,
      """|object OldCode {
         |  // This is old code, where var's blossom
         |  var oldVar = 1
         |}""".stripMargin)
    git.add(oldCode)
    addConf(fs, git)
    git.commit()

    val nonExistingHash = "a777777777777777777777777777777777777777"
    val obtained =
      runDiff(cli, ExitStatus.CommandLineError, "--diff-base", nonExistingHash)
    val expected =
      s"error: '$nonExistingHash' unknown revision or path not in the working tree."
    assert(obtained.startsWith(expected))

    val wrongHashFormat = "a777"
    val obtained2 =
      runDiff(cli, ExitStatus.CommandLineError, "--diff-base", wrongHashFormat)
    val expected2 =
      s"error: '$wrongHashFormat' unknown revision or path not in the working tree."
    assert(obtained2.startsWith(expected2))
  }

  gitTest("works on Patch") { (fs, git, cli) =>
    val code = "code.scala"
    val foo1 =
      """|object A {
         |  def foo() {}
         |}
         |""".stripMargin

    val bar1 =
      """|object B {
         |  def bar() {}
         |}
         |""".stripMargin

    val bar2 =
      """|object B {
         |  def bar(): Unit = {}
         |}
         |""".stripMargin

    fs.add(code, foo1)
    git.add(code)
    fs.add(confFile, "rules = ProcedureSyntax")
    git.add(confFile)
    git.commit()

    git.checkout("pr-1")
    fs.replace(
      code,
      s"""|$foo1
          |
          |$bar1""".stripMargin
    )
    git.add(code)
    git.commit()

    runDiff(cli, ExitStatus.Ok)
    val obtained = fs.read(code)
    val expected =
      s"""|$foo1
          |
          |$bar2""".stripMargin // only bar is modified
    assertNoDiff(obtained, expected)
  }

  private def runDiff(cli: Cli, expected: ExitStatus, args: String*): String =
    noColor(
      cli.run(
        expected,
        "--diff" +:
          args.toArray
      )
    )

  private val confFile = ".scalafix.conf"
  private def addConf(fs: Fs, git: Git): Unit = {
    addConf(fs)
    git.add(confFile)
  }

  private def addConf(fs: Fs): Unit = {
    fs.add(
      confFile,
      """|rules = DisableSyntax
         |DisableSyntax.keywords = [var]""".stripMargin)
  }

  private def noColor(in: String): String =
    fansi.Str(in).plainText

  private def gitTest(name: String, testTags: Tag*)(
      body: (Fs, Git, Cli) => Unit): Unit = {
    test(name, testTags: _*) {
      val fs = new Fs()
      val git = new Git(fs.workingDirectory)
      val cli = new Cli(fs.workingDirectory)
      body(fs, git, cli)
    }
  }

  private class Cli(workingDirectory: Path) {
    def run(expected: ExitStatus, args: Array[String]): String = {
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      val exit = scalafix.v1.Main.run(
        args,
        workingDirectory,
        ps
      )
      val output = new String(baos.toByteArray, StandardCharsets.UTF_8)
      assert(exit == expected, output)
      output
    }
  }
}
