package scalafix.tests.cli

import java.lang.ProcessBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, Path, StandardOpenOption}

import org.scalactic.source.Position
import org.scalatest._
import org.scalatest.FunSuite

import scala.collection.immutable.Seq
import scala.util._
import scalafix.cli
import scalafix.internal.cli.CommonOptions
import scalafix.testkit.DiffAssertions

class CliGitDiff() extends FunSuite with DiffAssertions {
  gitTest("it should handle addition") { (fs, git, cli) =>
    val oldCode = "old.scala"
    val newCode = "new.scala"
    val newCodeAbsPath = fs.absPath(newCode)

    git.init()
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
      s"""|$newCodeAbsPath:3: error: [DisableSyntax.keywords.var] keywords.var is disabled
          |  var newVar = 1
          |  ^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  gitTest("it should handle modification") { (fs, git, cli) =>
    val oldCode = "old.scala"
    val oldCodeAbsPath = fs.absPath(oldCode)

    git.init()
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
    git.add(oldCode)
    git.commit()

    val obtained = runDiff(cli)

    val expected =
      s"""|$oldCodeAbsPath:5: error: [DisableSyntax.keywords.var] keywords.var is disabled
          |  var newVar = 2
          |  ^
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  // gitTest("it should handle rename") { (fs, git, cli)

  // }

  // gitTest("it should handle deletion") { (fs, git, cli)

  // }

  private def runDiff(cli: Cli): String =
    noColor(cli.run("--diff"))

  private def addConf(fs: Fs, git: Git): Unit = {
    val confFile = ".scalafix.conf"
    fs.add(
      confFile,
      """|rules = DisableSyntax
         |DisableSyntax.keywords = [var]""".stripMargin)
    git.add(confFile)
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
    private var revision = 0

    def init(): Unit =
      git("init")

    def add(filename: String): Unit =
      git(s"add $filename")

    def rm(filename: String): Unit =
      git(s"rm $filename")

    def checkout(branch: String): Unit =
      git(s"checkout -b $branch")

    def commit(): Unit = {
      git(s"commit -m 'r$revision'")
      revision += 1
    }

    private def git(argsRaw: String): Unit = {
      val args = argsRaw.split(' ').toList
      import scala.collection.JavaConverters._
      val builder = new ProcessBuilder(("git" :: args).asJava)
      builder.directory(workingDirectory.toFile)
      builder.redirectErrorStream(true)
      val process = builder.start()
      // val input = process.getInputStream()
      // scala.io.Source
      //   .fromInputStream(input)
      //   .getLines
      //   .foreach(line => println(line))
      // input.close()

      val exitValue = process.waitFor()
      val ExitCodeDiff = 1
      val ExitCodeNoDiff = 0

      assert(
        exitValue == ExitCodeDiff ||
          exitValue == ExitCodeNoDiff,
        s"git diff exited with value $exitValue"
      )
    }
  }

  private class Cli(workingDirectory: Path) {
    def run(args: String*): String = {
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
      output
    }
  }
}
