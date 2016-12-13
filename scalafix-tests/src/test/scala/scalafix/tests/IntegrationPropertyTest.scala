package scalafix.tests

import scala.util.matching.Regex
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
  val RepoName: Regex = ".*/([^/].*).git".r
}
case class Command(cmd: String, optional: Boolean = false)

case class ItTest(name: String,
                  repo: String,
                  hash: String,
                  cmds: Seq[Command] = Command.default) {
  def repoName: String = repo match {
    case Command.RepoName(x) => x
    case _ =>
      throw new IllegalArgumentException(
        s"Unable to parse repo name from repo: $repo")
  }
  def workingPath: Path = ItTest.root / repoName
  def parentDir: File = workingPath.toIO.getParentFile
}

// Clones the repo, adds scalafix as a plugin and tests that the
// following commands success:
// 1. test:compile
// 2. test:compile
// 3. test:compile
abstract class IntegrationPropertyTest(t: ItTest, skip: Boolean = false)
    extends FunSuite
    with TimeLimits {

  private val isCi = sys.props.contains("CI")
  private val maxTime = Span(20, Minutes) // just in case.

  val hardClean = false
  val comprehensiveTest = false

  // Clones/cleans/checkouts
  def setup(t: ItTest): Unit = {
    t.parentDir.mkdirs()
    if (!t.workingPath.toIO.exists()) {
      %%("git", "clone", t.repo)(ItTest.root)
    }
    if (hardClean) {
      %%("git", "clean", "-fd")(t.workingPath)
    } else {
      %%("git", "checkout", "--", ".")(t.workingPath)
    }
    %%("git", "reset", "--hard", t.hash)(t.workingPath)
    // TODO(olafur) better solution.
    rm(t.workingPath / ".jvmopts") // avoid jvm "Conflicting collector combinations"
    write.over(
      t.workingPath / "project" / "build.properties",
      "sbt.version=0.13.13\n"
    )
    write.append(
      t.workingPath / "project" / "plugins.sbt",
      s"""
         |addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "${scalafix.Versions.version}")
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
        t.cmds.map(_.cmd).foreach(sbt)
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
      ),
      skip = true
    )

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
      )
    )

class ScalaJs
    extends IntegrationPropertyTest(
      ItTest(
        name = "Scala.js",
        repo = "https://github.com/scala-js/scala-js.git",
        hash = "8917b5a9bd8fb2175a112fc15c761050eeb4099f",
        cmds = Seq(
          Command("set scalafixEnabled in Global := true"),
          Command("compiler/test:compile"),
          Command("examples/test:compile")
        )
      ),
      skip = true // GenJsCode is hard: import renames + dependent types
    )

class ScalacheckShapeless
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalacheck-shapeless",
        repo = "https://github.com/alexarchambault/scalacheck-shapeless.git",
        hash = "bb25ecee23c42148f66d9b27920a89ba5cc189d2"
      ),
      skip = true // coursier can't resolve locally published snapshot on ci, sbt.ivy.home is not read.
    )
