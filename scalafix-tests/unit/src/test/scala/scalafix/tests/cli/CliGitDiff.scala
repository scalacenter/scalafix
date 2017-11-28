package scalafix.tests.cli

import java.lang.ProcessBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, Path}

import org.scalactic.source.Position
import org.scalatest._
import org.scalatest.FunSuite

import scala.collection.immutable.Seq
import scala.util._
import scalafix.cli
import scalafix.internal.cli.CommonOptions

class CliGitDiff() extends FunSuite {
  gitTest("it should handle addition"){(fs, git, cli) =>
    git.init()
    
    git.add("README.md")
    fs.add("a.scala")
    git.add("a.scala")
    git.commit()

    fs.add("b.scala")
    git.commit("some work")

    val obtained = cli.run()

    val expected =
      """|
         |
         |""".stripMargin

    assert(obtained == expected)
  }
  
  def gitTest(name: String)(body: (Fs, Git, Cli) => Unit): Unit = {
    test(name) {
      val fs = new Fs()
      val git = new Git()
      val cli = new Cli(fs.workingDirectory)

      body(fs, git, cli)
    }
  }

  class Fs() {
    val workingDirectory: Path = 
      Files.createTempDirectory(dirName)

    def add(filename: String, content: String): Unit = {

    }
    def append(filename: String, content: String): Unit = {
      
    }

    def rm(filename: String): Unit = {

    }
    def mv(src: String, dst: String): Unit = {

    }
  }

  class Git(){
    private var revision = 0

    def init(): Unit =
      git("init")

    def add(filename: String): Unit =
      git(s"add $filename")

    def rm(filename: String): Unit =
      git(s"rm $filename")

    def commit(): Unit = {
      git(s"commit -m 'r$revision'")
      revision += 1
    }

    private def git(argsRaw: String): Unit = {
      val args = argsRaw.split(' ').toSeq

      val builder = new ProcessBuilder("git", args: _*)
      builder.redirectErrorStream(true)
      val process = builder.start()
      val input = process.getInputStream()
      scala.io.Source.fromInputStream(input).getLines.foreach(line =>
        println(line)
      )
      input.close()

      val exitValue = process.waitFor()
      assert(exitValue == 0, s"git diff exited with value $exitValue")
    }
  }

  class Cli(workingDirectory: Path) {
    def run(): String = {
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      val exit = cli.Cli.runMain(
        Seq("--rules","DisableSyntax", "--diff"),
        CommonOptions(
          workingDirectory = workingDirectory,
          out = ps,
          err = ps
        )
      )
      val output = new String(baos.toByteArray(), StandardCharsets.UTF_8)
      output
    }
  }
}