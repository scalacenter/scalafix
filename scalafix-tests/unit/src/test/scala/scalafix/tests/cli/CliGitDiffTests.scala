package scalafix.tests.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

import org.scalatest._
import org.scalatest.FunSuite

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scalafix.cli
import scalafix.internal.cli.CommonOptions
import scalafix.testkit.DiffAssertions

class CliGitDiffTests() extends FunSuite with DiffAssertions {
  gitTest("addition") { (fs, git, cli) =>
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

    val obtained = runDiff(cli)

    val expected =
      s"""|Running DisableSyntax
          |$newCodeAbsPath:3:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 1
          |  ^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  gitTest("modification") { (fs, git, cli) =>
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

    val obtained = runDiff(cli)

    val expected =
      s"""|Running DisableSyntax
          |$oldCodeAbsPath:7:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 2
          |  ^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  gitTest("rename") { (fs, git, cli) =>
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

    val obtained = runDiff(cli)

    val expected =
      s"""|Running DisableSyntax
          |$newCodeAbsPath:5:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 2
          |  ^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  test("not a git repo") {
    val fs = new Fs()
    addConf(fs)
    val cli = new Cli(fs.workingDirectory)
    val obtained = runDiff(cli)
    val expected =
      s"error: ${fs.workingDirectory} is not a git repository"

    assert(obtained.startsWith(expected))
  }

  gitTest("custom base") { (fs, git, cli) =>
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

    val obtained = runDiff(cli, s"--diff-base=$baseBranch")

    val expected =
      s"""|Running DisableSyntax
          |$newCodeAbsPath:5:3: error: [DisableSyntax.keywords.var] var is disabled
          |  var newVar = 2
          |  ^
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

    val nonExistingHash = "7777777777777777777777777777777777777777"
    val obtained = runDiff(cli, s"--diff-base=$nonExistingHash")
    val expected =
      s"error: '$nonExistingHash' unknown revision or path not in the working tree."
    assert(obtained.startsWith(expected))

    val wrongHashFormat = "777"
    val obtained2 = runDiff(cli, s"--diff-base=$wrongHashFormat")
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

    runDiffOk(cli)
    val obtained = fs.read(code)
    val expected =
      s"""|$foo1
          |
          |$bar2""".stripMargin // only bar is modified
    assertNoDiff(obtained, expected)
  }

  private def runDiff(cli: Cli, args: String*): String =
    runDiff0(cli, false, args.toList)

  private def runDiffOk(cli: Cli, args: String*): String =
    runDiff0(cli, true, args.toList)

  private def runDiff0(cli: Cli, ok: Boolean, args: List[String]): String =
    noColor(cli.run(ok, "--non-interactive" :: "--diff" :: args))

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
    in.replaceAll("\u001B\\[[;\\d]*m", "")

  private def gitTest(name: String)(body: (Fs, Git, Cli) => Unit): Unit = {
    test(name) {
      val fs = new Fs()
      val git = new Git(fs.workingDirectory)
      val cli = new Cli(fs.workingDirectory)

      body(fs, git, cli)
    }
  }

  private class Fs() {
    val workingDirectory: Path =
      Files.createTempDirectory("scalafix")

    workingDirectory.toFile.deleteOnExit()

    def add(filename: String, content: String): Unit =
      write(filename, content, StandardOpenOption.CREATE_NEW)

    def append(filename: String, content: String): Unit =
      write(filename, content, StandardOpenOption.APPEND)

    def replace(filename: String, content: String): Unit = {
      rm(filename)
      add(filename, content)
    }

    def rm(filename: String): Unit =
      Files.delete(path(filename))

    def mv(src: String, dst: String): Unit =
      Files.move(path(src), path(dst))

    def read(src: String): String =
      Files.readAllLines(path(src)).asScala.mkString("\n")

    def absPath(filename: String): String =
      path(filename).toAbsolutePath.toString

    private def write(
        filename: String,
        content: String,
        op: StandardOpenOption): Unit = {
      Files.write(path(filename), content.getBytes, op)
    }

    private def path(filename: String): Path =
      workingDirectory.resolve(filename)
  }

  private class Git(workingDirectory: Path) {
    import org.eclipse.jgit.api.{Git => JGit}

    private val git = JGit.init().setDirectory(workingDirectory.toFile).call()
    private var revision = 0

    def add(filename: String): Unit =
      git.add().addFilepattern(filename).call()

    def rm(filename: String): Unit =
      git.rm().addFilepattern(filename).call()

    def checkout(branch: String): Unit =
      git.checkout().setCreateBranch(true).setName(branch).call()

    def deleteBranch(branch: String): Unit =
      git.branchDelete().setBranchNames(branch).call()

    def commit(): Unit = {
      git.commit().setMessage(s"r$revision").call()
      revision += 1
    }
  }

  private class Cli(workingDirectory: Path) {
    def run(ok: Boolean, args: List[String]): String = {
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      val exit = cli.Cli.runMain(
        args.to[Seq],
        CommonOptions(
          workingDirectory = workingDirectory.toAbsolutePath.toString,
          out = ps,
          err = ps
        )
      )
      val output = new String(baos.toByteArray(), StandardCharsets.UTF_8)
      assert(
        exit.isOk == ok,
        s"Expected OK exit status, obtained $exit. Details $output"
      )
      output
    }
  }
}
