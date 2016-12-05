package scalafix.tests

import scalafix.util.logger

import java.io.File

import ammonite.ops.Path
import ammonite.ops._
import org.scalatest.FunSuite
import org.scalatest.concurrent.TimeLimits
import org.scalatest.time.Minutes
import org.scalatest.time.Span

object ItTest {
  val root: Path = pwd / "target" / "it"
}

object Command {
  val testCompile =
    Command("test:compile", optional = true)
  val scalafixTask =
    Command("test:compile", optional = true)
  def default: Seq[Command] = Seq(
    testCompile,
    scalafixTask,
    testCompile
  )
}
case class Command(cmd: String, optional: Boolean)

case class ItTest(name: String,
                  repo: String,
                  hash: String,
                  cmds: Seq[Command] = Command.default) {
  def workingPath: Path = ItTest.root / name
  def workingDirectory: File = workingPath.toIO
  def parentDir: File = workingPath.toIO.getParentFile
  def pluginPath: Path = workingPath / "project" / "plugins.sbt"
}

// Clones the repo, adds scalafix as a plugin and tests that the
// following commands success:
// 1. test:compile
// 2. test:compile
// 3. test:compile
abstract class IntegrationPropertyTest(t: ItTest, skip: Boolean = false)
    extends FunSuite
    with TimeLimits {

  private val maxTime = Span(20, Minutes) // just in case.

  val hardClean = false
  val comprehensiveTest = false

  // Clones/cleans/checkouts
  def setup(t: ItTest): Unit = {
    t.parentDir.mkdirs()
    if (!t.workingDirectory.exists()) {
      %%("git", "clone", t.repo)(ItTest.root)
    }
    if (hardClean) {
      %%("git", "clean", "-fd")(t.workingPath)
    } else {
      %%("git", "checkout", "--", ".")(t.workingPath)
    }
    %%("git", "reset", "--hard", t.hash)(t.workingPath)
    if (t.pluginPath.toIO.exists()) t.pluginPath.toIO.createNewFile()
    // TODO(olafur) better solution.
    rm(t.workingPath / ".jvmopts") // avoid jvm "Conflicting collector combinations"
    write.append(
      t.pluginPath,
      s"""
         |addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "${scalafix.Versions.nightly}")
         |""".stripMargin
    )
  }

  case class FailOk(cmd: String, msg: String) extends Exception(msg)
  def check(): Unit = {
    def sbt(cmd: String): Unit = {
      val id = s"${t.name}/$cmd"
      logger.info(s"Running $id")
      failAfter(maxTime) {
        %("sbt", "++2.11.8", cmd)(t.workingPath)
      }
      logger.info(s"Completed $id")
    }
    val testFun: () => Any = { () =>
      setup(t)
      try {
        if (comprehensiveTest) sbt("test:compile")
        sbt("scalafix")
        if (comprehensiveTest) sbt("test:compile")
      } catch {
        case FailOk(cmd, msg) =>
          logger.warn(s"Failed to run $cmd, error $msg")
      }
    }

    if (skip) ignore(t.name)(testFun())
    else test(t.name)(testFun())
  }
  check()
}

class Slick
    extends IntegrationPropertyTest(
      ItTest(
        name = "slick",
        repo = "https://github.com/slick/slick.git",
        hash = "bd3c24be419ff2791c123067668c81e7de858915"
      ))

class Scalaz
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalaz",
        repo = "https://github.com/scalaz/scalaz.git",
        hash = "cba156fb2f1f178dbaa32cbca21e95f8199d2f91"
      ))

class Cats
    extends IntegrationPropertyTest(
      ItTest(
        name = "cats",
        repo = "https://github.com/typelevel/cats.git",
        hash = "31080daf3fd8c6ddd80ceee966a8b3eada578198"
      ))

class Monix
    extends IntegrationPropertyTest(
      ItTest(
        name = "monix",
        repo = "https://github.com/monix/monix.git",
        hash = "45c15b5989685668f5ad7ec886af6b74b881a7b4"
      ))

class ScalacheckShapeless
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalacheck-shapeless",
        repo = "https://github.com/alexarchambault/scalacheck-shapeless.git",
        hash = "1027b07ea07fe4ca4b1171d55e995d71201b2e6f"
      ))
